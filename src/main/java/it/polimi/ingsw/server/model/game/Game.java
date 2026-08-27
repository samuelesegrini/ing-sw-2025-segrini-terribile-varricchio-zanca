package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.model.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.building.BuildingTimer;
import it.polimi.ingsw.server.model.building.ComponentPool;
import it.polimi.ingsw.server.model.building.ShipBuilder;
import it.polimi.ingsw.server.model.building.StartSpacePolicy;
import it.polimi.ingsw.server.model.building.StartSpaces;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.goods.GoodsBank;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.projection.Projections;

import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.SkippedTurn;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.server.persistence.GameSnapshot;
import it.polimi.ingsw.common.protocol.PreparationCommand;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * One game of Galaxy Trucker, from the first tile drawn to the last credit counted.
 *
 * <p>The aggregate. It owns the ships, the pool everybody draws from, the hourglass, the deck
 * and the route, and it is the only thing that knows all of them at once. Everything above it
 * — the controller, the transports, the interfaces — deals with commands and events and never
 * touches a {@code Ship}.
 *
 * <p><b>It decides nothing about when things may happen.</b> That belongs to the current
 * {@link Phase}, which is what stops this class from growing the {@code if} chains that a
 * project like this usually accumulates. A command for the wrong phase is refused here,
 * before any phase sees it, simply because no phase claims it.
 *
 * <p><b>Nothing here is thread-safe, on purpose.</b> Commands are applied one at a time on one
 * thread, which the controller arranges (architecture § 4). That is what lets the whole model
 * beneath this be written without a single lock, and it is why simultaneous building actions
 * resolve by arrival order rather than by whoever won a race.
 */
public final class Game {

    /**
     * How long a game with one player left waits for somebody to come back.
     *
     * <p>A number with a reason rather than a magic one: long enough that a dropped connection,
     * a reconnecting client and a person walking back to their desk all fit inside it, and short
     * enough that the last player standing is not held there all afternoon.
     */
    public static final Duration DEFAULT_SOLO_TIMEOUT = Duration.ofMinutes(2);

    /** A stop on skipping, so that a phase which somehow keeps asking cannot spin for ever. */
    private static final int MOST_SKIPS_PER_TICK = 64;

    private final String id;
    private final LevelSpec level;
    private final List<Seat> seats;
    private final Map<PlayerColor, Ship> ships;
    private final Map<PlayerColor, ShipBuilder> builders;
    private final Map<PlayerColor, List<AdventureCardIdentity>> peeked = new EnumMap<>(PlayerColor.class);
    private final Set<PlayerColor> away = EnumSet.noneOf(PlayerColor.class);
    private final GameData data;
    private final RandomGenerator random;
    private final ComponentPool pool;
    private final BuildingTimer timer;
    private final AdventureDeck deck;
    private final StartSpaces starts;

    private final InstantSource clock;
    private final Duration soloTimeout;

    private Phase phase;
    private Flight flight;
    private List<ScoreSheet> scores;

    /** When the table last emptied to one player, or {@code null} while a game is playable. */
    private Instant aloneSince;

    /**
     * Everything this game has accepted, oldest first.
     *
     * <p>A game is a deterministic function of its level, its seats, its shuffle and this list:
     * one thread applies one command at a time and the catalogue ships with the build. So this
     * is what a snapshot keeps, and replaying it is what brings a game back.
     */
    private final List<GameSnapshot.Recorded> history = new java.util.ArrayList<>();

    /** Off while a game is being replayed, so recovery does not re-record its own history. */
    private boolean recording = true;

    private Game(String id, LevelSpec level, List<Seat> seats, Map<PlayerColor, Ship> ships,
                 Map<PlayerColor, ShipBuilder> builders, GameData data, RandomGenerator random,
                 ComponentPool pool, BuildingTimer timer, AdventureDeck deck, StartSpaces starts,
                 InstantSource clock, Duration soloTimeout) {
        this.clock = clock;
        this.soloTimeout = soloTimeout;
        this.id = id;
        this.level = level;
        this.seats = List.copyOf(seats);
        this.ships = ships;
        this.builders = builders;
        this.data = data;
        this.random = random;
        this.pool = pool;
        this.timer = timer;
        this.deck = deck;
        this.starts = starts;
    }

    /**
     * Sets up a game and opens the shipyard.
     *
     * @param id     what to call this game
     * @param level  which flight is being played
     * @param seats  who is playing, in seating order
     * @param data   the tiles, cards and boards the game is made of
     * @param random where the shuffling comes from
     * @param clock  where the hourglass reads the time
     * @return a game in its building phase
     * @throws IllegalArgumentException if the table is the wrong size for the level
     */
    public static Game create(String id, GameLevel level, List<Seat> seats, GameData data,
                              RandomGenerator random, InstantSource clock) {
        return create(id, level, seats, data, random, clock, DEFAULT_SOLO_TIMEOUT);
    }

    /**
     * Opens a game, saying how long it will wait for a second player before ending itself.
     *
     * @param id          what to call it
     * @param level       which rules
     * @param seats       who is playing
     * @param data        the catalogue
     * @param random      the shuffle
     * @param clock       what time it is
     * @param soloTimeout how long a game with one player left waits before awarding them the win
     * @return the game, in its building phase
     * @throws IllegalArgumentException if the table does not seat two to four players
     */
    public static Game create(String id, GameLevel level, List<Seat> seats, GameData data,
                              RandomGenerator random, InstantSource clock, Duration soloTimeout) {
        if (seats.size() < 2 || seats.size() > 4) {
            throw new IllegalArgumentException("a game seats two to four players, not " + seats.size());
        }
        LevelSpec spec = data.level(level);
        GoodsBank bank = new GoodsBank(data.bankStock());
        ComponentPool pool = new ComponentPool(data.tiles(), random);
        AdventureDeck deck = AdventureDeck.deal(spec.flightBoard().deck(), cardPools(data, spec), random);

        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        Map<PlayerColor, ShipBuilder> builders = new LinkedHashMap<>();
        for (Seat seat : seats) {
            Ship ship = new Ship(spec.shipBoard(), data.startingCabins().get(seat.colour()), bank);
            ships.put(seat.colour(), ship);
            builders.put(seat.colour(), new ShipBuilder(ship, pool, deck));
        }

        BuildingTimer timer = new BuildingTimer(spec.flightBoard().hourglassSpaces(), clock);
        StartSpaces starts = new StartSpaces(spec.flightBoard(), seats.size(),
                spec.rules().hourglass() ? StartSpacePolicy.CHOSEN_BY_PLAYER
                        : StartSpacePolicy.IN_FINISHING_ORDER);

        Game game = new Game(id, spec, seats, ships, builders, data, random, pool, timer, deck,
                starts, clock, soloTimeout);
        game.phase = new BuildingPhase(game);
        if (timer.isInPlay()) {
            timer.start();
        }
        return game;
    }

    private static Map<CardLevel, List<AdventureCardIdentity>> cardPools(GameData data, LevelSpec spec) {
        if (spec.flightBoard().deck().testFlightCardsOnly()) {
            return Map.of(CardLevel.LEVEL_I, data.testFlightCards());
        }
        return Map.of(CardLevel.LEVEL_I, data.cardsOfLevel(CardLevel.LEVEL_I),
                CardLevel.LEVEL_II, data.cardsOfLevel(CardLevel.LEVEL_II));
    }

    // ------------------------------------------------------------------ driving it

    /**
     * Applies one command.
     *
     * <p>The player has already been established from the connection the command arrived on.
     * Nothing here takes a client's word for who it is.
     *
     * @param player  who sent it
     * @param command what they want
     * @return what happened, or why nothing did
     */
    public Reaction apply(PlayerColor player, Command command) {
        if (!ships.containsKey(player)) {
            return new Reaction.Refused("there is no " + player + " player in this game");
        }
        Reaction reaction = phase.apply(player, command);
        if (!(reaction instanceof Reaction.Accepted accepted)) {
            return reaction;
        }
        // Only what was accepted. A refused command changed nothing, so replaying it would
        // change nothing either — and a history that included refusals would invite the reader
        // to think otherwise.
        accepted(player, command);
        List<Event> narration = new java.util.ArrayList<>(accepted.narration());
        narration.addAll(advance());
        return new Reaction.Accepted(narration);
    }

    /**
     * Lets time pass.
     *
     * <p>The hourglass is the one thing in this game that happens without anybody doing
     * anything, so something has to ask. The controller calls this on a timer; every other
     * transition is driven by a command.
     *
     * @return {@code true} if the phase changed
     */
    public List<Event> tick() {
        List<Event> told = new java.util.ArrayList<>();
        told.addAll(answerForAnybodyAway());
        told.addAll(mindTheLastPlayer());
        told.addAll(advance());
        return List.copyOf(told);
    }

    /**
     * Answers on behalf of whoever is not there.
     *
     * <p>A dropped connection must not stop three other people playing. What the answer is comes
     * from {@link SkippedTurn}, which is a rule and not a convenience: always the passive one,
     * always the same, and never something anybody could call unfair on the absent player's
     * behalf.
     *
     * <p>Building and crewing are skipped by taking the player to have finished where they
     * stand — there is nothing else "waiting for them" could mean once they are gone, and a
     * shipyard that never closes is a game that never starts.
     *
     * @return what to tell everybody
     */
    private List<Event> answerForAnybodyAway() {
        if (away.isEmpty() || phase.name() == GamePhase.FINISHED) {
            return List.of();
        }
        List<Event> told = new java.util.ArrayList<>();
        told.addAll(finishForAnybodyAway());

        // A loop, because one skipped answer usually leads to the next question — a volley is
        // four shots. Bounded, so that a phase which somehow keeps asking cannot spin for ever.
        for (int asked = 0; asked < MOST_SKIPS_PER_TICK; asked++) {
            Optional<PlayerPrompt> outstanding = phase.pending();
            if (outstanding.isEmpty() || !away.contains(outstanding.get().player())) {
                break;
            }
            PlayerPrompt prompt = outstanding.orElseThrow();
            Reaction answered = phase.apply(prompt.player(),
                    new FlightCommand.Answer(SkippedTurn.answerFor(prompt)));
            if (!(answered instanceof Reaction.Accepted accepted)) {
                // The passive answer was refused, which means the rules changed under us. Better
                // to stop and let somebody look than to hammer a question that will not take.
                break;
            }
            told.add(new GameEvent.TurnSkipped(prompt.player(), describe(prompt)));
            told.addAll(accepted.narration());
        }
        return List.copyOf(told);
    }

    /**
     * Closes the shipyard, or the crew hatch, on behalf of anybody who is away.
     */
    private List<Event> finishForAnybodyAway() {
        List<Event> told = new java.util.ArrayList<>();
        for (PlayerColor player : List.copyOf(away)) {
            Command onTheirBehalf = switch (phase.name()) {
                case BUILDING -> new BuildingCommand.FinishBuilding(null);
                case CREW_PLACEMENT -> new PreparationCommand.FinishPreparation();
                default -> null;
            };
            if (onTheirBehalf == null) {
                continue;
            }
            // Refused means they had already finished, which is exactly what we wanted anyway.
            if (phase.apply(player, onTheirBehalf) instanceof Reaction.Accepted accepted) {
                told.add(new GameEvent.TurnSkipped(player, phase.name() == GamePhase.BUILDING
                        ? "stopped building where they were"
                        : "flew with the crew they had"));
                told.addAll(accepted.narration());
            }
        }
        return List.copyOf(told);
    }

    /**
     * Suspends a game that has run out of players, and eventually ends it.
     *
     * <p>One player alone cannot finish a flight: the cards ask questions of an order of
     * players, and an order of one is not a game. So it waits — and says how long it will wait,
     * rather than either carrying on absurdly or ending the moment somebody's train goes into a
     * tunnel. If nobody comes back in time, the last player standing is the winner: everybody
     * else gives up, and the flight is scored as it stands.
     *
     * @return what to tell whoever is left
     */
    private List<Event> mindTheLastPlayer() {
        if (phase.name() == GamePhase.FINISHED || phase.name() == GamePhase.SCORING) {
            return List.of();
        }
        boolean alone = seats.size() - away.size() <= 1;
        if (!alone) {
            if (aloneSince == null) {
                return List.of();
            }
            aloneSince = null;
            return List.of(new GameEvent.GameResumed());
        }
        if (aloneSince == null) {
            aloneSince = clock.instant();
            return List.of(new GameEvent.GameSuspended(soloTimeout.toSeconds()));
        }
        Duration waited = Duration.between(aloneSince, clock.instant());
        if (waited.compareTo(soloTimeout) < 0) {
            return List.of();
        }
        return awardTheWinToWhoeverIsLeft();
    }

    /**
     * Ends a game nobody came back to.
     *
     * <p>Everybody away gives up, which leaves whoever is still here as the only ship still
     * flying — so the ordinary scoring makes them the winner, and there is no separate notion
     * of "winning by default" to keep in step with the real one.
     */
    private List<Event> awardTheWinToWhoeverIsLeft() {
        aloneSince = null;
        if (flight == null) {
            // Nobody has launched. There is nothing to score, so the game simply stops.
            phase = new ScoringPhase(this);
            return List.of(new GameEvent.GameEnded());
        }
        List<Event> told = new java.util.ArrayList<>();
        for (Seat seat : seats) {
            if (away.contains(seat.colour()) && !flight.retired().contains(seat.colour())) {
                flight.giveUp(seat.colour());
                told.add(new FlightEvent.ShipRetired(seat.colour(),
                        "away when the game ran out of players"));
            }
        }
        phase = new ScoringPhase(this);
        told.addAll(phase.onEntry());
        return List.copyOf(told);
    }

    /**
     * Returns everything this game has accepted, oldest first.
     *
     * @return the history, for keeping
     */
    public List<GameSnapshot.Recorded> history() {
        return List.copyOf(history);
    }

    private void accepted(PlayerColor player, Command command) {
        if (recording) {
            history.add(new GameSnapshot.Recorded(player, command));
        }
    }

    /**
     * Puts a game back by doing again everything it did.
     *
     * <p>Replay rather than reconstruction: the same seed deals the same cards, the same
     * commands in the same order reach the same state, and every one of them goes through the
     * ordinary {@code apply}. There is no second code path that could disagree with the first.
     *
     * <p>Its own history is not re-recorded as it goes — it is set afterwards to the list that
     * was replayed, so a recovered game keeps exactly the story it was given.
     *
     * @param snapshot what was kept
     * @param data     the catalogue, which ships with the build and so is the same as before
     * @param clock    what time it is now, which is not what time it was
     * @param soloTimeout how long this game waits for its last player
     * @return the game, as it was
     * @throws IllegalStateException if a command the game once accepted is now refused, which
     *                               means the rules have changed under the snapshot
     */
    public static Game restore(GameSnapshot snapshot, GameData data, InstantSource clock,
                               Duration soloTimeout) {
        List<Seat> seats = snapshot.seats().stream()
                .map(seated -> new Seat(seated.nickname(), seated.colour()))
                .toList();
        Game game = create(snapshot.gameId(), snapshot.level(), seats, data,
                new java.util.Random(snapshot.seed()), clock, soloTimeout);
        game.recording = false;
        try {
            for (GameSnapshot.Recorded done : snapshot.accepted()) {
                Reaction again = game.apply(done.player(), done.command());
                if (again instanceof Reaction.Refused refused) {
                    throw new IllegalStateException("replaying " + snapshot.gameId()
                            + " went wrong: a command it once accepted is now refused — "
                            + refused.reason());
                }
                game.tick();
            }
        } finally {
            game.recording = true;
        }
        game.history.addAll(snapshot.accepted());
        return game;
    }

    /** Says in a few words what was decided for somebody who was not there. */
    private static String describe(PlayerPrompt prompt) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave ignored -> "left an offer where it was";
            case PlayerPrompt.DeclarePower ignored -> "declared with no batteries spent";
            case PlayerPrompt.ArrangeCargo ignored -> "took none of the goods";
            case PlayerPrompt.GiveUpCrew crew -> "gave up " + crew.count() + " crew";
            case PlayerPrompt.ChooseDefence ignored -> "took the hit";
            case PlayerPrompt.ChooseFragment ignored -> "kept the largest piece";
            case PlayerPrompt.ChoosePlanet ignored -> "flew past the planets";
        };
    }

    /**
     * Moves through as many phases as are already finished, collecting what each says on the
     * way in.
     *
     * <p>A loop rather than one step, because a phase can be over the moment it starts: two
     * ships with nothing wrong with them pass through validation without anybody doing
     * anything, and a flight whose last card asks nobody anything runs to the end of the deck.
     */
    private List<Event> advance() {
        List<Event> entering = new java.util.ArrayList<>();
        Optional<Phase> next = phase.next();
        while (next.isPresent()) {
            phase = next.get();
            entering.addAll(phase.onEntry());
            next = phase.next();
        }
        return entering;
    }

    // ------------------------------------------------------------------ what it looks like

    /**
     * Builds the whole picture, as one player is allowed to see it.
     *
     * <p>Per recipient, because the tile in a player's hand and the pile they peeked at are
     * theirs alone. Everything else in here is public.
     *
     * @param recipient who is being told
     * @return everything they may know
     */
    public GameView viewFor(PlayerColor recipient) {
        List<PlayerView> players = seats.stream()
                .map(seat -> new PlayerView(seat.nickname(), seat.colour(),
                        !away.contains(seat.colour()),
                        flight != null && flight.hasGivenUp(seat.colour()),
                        flight == null ? 0 : flight.creditsEarned(seat.colour()),
                        Projections.of(ships.get(seat.colour()),
                                builders.get(seat.colour()).reserved())))
                .toList();

        return new GameView(id, level.level(), phase.name(), recipient, players,
                phase.name() == GamePhase.BUILDING ? buildingView(recipient) : null,
                flight == null ? null : Projections.of(flight,
                        phase.cardOnTheTable().orElse(null), phase.cardsLeft()),
                phase.pending().orElse(null),
                scores);
    }

    private it.polimi.ingsw.common.protocol.view.BuildingView buildingView(PlayerColor recipient) {
        Set<PlayerColor> finished = builders.entrySet().stream()
                .filter(entry -> entry.getValue().hasFinished())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(PlayerColor.class)));
        return Projections.of(builders.get(recipient), pool, timer, starts, finished,
                peeked.getOrDefault(recipient, List.of()));
    }

    // ------------------------------------------------------------------ what the phases need

    /**
     * Returns what this game is called.
     *
     * @return the game's identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns where the game has got to.
     *
     * @return the current phase
     */
    public GamePhase phase() {
        return phase.name();
    }

    /**
     * Returns who is playing, in seating order.
     *
     * @return the seats
     */
    public List<Seat> seats() {
        return seats;
    }

    /**
     * Records that a player's client has gone away, or come back.
     *
     * <p>The game carries on either way. This only changes what the others are shown, so that
     * a view can say why nobody is taking somebody's turn rather than appearing to hang.
     *
     * @param player    whose connection changed
     * @param connected whether they are now attached
     */
    public void connectionChanged(PlayerColor player, boolean connected) {
        if (connected) {
            away.remove(player);
        } else {
            away.add(player);
        }
    }

    /**
     * Tells whether a player's client is attached.
     *
     * @param player who
     * @return {@code true} while they are connected
     */
    public boolean isConnected(PlayerColor player) {
        return !away.contains(player);
    }

    LevelSpec level() {
        return level;
    }

    /**
     * Returns where this game's randomness comes from.
     *
     * <p>The same source that shuffled the tiles and dealt the cards also throws the dice, so
     * a game is reproducible from its seed. That is what makes a flight testable, and it is
     * the difference between a bug report that can be replayed and one that cannot.
     *
     * @return the generator
     */
    RandomGenerator random() {
        return random;
    }

    /**
     * Returns the rules behind a card.
     *
     * @param cardId which card
     * @return what it does
     * @throws IllegalStateException if the data has a card with no rules, which the coverage
     *                               test exists to make impossible
     */
    it.polimi.ingsw.server.model.adventure.AdventureCard rulesFor(String cardId) {
        return data.playableCard(cardId).orElseThrow(() ->
                new IllegalStateException(cardId + " has no rules behind it"));
    }

    Map<PlayerColor, Ship> ships() {
        return ships;
    }

    Map<PlayerColor, ShipBuilder> builders() {
        return builders;
    }

    Map<PlayerColor, List<AdventureCardIdentity>> peeked() {
        return peeked;
    }

    BuildingTimer timer() {
        return timer;
    }

    StartSpaces starts() {
        return starts;
    }

    AdventureDeck deck() {
        return deck;
    }

    List<PlayerColor> colours() {
        return seats.stream().map(Seat::colour).toList();
    }

    Flight flight() {
        return flight;
    }

    void launch(Flight launched) {
        this.flight = launched;
    }

    void settle(List<ScoreSheet> sheets) {
        this.scores = List.copyOf(sheets);
    }
}

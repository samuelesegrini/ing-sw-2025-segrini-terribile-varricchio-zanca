package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The flight: cards turned over one at a time, and the route flown.
 *
 * <p>Thin, and meant to stay thin. Every card is a sequence of questions and the answers to
 * them, and the framework that knows which question comes next was written and tested in M3.
 * This phase turns a card over, asks who is being asked, passes answers through, and clears up
 * afterwards. <b>It never learns which card it is holding</b> — if it ever needs to, a card has
 * been written wrong.
 *
 * <p>The clearing up is the part with rules in it. After every card the route is checked for
 * players the rules push out: a lap behind the leader, or nobody left alive aboard (manual
 * p.14). Those are not the same as giving up, and from outside all three look identical, which
 * is why the event says which it was.
 */
final class FlightPhase implements Phase {

    private final Game game;
    private final Deque<AdventureCardIdentity> deck;
    private final List<Event> narration = new ArrayList<>();

    private AdventureCardIdentity card;
    private AdventureResolution resolution;

    FlightPhase(Game game) {
        this.game = game;
        this.deck = new ArrayDeque<>(game.deck().cards());
    }

    @Override
    public GamePhase name() {
        return GamePhase.FLIGHT;
    }

    @Override
    public Optional<AdventureCardIdentity> cardOnTheTable() {
        return Optional.ofNullable(card);
    }

    @Override
    public int cardsLeft() {
        return deck.size();
    }

    @Override
    public Optional<PlayerPrompt> pending() {
        return resolution == null ? Optional.empty() : resolution.pending();
    }

    // ------------------------------------------------------------------ commands

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        if (game.flight().hasGivenUp(player)) {
            return new Reaction.Refused("this ship has already left the route");
        }
        return switch (command) {
            case FlightCommand.GiveUp ignored -> giveUp(player);
            case FlightCommand.Answer answer -> answer(player, answer.choice());
            default -> new Reaction.Refused("the ships are flying");
        };
    }

    private Reaction giveUp(PlayerColor player) {
        narration.clear();
        game.flight().giveUp(player);
        narration.add(new FlightEvent.ShipRetired(player, "gave up the flight"));
        if (waitingOn(player)) {
            // The card was in the middle of asking them something. Abandoning the card is the
            // honest thing to do: a resolution has no notion of a player disappearing halfway
            // through, and inventing an answer on their behalf would apply effects to a ship
            // that is no longer in the game.
            finishCard();
        }
        carryOn();
        return new Reaction.Accepted(List.copyOf(narration));
    }

    private Reaction answer(PlayerColor player, PlayerChoice choice) {
        Optional<PlayerPrompt> outstanding = pending();
        if (outstanding.isEmpty()) {
            return new Reaction.Refused("nothing is waiting to be answered");
        }
        if (outstanding.get().player() != player) {
            return new Reaction.Refused("the game is waiting for the "
                    + outstanding.get().player() + " player, not you");
        }
        if (choice.player() != player) {
            // The payload names a player because the model needed that before there was a
            // network. Over a socket it is a claim, and this is where the claim is checked.
            return new Reaction.Refused("that answer is signed by the " + choice.player()
                    + " player");
        }
        narration.clear();
        try {
            resolution.submit(choice);
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
        carryOn();
        return new Reaction.Accepted(List.copyOf(narration));
    }

    private boolean waitingOn(PlayerColor player) {
        return pending().map(prompt -> prompt.player() == player).orElse(false);
    }

    // ------------------------------------------------------------------ turning cards over

    /**
     * Draws cards and clears them up until the flight is waiting for somebody, or is over.
     *
     * <p>A loop rather than one step, because a card can resolve without asking anybody
     * anything — Stardust and Epidemic never do — and a flight that stopped after each of
     * those would need something to poke it.
     */
    private void carryOn() {
        while (true) {
            if (resolution != null && resolution.pending().isPresent()) {
                narration.add(new FlightEvent.Awaiting(resolution.pending().orElseThrow()));
                return;
            }
            if (resolution != null) {
                finishCard();
            }
            if (game.flight().isDeserted() || deck.isEmpty()) {
                return;
            }
            turnOverTheNextCard();
        }
    }

    private void turnOverTheNextCard() {
        card = deck.poll();
        narration.add(new FlightEvent.CardRevealed(card));
        AdventureCard rules = game.rulesFor(card.id());
        resolution = rules.resolve(game.flight());
    }

    private void finishCard() {
        resolution = null;
        card = null;
        narration.add(new FlightEvent.CardResolved());
        pushOutWhoeverCannotCarryOn();
    }

    /**
     * Takes off the route anybody the rules will not let stay.
     *
     * <p>A lap behind the leader, or a ship with nobody alive aboard (p.14). Reported
     * separately from giving up because from outside the two look the same and mean very
     * different things.
     */
    private void pushOutWhoeverCannotCarryOn() {
        Set<PlayerColor> forced = game.flight().playersForcedOut();
        game.flight().enforceGiveUpRules();
        forced.forEach(player -> narration.add(new FlightEvent.ShipRetired(player,
                game.flight().route().isFlying(player)
                        ? "no crew left alive aboard"
                        : "a whole lap behind the leader")));
    }

    /**
     * Turns over the first card.
     *
     * <p>Here rather than in the constructor so that a half-built phase never emits events,
     * and so that what happens on the way in reaches the players who were watching the fleet
     * launch rather than being discovered in the next state.
     */
    @Override
    public List<Event> onEntry() {
        narration.clear();
        carryOn();
        return List.copyOf(narration);
    }

    @Override
    public Optional<Phase> next() {
        if (resolution != null || (!deck.isEmpty() && !game.flight().isDeserted())) {
            return Optional.empty();
        }
        return Optional.of(new ScoringPhase(game));
    }
}

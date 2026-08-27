package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.server.model.flight.Dice;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Filling the cabins, and then launching.
 *
 * <p>Two humans to a cabin, or one alien where a life support module of its colour is
 * <em>welded to</em> it — welded, not merely next to it (manual p.9). That distinction runs
 * through the whole model and is the one most often got wrong, which is why the ship answers
 * the question rather than this phase counting neighbours.
 *
 * <p>Declaring ready fills every cabin still empty with people, because that is the manual's
 * default and it saves a player four identical commands.
 */
final class CrewPhase implements Phase {

    private final Game game;
    private final Set<PlayerColor> ready = EnumSet.noneOf(PlayerColor.class);

    CrewPhase(Game game) {
        this.game = game;
    }

    @Override
    public GamePhase name() {
        return GamePhase.CREW_PLACEMENT;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        if (ready.contains(player)) {
            return new Reaction.Refused("this ship is crewed and waiting to launch");
        }
        Ship ship = game.ships().get(player);
        try {
            return switch (command) {
                case PreparationCommand.BoardCrew board -> {
                    board.alienIfAny().ifPresentOrElse(
                            alien -> ship.boardAlienIn(board.cabin(), alien),
                            () -> ship.boardHumansIn(board.cabin()));
                    yield Reaction.Accepted.quietly();
                }
                case PreparationCommand.FinishPreparation ignored -> {
                    readyUp(player, ship);
                    yield Reaction.Accepted.quietly();
                }
                default -> new Reaction.Refused("the ships are being crewed");
            };
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
    }

    /**
     * Launches an absent player with whatever crew their cabins already hold.
     *
     * <p>The same thing declaring ready does, which is the point: an absent player is not
     * given a worse crew than a present one who said nothing, and the fleet is not held on
     * the starting line by somebody who is not coming back.
     *
     * @param player who is away
     * @return the ship crewed and ready, or a refusal if it already was
     */
    @Override
    public Reaction finishFor(PlayerColor player) {
        if (ready.contains(player)) {
            return new Reaction.Refused("this ship is crewed and waiting to launch");
        }
        try {
            readyUp(player, game.ships().get(player));
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
        return new Reaction.Accepted(List.of(
                new GameEvent.TurnSkipped(player, "flew with the crew they had")));
    }

    /**
     * Fills the cabins nobody has filled and marks the ship ready.
     *
     * <p>Shared by declaring ready and by being launched while away, so the two cannot drift
     * into meaning different things.
     */
    private void readyUp(PlayerColor player, Ship ship) {
        ship.fillRemainingCabinsWithHumans();
        ready.add(player);
    }

    @Override
    public Optional<Phase> next() {
        if (ready.size() < game.colours().size()) {
            return Optional.empty();
        }
        game.launch(new Flight(game.level(), game.ships(), startingPositions(),
                Dice.fair(game.random())));
        return Optional.of(new FlightPhase(game));
    }

    private Map<PlayerColor, Integer> startingPositions() {
        Map<PlayerColor, Integer> positions = new LinkedHashMap<>();
        game.colours().forEach(player -> positions.put(player,
                game.starts().routePositionOf(player).orElseThrow(() ->
                        new IllegalStateException(player + " never took a place on the starting line"))));
        return positions;
    }
}

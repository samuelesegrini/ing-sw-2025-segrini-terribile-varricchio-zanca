package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.server.model.flight.Dice;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumSet;
import java.util.LinkedHashMap;
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
                    ship.fillRemainingCabinsWithHumans();
                    ready.add(player);
                    yield Reaction.Accepted.quietly();
                }
                default -> new Reaction.Refused("the ships are being crewed");
            };
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
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

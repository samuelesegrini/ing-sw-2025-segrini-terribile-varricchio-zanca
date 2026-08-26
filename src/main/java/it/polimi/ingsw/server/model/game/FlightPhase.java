package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.FlightEvent;

import java.util.List;
import java.util.Optional;

/**
 * The flight: cards turned over, and the route flown.
 *
 * <p>At this point the phase does what a flight does before its first card is drawn, which is
 * accept a player giving up and refuse an answer to a question nobody has asked. Turning cards
 * over and driving their resolutions is the next piece of work; the resolution framework it
 * will use is already written and already tested, so this phase is meant to stay thin. If it
 * ever starts knowing which card it is holding, something has gone wrong.
 */
final class FlightPhase implements Phase {

    private final Game game;

    FlightPhase(Game game) {
        this.game = game;
    }

    @Override
    public GamePhase name() {
        return GamePhase.FLIGHT;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        if (game.flight().hasGivenUp(player)) {
            return new Reaction.Refused("this ship has already left the route");
        }
        try {
            return switch (command) {
                case FlightCommand.GiveUp ignored -> {
                    game.flight().giveUp(player);
                    yield new Reaction.Accepted(List.of(
                            new FlightEvent.ShipRetired(player, "gave up the flight")));
                }
                case FlightCommand.Answer ignored ->
                        new Reaction.Refused("nothing is waiting to be answered");
                default -> new Reaction.Refused("the ships are flying");
            };
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
    }

    @Override
    public Optional<Phase> next() {
        return Optional.empty();
    }
}

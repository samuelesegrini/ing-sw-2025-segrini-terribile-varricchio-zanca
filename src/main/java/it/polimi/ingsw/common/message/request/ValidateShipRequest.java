package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.ShipValidationEvent;
import it.polimi.ingsw.common.message.response.ValidateShipResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**

 Request to validate the ship construction.
 */
public class ValidateShipRequest extends AbstractRequest {
    @Override
    public Response execute(RequestContext context) {
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }
        PlayerId playerId = context.getPlayerId();
        Player player = session.getPlayer(playerId);
        Ship ship = player.getShip();

        // Validate ship
        List<String> errors = new ArrayList<>();

        // Check connection errors
        List<Component> badConnections = ship.checkConnectingErrors();
        if (!badConnections.isEmpty()) {
            errors.add("Found " + badConnections.size() + " connection errors");
        }

        // Check if ship is connected
        List<Ship> splitShips = ship.splitBoard(ship.getBoard()[2][3]); // Check from starting position
        if (splitShips.size() > 1) {
            errors.add("Ship is not fully connected");
        }

        // Check minimum requirements
        ship.updateStats();
        if (ship.getEngines() < 1) {
            errors.add("Ship must have at least one engine");
        }

        if (ship.getCrew() < 2) {
            errors.add("Ship must have at least 2 crew members");
        }

        // Publish validation event
        ShipValidationEvent event = new ShipValidationEvent(
                session.getGameId(),
                playerId.toString(),
                context.getPlayerRegistry().getPlayerNickname(playerId),
                errors.isEmpty(),
                errors
        );
        context.publishEvent(event);

        if (errors.isEmpty()) {
            // Mark player as ready
            session.setPlayerReady(playerId, true);
            // Return a proper, concrete response object
            return new ValidateShipResponse(getCorrelationId(), true, Collections.emptyList());
        } else {
            // Return a proper, concrete response object with the list of errors
            return new ValidateShipResponse(getCorrelationId(), false, errors);
        }
    }
}
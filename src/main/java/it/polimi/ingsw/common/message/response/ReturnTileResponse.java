package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.UUID;

/**
 * Response to a reserve tile request. // TODO
 */
public class ReturnTileResponse extends AbstractResponse {
    // TODO


    /**
     *
     * @param correlationId
     */
    // Legacy constructor for backward compatibility
    public ReturnTileResponse(UUID correlationId) {
        super(correlationId);
        // TODO
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        context.getController().getUI().onReturnTileResponse(this);
    }
}
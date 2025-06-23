package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response to tile placement request.
 */
public class PlaceTileResponse extends AbstractResponse {

    public PlaceTileResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Confirmation already handled by event
    }
}
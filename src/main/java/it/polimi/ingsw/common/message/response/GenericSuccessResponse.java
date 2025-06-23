package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Generic success response for simple acknowledgments.
 */
public class GenericSuccessResponse extends AbstractResponse {
    public GenericSuccessResponse(UUID correlationId) {
        super(correlationId);
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // Default: no specific client action needed
    }
}
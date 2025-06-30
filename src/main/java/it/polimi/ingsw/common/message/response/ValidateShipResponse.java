package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a ValidateShipRequest.
 * Acknowledges that the request was processed successfully.
 * The ShipValidationEvent is the single source of truth for state updates.
 */
public class ValidateShipResponse extends AbstractResponse {

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     */

    public ValidateShipResponse(UUID correlationId) {
        super(correlationId);
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ValidateShipResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 VALIDATE SHIP RESPONSE - Received confirmation that the validate ship request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Validate ship request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the ship validation state here.
            // The ShipValidationEvent handler is responsible for all state updates.
        } else {
            // Show error notification for failed ship validation
            context.showNotification(
                    "Validate Ship Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to validate ship",
                    NotificationType.ERROR
            );
        }

        context.getController().getUI().onValidateShipResponse(this);
    }
}
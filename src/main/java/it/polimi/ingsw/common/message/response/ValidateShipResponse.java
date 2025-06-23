package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**

 Response to ship validation request.
 Contains the result of the validation and any errors found.
 */
public class ValidateShipResponse extends AbstractResponse {
    private final boolean isValid;
    private final List<String> errors;
    /**
     Constructs a new ValidateShipResponse.
     @param correlationId The ID of the original request.
     @param isValid Whether the ship is valid.
     @param errors A list of validation errors, or an empty list if valid.
     */
    public ValidateShipResponse(UUID correlationId, boolean isValid, List<String> errors) {
        // The 'success' flag of the response maps directly to 'isValid'.
        // The error message is a concatenation of the validation errors.
        super(correlationId, isValid,
                isValid ? "Ship is valid" : "Ship validation failed",
                isValid ? null : "SHIP_VALIDATION_ERROR");
        this.isValid = isValid;
        this.errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
    }
    public boolean isValid() {
        return isValid;
    }
    public List<String> getErrors() {
        return errors;
    }
    @Override
    public void handleOnClient(ClientContext context) {
        // The ShipValidationEvent will handle most of the UI updates.
        // This response can provide a direct notification to the player who made the request.
        if (isValid) {
            context.showNotification(
                    "Ship Validated",
                    "Your ship is ready for flight!",
                    NotificationType.SUCCESS
            );
        } else {
            String errorDetails = String.join("\n- ", errors);
            context.showNotification(
                    "Ship Invalid",
                    "Your ship has the following problems:\n- " + errorDetails,
                    NotificationType.ERROR
            );
        }
    }
}
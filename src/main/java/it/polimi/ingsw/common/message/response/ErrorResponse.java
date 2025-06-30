package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Generic error response that can be used for any failed request.
 */
public class ErrorResponse extends AbstractResponse {
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
    public static final String AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param errorMessage The error message
     * @param errorCode The error code
     */

    public ErrorResponse(UUID correlationId, String errorMessage, String errorCode) {
        super(correlationId, false, errorMessage, errorCode);
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        // Show error to user
        context.showError("Request Failed", getErrorMessage());

        context.getController().getUI().onErrorResponse(this);
    }
}
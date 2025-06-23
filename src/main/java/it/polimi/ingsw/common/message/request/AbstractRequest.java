package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.AbstractMessage;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import java.util.UUID;

/**
 * Abstract base class for request messages.
 */
public abstract class AbstractRequest extends AbstractMessage implements Request {
    private final UUID correlationId;
    
    protected AbstractRequest() {
        super();
        this.correlationId = UUID.randomUUID();
    }
    
    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public ValidationResult validate() {
        // Default implementation - no validation errors
        return ValidationResult.success();
    }
    
    /**
     * Helper method to create an error response.
     * @param errorMessage The error message
     * @param errorCode The error code
     * @return An error response
     */
    protected Response createErrorResponse(String errorMessage, String errorCode) {
        return new ErrorResponse(correlationId, errorMessage, errorCode);
    }
    
    /**
     * Helper method to create a success response.
     * @return A success response
     */
    protected Response createSuccessResponse() {
        return new GenericSuccessResponse(correlationId);
    }
}
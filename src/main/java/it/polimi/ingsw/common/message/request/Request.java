package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import java.util.UUID;

/**
 * Interface for request messages that can be executed on the server.
 * Implements the Command pattern for self-handling requests.
 */
public interface Request extends Message {
    /**
     * Gets the correlation ID for matching responses to requests.
     * @return The correlation ID
     */
    UUID getCorrelationId();

    /**
     * Validates this request before execution.
     * @return ValidationResult containing any validation errors
     */
    ValidationResult validate();

    /**
     * Executes this request on the server side.
     * @param context The execution context providing access to server resources
     * @return A response message
     */
    Response execute(RequestContext context);
}
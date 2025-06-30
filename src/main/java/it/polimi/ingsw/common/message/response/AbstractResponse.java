package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.message.AbstractMessage;

import java.util.UUID;

/**
 * Abstract base class for response messages.
 */
public abstract class AbstractResponse extends AbstractMessage implements Response {
    private final UUID correlationId;
    private final boolean success;
    private final String errorMessage;
    private final String errorCode;

    /**
     * constructor
     * @param correlationId
     * @param success
     * @param errorMessage The error message
     * @param errorCode The error code
     */

    protected AbstractResponse(UUID correlationId, boolean success, 
                              String errorMessage, String errorCode) {
        super();
        this.correlationId = correlationId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    /**
     * constructor
     * @param correlationId The correlation ID
     *
     */

    protected AbstractResponse(UUID correlationId) {
        this(correlationId, true, null, null);
    }

    /**
     * constructor
     * @param correlationId The correlation ID
     * @param success whether it was successful or not
     * @param errorMessage The error message
     */

    protected AbstractResponse(UUID correlationId, boolean success, String errorMessage) {
        this(correlationId, success, errorMessage, null);
    }

    /**
     *
     * @return correlationId
     */

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    /**
     *
     * @return success
     */

    @Override
    public boolean isSuccess() {
        return success;
    }

    /**
     *
     * @return The errorMessage
     */

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     *
     * @return The errorCode
     */

    @Override
    public String getErrorCode() {
        return errorCode;
    }
}
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
    
    protected AbstractResponse(UUID correlationId, boolean success, 
                              String errorMessage, String errorCode) {
        super();
        this.correlationId = correlationId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }
    
    protected AbstractResponse(UUID correlationId) {
        this(correlationId, true, null, null);
    }
    
    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public boolean isSuccess() {
        return success;
    }
    
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String getErrorCode() {
        return errorCode;
    }
}
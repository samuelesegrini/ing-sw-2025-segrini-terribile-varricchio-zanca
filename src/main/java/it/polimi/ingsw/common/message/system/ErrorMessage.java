package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;

public class ErrorMessage extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String reason;
    private final ErrorType errorType; // Optional: to categorize errors

    public enum ErrorType {
        GENERAL,
        VALIDATION,
        ILLEGAL_ACTION,
        SERVER_INTERNAL,
        NETWORK_ISSUE,
        AUTHENTICATION_FAILURE // Could be used by ServerLoginResponse internally too
        // Add more as needed
    }

    public ErrorMessage(String reason) {
        this(reason, ErrorType.GENERAL);
    }

    public ErrorMessage(String reason, ErrorType errorType) {
        super();
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Error reason cannot be null or empty.");
        }
        this.reason = reason;
        this.errorType = errorType != null ? errorType : ErrorType.GENERAL;
    }

    public String getReason() {
        return reason;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "ErrorMessage{" +
                "reason='" + reason + '\'' +
                ", errorType=" + errorType +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
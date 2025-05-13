package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data Transfer Object for a ship building validation error.
 */
public record ValidationErrorDTO(
        String errorCode,    // e.g., "INVALID_CONNECTION", "OCCUPIED_SQUARE"
        String message,      // Human-readable description
        PositionDTO errorPosition, // Optional: where the error occurred
        String componentIdInError // Optional: which component instance caused it
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ValidationErrorDTO {
        Objects.requireNonNull(errorCode, "errorCode cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
    }

    // Constructor without optional fields
    public ValidationErrorDTO(String errorCode, String message) {
        this(errorCode, message, null, null);
    }

    @Override
    public String toString() {
        return "ValidationErrorDTO{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                (errorPosition != null ? ", errorPosition=" + errorPosition : "") +
                (componentIdInError != null ? ", componentIdInError='" + componentIdInError + '\'' : "") +
                '}';
    }
}
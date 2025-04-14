package it.polimi.ingsw.network.dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing the result of a player action.
 * Used to confirm success/failure and provide details.
 */
public class ActionResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message; // Optional message (e.g., reason for failure)
    // Add specific result data if needed, e.g.:
    // private ComponentDTO acquiredComponent;
    // private ShipDTO updatedShip;

    public ActionResultDTO() {
        // Default constructor
    }

    public ActionResultDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods for convenience
    public static ActionResultDTO success() {
        return new ActionResultDTO(true, "Action successful.");
    }

    public static ActionResultDTO failure(String reason) {
        return new ActionResultDTO(false, reason);
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
package it.polimi.ingsw.network.dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing the current phase of the game.
 */
public class GamePhaseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String phaseName; // e.g., "SETUP", "BUILDING", "FLIGHT", "SCORING"
    // Add other relevant phase details if needed, e.g., current turn player ID
    // private String currentPlayerId;

    public GamePhaseDTO() {
        // Default constructor
    }

    public GamePhaseDTO(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    // Add getters/setters for other fields
}
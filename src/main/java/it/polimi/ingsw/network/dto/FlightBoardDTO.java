package it.polimi.ingsw.network.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing the state of the flight board.
 */
public class FlightBoardDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Example fields - adjust based on actual FlightBoard model structure
    private int totalLength;
    private Map<String, Integer> playerPositions; // Map<PlayerIdString, PositionIndex>
    private List<String> revealedAdventureCardTypes; // Or full AdventureCardDTOs

    public FlightBoardDTO() {
        // Default constructor
    }

    // --- Getters and Setters ---

    public int getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(int totalLength) {
        this.totalLength = totalLength;
    }

    public Map<String, Integer> getPlayerPositions() {
        return playerPositions;
    }

    public void setPlayerPositions(Map<String, Integer> playerPositions) {
        this.playerPositions = playerPositions;
    }

    public List<String> getRevealedAdventureCardTypes() {
        return revealedAdventureCardTypes;
    }

    public void setRevealedAdventureCardTypes(List<String> revealedAdventureCardTypes) {
        this.revealedAdventureCardTypes = revealedAdventureCardTypes;
    }
}
package it.polimi.ingsw.network.dto;

import java.io.Serializable;

import it.polimi.ingsw.model.enums.flight.FlightStatus;

/**
 * Data Transfer Object representing a player's state.
 */
public class PlayerDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String playerId; // String representation (e.g., PlayerId.toString())
    private String playerName; // Nickname
    private String playerColor; // String representation of PlayerColor enum
    private FlightStatus status;
    private int credits;
    private int rank; // Or score if applicable
    private ShipDTO ship; // The player's ship state
    // Add other relevant player state: ready status, components in hand (as DTOs), etc.
    // private boolean ready;

    public PlayerDTO() {
        // Default constructor
    }

    // Constructor with fields might be useful for conversion
    public PlayerDTO(String playerId, String playerName, String playerColor, FlightStatus status, int credits, int rank, ShipDTO ship) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerColor = playerColor;
        this.status = status;
        this.credits = credits;
        this.rank = rank;
        this.ship = ship;
    }

    // --- Getters and Setters ---

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(String playerColor) {
        this.playerColor = playerColor;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public ShipDTO getShip() {
        return ship;
    }

    public void setShip(ShipDTO ship) {
        this.ship = ship;
    }
}
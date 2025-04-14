package it.polimi.ingsw.network.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object representing the complete state of a game.
 * Used to send the full game picture to clients, especially on join/reconnect.
 */
public class GameStateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private GamePhaseDTO gamePhase;
    private List<PlayerDTO> players;
    private ComponentDeckDTO componentPool;
    private FlightBoardDTO flightBoard;
    private String currentTurnPlayerId; // String representation of PlayerId

    public GameStateDTO() {
        // Default constructor
    }

    // --- Getters and Setters ---

    public GamePhaseDTO getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhaseDTO gamePhase) {
        this.gamePhase = gamePhase;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public ComponentDeckDTO getComponentPool() {
        return componentPool;
    }

    public void setComponentPool(ComponentDeckDTO componentPool) {
        this.componentPool = componentPool;
    }

    public FlightBoardDTO getFlightBoard() {
        return flightBoard;
    }

    public void setFlightBoard(FlightBoardDTO flightBoard) {
        this.flightBoard = flightBoard;
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }

    public void setCurrentTurnPlayerId(String currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
    }
}
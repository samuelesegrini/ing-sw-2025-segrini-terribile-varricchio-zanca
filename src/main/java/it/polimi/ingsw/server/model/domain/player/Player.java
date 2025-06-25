package it.polimi.ingsw.server.model.domain.player;

import it.polimi.ingsw.server.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.crew.CrewType;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;

import java.io.Serializable;
import java.util.*;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PlayerId playerId;
    private PlayerFlightData playerFlightData;
    private PlayerColor color;
    private Map<CrewType, Integer> crew;
    private int credits;
    private int crewMembers;
    private Ship ship;
    private int finalScore;
    private Component heldComponent;
    private boolean ready;

    /**
     * Creates a new player with the specified ID
     * 
     * @param playerId The unique identifier for the player
     * @throws IllegalArgumentException if playerId is null
     */
    public Player(PlayerId playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        this.playerId = playerId;
        this.playerFlightData = new PlayerFlightData(0);
        this.crew = new HashMap<>();
        this.credits = 0;
        this.crewMembers = 0;
        this.heldComponent = null;
        this.ready = false;
    }

    /**
     * Creates a new player with the specified ID and color
     * 
     * @param playerId The unique identifier for the player
     * @param color The player's color
     * @throws IllegalArgumentException if playerId or color is null
     */
    public Player(PlayerId playerId, PlayerColor color) {
        this(playerId);
        if (color == null) {
            throw new IllegalArgumentException("Player color cannot be null");
        }
        this.color = color;
    }

    public PlayerId getId() {
        return this.playerId;
    }
    
    /**
     * Gets the player ID (alias for getId)
     * @return The player ID
     */
    public PlayerId getPlayerId() {
        return this.playerId;
    }
    
    /**
     * Gets the player's nickname from the PlayerId
     * @return The player's nickname
     */
    public String getNickname() {
        return this.playerId.getNickname();
    }

    public PlayerFlightData getFlightData() {
        return this.playerFlightData;
    }

    public void setFlightData(PlayerFlightData flightData) {
        if (flightData == null) {
            throw new IllegalArgumentException("Flight data cannot be null");
        }
        this.playerFlightData = flightData;
    }

    public PlayerColor getColor() {
        return this.color;
    }

    public void setColor(PlayerColor color) {
        if (color == null) {
            throw new IllegalArgumentException("Player color cannot be null");
        }
        this.color = color;
    }

    public Ship getShip() {
        return this.ship;
    }

    public void setShip(Ship ship) {
        if (ship == null) {
            throw new IllegalArgumentException("Ship cannot be null");
        }
        this.ship = ship;
    }

    public int getCredits() {
        return this.credits;
    }
    /**
     * Adds the specified credits to the player's total.
     *
     * @param credits number of credits to add to the player
     * @throws IllegalArgumentException if credits is negative
     */
    public void addCredits(int credits) {
        if (credits < 0) {
            throw new IllegalArgumentException("Credits to add cannot be negative");
        }
        this.credits += credits;
    }

    /**
     * Subtracts the specified credits from the player's total.
     *
     * @param credits number of credits to subtract from the player
     * @throws IllegalArgumentException if credits is negative or greater than current credits
     */
    public void subtractCredits(int credits) {
        if (credits < 0) {
            throw new IllegalArgumentException("Credits to subtract cannot be negative");
        }
        if (credits > this.credits) {
            throw new IllegalArgumentException("Cannot subtract more credits than available");
        }
        this.credits -= credits;
    }
    
    /**
     * Gets the player's final score
     * @return The final score
     */
    public int getFinalScore() {
        return finalScore;
    }
    
    /**
     * Sets the player's final score
     * @param finalScore The final score to set
     */
    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }
    
    /**
     * Gets the component currently held by the player
     * @return List containing the held component (for compatibility), or empty list if none
     */
    public List<Component> getHeldComponents() {
        return heldComponent != null ? List.of(heldComponent) : Collections.emptyList();
    }
    
    /**
     * Gets the single held component
     * @return The held component, or null if none
     */
    public Component getHeldComponent() {
        return heldComponent;
    }
    
    /**
     * Sets the component held by the player
     * @param component The component to hold (replaces any existing one)
     */
    public void setHeldComponent(Component component) {
        this.heldComponent = component;
    }
    
    /**
     * Adds a component to the player's hand (replaces any existing one)
     * @param component The component to add
     */
    public void addComponent(Component component) {
        this.heldComponent = component;
    }
    
    /**
     * Removes the held component from the player's hand
     * @param component The component to remove
     * @return true if the component was removed
     */
    public boolean removeComponent(Component component) {
        if (this.heldComponent == component) {
            this.heldComponent = null;
            return true;
        }
        return false;
    }
    
    /**
     * Clears the held component
     */
    public void clearHeldComponent() {
        this.heldComponent = null;
    }
    
    /**
     * Gets the ready status of the player
     * @return true if the player is ready
     */
    public boolean isReady() {
        return ready;
    }
    
    /**
     * Sets the ready status of the player
     * @param ready true if the player is ready
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(playerId, player.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + playerId +
                ", color=" + color +
                ", credits=" + credits +
                ", crewMembers=" + crewMembers +
                '}';
    }
}


package it.polimi.ingsw.model.domain.player;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.player.PlayerColor;
import it.polimi.ingsw.model.enums.ship.ComponentType;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Player {
    private final PlayerId playerId;
    private PlayerFlightData playerFlightData;
    private PlayerColor color;
    private Map<CrewType, Integer> crew;
    private int credits;
    private int crewMembers;
    private Ship ship;

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


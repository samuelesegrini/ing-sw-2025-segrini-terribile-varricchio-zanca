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

    public int getCredits() {
        return this.credits;
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
     * Returns the number of the player's crew members
     *
     * @return number of the crew members
     */
    public int getTotalCrewMember() {
        return this.crewMembers;
    }

    /**
     * Updates the total number of crew members
     * 
     * @param crewMembers number of crew members to add/subtract
     * @param penalty true if subtracting members, false if adding
     * @throws IllegalArgumentException if resulting crew members would be negative
     */
    public void updateCrewMember(int crewMembers, boolean penalty) {
        int newTotal = penalty ? this.crewMembers - crewMembers : this.crewMembers + crewMembers;
        if (newTotal < 0) {
            throw new IllegalArgumentException("Total crew members cannot be negative");
        }
        this.crewMembers = newTotal;
    }

    /**
     * Updates the number of crew members of a specific type.
     *
     * @param type type of the crew members
     * @param members number of crew members to add
     * @throws IllegalArgumentException if type is null or members is negative
     * @return the new total of crew members of that type
     */
    public int addCrewMember(CrewType type, int members) {
        if (type == null) {
            throw new IllegalArgumentException("CrewType cannot be null");
        }
        if (members < 0) {
            throw new IllegalArgumentException("Members to add cannot be negative");
        }
        int newTotal = crew.getOrDefault(type, 0) + members;
        crew.put(type, newTotal);
        return newTotal;
    }

    /**
     * Subtracts crew members of a specific type
     * 
     * @param type type of the crew members
     * @param members number of crew members to subtract
     * @throws IllegalArgumentException if type is null, members is negative, or would result in negative crew
     */
    public void subtractCrewMember(CrewType type, int members) {
        if (type == null) {
            throw new IllegalArgumentException("CrewType cannot be null");
        }
        if (members < 0) {
            throw new IllegalArgumentException("Members to subtract cannot be negative");
        }
        int currentMembers = crew.getOrDefault(type, 0);
        if (members > currentMembers) {
            throw new IllegalArgumentException("Cannot subtract more crew members than available");
        }
        crew.put(type, currentMembers - members);
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


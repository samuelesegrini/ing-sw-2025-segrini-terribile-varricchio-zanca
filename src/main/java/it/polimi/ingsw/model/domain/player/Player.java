package it.polimi.ingsw.model.domain.player;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.player.PlayerColor;
import it.polimi.ingsw.model.enums.ship.ComponentType;

import java.security.InvalidParameterException;
import java.util.Map;

public class Player {
    private PlayerId playerId;
    private PlayerFlightData playerFlightData;
    private PlayerColor color;
    private Map<CrewType, Integer> crew;
    private int credits;
    private int crewMembers;
    private Ship ship;

    public PlayerId getId() {
        return this.playerId;
    }

    public PlayerFlightData getFlightData() {
        return this.playerFlightData;
    }

    public PlayerColor getColor() {
        return this.color;
    }

    public int getCredits() {
        return this.credits;
    }

    public Ship getShip() {
        return this.ship;
    }

    /**
     * Adds the specified credits to the player's total.
     *
     * @param credits number of credits to add to the player
     */
    public void addCredits(int credits) {
        this.credits += credits;
    }

    /**
     * Subtracts the specified credits to the player's total.
     *
     * @param credits number of credits to subtracts to the player
     */
    public void subtractCredits(int credits) {
        this.credits -= credits;
    }

    /**
     * Returns the number of the player's crew member
     *
     * @return number of the crew members
     */
    public int getTotalCrewMember() {
        return this.crewMembers;
    }
    public void updateCrewMember(int crewMembers, boolean penalty) {
        if (penalty){
            this.crewMembers -= crewMembers;
        }
        else{
            this.crewMembers += crewMembers;
        }
    }

    /**
     * Updates the number of crew members of a specific type.
     *
     * @param type    type of the crew members
     * @param members new total number of crew members
     */
    public int addCrewMember(CrewType type, int members) throws IllegalArgumentException {
        if (type != null) {
            crew.put(type, crew.getOrDefault(type, 0) + members);
        } else {
            throw new IllegalArgumentException("CrewType cannot be null");
        }
    }

    public void subtractCrewMember(CrewType type, int members) throws IllegalArgumentException {
        if (type != null) {
            crew.put(type, crew.getOrDefault(type, 0) - members);
        } else {
            throw new IllegalArgumentException("CrewType cannot be null");
        }
    }
}


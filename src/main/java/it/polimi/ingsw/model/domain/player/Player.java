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
    private Map <CrewType, Integer> crew;
    private int credits;
    private Ship ship;


    /**
     * Returns player's ID, which includes UUID and nickname.
     * @return player's Id
     * @see PlayerId
     */
    public PlayerId getId(){
        return this.playerId;
    }

    /**
     * Returns player's flight data, which includes position and laps completed.
     * @return player's flight data
     * @see PlayerFlightData
     */
    public PlayerFlightData getFlightData(){
        return this.playerFlightData;
    }

    /**
     * Returns a string representing player's color.
     * @return player's color
     */
    public String getColor(){
        return this.color;
    }

    public Ship getShip(){ return this.ship;}

    /**
     * Adds the specified credits to the player's total.
     * @param credits number of credits to add to the player
     */
    public void addCredits(int credits){
        this.credits += credits;
    }

    public int getCredits(){}

    /**
     * Subtracts the specified credits to the player's total.
     * @param credits number of credits to subtracts to the player
     */
    public void subtractCredits (int credits){
        this.credits=this.credits-credits;
    }

    /**
     * Returns the number of the player's crew member
     * @return number of the crew members
     */
    public int getTotalCrewMember() {
        int total = 0;
        for (CrewType t : crew.keySet()) {
            total += crew.get(t);
        }
        return total;
    }

    /**
     * Updates the number of crew members of a specific type.
     * @param type type of the crew members
     * @param members new total number of crew members
     */
    public void updateCrewMember (CrewType type, int members) throws IllegalArgumentException {
        if (type != null) {
            crew.put(type, crew.getOrDefault(type, 0) + members);
        }
        else {
           throws new IllegalArgumentException("CrewType cannot be null");
        }
    }
    public void subtractCrewMember (CrewType type, int members) throws IllegalArgumentException {
        if (type != null) {
            //dovremmo aggiornare anche la capacità dei componenti
            crew.put(type, crew.getOrDefault(type, 0) - members);
        }
        else {
            throw new IllegalArgumentException("CrewType cannot be null");
        }
    }
}


package it.polimi.ingsw.model.domain.player;

import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.player.PlayerColor;

public class Player {
    private PlayerId playerId;
    private PlayerColor color;
    private int credits;
    private Ship ship;

    /**
     * Moves the player to the specified position.
     * @param position int representing the position on the Route
     */
    public void moveTo (int position){}

    /**
     * Returns player's ID, which includes UUID and nickname.
     * @return player's Id
     * @see PlayerId
     */
    public int getId(){ return 0; }

    /**
     * Returns a string representing player's username.
     * @return player's username
     */
    public String getUsername(){ return ""; }

    /**
     * Returns a string representing player's color.
     * @return player's color
     */
    public String getColor(){ return ""; }

    /**
     * Returns the number of credits earned by the player.
     * @return number of credits
     */
    public int getCredits(){ return 0; }

    /**
     * Adds the specified credits to the player's total.
     * @param credits number of credits to add to the player
     */
    public void addCredits(int credits){}

    /**
     * Subtracts the specified credits to the player's total.
     * @param credits number of credits to subtracts to the player
     */
    public void subtractCredits (int credits){}

    /**
     * Returns the number of the player's crew member
     * @return number of the crew members
     */
    public int getTotalCrewMember(){ return 0; }

    /**
     * Updates the number of crew members of a specific type.
     * @param type type of the crew members
     * @param members new total number of crew members
     */
    public void updateCrewMember (CrewType type, int members){}
}
package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.flightboard.PlayerId;

public class Player(){
    private PlayerId playerId;
    private PlayerColor color;
    private int credits;
    private Ship ship;

    /**
     * Moves the player to the specified position.
     * @param position int representing the position on the Route
     */
    public void moveTo (int position){};

    /**
     * Returns player's ID, which includes UUID and nickname.
     * @return player's Id
     * @see it.polimi.ingsw.model.player.PlayerId
     */
    public int getId(){};

    /**
     * Returns a string representing player's username.
     * @return player's username
     */
    public String getUsername(){};

    /**
     * Returns a string representing player's color.
     * @return player's color
     */
    public String getColor(){};

    /**
     * Returns the number of credits earned by the player.
     * @return number of credits
     */
    public int getCredits(){};

    /**
     * Adds the specified credits to the player's total.
     * @param credits number of credits to add to the player
     */
    public void addCredits(int credits){};

    /**
     * Subtracts the specified credits to the player's total.
     * @param credits number of credits to subtracts to the player
     */
    public void subtractCredits (int credits){};

    /**
     * Returns the number of the player's crew member
     * @return number of the crew members
     */
    public int getTotalCrewMember(){};

    /**
     * Updates the number of crew members of a specific type.
     * @param type type of the crew members
     * @param members new total number of crew members
     */
    public updateCrewMember (CrewType type, int members){};
}
package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.flightboard.PlayerId;

public class Player(){
    private PlayerId playerId;
    private PlayerColor color;
    private int credits;
    private Ship ship;

    public void moveTo (int position){};
    public int getId();
    public String getUsername();
    public String getColor();
    public int getCredits();
    public void addCredits(int credits);
    public void subtractCredits (int credits);
    public int getTotalCrewMember();
    public updateCrewMember (CrewType type, int members);
}
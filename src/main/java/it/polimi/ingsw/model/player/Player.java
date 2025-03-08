package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.player.PlayerId;
import it.polimi.ingsw.model.ship.Ship;

public class Player(){
    private PlayerId playerId;
    private PlayerColor color;
    private int credits;
    private Ship ship;

    public void moveTo (int position){};

    public int getId() {
        return 0;
    }

    public String getUsername() {
        return null;
    }

    public String getColor() {
        return null;
    }

    public int getCredits() {
        return 0;
    }

    public void addCredits(int credits) {

    }

    public void subtractCredits(int credits) {

    }

    public int getTotalCrewMember() {
        return 0;
    }

    public void updateCrewMember(CrewType type, int members) {

    }
}
package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.enums.ship.Direction;

public class Position {
    private int x;
    private int y;

    public Position(int x, int y) {}

    // returns x coordinate
    public int getX() {return 0;}

    // returns y coordinate
    public int getY() {return 0;}

    public Position offsetBy(Direction direction) {return null;}
    public int hashCode() {return 0;}
}

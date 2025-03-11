package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.enums.ship.Direction;

public class Position {
    private int x;
    private int y;

    /**
     * Constructor defined by user.
     * @param x Horizontal coordinate.
     * @param y Vertical coordinate.
     */
    public Position(int x, int y) {}

    /**
     * Returns the horizontal coordinate.
     * @return The x-coordinate.
     */
    public int getX() {return 0;}

    /**
     * Returns the vertical coordinate.
     * @return The y-coordinate.
     */
    public int getY() {return 0;};
    /**
     * Returns a new Position offset by the specified direction.
     * @param direction The direction to offset the position.
     * @return A new Position with the offset applied.
     */
    public Position offsetBy(Direction direction) {return null;}

    /**
     * Returns a hash code value for the Position.
     * @return The hash code of the Position.
     */
    public int hashCode() {return 0;}
}

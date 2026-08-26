package it.polimi.ingsw.common.game;

/**
 * One of the four sides of a component tile, and one of the four directions a threat
 * can come from.
 *
 * <p>{@link #NORTH} is the bow of the ship, the side facing away from the player. The
 * manual calls it "in avanti": cannons pointing north count full firepower and engine
 * exhausts must point {@link #SOUTH}.
 */
public enum Direction {

    /** Away from the player. The ship's bow. */
    NORTH,

    /** To the player's right. */
    EAST,

    /** Toward the player. The ship's stern. */
    SOUTH,

    /** To the player's left. */
    WEST;

    /**
     * Returns the direction facing this one.
     *
     * <p>Two adjacent tiles touch on opposite sides: the north side of one meets the
     * south side of its neighbour above.
     *
     * @return the opposite direction
     */
    public Direction opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    /**
     * Returns the direction a quarter turn clockwise from this one.
     *
     * @return the next direction clockwise
     */
    public Direction clockwise() {
        return values()[(ordinal() + 1) % 4];
    }

    /**
     * Returns where this direction ends up once a tile is rotated.
     *
     * @param rotation the rotation applied to the tile, never {@code null}
     * @return the direction this side points to after the rotation
     */
    public Direction rotatedBy(Rotation rotation) {
        return values()[(ordinal() + rotation.quarterTurns()) % 4];
    }

    /**
     * Tells whether this direction runs along a column rather than a row.
     *
     * <p>Threats arriving from north or south travel down or up a column; threats from
     * east or west travel along a row. Dice address a column in the first case and a
     * row in the second.
     *
     * @return {@code true} for north and south, {@code false} for east and west
     */
    public boolean addressesColumn() {
        return this == NORTH || this == SOUTH;
    }
}

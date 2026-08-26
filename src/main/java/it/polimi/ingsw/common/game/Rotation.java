package it.polimi.ingsw.common.game;

/**
 * How far a component tile is turned from the orientation it is printed in.
 *
 * <p>Tiles are catalogued in their printed orientation and rotated when placed. The
 * artwork is consistent about what that orientation means: engine exhausts point
 * {@link Direction#SOUTH}, cannon muzzles point {@link Direction#NORTH}, and shields
 * cover north and east. Every one of the 152 tiles was checked against its image.
 */
public enum Rotation {

    /** The tile as printed. */
    NONE(0),

    /** A quarter turn clockwise. */
    CLOCKWISE_90(1),

    /** Upside down. */
    CLOCKWISE_180(2),

    /** A quarter turn counter-clockwise. */
    CLOCKWISE_270(3);

    private final int quarterTurns;

    Rotation(int quarterTurns) {
        this.quarterTurns = quarterTurns;
    }

    /**
     * Returns how many quarter turns clockwise this rotation represents.
     *
     * @return a value between 0 and 3
     */
    public int quarterTurns() {
        return quarterTurns;
    }

    /**
     * Returns the rotation that undoes this one.
     *
     * <p>Needed to go from a side of the placed tile back to the side of the printed
     * tile it came from.
     *
     * @return the inverse rotation
     */
    public Rotation inverse() {
        return values()[(4 - quarterTurns) % 4];
    }
}

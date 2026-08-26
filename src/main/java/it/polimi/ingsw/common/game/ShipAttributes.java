package it.polimi.ingsw.common.game;

import java.io.Serializable;

/**
 * What a ship is worth right now, for the three things cards compare.
 *
 * <p>Firepower is carried in halves rather than as a fraction. A single cannon that
 * does not face the bow is worth exactly one half, and manual p.11 goes out of its way
 * to say that 5½ beats 5 and loses to 6. Anything that rounded, in either direction,
 * would change who takes the cannon fire in a combat zone.
 *
 * @param firepowerHalves firepower, counted in halves of a point
 * @param enginePower     engine power, a whole number
 * @param crew            humans and aliens aboard
 */
public record ShipAttributes(int firepowerHalves, int enginePower, int crew) implements Serializable {

    /**
     * Returns firepower as a number for display.
     *
     * <p>For showing to a player only. Comparisons use {@link #firepowerHalves()},
     * which cannot lose a half to floating point.
     *
     * @return firepower in points
     */
    public double firepower() {
        return firepowerHalves / 2.0;
    }
}

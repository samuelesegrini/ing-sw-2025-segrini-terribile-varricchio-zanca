package it.polimi.ingsw.common.game;

import java.io.Serializable;


/**
 * One incoming threat: what it is, where it comes from, and which line it is on.
 *
 * <p>The line is named by a roll of two dice, so it ranges from 2 to 12 while the ship
 * boards print columns 4 to 10 and rows 5 to 9. A roll outside those ranges names no
 * line at all and misses cleanly, which is a real and reasonably common outcome rather
 * than an error (manual p.14 shows one).
 *
 * @param kind    what is coming
 * @param from    the direction it arrives from
 * @param diceSum the sum of the two dice that named its line
 */
public record Hit(HitKind kind, Direction from, int diceSum) implements Serializable {

    /**
     * Validates the threat.
     *
     * @throws IllegalArgumentException if the dice sum is not something two dice can roll
     * @throws NullPointerException     if the kind or the direction is {@code null}
     */
    public Hit {
        if (kind == null || from == null) {
            throw new NullPointerException("a hit needs a kind and a direction");
        }
        if (diceSum < 2 || diceSum > 12) {
            throw new IllegalArgumentException("two dice cannot sum to " + diceSum);
        }
    }
}

package it.polimi.ingsw.common.game;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What one threat did to a ship.
 *
 * <p>Reported rather than merely applied, because a client has to be able to tell a
 * player what happened: whether the shot missed the ship entirely, whether their shield
 * earned its battery, and which components they have just lost.
 *
 * <p>The two positions are {@code null} rather than {@link Optional} because this record
 * crosses the wire, and RMI marshals with Java serialization, which cannot carry an
 * {@code Optional}. Read them through {@link #targetIfAny()} and {@link #destroyedIfAny()}.
 *
 * @param target    the component the threat was aimed at, {@code null} when it missed the ship
 * @param outcome   how it ended
 * @param destroyed the component destroyed, {@code null} when nothing was
 * @param fragments the pieces the ship is now in, largest first; more than one means the
 *                  player has to choose which to keep flying
 */
public record DamageReport(Position target,
                           Outcome outcome,
                           Position destroyed,
                           List<Set<Position>> fragments) implements Serializable {

    /**
     * How a threat ended up.
     */
    public enum Outcome {

        /** The dice named a line with nothing on it, or no line at all. */
        MISSED,

        /** It struck a smooth side and bounced off without help. */
        BOUNCED,

        /** A shield or a cannon stopped it, at the cost of a battery. */
        DEFENDED,

        /** It got through and destroyed what it hit. */
        DESTROYED
    }

    /**
     * Takes a defensive copy of the fragments.
     *
     * @throws NullPointerException if the outcome is {@code null}
     */
    public DamageReport {
        if (outcome == null) {
            throw new NullPointerException("a damage report needs an outcome");
        }
        fragments = fragments.stream().map(Set::copyOf).toList();
    }

    /**
     * Returns the component the threat was aimed at.
     *
     * @return the target, or empty when the threat missed the ship entirely
     */
    public Optional<Position> targetIfAny() {
        return Optional.ofNullable(target);
    }

    /**
     * Returns the component the threat destroyed.
     *
     * @return the wreck, or empty when the ship came through it
     */
    public Optional<Position> destroyedIfAny() {
        return Optional.ofNullable(destroyed);
    }

    /**
     * Tells whether the ship came through untouched.
     *
     * @return {@code true} when nothing was destroyed
     */
    public boolean shipIsIntact() {
        return outcome != Outcome.DESTROYED;
    }

    /**
     * Tells whether the player now has to choose which piece of their ship to keep.
     *
     * <p>The manual offers the choice whenever a ship breaks up (p.10), even when one
     * piece is obviously the one worth keeping.
     *
     * @return {@code true} when the ship is in more than one piece
     */
    public boolean brokeUp() {
        return fragments.size() > 1;
    }
}

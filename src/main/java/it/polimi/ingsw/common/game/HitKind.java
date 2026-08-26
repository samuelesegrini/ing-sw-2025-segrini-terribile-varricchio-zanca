package it.polimi.ingsw.common.game;

/**
 * The four things that can come at a ship, and what can be done about each.
 *
 * <p>The differences between them are the whole defensive game (manual p.10, p.13). A
 * well built ship shrugs off small meteors for nothing; a shield turns aside small
 * meteors and light fire for a battery; only a cannon stops a big meteor; and nothing at
 * all stops heavy fire.
 */
public enum HitKind {

    /**
     * Bounces off a smooth side for free, and off a shield for a battery. Destroys an
     * exposed connector otherwise.
     */
    SMALL_METEOR,

    /**
     * Ignores smooth sides and shields alike. The only defence is shooting it, and only
     * from the right direction (manual p.19).
     */
    BIG_METEOR,

    /** Stopped by a correctly oriented shield, at a battery. Nothing else works. */
    LIGHT_FIRE,

    /** No defence exists. The only hope is a roll that misses the ship entirely. */
    HEAVY_FIRE;

    /**
     * Tells whether a shield can turn this aside.
     *
     * @return {@code true} for small meteors and light fire
     */
    public boolean stoppableByShield() {
        return this == SMALL_METEOR || this == LIGHT_FIRE;
    }

    /**
     * Tells whether a cannon can destroy this before it lands.
     *
     * @return {@code true} only for big meteors
     */
    public boolean stoppableByCannon() {
        return this == BIG_METEOR;
    }

    /**
     * Tells whether this bounces off a smooth side without any help.
     *
     * <p>The reason a tidy ship survives a meteor swarm: only an exposed connector gives
     * a small meteor something to catch on (manual p.13).
     *
     * @return {@code true} only for small meteors
     */
    public boolean bouncesOffSmoothSides() {
        return this == SMALL_METEOR;
    }
}

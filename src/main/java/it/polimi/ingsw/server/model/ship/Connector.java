package it.polimi.ingsw.server.model.ship;

/**
 * What a component tile carries on one of its four sides.
 *
 * <p>Two touching sides must form a legal joint, which the manual (p.5) defines by
 * pipe count: a connector joins another of the same kind, a three-pipe connector
 * joins anything, and a smooth side joins nothing at all.
 */
public enum Connector {

    /** A smooth side. Not a connector; it joins nothing, not even another smooth side. */
    PLAIN,

    /** One pipe. Joins a single or a universal connector. */
    SINGLE,

    /** Two pipes. Joins a double or a universal connector. */
    DOUBLE,

    /** Three pipes. Joins any connector. */
    UNIVERSAL;

    /**
     * Tells whether this side can be welded to the given one.
     *
     * <p>Note that two {@link #PLAIN} sides sitting next to each other is legal — it is
     * simply not a joint. This method answers "do these form a connection", not "is
     * this placement allowed"; the ship validator needs both questions.
     *
     * @param other the connector on the facing side of the neighbouring tile
     * @return {@code true} if the two sides form a legal joint
     */
    public boolean joinsTo(Connector other) {
        if (this == PLAIN || other == PLAIN) {
            return false;
        }
        return this == UNIVERSAL || other == UNIVERSAL || this == other;
    }

    /**
     * Tells whether this side carries pipes at all.
     *
     * <p>Only sides that carry pipes can be exposed connectors, which is what Stardust
     * counts and what the prettiest ship reward measures.
     *
     * @return {@code true} for anything but {@link #PLAIN}
     */
    public boolean isConnector() {
        return this != PLAIN;
    }
}

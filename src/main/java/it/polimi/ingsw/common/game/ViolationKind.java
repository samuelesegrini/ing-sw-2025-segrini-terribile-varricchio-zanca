package it.polimi.ingsw.common.game;

/**
 * The ways a ship can break the assembly rules.
 *
 * <p>These are the checks manual p.8 lists under "attenzione a questi errori", the ones
 * the other players would run through by eye in the physical game. Here the application
 * runs them, as {@code requirements.pdf} § 2.1 requires.
 *
 * <p>One rule from that list is missing on purpose: a component outside the assembly
 * area. Placement refuses an off-outline cell the moment it is attempted, so by the time
 * a ship is validated no such component can exist. It is enforced earlier rather than
 * not at all.
 */
public enum ViolationKind {

    /**
     * Two touching sides both carry pipes but cannot be welded — a single connector
     * facing a double one.
     */
    INCOMPATIBLE_CONNECTORS,

    /**
     * A connector faces the smooth side of its neighbour. Smooth sides join nothing, so
     * a connector pressed against one is a broken joint rather than no joint.
     */
    CONNECTOR_MEETS_SMOOTH_SIDE,

    /** An engine is turned so that its exhaust does not point at the stern. */
    ENGINE_NOT_FACING_STERN,

    /** A component sits in the cell an engine fires into. */
    BLOCKED_ENGINE_EXHAUST,

    /** A component sits in the cell a cannon fires into. */
    BLOCKED_CANNON_MUZZLE,

    /** The ship is in more than one piece: something is welded to nothing. */
    DISCONNECTED
}

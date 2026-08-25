package it.polimi.ingsw.server.model.building;

/**
 * How a player who finishes building gets their place on the route.
 *
 * <p>The two levels differ, and the difference is easy to miss because the manual states
 * each rule exactly once, sixty pages apart.
 */
public enum StartSpacePolicy {

    /**
     * Spaces go out in finishing order: first to finish takes space 1, second takes
     * space 2, and so on `[p.8]`. No choice is offered.
     */
    IN_FINISHING_ORDER,

    /**
     * The player picks any free space, except those numbered higher than the number of
     * players in the game `[p.17]`. With two players only spaces 1 and 2 are on offer,
     * whoever finishes first.
     */
    CHOSEN_BY_PLAYER
}

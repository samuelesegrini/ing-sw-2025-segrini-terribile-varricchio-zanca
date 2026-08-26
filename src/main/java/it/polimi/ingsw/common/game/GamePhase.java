package it.polimi.ingsw.common.game;

/**
 * Where a game has got to.
 *
 * <p>The phases run in this order and never go back. Each one decides which commands are
 * legal, which is why the client is told the phase rather than left to infer it from what
 * it can see.
 */
public enum GamePhase {

    /** Waiting for the seats to fill. Nothing has been built yet. */
    LOBBY,

    /** The shipyard is open: tiles are being drawn, turned and welded (manual p.5). */
    BUILDING,

    /** Ships are being checked and illegal components pulled off (p.9). */
    VALIDATION,

    /** Cabins are being filled with humans and aliens (p.9). */
    CREW_PLACEMENT,

    /** Cards are being turned over and the route flown (p.10). */
    FLIGHT,

    /** The flight is over and the ledger is being settled (p.15). */
    SCORING,

    /** Nothing more will happen. */
    FINISHED
}

package it.polimi.ingsw.client.view.gui;

/**
 * Which of the windows a player should be looking at.
 *
 * <p>One per thing a player does, which is not quite one per phase: repairing a ship and
 * crewing it are separate screens because they are separate jobs, and a player who has finished
 * building watches the shipyard rather than getting a screen of their own.
 */
public enum Screen {

    /** Nobody has said who they are yet. */
    LOGIN,

    /** Choosing a table: what is open, or opening one. */
    LOBBY,

    /** Building: the heap, the hand, and a board to weld it to. */
    SHIPYARD,

    /** Throwing off what cannot stay, and choosing which piece to keep. */
    REPAIRS,

    /** Filling the cabins before the launch. */
    CREW,

    /** The route, the card on the table, and whatever it is asking. */
    FLIGHT,

    /** The ledger, once there is one. */
    LEDGER
}

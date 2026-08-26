package it.polimi.ingsw.client.view;

/**
 * A way of playing, chosen at startup.
 *
 * <p>Requirement C3 asks for the interface to be picked before anything else happens, which
 * means there has to be more than one thing it could be. Two implementations, one method, and
 * nothing anywhere else that knows which was chosen.
 */
public interface UserInterface {

    /**
     * Runs until the player stops playing.
     *
     * <p>Owns the thread it is called on. Events arrive on other threads and are read from the
     * client's state rather than delivered here.
     */
    void run();
}

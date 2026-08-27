package it.polimi.ingsw.common.transport;

/**
 * A way in: something clients connect to, that can be counted and shut.
 *
 * <p>There are two, they present the same four members, and until this existed nothing said
 * so — {@code RmiServer}'s own Javadoc claimed to be <em>"the same shape as SocketServer on
 * purpose"</em>, which is a comment doing an interface's job. Two adapters is what makes this
 * a real seam rather than a hypothetical one.
 *
 * <p>What is deliberately <em>not</em> here is anything about how a door is opened. Binding a
 * server socket and standing up an RMI registry have nothing in common and take different
 * arguments, so each adapter keeps its own {@code listening} factory. What a caller holds
 * afterwards is the same either way, which is the half that matters: a server with two doors
 * open should not have to hold them differently, because the point of requirement S5 is that
 * players using them are not special cases of each other.
 */
public interface Doorway extends AutoCloseable {

    /**
     * Returns the port this door is actually on.
     *
     * <p>Worth having because a door opened on port zero is given one, which is how a test
     * runs without picking a number and hoping.
     *
     * @return the bound port
     */
    int port();

    /**
     * Returns how many clients are connected through it.
     *
     * @return the number of open channels
     */
    int connectionCount();

    /**
     * Shuts the door, and every connection that came through it.
     *
     * <p>Narrowed from {@link AutoCloseable} so that it throws nothing: a caller shutting down
     * has nothing useful to do with a failure, and one that had to catch something per door
     * would be a caller with a half-closed server.
     */
    @Override
    void close();
}

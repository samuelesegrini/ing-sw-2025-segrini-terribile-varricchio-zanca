package it.polimi.ingsw.common.transport;

/**
 * A connection could not be made, or could not be listened for.
 *
 * <p>Only ever thrown while setting up. Once a channel is open, a transport failure is not
 * an exception any more — it is a disconnection, and disconnections are reported to the
 * listener because that is a thing this game is expected to survive rather than a thing that
 * went wrong.
 */
public class TransportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Reports a failure to establish a connection.
     *
     * @param message what was being attempted
     * @param cause   what stopped it
     */
    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}

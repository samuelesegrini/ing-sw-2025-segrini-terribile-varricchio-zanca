package it.polimi.ingsw.server.data;

import java.io.Serial;

/**
 * Raised when the shipped game data cannot be turned into a usable catalogue.
 *
 * <p>Always names the entry at fault. Bad data is a packaging error rather than
 * something a running game can recover from, so this fails startup loudly instead of
 * letting a half-loaded catalogue reach the table.
 */
public class GameDataException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception describing what could not be read.
     *
     * @param message what was wrong, naming the offending entry
     */
    public GameDataException(String message) {
        super(message);
    }

    /**
     * Creates an exception describing what could not be read, with its cause.
     *
     * @param message what was wrong, naming the offending entry
     * @param cause   the underlying failure
     */
    public GameDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

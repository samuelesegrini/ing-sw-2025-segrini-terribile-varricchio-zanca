package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

import java.util.Objects;

/**
 * Command sent by a client to update their ready status in a game lobby.
 */
public class SetPlayerReadyCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final boolean ready;

    /**
     * Creates a command to set player's ready status.
     * 
     * @param sessionId The ID of the session
     * @param ready The new ready status (true = ready, false = not ready)
     */
    public SetPlayerReadyCommand(String sessionId, boolean ready) {
        super();
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.ready = ready;
    }

    /**
     * Gets the session ID.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the ready status.
     * 
     * @return true if the player is ready, false otherwise
     */
    public boolean isReady() {
        return ready;
    }

    @Override
    public String toString() {
        return "SetPlayerReadyCommand{" +
                "sessionId='" + sessionId + '\'' +
                ", ready=" + ready +
                ", timestamp=" + getTimestamp() +
                '}';
    }
} 
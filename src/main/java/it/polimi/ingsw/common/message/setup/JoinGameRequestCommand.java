package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

import java.util.Objects;

/**
 * Command sent by a client to request joining an existing game session.
 */
public class JoinGameRequestCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    private final String sessionId;

    public JoinGameRequestCommand(String sessionId) {
        super();
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "JoinGameRequestCommand{" +
                "sessionId='" + sessionId + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
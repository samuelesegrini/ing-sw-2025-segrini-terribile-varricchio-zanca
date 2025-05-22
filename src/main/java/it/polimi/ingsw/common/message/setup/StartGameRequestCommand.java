package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.Command;
import it.polimi.ingsw.common.message.Message;

/**
 * Client-to-server command to request starting a game session.
 * This command should only be sent by the host of the session.
 */
public class StartGameRequestCommand implements Message, Command {
    private static final long serialVersionUID = 1L;
    
    private final String sessionId;
    
    /**
     * Creates a command to start a game session.
     * 
     * @param sessionId The ID of the session to start
     */
    public StartGameRequestCommand(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the session ID to start.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
} 
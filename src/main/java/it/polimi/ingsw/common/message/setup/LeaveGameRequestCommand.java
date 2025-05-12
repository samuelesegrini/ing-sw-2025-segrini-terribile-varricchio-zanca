package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.Message;

/**
 * Client-to-server command to request leaving a game session.
 */
public class LeaveGameRequestCommand implements Message {
    private static final long serialVersionUID = 1L;
    
    private final String sessionId;
    
    /**
     * Creates a command to leave a game session.
     * 
     * @param sessionId The ID of the session to leave
     */
    public LeaveGameRequestCommand(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Gets the session ID to leave.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
} 
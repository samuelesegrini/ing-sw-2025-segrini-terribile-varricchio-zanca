package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;

public class ServerLoginResponse extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String playerId; // Only relevant if success is true
    private final String message;  // Reason for failure, or welcome message

    /**
     * Constructor for a successful login.
     * @param playerId The unique ID assigned to the player by the server.
     * @param welcomeMessage A welcome message.
     */
    public ServerLoginResponse(String playerId, String welcomeMessage) {
        super();
        this.success = true;
        this.playerId = playerId;
        this.message = welcomeMessage;
    }

    /**
     * Constructor for a failed login.
     * @param reason The reason for the login failure.
     */
    public ServerLoginResponse(String reason) {
        super();
        this.success = false;
        this.playerId = null; // No player ID on failure
        this.message = reason;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ServerLoginResponse{" +
                "success=" + success +
                ", playerId='" + playerId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
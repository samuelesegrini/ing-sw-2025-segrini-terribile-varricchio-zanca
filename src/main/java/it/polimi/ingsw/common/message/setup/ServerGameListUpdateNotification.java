package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;

/**
 * Server-to-client notification that the game list has been updated.
 * Clients should refresh their game lists when receiving this message.
 */
public class ServerGameListUpdateNotification extends BaseMessage {
    private static final long serialVersionUID = 1L;
    
    /**
     * Default constructor for notification.
     */
    public ServerGameListUpdateNotification() {
        super(); // Call BaseMessage constructor to initialize timestamp
        // Empty constructor - notification has no payload
    }
} 
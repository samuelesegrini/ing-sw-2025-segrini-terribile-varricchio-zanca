package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerGameListUpdateNotificationTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(ServerGameListUpdateNotification.class),
                "ServerGameListUpdateNotification should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(ServerGameListUpdateNotification.class),
                "ServerGameListUpdateNotification should implement Message interface");
    }

    @Test
    void constructorCreatesInstance() {
        ServerGameListUpdateNotification notification = new ServerGameListUpdateNotification();
        assertNotNull(notification, "Constructor should create a valid instance");
    }
    
    @Test
    void multipleInstancesAreUnique() throws InterruptedException {
        ServerGameListUpdateNotification notification1 = new ServerGameListUpdateNotification();
        
        // Sleep to ensure different timestamps
        Thread.sleep(10);
        
        ServerGameListUpdateNotification notification2 = new ServerGameListUpdateNotification();
        
        // Can't directly compare notifications, so make sure their toString representations are different
        // due to different timestamps
        assertNotEquals(notification1.toString(), notification2.toString(),
                "Different instances should have different string representations due to timestamps");
    }
    
    @Test
    void toStringContainsTimestamp() {
        ServerGameListUpdateNotification notification = new ServerGameListUpdateNotification();
        String toString = notification.toString();
        
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
package it.polimi.ingsw.common.message;

import it.polimi.ingsw.common.message.setup.GameListResponseEvent;
import it.polimi.ingsw.common.message.setup.JoinGameRequestCommand;
import it.polimi.ingsw.common.message.setup.PlayerJoinedGameSessionNotification;
import it.polimi.ingsw.common.message.setup.ServerGameListUpdateNotification;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void messageExtendsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(Message.class),
                "Message interface should extend Serializable");
    }
    
    @Test
    void verifyMessageImplementations() {
        // Create a list of classes that should implement Message
        List<Class<?>> messageClasses = Arrays.asList(
                BaseMessage.class,
                GameListResponseEvent.class,
                JoinGameRequestCommand.class,
                PlayerJoinedGameSessionNotification.class,
                ServerGameListUpdateNotification.class,
                ErrorMessage.class,
                ServerLoginResponse.class
        );
        
        // Verify all of them implement the Message interface
        for (Class<?> clazz : messageClasses) {
            assertTrue(Message.class.isAssignableFrom(clazz),
                    clazz.getSimpleName() + " should implement Message interface");
        }
    }
    
    @Test
    void baseMessageImplementsMessageCorrectly() {
        // BaseMessage is abstract, so we can't instantiate it directly
        // Instead, we check that it implements Message
        assertTrue(Message.class.isAssignableFrom(BaseMessage.class),
                "BaseMessage should implement Message interface");
    }
    
    @Test
    void messageRequiresNoMethods() {
        // The Message interface doesn't require any methods to be implemented
        // This test verifies that a minimal implementation can be created
        Message minimalMessage = new Message() {
            private static final long serialVersionUID = 1L;
            // No methods needed
        };
        
        assertNotNull(minimalMessage, "A minimal Message implementation should be possible");
        
        // Verify it's actually Serializable
        assertTrue(minimalMessage instanceof Serializable, 
                "Message implementation should be instance of Serializable");
    }
} 
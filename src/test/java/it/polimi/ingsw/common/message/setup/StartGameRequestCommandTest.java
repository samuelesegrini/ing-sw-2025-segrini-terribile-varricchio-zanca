package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class StartGameRequestCommandTest {

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(StartGameRequestCommand.class),
                "StartGameRequestCommand should implement Message interface");
    }
    
    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(StartGameRequestCommand.class),
                "StartGameRequestCommand should implement Serializable");
    }

    @Test
    void constructorSetsSessionId() {
        StartGameRequestCommand command = new StartGameRequestCommand("game-123");
        assertEquals("game-123", command.getSessionId());
    }
    
    @Test
    void constructorWorksWithNullSessionId() {
        // The command should accept null sessionId without throwing an exception
        StartGameRequestCommand command = new StartGameRequestCommand(null);
        
        assertNull(command.getSessionId());
    }
    
    @Test
    void differentInstancesWithDifferentSessionIds() {
        StartGameRequestCommand command1 = new StartGameRequestCommand("game-123");
        StartGameRequestCommand command2 = new StartGameRequestCommand("game-456");
        
        assertEquals("game-123", command1.getSessionId());
        assertEquals("game-456", command2.getSessionId());
        
        // Verify each instance maintains its own session ID
        assertNotEquals(command1.getSessionId(), command2.getSessionId());
    }
} 
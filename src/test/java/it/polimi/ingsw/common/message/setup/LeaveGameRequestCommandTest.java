package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class LeaveGameRequestCommandTest {

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(LeaveGameRequestCommand.class),
                "LeaveGameRequestCommand should implement Message interface");
    }
    
    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(LeaveGameRequestCommand.class),
                "LeaveGameRequestCommand should implement Serializable");
    }

    @Test
    void constructorSetsSessionId() {
        LeaveGameRequestCommand command = new LeaveGameRequestCommand("game-123");
        assertEquals("game-123", command.getSessionId());
    }
    
    @Test
    void constructorWorksWithNullSessionId() {
        // The command should accept null sessionId without throwing an exception
        LeaveGameRequestCommand command = new LeaveGameRequestCommand(null);
        
        assertNull(command.getSessionId());
    }
    
    @Test
    void differentInstancesWithDifferentSessionIds() {
        LeaveGameRequestCommand command1 = new LeaveGameRequestCommand("game-123");
        LeaveGameRequestCommand command2 = new LeaveGameRequestCommand("game-456");
        
        assertEquals("game-123", command1.getSessionId());
        assertEquals("game-456", command2.getSessionId());
        
        // Verify each instance maintains its own session ID
        assertNotEquals(command1.getSessionId(), command2.getSessionId());
    }
} 
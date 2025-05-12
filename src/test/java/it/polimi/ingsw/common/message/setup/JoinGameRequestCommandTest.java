package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinGameRequestCommandTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(JoinGameRequestCommand.class),
                "JoinGameRequestCommand should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(JoinGameRequestCommand.class),
                "JoinGameRequestCommand should implement Message interface");
    }

    @Test
    void constructorSetsSessionId() {
        JoinGameRequestCommand command = new JoinGameRequestCommand("game-123");
        assertEquals("game-123", command.getSessionId());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        assertThrows(NullPointerException.class, () -> new JoinGameRequestCommand(null));
    }
    
    @Test
    void differentInstancesHaveIndependentSessionIds() {
        JoinGameRequestCommand command1 = new JoinGameRequestCommand("game-123");
        JoinGameRequestCommand command2 = new JoinGameRequestCommand("game-456");
        
        assertEquals("game-123", command1.getSessionId());
        assertEquals("game-456", command2.getSessionId());
    }
    
    @Test
    void toStringIncludesAllFields() {
        JoinGameRequestCommand command = new JoinGameRequestCommand("test-session-id");
        String toString = command.toString();
        
        assertTrue(toString.contains("test-session-id"), "toString should include session ID");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetPlayerReadyCommandTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(SetPlayerReadyCommand.class),
                "SetPlayerReadyCommand should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(SetPlayerReadyCommand.class),
                "SetPlayerReadyCommand should implement Message interface");
    }

    @Test
    void constructorSetsSessionIdAndReadyStatus() {
        SetPlayerReadyCommand readyCommand = new SetPlayerReadyCommand("game-123", true);
        assertEquals("game-123", readyCommand.getSessionId());
        assertTrue(readyCommand.isReady());
        
        SetPlayerReadyCommand notReadyCommand = new SetPlayerReadyCommand("game-456", false);
        assertEquals("game-456", notReadyCommand.getSessionId());
        assertFalse(notReadyCommand.isReady());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        assertThrows(NullPointerException.class, () -> new SetPlayerReadyCommand(null, true));
    }
    
    @Test
    void differentInstancesHaveIndependentValues() {
        SetPlayerReadyCommand command1 = new SetPlayerReadyCommand("game-123", true);
        SetPlayerReadyCommand command2 = new SetPlayerReadyCommand("game-456", false);
        
        assertEquals("game-123", command1.getSessionId());
        assertTrue(command1.isReady());
        
        assertEquals("game-456", command2.getSessionId());
        assertFalse(command2.isReady());
    }
    
    @Test
    void toStringIncludesAllFields() {
        SetPlayerReadyCommand readyCommand = new SetPlayerReadyCommand("test-session", true);
        String readyString = readyCommand.toString();
        
        assertTrue(readyString.contains("test-session"), "toString should include session ID");
        assertTrue(readyString.contains("ready=true"), "toString should include ready status");
        assertTrue(readyString.contains("timestamp"), "toString should include timestamp");
        
        SetPlayerReadyCommand notReadyCommand = new SetPlayerReadyCommand("test-session", false);
        String notReadyString = notReadyCommand.toString();
        
        assertTrue(notReadyString.contains("ready=false"), "toString should include ready status");
    }
} 
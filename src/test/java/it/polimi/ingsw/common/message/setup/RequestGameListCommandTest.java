package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestGameListCommandTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(RequestGameListCommand.class),
                "RequestGameListCommand should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(RequestGameListCommand.class),
                "RequestGameListCommand should implement Message interface");
    }

    @Test
    void constructorCreatesInstance() {
        RequestGameListCommand command = new RequestGameListCommand();
        assertNotNull(command, "Constructor should create a valid instance");
    }
    
    @Test
    void toStringContainsTimestamp() {
        RequestGameListCommand command = new RequestGameListCommand();
        String toString = command.toString();
        
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
    
    @Test
    void multipleInstancesHaveUniqueTimestamps() throws InterruptedException {
        RequestGameListCommand command1 = new RequestGameListCommand();
        
        // Sleep to ensure different timestamps
        Thread.sleep(10);
        
        RequestGameListCommand command2 = new RequestGameListCommand();
        
        // Each instance of RequestGameListCommand should have a unique timestamp from BaseMessage
        assertNotEquals(command1.toString(), command2.toString(),
                "Different instances should have different timestamps");
    }
} 
package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientLoginRequestTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(ClientLoginRequest.class),
                "ClientLoginRequest should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(ClientLoginRequest.class),
                "ClientLoginRequest should implement Message interface");
    }

    @Test
    void constructorSetsNickname() {
        ClientLoginRequest request = new ClientLoginRequest("testUser");
        assertEquals("testUser", request.getNickname());
    }

    @Test
    void nicknameImmutable() {
        ClientLoginRequest request = new ClientLoginRequest("testUser");
        assertEquals("testUser", request.getNickname());
        
        // Create a new request with different nickname
        ClientLoginRequest request2 = new ClientLoginRequest("anotherUser");
        assertEquals("anotherUser", request2.getNickname());
        
        // First request should still have its original nickname
        assertEquals("testUser", request.getNickname(), 
                "Original request's nickname should not be affected by new instances");
    }

    @Test
    void toStringContainsNickname() {
        ClientLoginRequest request = new ClientLoginRequest("johndoe");
        String toString = request.toString();
        
        assertTrue(toString.contains("johndoe"), "toString should contain nickname");
        assertTrue(toString.contains("timestamp"), "toString should contain timestamp");
    }
} 
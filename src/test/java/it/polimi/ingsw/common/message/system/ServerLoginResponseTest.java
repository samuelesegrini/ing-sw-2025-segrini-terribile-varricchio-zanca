package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerLoginResponseTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(ServerLoginResponse.class),
                "ServerLoginResponse should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(ServerLoginResponse.class),
                "ServerLoginResponse should implement Message interface");
    }

    @Test
    void constructorForSuccessfulLogin() {
        ServerLoginResponse response = new ServerLoginResponse("player123", "Welcome!");
        
        assertTrue(response.isSuccess(), "Login should be marked as successful");
        assertEquals("player123", response.getPlayerId(), "Player ID should match the one provided");
        assertEquals("Welcome!", response.getMessage(), "Welcome message should match the one provided");
    }
    
    @Test
    void constructorForFailedLogin() {
        ServerLoginResponse response = new ServerLoginResponse("Nickname already in use");
        
        assertFalse(response.isSuccess(), "Login should be marked as failed");
        assertNull(response.getPlayerId(), "Player ID should be null for failed login");
        assertEquals("Nickname already in use", response.getMessage(), 
                "Failed login message should contain the reason");
    }
    
    @Test
    void successfulAndFailedLoginsDifferentiate() {
        ServerLoginResponse success = new ServerLoginResponse("player123", "Welcome!");
        ServerLoginResponse failure = new ServerLoginResponse("Login error");
        
        assertTrue(success.isSuccess(), "First response should be successful");
        assertFalse(failure.isSuccess(), "Second response should be a failure");
    }
    
    @Test
    void toStringIncludesAllFields() {
        ServerLoginResponse success = new ServerLoginResponse("player123", "Welcome!");
        String successString = success.toString();
        
        ServerLoginResponse failure = new ServerLoginResponse("Login error");
        String failureString = failure.toString();
        
        // Check success response
        assertTrue(successString.contains("success=true"), "toString should show success=true");
        assertTrue(successString.contains("player123"), "toString should include player ID");
        assertTrue(successString.contains("Welcome!"), "toString should include welcome message");
        assertTrue(successString.contains("timestamp"), "toString should include timestamp");
        
        // Check failure response
        assertTrue(failureString.contains("success=false"), "toString should show success=false");
        assertTrue(failureString.contains("Login error"), "toString should include error message");
    }
} 
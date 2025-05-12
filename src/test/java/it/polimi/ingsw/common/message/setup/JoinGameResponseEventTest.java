package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinGameResponseEventTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(JoinGameResponseEvent.class),
                "JoinGameResponseEvent should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(JoinGameResponseEvent.class),
                "JoinGameResponseEvent should implement Message interface");
    }

    @Test
    void constructorForSuccessfulJoin() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One", false),
                new PlayerInfoDTO("player2", "Player Two", true)
        );
        
        JoinGameResponseEvent response = new JoinGameResponseEvent("game-123", players);
        
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertEquals("game-123", response.getSessionId());
        assertNull(response.getErrorMessage(), "Error message should be null for successful join");
        assertNotNull(response.getPlayersInThisLobby(), "Players list should not be null");
        assertEquals(2, response.getPlayersInThisLobby().size(), "Players list should have 2 entries");
    }
    
    @Test
    void constructorForFailedJoin() {
        JoinGameResponseEvent response = new JoinGameResponseEvent("game-123", "Game is full");
        
        assertFalse(response.isSuccess(), "Response should indicate failure");
        assertEquals("game-123", response.getSessionId());
        assertEquals("Game is full", response.getErrorMessage());
        assertNull(response.getPlayersInThisLobby(), "Players list should be null for failed join");
    }
    
    @Test
    void constructorForFailedJoinWithNullSessionId() {
        JoinGameResponseEvent response = new JoinGameResponseEvent(null, "Invalid session ID");
        
        assertFalse(response.isSuccess(), "Response should indicate failure");
        assertNull(response.getSessionId(), "Session ID can be null for failed join with invalid ID");
        assertEquals("Invalid session ID", response.getErrorMessage());
        assertNull(response.getPlayersInThisLobby(), "Players list should be null for failed join");
    }
    
    @Test
    void nullErrorMessageThrowsException() {
        assertThrows(NullPointerException.class, () -> new JoinGameResponseEvent("game-123", (String)null));
    }
    
    @Test
    void nullSessionIdForSuccessThrowsException() {
        List<PlayerInfoDTO> players = new ArrayList<>();
        assertThrows(NullPointerException.class, () -> new JoinGameResponseEvent(null, players));
    }
    
    @Test
    void nullPlayersListThrowsException() {
        assertThrows(NullPointerException.class, () -> new JoinGameResponseEvent("game-123", (List<PlayerInfoDTO>)null));
    }
    
    @Test
    void playersListIsCopiedInConstructor() {
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Player One"));
        
        JoinGameResponseEvent response = new JoinGameResponseEvent("game-123", originalList);
        
        // Add to original list should not affect response's list
        originalList.add(new PlayerInfoDTO("player2", "Player Two"));
        
        assertEquals(1, response.getPlayersInThisLobby().size(), 
                "Response's player list should not be affected by changes to original list");
    }
    
    @Test
    void getPlayersInThisLobbyReturnsCopy() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        JoinGameResponseEvent response = new JoinGameResponseEvent("game-123", players);
        List<PlayerInfoDTO> returnedList = response.getPlayersInThisLobby();
        
        // Verify original list and returned list are different objects
        assertNotSame(players, returnedList, "Returned list should be a new instance, not the original");
        
        // Modifying the returned list should be possible
        returnedList.add(new PlayerInfoDTO("player3", "Player Three"));
        
        // The next call should return a different list with only the original two players
        List<PlayerInfoDTO> secondReturnedList = response.getPlayersInThisLobby();
        assertEquals(2, secondReturnedList.size(), 
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        JoinGameResponseEvent successResponse = new JoinGameResponseEvent("game-123", players);
        String successString = successResponse.toString();
        
        JoinGameResponseEvent failureResponse = new JoinGameResponseEvent("game-456", "Session full");
        String failureString = failureResponse.toString();
        
        // Success response
        assertTrue(successString.contains("success=true"), "toString should indicate success");
        assertTrue(successString.contains("game-123"), "toString should include session ID");
        assertTrue(successString.contains("2"), "toString should include player count");
        assertTrue(successString.contains("timestamp"), "toString should include timestamp");
        
        // Failure response
        assertTrue(failureString.contains("success=false"), "toString should indicate failure");
        assertTrue(failureString.contains("game-456"), "toString should include session ID");
        assertTrue(failureString.contains("Session full"), "toString should include error message");
    }
} 
package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.model.GameSessionState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionStateChangedEventTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(GameSessionStateChangedEvent.class),
                "GameSessionStateChangedEvent should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(GameSessionStateChangedEvent.class),
                "GameSessionStateChangedEvent should implement Message interface");
    }

    @Test
    void constructorSetsAllFields() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(
                "game-123", GameSessionState.LOBBY, GameSessionState.FLIGHT, players);
        
        assertEquals("game-123", event.getSessionId());
        assertEquals("LOBBY", event.getOldStateName());
        assertEquals("FLIGHT", event.getNewStateName());
        assertEquals(2, event.getPlayersInSession().size());
    }
    
    @Test
    void sameStateTwoPlayers() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(
                "game-123", GameSessionState.LOBBY, GameSessionState.LOBBY, players);
        
        assertEquals("game-123", event.getSessionId());
        assertEquals("LOBBY", event.getOldStateName());
        assertEquals("LOBBY", event.getNewStateName());
        assertEquals(2, event.getPlayersInSession().size());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        List<PlayerInfoDTO> players = Arrays.asList(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, () -> 
                new GameSessionStateChangedEvent(null, GameSessionState.LOBBY, GameSessionState.SHIP_BUILDING, players));
    }
    
    @Test
    void nullNewStateThrowsException() {
        List<PlayerInfoDTO> players = Arrays.asList(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, () -> 
                new GameSessionStateChangedEvent("game-123", GameSessionState.LOBBY, null, players));
    }
    
    @Test
    void nullPlayersListThrowsException() {
        assertThrows(NullPointerException.class, () -> 
                new GameSessionStateChangedEvent("game-123", GameSessionState.LOBBY, GameSessionState.SHIP_BUILDING, null));
    }
    
    @Test
    void playersListIsCopiedInConstructor() {
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Player One"));
        
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(
                "game-123", GameSessionState.LOBBY, GameSessionState.SHIP_BUILDING, originalList);
        
        // Add to original list should not affect event's list
        originalList.add(new PlayerInfoDTO("player2", "Player Two"));
        
        assertEquals(1, event.getPlayersInSession().size(),
                "Event's player list should not be affected by changes to original list");
    }
    
    @Test
    void getPlayersInSessionReturnsCopy() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(
                "game-123", GameSessionState.LOBBY, GameSessionState.SHIP_BUILDING, players);
        
        List<PlayerInfoDTO> returnedList = event.getPlayersInSession();
        
        // Verify original list and returned list are different objects
        assertNotSame(players, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new PlayerInfoDTO("player3", "Player Three"));
        
        // The next call should return a different list with only the original two players
        List<PlayerInfoDTO> secondReturnedList = event.getPlayersInSession();
        assertEquals(2, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(
                "game-123", GameSessionState.LOBBY, GameSessionState.SHIP_BUILDING, players);
        
        String toString = event.toString();
        
        assertTrue(toString.contains("game-123"), "toString should include session ID");
        assertTrue(toString.contains("LOBBY"), "toString should include old state");
        assertTrue(toString.contains("SHIP_BUILDING"), "toString should include new state");
        assertTrue(toString.contains("2"), "toString should include player count");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LobbyStateUpdateEventTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(LobbyStateUpdateEvent.class),
                "LobbyStateUpdateEvent should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(LobbyStateUpdateEvent.class),
                "LobbyStateUpdateEvent should implement Message interface");
    }

    @Test
    void constructorSetsAllFields() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One", true),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        LobbyStateUpdateEvent event = new LobbyStateUpdateEvent("game-123", players);
        
        assertEquals("game-123", event.getGameSessionId());
        assertEquals(2, event.getPlayersInLobby().size());
        assertEquals("Player One", event.getPlayersInLobby().get(0).getNickname());
        assertEquals("Player Two", event.getPlayersInLobby().get(1).getNickname());
    }
    
    @Test
    void nullPlayersListThrowsException() {
        assertThrows(NullPointerException.class, 
                () -> new LobbyStateUpdateEvent("game-123", null));
    }
    
    @Test
    void emptyPlayersListIsValid() {
        List<PlayerInfoDTO> emptyList = new ArrayList<>();
        
        LobbyStateUpdateEvent event = new LobbyStateUpdateEvent("game-123", emptyList);
        
        assertEquals(0, event.getPlayersInLobby().size(), 
                "Players list should be empty but not null");
    }
    
    @Test
    void playersListIsCopiedInConstructor() {
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Player One"));
        
        LobbyStateUpdateEvent event = new LobbyStateUpdateEvent("game-123", originalList);
        
        // Add to original list should not affect event's list
        originalList.add(new PlayerInfoDTO("player2", "Player Two"));
        
        assertEquals(1, event.getPlayersInLobby().size(),
                "Event's player list should not be affected by changes to original list");
    }
    
    @Test
    void getPlayersInLobbyReturnsCopy() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        LobbyStateUpdateEvent event = new LobbyStateUpdateEvent("game-123", players);
        
        List<PlayerInfoDTO> returnedList = event.getPlayersInLobby();
        
        // Verify original list and returned list are different objects
        assertNotSame(players, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new PlayerInfoDTO("player3", "Player Three"));
        
        // The next call should return a different list with only the original two players
        List<PlayerInfoDTO> secondReturnedList = event.getPlayersInLobby();
        assertEquals(2, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        LobbyStateUpdateEvent event = new LobbyStateUpdateEvent("game-123", players);
        
        String toString = event.toString();
        
        assertTrue(toString.contains("game-123"), "toString should include session ID");
        assertTrue(toString.contains("Player One"), "toString should include player information");
        assertTrue(toString.contains("Player Two"), "toString should include player information");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
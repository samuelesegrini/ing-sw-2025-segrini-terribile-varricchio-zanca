package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerJoinedGameSessionNotificationTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(PlayerJoinedGameSessionNotification.class),
                "PlayerJoinedGameSessionNotification should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(PlayerJoinedGameSessionNotification.class),
                "PlayerJoinedGameSessionNotification should implement Message interface");
    }

    @Test
    void constructorSetsAllFields() {
        PlayerInfoDTO newPlayer = new PlayerInfoDTO("player3", "Player Three");
        List<PlayerInfoDTO> allPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two"),
                newPlayer
        );
        
        PlayerJoinedGameSessionNotification notification = new PlayerJoinedGameSessionNotification(
                "game-123", newPlayer, allPlayers);
        
        assertEquals("game-123", notification.getSessionId());
        assertEquals(newPlayer, notification.getJoinedPlayer());
        assertEquals(3, notification.getAllPlayersInSession().size());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        PlayerInfoDTO player = new PlayerInfoDTO("player1", "Player One");
        List<PlayerInfoDTO> players = List.of(player);
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerJoinedGameSessionNotification(null, player, players));
    }
    
    @Test
    void nullJoinedPlayerThrowsException() {
        List<PlayerInfoDTO> players = List.of(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerJoinedGameSessionNotification("game-123", null, players));
    }
    
    @Test
    void nullAllPlayersListThrowsException() {
        PlayerInfoDTO player = new PlayerInfoDTO("player1", "Player One");
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerJoinedGameSessionNotification("game-123", player, null));
    }
    
    @Test
    void allPlayersListIsCopiedInConstructor() {
        PlayerInfoDTO newPlayer = new PlayerInfoDTO("player2", "Player Two");
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Player One"));
        originalList.add(newPlayer);
        
        PlayerJoinedGameSessionNotification notification = new PlayerJoinedGameSessionNotification(
                "game-123", newPlayer, originalList);
        
        // Add to original list should not affect notification's list
        originalList.add(new PlayerInfoDTO("player3", "Player Three"));
        
        assertEquals(2, notification.getAllPlayersInSession().size(),
                "Notification's player list should not be affected by changes to original list");
    }
    
    @Test
    void getAllPlayersInSessionReturnsCopy() {
        PlayerInfoDTO newPlayer = new PlayerInfoDTO("player3", "Player Three");
        List<PlayerInfoDTO> allPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two"),
                newPlayer
        );
        
        PlayerJoinedGameSessionNotification notification = new PlayerJoinedGameSessionNotification(
                "game-123", newPlayer, allPlayers);
        
        List<PlayerInfoDTO> returnedList = notification.getAllPlayersInSession();
        
        // Verify original list and returned list are different objects
        assertNotSame(allPlayers, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new PlayerInfoDTO("player4", "Player Four"));
        
        // The next call should return a different list with only the original three players
        List<PlayerInfoDTO> secondReturnedList = notification.getAllPlayersInSession();
        assertEquals(3, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        PlayerInfoDTO newPlayer = new PlayerInfoDTO("player3", "Player Three");
        List<PlayerInfoDTO> allPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two"),
                newPlayer
        );
        
        PlayerJoinedGameSessionNotification notification = new PlayerJoinedGameSessionNotification(
                "game-123", newPlayer, allPlayers);
        
        String toString = notification.toString();
        
        assertTrue(toString.contains("game-123"), "toString should include session ID");
        assertTrue(toString.contains("Player Three"), "toString should include joined player nickname");
        assertTrue(toString.contains("3"), "toString should include player count");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
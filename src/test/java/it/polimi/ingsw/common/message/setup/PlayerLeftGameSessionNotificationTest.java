package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLeftGameSessionNotificationTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(PlayerLeftGameSessionNotification.class),
                "PlayerLeftGameSessionNotification should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(PlayerLeftGameSessionNotification.class),
                "PlayerLeftGameSessionNotification should implement Message interface");
    }

    @Test
    void constructorSetsAllFields() {
        List<PlayerInfoDTO> remainingPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                "game-123", "player3", "Player Three", remainingPlayers, true);
        
        assertEquals("game-123", notification.getSessionId());
        assertEquals("player3", notification.getLeftPlayerId());
        assertEquals("Player Three", notification.getLeftPlayerNickname());
        assertEquals(2, notification.getRemainingPlayersInSession().size());
        assertTrue(notification.wasHost());
    }
    
    @Test
    void constructorForNonHostLeaver() {
        List<PlayerInfoDTO> remainingPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One", false, true), // The remaining host
                new PlayerInfoDTO("player3", "Player Three")
        );
        
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                "game-123", "player2", "Player Two", remainingPlayers, false);
        
        assertEquals("game-123", notification.getSessionId());
        assertEquals("player2", notification.getLeftPlayerId());
        assertEquals("Player Two", notification.getLeftPlayerNickname());
        assertEquals(2, notification.getRemainingPlayersInSession().size());
        assertFalse(notification.wasHost());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        List<PlayerInfoDTO> players = Arrays.asList(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerLeftGameSessionNotification(null, "player2", "Player Two", players, false));
    }
    
    @Test
    void nullLeftPlayerIdThrowsException() {
        List<PlayerInfoDTO> players = Arrays.asList(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerLeftGameSessionNotification("game-123", null, "Player Two", players, false));
    }
    
    @Test
    void nullLeftPlayerNicknameThrowsException() {
        List<PlayerInfoDTO> players = Arrays.asList(new PlayerInfoDTO("player1", "Player One"));
        
        assertThrows(NullPointerException.class, 
                () -> new PlayerLeftGameSessionNotification("game-123", "player2", null, players, false));
    }
    
    @Test
    void nullRemainingPlayersListThrowsException() {
        assertThrows(NullPointerException.class, 
                () -> new PlayerLeftGameSessionNotification("game-123", "player2", "Player Two", null, false));
    }
    
    @Test
    void remainingPlayersListIsCopiedInConstructor() {
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Player One"));
        
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                "game-123", "player2", "Player Two", originalList, false);
        
        // Add to original list should not affect notification's list
        originalList.add(new PlayerInfoDTO("player3", "Player Three"));
        
        assertEquals(1, notification.getRemainingPlayersInSession().size(),
                "Notification's player list should not be affected by changes to original list");
    }
    
    @Test
    void getRemainingPlayersInSessionReturnsCopy() {
        List<PlayerInfoDTO> remainingPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                "game-123", "player3", "Player Three", remainingPlayers, true);
        
        List<PlayerInfoDTO> returnedList = notification.getRemainingPlayersInSession();
        
        // Verify original list and returned list are different objects
        assertNotSame(remainingPlayers, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible but not affect the internal state
        returnedList.add(new PlayerInfoDTO("player4", "Player Four"));
        
        List<PlayerInfoDTO> secondReturnedList = notification.getRemainingPlayersInSession();
        assertEquals(2, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        List<PlayerInfoDTO> remainingPlayers = Arrays.asList(
                new PlayerInfoDTO("player1", "Player One"),
                new PlayerInfoDTO("player2", "Player Two")
        );
        
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                "game-123", "player3", "Player Three", remainingPlayers, true);
        
        String toString = notification.toString();
        
        assertTrue(toString.contains("game-123"), "toString should include session ID");
        assertTrue(toString.contains("Player Three"), "toString should include left player nickname");
        assertTrue(toString.contains("wasHost=true"), "toString should include host status");
        assertTrue(toString.contains("2"), "toString should include remaining player count");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 
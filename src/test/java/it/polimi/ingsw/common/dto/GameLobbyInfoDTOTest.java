package it.polimi.ingsw.common.dto;

import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class GameLobbyInfoDTOTest {

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(GameLobbyInfoDTO.class),
                "GameLobbyInfoDTO should implement Serializable");
    }

    @Test
    void constructorSetsAllFields() {
        GameLobbyInfoDTO lobbyInfo = new GameLobbyInfoDTO(
                "game-123", "Test Game", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        assertEquals("game-123", lobbyInfo.getSessionId());
        assertEquals("Test Game", lobbyInfo.getGameName());
        assertEquals(2, lobbyInfo.getCurrentPlayerCount());
        assertEquals(4, lobbyInfo.getMaxPlayers());
        assertEquals(GameSessionState.LOBBY, lobbyInfo.getGameSessionState());
        assertEquals(GameLevel.TEST_FLIGHT, lobbyInfo.getGameLevel());
    }
    
    @Test
    void nullSessionIdThrowsException() {
        assertThrows(NullPointerException.class, () -> 
                new GameLobbyInfoDTO(null, "Test Game", 2, 4, 
                        GameSessionState.LOBBY, GameLevel.TEST_FLIGHT));
    }
    
    @Test
    void nullGameNameThrowsException() {
        assertThrows(NullPointerException.class, () -> 
                new GameLobbyInfoDTO("game-123", null, 2, 4, 
                        GameSessionState.LOBBY, GameLevel.TEST_FLIGHT));
    }
    
    @Test
    void nullGameSessionStateThrowsException() {
        assertThrows(NullPointerException.class, () -> 
                new GameLobbyInfoDTO("game-123", "Test Game", 2, 4, 
                        null, GameLevel.TEST_FLIGHT));
    }
    
    @Test
    void nullGameLevelThrowsException() {
        assertThrows(NullPointerException.class, () -> 
                new GameLobbyInfoDTO("game-123", "Test Game", 2, 4, 
                        GameSessionState.LOBBY, null));
    }
    
    @Test
    void equalityBasedOnSessionIdOnly() {
        GameLobbyInfoDTO lobbyInfo1 = new GameLobbyInfoDTO(
                "game-123", "Test Game 1", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        GameLobbyInfoDTO lobbyInfo2 = new GameLobbyInfoDTO(
                "game-123", "Test Game 2", 3, 5,
                GameSessionState.SHIP_BUILDING, GameLevel.LEVEL_II);
        
        GameLobbyInfoDTO lobbyInfo3 = new GameLobbyInfoDTO(
                "game-456", "Test Game 1", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        // Same session ID should be equal despite other fields being different
        assertEquals(lobbyInfo1, lobbyInfo2, "DTOs with same session ID should be equal");
        
        // Different session ID should not be equal even if other fields match
        assertNotEquals(lobbyInfo1, lobbyInfo3, "DTOs with different session IDs should not be equal");
    }
    
    @Test
    void hashCodeBasedOnSessionIdOnly() {
        GameLobbyInfoDTO lobbyInfo1 = new GameLobbyInfoDTO(
                "game-123", "Test Game 1", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        GameLobbyInfoDTO lobbyInfo2 = new GameLobbyInfoDTO(
                "game-123", "Test Game 2", 3, 5,
                GameSessionState.SHIP_BUILDING, GameLevel.LEVEL_II);
        
        GameLobbyInfoDTO lobbyInfo3 = new GameLobbyInfoDTO(
                "game-456", "Test Game 1", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        // Same session ID should produce same hash code
        assertEquals(lobbyInfo1.hashCode(), lobbyInfo2.hashCode(), 
                "DTOs with same session ID should have the same hash code");
        
        // Different session ID likely produces different hash code
        // Note: This is probabilistic but very likely given the hash function
        assertNotEquals(lobbyInfo1.hashCode(), lobbyInfo3.hashCode(), 
                "DTOs with different session IDs should have different hash codes");
    }
    
    @Test
    void toStringContainsAllFields() {
        GameLobbyInfoDTO lobbyInfo = new GameLobbyInfoDTO(
                "game-123", "Test Game", 2, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        String toString = lobbyInfo.toString();
        
        assertTrue(toString.contains("game-123"), "toString should include session ID");
        assertTrue(toString.contains("Test Game"), "toString should include game name");
        assertTrue(toString.contains("2/4"), "toString should include player counts");
        assertTrue(toString.contains("LOBBY"), "toString should include game state");
        assertTrue(toString.contains("TEST_FLIGHT"), "toString should include game level");
    }
} 
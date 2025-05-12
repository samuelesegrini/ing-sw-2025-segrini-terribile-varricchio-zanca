package it.polimi.ingsw.common.dto;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInfoDTOTest {

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(PlayerInfoDTO.class),
                "PlayerInfoDTO should implement Serializable");
    }

    @Test
    void constructorWithPlayerIdAndNickname() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John");
        
        assertEquals("123", player.getPlayerId());
        assertEquals("John", player.getNickname());
        assertFalse(player.isReady());
        assertFalse(player.isHost());
    }
    
    @Test
    void constructorWithPlayerIdNicknameAndReadyStatus() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John", true);
        
        assertEquals("123", player.getPlayerId());
        assertEquals("John", player.getNickname());
        assertTrue(player.isReady());
        assertFalse(player.isHost());
    }
    
    @Test
    void constructorWithAllParameters() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John", true, true);
        
        assertEquals("123", player.getPlayerId());
        assertEquals("John", player.getNickname());
        assertTrue(player.isReady());
        assertTrue(player.isHost());
    }
    
    @Test
    void nullPlayerIdThrowsException() {
        assertThrows(NullPointerException.class, () -> new PlayerInfoDTO(null, "John"));
    }
    
    @Test
    void nullNicknameThrowsException() {
        assertThrows(NullPointerException.class, () -> new PlayerInfoDTO("123", null));
    }
    
    @Test
    void setReadyChangesState() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John");
        assertFalse(player.isReady());
        
        player.setReady(true);
        assertTrue(player.isReady());
        
        player.setReady(false);
        assertFalse(player.isReady());
    }
    
    @Test
    void setHostChangesState() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John");
        assertFalse(player.isHost());
        
        player.setHost(true);
        assertTrue(player.isHost());
        
        player.setHost(false);
        assertFalse(player.isHost());
    }
    
    @Test
    void equalityBasedOnPlayerId() {
        PlayerInfoDTO player1 = new PlayerInfoDTO("123", "John", true, true);
        PlayerInfoDTO player2 = new PlayerInfoDTO("123", "Jane", false, false);
        PlayerInfoDTO player3 = new PlayerInfoDTO("456", "John", true, true);
        
        // Same player ID, different other fields
        assertEquals(player1, player2);
        assertEquals(player1.hashCode(), player2.hashCode());
        
        // Different player ID
        assertNotEquals(player1, player3);
        assertNotEquals(player1.hashCode(), player3.hashCode());
    }
    
    @Test
    void toStringContainsAllFields() {
        PlayerInfoDTO player = new PlayerInfoDTO("123", "John", true, true);
        String toString = player.toString();
        
        assertTrue(toString.contains("123"), "toString should include playerId");
        assertTrue(toString.contains("John"), "toString should include nickname");
        assertTrue(toString.contains("isReady=true"), "toString should include ready status");
        assertTrue(toString.contains("isHost=true"), "toString should include host status");
    }
} 
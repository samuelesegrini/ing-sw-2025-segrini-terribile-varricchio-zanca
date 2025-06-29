package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Test class for PlayerSessionRegistry
 */
public class PlayerSessionRegistryTest {

    private PlayerSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PlayerSessionRegistry();
    }

    @Test
    void testRegisterPlayer_Success() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";

        // When
        boolean result = registry.registerPlayer(clientId, playerId, nickname);

        // Then
        assertTrue(result);
        assertTrue(registry.isPlayerRegistered(clientId));
        assertEquals(playerId, registry.getPlayerIdForClient(clientId));
        assertEquals(clientId, registry.getClientIdForPlayer(playerId));
        assertEquals(nickname, registry.getPlayerNickname(playerId));
        assertTrue(registry.isNicknameInUse(nickname));
    }

    @Test
    void testRegisterPlayer_DuplicateNickname() {
        // Given
        String clientId1 = "client1";
        PlayerId playerId1 = new PlayerId(UUID.randomUUID(), "player1");
        String clientId2 = "client2";
        PlayerId playerId2 = new PlayerId(UUID.randomUUID(), "player2");
        String nickname = "TestPlayer";

        // When
        boolean firstRegistration = registry.registerPlayer(clientId1, playerId1, nickname);
        boolean secondRegistration = registry.registerPlayer(clientId2, playerId2, nickname);

        // Then
        assertTrue(firstRegistration);
        assertFalse(secondRegistration);
        assertTrue(registry.isPlayerRegistered(clientId1));
        assertFalse(registry.isPlayerRegistered(clientId2));
    }

    @Test
    void testUnregisterPlayer() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(clientId, playerId, nickname);

        // When
        registry.unregisterPlayer(clientId);

        // Then
        assertFalse(registry.isPlayerRegistered(clientId));
        assertNull(registry.getPlayerIdForClient(clientId));
        assertNull(registry.getClientIdForPlayer(playerId));
        assertFalse(registry.isNicknameInUse(nickname));

        // Player session should still exist for reconnection
        assertEquals(nickname, registry.getPlayerNickname(playerId));
    }

    @Test
    void testUnregisterPlayer_NonExistent() {
        // Given
        String nonExistentClientId = "nonExistent";

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> registry.unregisterPlayer(nonExistentClientId));
    }

    @Test
    void testValidateReconnection() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(clientId, playerId, nickname);

        // Get the generated token through reflection or assume it exists
        // For this test, we'll test the method behavior

        // When & Then
        // Since we can't access the generated token directly, we test invalid token
        assertFalse(registry.validateReconnection(playerId, "invalidToken"));
        assertFalse(registry.validateReconnection("invalidPlayer", "anyToken"));
    }

    @Test
    void testRestoreSession() {
        // Given
        String oldClientId = "oldClient";
        String newClientId = "newClient";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(oldClientId, playerId, nickname);
        registry.unregisterPlayer(oldClientId);

        // When
        registry.restoreSession(newClientId, playerId);

        // Then
        assertTrue(registry.isPlayerRegistered(newClientId));
        assertEquals(playerId, registry.getPlayerIdForClient(newClientId));
        assertEquals(newClientId, registry.getClientIdForPlayer(playerId));
        assertEquals(nickname, registry.getPlayerNickname(playerId));
    }

    @Test
    void testRestoreSession_NonExistentPlayer() {
        // Given
        String newClientId = "newClient";
        String nonExistentPlayerId = "nonExistent";

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> registry.restoreSession(newClientId, nonExistentPlayerId));
        assertFalse(registry.isPlayerRegistered(newClientId));
    }

    @Test
    void testMultiplePlayers() {
        // Given
        String[] clientIds = {"client1", "client2", "client3"};
        PlayerId[] playerIds = {new PlayerId(UUID.randomUUID(), "player1"), new PlayerId(UUID.randomUUID(), "player2"), new PlayerId(UUID.randomUUID(), "player3") };
        String[] nicknames = {"Alice", "Bob", "Charlie"};

        // When
        for (int i = 0; i < 3; i++) {
            assertTrue(registry.registerPlayer(clientIds[i], playerIds[i], nicknames[i]));
        }

        // Then
        for (int i = 0; i < 3; i++) {
            assertTrue(registry.isPlayerRegistered(clientIds[i]));
            assertEquals(playerIds[i], registry.getPlayerIdForClient(clientIds[i]));
            assertEquals(clientIds[i], registry.getClientIdForPlayer(playerIds[i]));
            assertEquals(nicknames[i], registry.getPlayerNickname(playerIds[i]));
            assertTrue(registry.isNicknameInUse(nicknames[i]));
        }

        assertEquals(3, registry.getAllClientIds().size());
    }

    @Test
    void testEdgeCases() {
        // Test with null values
        assertNull(registry.getPlayerIdForClient(null));
        assertNull(registry.getClientIdForPlayer(null));
        assertNull(registry.getPlayerNickname((PlayerId) null));
        assertNull(registry.getPlayerInfo(null));

        // Test with empty strings
        assertNull(registry.getPlayerIdForClient(""));
        assertNull(registry.getClientIdForPlayer(null));
        assertNull(registry.getPlayerNickname(""));
        assertNull(registry.getPlayerInfo(""));

        // Test nickname check with null
        assertFalse(registry.isNicknameInUse(null));
    }

    @Test
    void testConsistency() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";

        // Register
        assertTrue(registry.registerPlayer(clientId, playerId, nickname));

        // Verify initial state
        assertTrue(registry.isPlayerRegistered(clientId));
        assertTrue(registry.isNicknameInUse(nickname));

        // Unregister
        registry.unregisterPlayer(clientId);

        // Verify after unregistration
        assertFalse(registry.isPlayerRegistered(clientId));
        assertFalse(registry.isNicknameInUse(nickname));

        // Register same nickname again (should work now)
        String newClientId = "client2";
        PlayerId newPlayerId = new PlayerId(UUID.randomUUID(), "player2");;
        assertTrue(registry.registerPlayer(newClientId, newPlayerId, nickname));
        assertTrue(registry.isNicknameInUse(nickname));
    }
    @Test
    @DisplayName("Test registerPlayer with null parameters")
    void testRegisterPlayer_NullParameters() {
        // Test null clientId
        assertFalse(registry.registerPlayer(null, new PlayerId(UUID.randomUUID(), "player1"), "nickname"));

        // Test empty clientId
        assertFalse(registry.registerPlayer("", new PlayerId(UUID.randomUUID(), "player1"), "nickname"));

        // Test null playerId
        assertFalse(registry.registerPlayer("client1", null, "nickname"));

        // Test null nickname
        assertFalse(registry.registerPlayer("client1", new PlayerId(UUID.randomUUID(), "player1"), null));

        // Test empty nickname
        assertFalse(registry.registerPlayer("client1", new PlayerId(UUID.randomUUID(), "player1"), ""));
    }

    @Test
    @DisplayName("Test unregisterPlayer with null/empty clientId")
    void testUnregisterPlayer_NullEmptyClientId() {
        // Should not throw exception with null clientId
        assertDoesNotThrow(() -> registry.unregisterPlayer(null));

        // Should not throw exception with empty clientId
        assertDoesNotThrow(() -> registry.unregisterPlayer(""));
    }

    @Test
    @DisplayName("Test validateReconnection with String overload")
    void testValidateReconnection_StringOverload() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(clientId, playerId, nickname);

        // Test with null/empty parameters
        assertFalse(registry.validateReconnection((String) null, "token"));
        assertFalse(registry.validateReconnection("", "token"));
        assertFalse(registry.validateReconnection(playerId.toString(), null));
        assertFalse(registry.validateReconnection(playerId.toString(), ""));

        // Test with invalid player ID string
        assertFalse(registry.validateReconnection("invalid-player-id", "token"));
    }

    @Test
    @DisplayName("Test restoreSession with String overload")
    void testRestoreSession_StringOverload() {
        // Given
        String oldClientId = "oldClient";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(oldClientId, playerId, nickname);
        registry.unregisterPlayer(oldClientId);

        // Test successful restore with String overload
        String newClientId = "newClient";
        String playerIdString = playerId.toString();

        // Call the String overload method
        assertDoesNotThrow(() -> registry.restoreSession(newClientId, playerIdString));

        assertDoesNotThrow(() -> registry.restoreSession(null, playerIdString));
        assertDoesNotThrow(() -> registry.restoreSession("", playerIdString));
        assertDoesNotThrow(() -> registry.restoreSession("client3", (String) null));
        assertDoesNotThrow(() -> registry.restoreSession("client4", ""));

        // Verify the invalid calls didn't register anything
        assertFalse(registry.isPlayerRegistered("client3"));
        assertFalse(registry.isPlayerRegistered("client4"));
    }

    @Test
    @DisplayName("Test getPlayerNickname with String overload")
    void testGetPlayerNickname_StringOverload() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";
        registry.registerPlayer(clientId, playerId, nickname);

        // Test with null/empty parameters - these should return null
        assertNull(registry.getPlayerNickname((String) null));
        assertNull(registry.getPlayerNickname(""));

        assertNull(registry.getPlayerNickname("invalid-player-id"));

        String playerIdString = playerId.toString();
        String result = registry.getPlayerNickname(playerIdString);

        assertDoesNotThrow(() -> registry.getPlayerNickname(playerIdString));
    }

    @Test
    @DisplayName("Test isNicknameInUse with null/empty parameters")
    void testIsNicknameInUse_NullEmpty() {
        // Test with null nickname
        assertFalse(registry.isNicknameInUse(null));

        // Test with empty nickname
        assertFalse(registry.isNicknameInUse(""));
    }

    @Test
    @DisplayName("Test isPlayerRegistered with null/empty parameters")
    void testIsPlayerRegistered_NullEmpty() {
        // Test with null clientId
        assertFalse(registry.isPlayerRegistered(null));

        // Test with empty clientId
        assertFalse(registry.isPlayerRegistered(""));
    }

    @Test
    @DisplayName("Test getPlayerIdForClient with null/empty parameters")
    void testGetPlayerIdForClient_NullEmpty() {
        // Test with null clientId
        assertNull(registry.getPlayerIdForClient(null));

        // Test with empty clientId
        assertNull(registry.getPlayerIdForClient(""));
    }

    @Test
    @DisplayName("Test getClientIdForPlayer with null parameter")
    void testGetClientIdForPlayer_Null() {
        // Test with null playerId
        assertNull(registry.getClientIdForPlayer(null));
    }

    @Test
    @DisplayName("Test session reconnection flow")
    void testSessionReconnectionFlow() {
        // Given
        String originalClientId = "originalClient";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";

        // Register player
        assertTrue(registry.registerPlayer(originalClientId, playerId, nickname));

        // Simulate disconnection
        registry.unregisterPlayer(originalClientId);

        // Verify player is not registered but session still exists
        assertFalse(registry.isPlayerRegistered(originalClientId));
        assertEquals(nickname, registry.getPlayerNickname(playerId)); // Session should persist

        // Restore session with new client
        String newClientId = "newClient";
        registry.restoreSession(newClientId, playerId);

        // Verify restoration
        assertTrue(registry.isPlayerRegistered(newClientId));
        assertEquals(playerId, registry.getPlayerIdForClient(newClientId));
        assertEquals(newClientId, registry.getClientIdForPlayer(playerId));
        assertEquals(nickname, registry.getPlayerNickname(playerId));

        // Verify old client is no longer mapped
        assertFalse(registry.isPlayerRegistered(originalClientId));
        assertNull(registry.getPlayerIdForClient(originalClientId));
    }

    @Test
    @DisplayName("Test getAllClientIds returns correct size")
    void testGetAllClientIds() {
        // Initially empty
        assertEquals(0, registry.getAllClientIds().size());

        // Add some players
        registry.registerPlayer("client1", new PlayerId(UUID.randomUUID(), "player1"), "Player1");
        registry.registerPlayer("client2", new PlayerId(UUID.randomUUID(), "player2"), "Player2");

        assertEquals(2, registry.getAllClientIds().size());
        assertTrue(registry.getAllClientIds().contains("client1"));
        assertTrue(registry.getAllClientIds().contains("client2"));

        // Remove one player
        registry.unregisterPlayer("client1");
        assertEquals(1, registry.getAllClientIds().size());
        assertFalse(registry.getAllClientIds().contains("client1"));
        assertTrue(registry.getAllClientIds().contains("client2"));
    }

    @Test
    @DisplayName("Test getPlayerInfo returns correct information")
    void testGetPlayerInfo() {
        // Given
        String clientId = "client1";
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player1");
        String nickname = "TestPlayer";

        registry.registerPlayer(clientId, playerId, nickname);

        // When
        Map<String, String> info = registry.getPlayerInfo(clientId);

        // Then
        assertNotNull(info);
        assertEquals(playerId.toString(), info.get("playerId"));
        assertEquals(nickname, info.get("nickname"));
        assertEquals("true", info.get("connected"));

        // Test after unregistration
        registry.unregisterPlayer(clientId);
        assertNull(registry.getPlayerInfo(clientId));
    }

    @Test
    @DisplayName("Test concurrent nickname registration")
    void testConcurrentNicknameRegistration() {
        String nickname = "TestPlayer";

        // Register first player with nickname
        assertTrue(registry.registerPlayer("client1", new PlayerId(UUID.randomUUID(), "player1"), nickname));
        assertTrue(registry.isNicknameInUse(nickname));

        // Try to register second player with same nickname
        assertFalse(registry.registerPlayer("client2", new PlayerId(UUID.randomUUID(), "player2"), nickname));

        // Unregister first player
        registry.unregisterPlayer("client1");
        assertFalse(registry.isNicknameInUse(nickname));

        // Now second registration should work
        assertTrue(registry.registerPlayer("client2", new PlayerId(UUID.randomUUID(), "player2"), nickname));
        assertTrue(registry.isNicknameInUse(nickname));
    }
}
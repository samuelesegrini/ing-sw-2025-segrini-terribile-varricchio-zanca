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
}
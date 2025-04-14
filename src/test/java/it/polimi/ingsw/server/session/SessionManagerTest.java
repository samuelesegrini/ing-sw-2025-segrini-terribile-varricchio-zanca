package it.polimi.ingsw.server.session;

import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager sessionManager;
    private PlayerId playerId;
    private String gameId;

    @BeforeEach
    void setUp() {
        // Initialize the SessionManager and test data before each test
        sessionManager = new SessionManager(playerId -> {}); // Empty callback
        playerId = new PlayerId(UUID.randomUUID(), "Player1");
        gameId = "Game1";
    }

    @Test
    void testRegisterNewSession() {
        // Test the registration of a new session
        SessionInfo session = sessionManager.registerNewSession(playerId, gameId);

        assertNotNull(session, "The session should not be null");
        assertEquals(playerId, session.getPlayerId(), "The player ID should match");
        assertEquals(gameId, session.getGameId(), "The game ID should match");
        assertEquals(FlightStatus.RACING, session.getStatus(), "The initial status should be RACING");
    }

    @Test
    void testValidateReconnectionAttempt() {
        // Register a new session
        SessionInfo session = sessionManager.registerNewSession(playerId, gameId);

        // Simulate disconnection
        sessionManager.registerDisconnection(playerId);

        // Validate reconnection with correct token
        boolean isValid = sessionManager.validateReconnectionAttempt(playerId, session.getSessionToken());
        assertTrue(isValid, "Reconnection should be valid with correct token and ABANDONED status");

        // Validate reconnection with incorrect token
        boolean isInvalidToken = sessionManager.validateReconnectionAttempt(playerId, "InvalidToken");
        assertFalse(isInvalidToken, "Reconnection should be invalid with incorrect token");

        // Simulate reconnection (changing status to RACING)
        sessionManager.registerConnection(playerId);

        // Validate reconnection when status is not ABANDONED
        boolean isInvalidStatus = sessionManager.validateReconnectionAttempt(playerId, session.getSessionToken());
        assertFalse(isInvalidStatus, "Reconnection should be invalid when status is not ABANDONED");
    }

    @Test
    void testValidateSessionToken() {
        // Test the validation of a session token
        SessionInfo session = sessionManager.registerNewSession(playerId, gameId);
        boolean isValid = sessionManager.validateSessionToken(playerId, session.getSessionToken());

        assertTrue(isValid, "The session token should be valid");
    }

    @Test
    void testValidateSessionToken_InvalidToken() {
        // Test the validation of an invalid session token
        sessionManager.registerNewSession(playerId, gameId);
        boolean isValid = sessionManager.validateSessionToken(playerId, "InvalidToken");

        assertFalse(isValid, "The invalid session token should not be accepted");
    }

    @Test
    void testRegisterDisconnection() {
        // Test marking a player as disconnected
        sessionManager.registerNewSession(playerId, gameId);
        sessionManager.registerDisconnection(playerId);

        FlightStatus status = sessionManager.getPlayerStatus(playerId);
        assertEquals(FlightStatus.ABANDONED, status, "The status should be ABANDONED after disconnection");
    }

    @Test
    void testRegisterConnection() {
        // Test marking a player as connected after disconnection
        sessionManager.registerNewSession(playerId, gameId);
        sessionManager.registerDisconnection(playerId);
        sessionManager.registerConnection(playerId);

        FlightStatus status = sessionManager.getPlayerStatus(playerId);
        assertEquals(FlightStatus.RACING, status, "The status should be RACING after reconnection");
    }

    @Test
    void testCheckTimeouts() throws InterruptedException {
        // Test detecting and removing timed-out sessions
        sessionManager.registerNewSession(playerId, gameId);

        // Simulate a timeout
        Thread.sleep(10);
        List<PlayerId> timedOutPlayers = sessionManager.checkTimeouts(5);

        assertTrue(timedOutPlayers.contains(playerId), "The player should be considered timed out");
    }

    @Test
    void testRemovePlayerSession() {
        // Test removing a player's session
        sessionManager.registerNewSession(playerId, gameId);
        sessionManager.removePlayerSession(playerId);

        assertNull(sessionManager.getPlayerStatus(playerId), "The player's session should be removed");
    }

    @Test
    void testPlayerRemovalCallback() {
        // Test triggering the callback when a session is removed
        AtomicBoolean callbackTriggered = new AtomicBoolean(false);
        sessionManager = new SessionManager(playerId -> callbackTriggered.set(true));

        sessionManager.registerNewSession(playerId, gameId);
        sessionManager.removePlayerSession(playerId);

        assertTrue(callbackTriggered.get(), "The callback should be triggered when a session is removed");
    }
}
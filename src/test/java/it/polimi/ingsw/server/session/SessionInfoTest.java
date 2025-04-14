package it.polimi.ingsw.server.session;

import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SessionInfoTest {

    private PlayerId playerId;
    private String gameId;
    private SessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        playerId = new PlayerId(UUID.randomUUID(), "Player1");
        gameId = "Game1";
        sessionInfo = new SessionInfo(playerId, gameId);
    }

    @Test
    void testConstructor() {
        // Verify that the constructor initializes fields correctly
        assertEquals(playerId, sessionInfo.getPlayerId(), "PlayerId should match the one provided");
        assertEquals(gameId, sessionInfo.getGameId(), "GameId should match the one provided");
        assertNotNull(sessionInfo.getSessionToken(), "SessionToken should not be null");
        assertEquals(FlightStatus.RACING, sessionInfo.getStatus(), "Initial status should be RACING");
        assertTrue(sessionInfo.getLastActivity() > 0, "LastActivity should be initialized with a valid timestamp");
    }

    @Test
    void testUpdateLastActivity() throws InterruptedException {
        // Verify that lastActivity is updated correctly
        long initialTimestamp = sessionInfo.getLastActivity();
        Thread.sleep(10); // Ensure a measurable time difference
        sessionInfo.updateLastActivity();
        assertTrue(sessionInfo.getLastActivity() > initialTimestamp, "LastActivity should be updated to a later timestamp");
    }

    @Test
    void testEqualsAndHashCode() {
        // Verify equals and hashCode behavior
        SessionInfo sameSession = new SessionInfo(playerId, gameId);
        assertNotEquals(sessionInfo, sameSession, "Sessions with different tokens should not be equal");
        assertNotEquals(sessionInfo.hashCode(), sameSession.hashCode(), "Hash codes should differ for different tokens");

        SessionInfo identicalSession = new SessionInfo(playerId, gameId);
        identicalSession.setStatus(sessionInfo.getStatus());
        assertNotEquals(sessionInfo, identicalSession, "Sessions with identical fields but different tokens should not be equal");
    }

    @Test
    void testToString() {
        // Verify that toString provides meaningful output
        String toStringOutput = sessionInfo.toString();
        assertTrue(toStringOutput.contains(playerId.toString()), "toString should include PlayerId");
        assertTrue(toStringOutput.contains(gameId), "toString should include GameId");
        assertTrue(toStringOutput.contains(sessionInfo.getStatus().toString()), "toString should include Status");
    }
}
package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.event.PlayerJoinedGameEvent;
import it.polimi.ingsw.common.message.event.PlayerLeftGameEvent;
import it.polimi.ingsw.common.message.event.PlayerReadyChangedEvent;
import it.polimi.ingsw.common.message.event.GameStartedEvent;
import it.polimi.ingsw.common.message.event.BuildingTimerFlippedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

private GameSession gameSession;
private PlayerId creatorId;
private PlayerId player2Id;
private PlayerId player3Id;
private PlayerId player4Id;
private GameConfigurationManager configManager;
private PlayerSessionRegistry playerRegistry;
private TestPropertyChangeListener propertyChangeListener;

@BeforeEach
void setUp() {
creatorId = new PlayerId(UUID.randomUUID(), "creator123");
player2Id = new PlayerId(UUID.randomUUID(), "player2");
player3Id = new PlayerId(UUID.randomUUID(), "player3");
player4Id = new PlayerId(UUID.randomUUID(), "player4");

configManager = createTestConfigManager();
playerRegistry = createTestPlayerRegistry();
propertyChangeListener = new TestPropertyChangeListener();

gameSession = new GameSession(
        "test-game-1",
        "Test Game",
        creatorId,
        4, // maxPlayers
        GameLevel.TEST_FLIGHT,
        configManager,
        playerRegistry
);

gameSession.addPropertyChangeListener(propertyChangeListener);
}

private GameConfigurationManager createTestConfigManager() {
GameConfigurationManager configManager = new GameConfigurationManager();
try {
    configManager.loadAllConfigurations(
            "/json/components.json",
            "/json/adventure_cards.json",
            "/json/game_configurations.json"
    );
} catch (IOException e) {
    throw new RuntimeException("Errore nel caricamento delle configurazioni di test", e);
}
return configManager;
}

private PlayerSessionRegistry createTestPlayerRegistry() {
PlayerSessionRegistry registry = new PlayerSessionRegistry();
registry.registerPlayer("client-creator", creatorId, "creator123");
registry.registerPlayer("client-player2", player2Id, "player2");
registry.registerPlayer("client-player3", player3Id, "player3");
registry.registerPlayer("client-player4", player4Id, "player4");
return registry;
}

private static class TestPropertyChangeListener implements PropertyChangeListener {
private PropertyChangeEvent lastEvent;
private int eventCount = 0;

@Override
public void propertyChange(PropertyChangeEvent evt) {
    this.lastEvent = evt;
    this.eventCount++;
}

public PropertyChangeEvent getLastEvent() {
    return lastEvent;
}

public int getEventCount() {
    return eventCount;
}

public void reset() {
    lastEvent = null;
    eventCount = 0;
}
}

@Nested
@DisplayName("Constructor Tests")
class ConstructorTests {

@Test
@DisplayName("Constructor should create GameSession with creator as first player")
void testConstructor() {
    assertEquals("test-game-1", gameSession.getGameId());
    assertEquals("Test Game", gameSession.getGameName());
    assertEquals(creatorId, gameSession.getCreatorId());
    assertEquals(4, gameSession.getMaxPlayers());
    assertEquals(1, gameSession.getPlayerCount());
    assertEquals(GamePhase.SETUP, gameSession.getCurrentPhase());
    assertFalse(gameSession.isStarted());
    assertFalse(gameSession.isEnded());
    assertTrue(gameSession.canJoin());

    assertTrue(gameSession.getPlayerIds().contains(creatorId));
    assertTrue(gameSession.isCreator(creatorId));

    Player creatorPlayer = gameSession.getPlayer(creatorId);
    assertNotNull(creatorPlayer);
    assertTrue(creatorPlayer.isReady());

    GameModel gameModel = gameSession.getGameModel();
    assertNotNull(gameModel);
    assertEquals("test-game-1", gameModel.getGameId());
    assertEquals("Test Game", gameModel.getGameName());
    assertEquals(GameLevel.TEST_FLIGHT, gameModel.getGameLevel());
}

@Test
@DisplayName("Constructor with different game levels")
void testConstructorWithDifferentLevels() {
    GameSession noviceSession = new GameSession(
            "novice-game", "Novice Game", creatorId, 3,
            GameLevel.LEVEL_II, configManager, playerRegistry
    );

    assertEquals(GameLevel.LEVEL_II, noviceSession.getGameModel().getGameLevel());
    assertEquals(3, noviceSession.getMaxPlayers());

    GameSession advancedSession = new GameSession(
            "advanced-game", "Advanced Game", creatorId, 4,
            GameLevel.LEVEL_II, configManager, playerRegistry
    );

    assertEquals(GameLevel.LEVEL_II, advancedSession.getGameModel().getGameLevel());
    assertEquals(4, advancedSession.getMaxPlayers());
}

@Test
@DisplayName("Constructor should handle creator setup failure gracefully")
void testConstructorWithCreatorSetupFailure() {
    // Test with a creator that might fail to be set as ready
    PlayerId invalidCreator = new PlayerId(UUID.randomUUID(), "invalid");

    // Don't register the invalid creator in the registry
    GameSession sessionWithInvalidCreator = new GameSession(
            "invalid-game", "Invalid Game", invalidCreator, 4,
            GameLevel.TEST_FLIGHT, configManager, playerRegistry
    );

    // Should still create the session
    assertEquals("invalid-game", sessionWithInvalidCreator.getGameId());
    assertEquals(invalidCreator, sessionWithInvalidCreator.getCreatorId());
}
}

@Nested
@DisplayName("Player Management Tests")
class PlayerManagementTests {

    @Test
    @DisplayName("Should add players successfully")
    void testAddPlayer() {
        propertyChangeListener.reset();

        assertTrue(gameSession.addPlayer(player2Id));
        assertEquals(2, gameSession.getPlayerCount());
        assertTrue(gameSession.getPlayerIds().contains(player2Id));
        assertFalse(gameSession.isCreator(player2Id));

        Player player2 = gameSession.getPlayer(player2Id);
        assertNotNull(player2);
        assertFalse(player2.isReady());

        assertEquals(1, propertyChangeListener.getEventCount());
        assertEquals("eventPublished", propertyChangeListener.getLastEvent().getPropertyName());

        // Verify the event is of correct type
        Object eventValue = propertyChangeListener.getLastEvent().getNewValue();
        assertTrue(eventValue instanceof PlayerJoinedGameEvent);
        PlayerJoinedGameEvent joinEvent = (PlayerJoinedGameEvent) eventValue;
        assertEquals(player2Id, joinEvent.getPlayerId());
    }

    @Test
    @DisplayName("Should not add player if game is full")
    void testAddPlayerWhenFull() {
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);
        gameSession.addPlayer(player4Id);
        assertEquals(4, gameSession.getPlayerCount());

        PlayerId player5Id = new PlayerId(UUID.randomUUID(), "player5");
        playerRegistry.registerPlayer("client-player5", player5Id, "player5");
        assertFalse(gameSession.addPlayer(player5Id));
        assertEquals(4, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should not add player if game is started")
    void testAddPlayerWhenStarted() {
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        assertFalse(gameSession.addPlayer(player3Id));
        assertEquals(2, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should remove player successfully")
    void testRemovePlayer() {
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);
        assertEquals(3, gameSession.getPlayerCount());

        propertyChangeListener.reset();
        assertTrue(gameSession.removePlayer(player2Id));
        assertEquals(2, gameSession.getPlayerCount());
        assertFalse(gameSession.getPlayerIds().contains(player2Id));

        Player removedPlayer = gameSession.getPlayer(player2Id);
        assertNull(removedPlayer);

        assertEquals(1, propertyChangeListener.getEventCount());
        assertEquals("eventPublished", propertyChangeListener.getLastEvent().getPropertyName());

        Object eventValue = propertyChangeListener.getLastEvent().getNewValue();
        assertTrue(eventValue instanceof PlayerLeftGameEvent);
    }

    @Test
    @DisplayName("Should not remove non-existent player")
    void testRemoveNonExistentPlayer() {
        assertFalse(gameSession.removePlayer(player2Id));
        assertEquals(1, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should handle removing creator")
    void testRemoveCreator() {
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);
        assertEquals(3, gameSession.getPlayerCount());

        assertTrue(gameSession.removePlayer(creatorId));
        assertEquals(2, gameSession.getPlayerCount());
        assertFalse(gameSession.getPlayerIds().contains(creatorId));

        // Creator should still be the creator ID even if removed
        assertEquals(creatorId, gameSession.getCreatorId());
    }

//        @Test
//        @DisplayName("Should return player components when removed")
//        void testReturnPlayerComponents() {
//            gameSession.addPlayer(player2Id);
//            gameSession.setPlayerReady(player2Id, true);
//            gameSession.startGame();
//
//            // Get a component for the player
//            GameModel gameModel = gameSession.getGameModel();
//            ComponentDeck deck = gameModel.getComponentDeck();
//            Player player = gameSession.getPlayer(player2Id);
//
//            // Simulate player having components
//            deck.draw().ifPresent(player::setHeldComponent);
//
//            // Remove player - should return components
//            assertTrue(gameSession.removePlayer(player2Id));
//
//            // Verify player is removed
//            assertNull(gameSession.getPlayer(player2Id));
//        }
//    }

    @Nested
    @DisplayName("Game End Scenarios")
    class GameEndTests {

        @Test
        @DisplayName("Should end game when all players leave")
        void testEndGameWhenAllPlayersLeave() {
            gameSession.addPlayer(player2Id);
            assertFalse(gameSession.isEnded());

            gameSession.removePlayer(creatorId);
            gameSession.removePlayer(player2Id);

            assertTrue(gameSession.isEnded());
            assertEquals(GamePhase.END, gameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should end game when not enough players during game")
        void testEndGameNotEnoughPlayersAfterStart() {
            gameSession.addPlayer(player2Id);
            gameSession.addPlayer(player3Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.setPlayerReady(player3Id, true);
            gameSession.startGame();

            assertFalse(gameSession.isEnded());
            assertTrue(gameSession.isStarted());

            // Remove players until less than 2 remain
            gameSession.removePlayer(player2Id);
            gameSession.removePlayer(player3Id);

            assertTrue(gameSession.isEnded());
            assertEquals(GamePhase.END, gameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle double game end gracefully")
        void testDoubleGameEnd() throws Exception {
            gameSession.addPlayer(player2Id);

            // Use reflection to call endGame method directly
            Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
            endGameMethod.setAccessible(true);

            // End game first time
            endGameMethod.invoke(gameSession, "Test end");
            assertTrue(gameSession.isEnded());

            // End game second time - should not cause issues
            assertDoesNotThrow(() -> {
                try {
                    endGameMethod.invoke(gameSession, "Test end again");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Nested
    @DisplayName("Ready Status Tests")
    class ReadyStatusTests {

        @Test
        @DisplayName("Should set player ready status")
        void testSetPlayerReady() {
            gameSession.addPlayer(player2Id);
            assertFalse(gameSession.getPlayer(player2Id).isReady());

            propertyChangeListener.reset();

            gameSession.setPlayerReady(player2Id, true);
            assertTrue(gameSession.getPlayer(player2Id).isReady());

            assertEquals(1, propertyChangeListener.getEventCount());
            assertEquals("eventPublished", propertyChangeListener.getLastEvent().getPropertyName());

            Object eventValue = propertyChangeListener.getLastEvent().getNewValue();
            assertTrue(eventValue instanceof PlayerReadyChangedEvent);
            PlayerReadyChangedEvent readyEvent = (PlayerReadyChangedEvent) eventValue;
            assertEquals(player2Id, readyEvent.getPlayerId());
            assertTrue(readyEvent.isReady());
        }

        @Test
        @DisplayName("Creator can change ready status")
        void testCreatorCanChangeReadyStatus() {
            assertTrue(gameSession.getPlayer(creatorId).isReady());

            gameSession.setPlayerReady(creatorId, false);
            assertFalse(gameSession.getPlayer(creatorId).isReady());

            gameSession.setPlayerReady(creatorId, true);
            assertTrue(gameSession.getPlayer(creatorId).isReady());
        }

        @Test
        @DisplayName("Should handle setting ready status for non-existent player")
        void testSetReadyForNonExistentPlayer() {
            PlayerId nonExistentPlayer = new PlayerId(UUID.randomUUID(), "nonexistent");

            // Should not throw exception
            assertDoesNotThrow(() -> {
                gameSession.setPlayerReady(nonExistentPlayer, true);
            });
        }

        @Test
        @DisplayName("Should check if all players are ready")
        void testAreAllPlayersReady() {
            assertTrue(gameSession.areAllPlayersReady());

            gameSession.addPlayer(player2Id);
            assertFalse(gameSession.areAllPlayersReady());

            gameSession.setPlayerReady(player2Id, true);
            assertTrue(gameSession.areAllPlayersReady());

            gameSession.setPlayerReady(creatorId, false);
            assertFalse(gameSession.areAllPlayersReady());
        }

        @Test
        @DisplayName("Should handle all players ready with empty game")
        void testAllPlayersReadyWithEmptyGame() {
            // Remove the creator to test empty game scenario
            gameSession.removePlayer(creatorId);

            // With no players, should not be ready
            assertFalse(gameSession.areAllPlayersReady());
        }
    }

    @Nested
    @DisplayName("Game Start Tests")
    class GameStartTests {

        @Test
        @DisplayName("Should check if game can start")
        void testCanStart() {
            assertFalse(gameSession.canStart());

            gameSession.addPlayer(player2Id);
            assertFalse(gameSession.canStart());

            gameSession.setPlayerReady(player2Id, true);
            assertTrue(gameSession.canStart());

            gameSession.setPlayerReady(creatorId, false);
            assertFalse(gameSession.canStart());
        }

        @Test
        @DisplayName("Should start game successfully")
        void testStartGame() {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);

            propertyChangeListener.reset();

            assertFalse(gameSession.isStarted());
            assertTrue(gameSession.startGame());
            assertTrue(gameSession.isStarted());
            assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());
            assertFalse(gameSession.canJoin());

            assertTrue(propertyChangeListener.getEventCount() > 0);
            assertEquals("eventPublished", propertyChangeListener.getLastEvent().getPropertyName());

            Object eventValue = propertyChangeListener.getLastEvent().getNewValue();
            assertTrue(eventValue instanceof GameStartedEvent);
        }

        @Test
        @DisplayName("Should not start game if conditions not met")
        void testStartGameFails() {
            assertFalse(gameSession.startGame());
            assertFalse(gameSession.isStarted());

            gameSession.addPlayer(player2Id);
            assertFalse(gameSession.startGame());
            assertFalse(gameSession.isStarted());
        }

        @Test
        @DisplayName("Should not start game twice")
        void testStartGameTwice() {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);

            assertTrue(gameSession.startGame());
            assertTrue(gameSession.isStarted());

            // Try to start again
            assertFalse(gameSession.startGame());
        }

        @Test
        @DisplayName("Should not start game if already ended")
        void testStartGameWhenEnded() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);

            // Use reflection to end the game
            Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
            endGameMethod.setAccessible(true);
            endGameMethod.invoke(gameSession, "Test end");

            assertFalse(gameSession.startGame());
            assertFalse(gameSession.isStarted());
        }
    }

    @Nested
    @DisplayName("Phase Management Tests")
    class PhaseManagementTests {

        @Test
        @DisplayName("Should transition to building phase")
        void testTransitionToBuildingPhase() {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle phase transition to flight")
        void testTransitionToFlightPhase() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Use reflection to call transitionToPhase
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

            assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle phase transition to end")
        void testTransitionToEndPhase() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.END);

            assertEquals(GamePhase.END, gameSession.getCurrentPhase());
            assertTrue(gameSession.isEnded());
        }

        @Test
        @DisplayName("Should handle building phase start")
        void testStartBuildingPhase() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Building phase should start automatically
            assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

            // Test that phase start time is set
            Field phaseStartTimeField = GameSession.class.getDeclaredField("phaseStartTime");
            phaseStartTimeField.setAccessible(true);
            long phaseStartTime = (long) phaseStartTimeField.get(gameSession);
            assertTrue(phaseStartTime > 0);
        }

        @Test
        @DisplayName("Should handle flight phase start")
        void testStartFlightPhase() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Transition to flight phase
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

            assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());
        }
    }

    @Nested
    @DisplayName("Timer System Tests")
    class TimerSystemTests {

        @Test
        @DisplayName("Should support timer system for non-TEST_FLIGHT levels")
        void testTimerSupportedForNonTestFlightLevels() {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            assertEquals(GamePhase.BUILDING, timerGameSession.getCurrentPhase());
            assertEquals(GameLevel.LEVEL_II, timerGameSession.getGameModel().getGameLevel());

            BuildingTimer timer = timerGameSession.getGameModel().getBuildingTimer();
            assertNotNull(timer, "Timer should be available for LEVEL_II");
        }

        @Test
        @DisplayName("Should check timer system support correctly")
        void testTimerSystemSupport() throws Exception {
            Method supportsTimerMethod = GameSession.class.getDeclaredMethod("supportsTimerSystem", GameLevel.class);
            supportsTimerMethod.setAccessible(true);

            assertFalse((Boolean) supportsTimerMethod.invoke(gameSession, GameLevel.TEST_FLIGHT));
            assertTrue((Boolean) supportsTimerMethod.invoke(gameSession, GameLevel.LEVEL_II));
        }

        @Test
        @DisplayName("Should only allow timer flipping during BUILDING phase")
        void testTimerFlippingOnlyDuringBuildingPhase() {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);

            assertEquals(GamePhase.SETUP, timerGameSession.getCurrentPhase());
            assertThrows(IllegalStateException.class, () -> {
                timerGameSession.flipBuildingTimer(creatorId.getNickname(), true);
            });

            timerGameSession.startGame();
            assertEquals(GamePhase.BUILDING, timerGameSession.getCurrentPhase());

            assertDoesNotThrow(() -> {
                timerGameSession.flipBuildingTimer(creatorId.getNickname(), true);
            });
        }

        @Test
        @DisplayName("Should handle timer events correctly")
        void testTimerEventHandling() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            Method handleTimerEventMethod = GameSession.class.getDeclaredMethod("handleTimerEvent",
                    BuildingTimer.TimerEvent.class, String.class, long.class);
            handleTimerEventMethod.setAccessible(true);

            // Test BUILDING_ENDED event
            assertDoesNotThrow(() -> {
                handleTimerEventMethod.invoke(timerGameSession,
                        BuildingTimer.TimerEvent.BUILDING_ENDED, creatorId.getNickname(), 0L);
            });

            // Should transition to FLIGHT phase
            assertEquals(GamePhase.FLIGHT, timerGameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle timer expiration events")
        void testTimerExpirationEvents() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            Method broadcastTimerExpiredMethod = GameSession.class.getDeclaredMethod("broadcastTimerExpired",
                    BuildingTimer.TimerEvent.class, long.class);
            broadcastTimerExpiredMethod.setAccessible(true);

            // Should not throw exceptions
            assertDoesNotThrow(() -> {
                broadcastTimerExpiredMethod.invoke(timerGameSession,
                        BuildingTimer.TimerEvent.FIRST_TIMER_EXPIRED, 30000L);
            });

            assertDoesNotThrow(() -> {
                broadcastTimerExpiredMethod.invoke(timerGameSession,
                        BuildingTimer.TimerEvent.SECOND_TIMER_EXPIRED, 0L);
            });
        }

        @Test
        @DisplayName("Should end building phase correctly")
        void testEndBuildingPhase() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            assertEquals(GamePhase.BUILDING, timerGameSession.getCurrentPhase());

            Method endBuildingPhaseMethod = GameSession.class.getDeclaredMethod("endBuildingPhase");
            endBuildingPhaseMethod.setAccessible(true);
            endBuildingPhaseMethod.invoke(timerGameSession);

            assertEquals(GamePhase.FLIGHT, timerGameSession.getCurrentPhase());
        }

        @Test
        @DisplayName("Should validate timer flip requirements")
        void testTimerFlipValidation() {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            // Test flipping with valid player
            BuildingTimer.FlipResult result = timerGameSession.flipBuildingTimer(creatorId.getNickname(), true);
            assertNotNull(result);

            // Test flipping with invalid player
            BuildingTimer.FlipResult invalidResult = timerGameSession.flipBuildingTimer("nonexistent", true);
            assertNotNull(invalidResult);
        }
    }

    @Nested
    @DisplayName("Property Change Support Tests")
    class PropertyChangeSupportTests {

        @Test
        @DisplayName("Should add and remove property change listeners")
        void testPropertyChangeListenerManagement() {
            TestPropertyChangeListener listener1 = new TestPropertyChangeListener();
            TestPropertyChangeListener listener2 = new TestPropertyChangeListener();

            gameSession.addPropertyChangeListener(listener1);
            gameSession.addPropertyChangeListener(listener2);

            // Trigger an event
            gameSession.addPlayer(player2Id);

            assertEquals(1, listener1.getEventCount());
            assertEquals(1, listener2.getEventCount());

            // Remove one listener
            gameSession.removePropertyChangeListener(listener1);

            // Trigger another event
            gameSession.addPlayer(player3Id);

            // listener1 should not receive the new event
            assertEquals(1, listener1.getEventCount());
            assertEquals(2, listener2.getEventCount());
        }

        @Test
        @DisplayName("Should fire property change events manually")
        void testManualPropertyChangeEvent() {
            TestPropertyChangeListener listener = new TestPropertyChangeListener();
            gameSession.addPropertyChangeListener(listener);

            gameSession.firePropertyChange("testProperty", "oldValue", "newValue");

            assertEquals(1, listener.getEventCount());
            assertEquals("testProperty", listener.getLastEvent().getPropertyName());
            assertEquals("oldValue", listener.getLastEvent().getOldValue());
            assertEquals("newValue", listener.getLastEvent().getNewValue());
        }

        @Nested
        @DisplayName("Ship Validation Tests")
        class ShipValidationTests {

            @Test
            @DisplayName("Should check if player ship is finished")
            void testIsPlayerShipFinished() {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Test with ready player
                gameSession.setPlayerReady(player2Id, true);
                assertTrue(gameSession.isPlayerShipFinished(player2Id));

                // Test with non-ready player
                gameSession.setPlayerReady(player2Id, false);
                // Result depends on ship configuration - test both cases
                boolean result = gameSession.isPlayerShipFinished(player2Id);
                // Should be false for non-ready player with incomplete ship
                assertFalse(result);
            }

            @Test
            @DisplayName("Should handle ship finished check for non-existent player")
            void testIsPlayerShipFinishedNonExistentPlayer() {
                PlayerId nonExistentPlayer = new PlayerId(UUID.randomUUID(), "nonexistent");
                assertFalse(gameSession.isPlayerShipFinished(nonExistentPlayer));
            }

            @Test
            @DisplayName("Should validate all ships during phase transition")
            void testValidateAllShips() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Use reflection to call validateAllShips
                Method validateMethod = GameSession.class.getDeclaredMethod("validateAllShips");
                validateMethod.setAccessible(true);

                // Should not throw exception
                assertDoesNotThrow(() -> {
                    try {
                        validateMethod.invoke(gameSession);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        @Nested
        @DisplayName("Player Order Management Tests")
        class PlayerOrderTests {

            @Test
            @DisplayName("Should update player order for flight phase")
            void testUpdatePlayerOrder() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Transition to flight phase
                Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
                transitionMethod.setAccessible(true);
                transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

                // Use reflection to call updatePlayerOrder
                Method updateOrderMethod = GameSession.class.getDeclaredMethod("updatePlayerOrder");
                updateOrderMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        updateOrderMethod.invoke(gameSession);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        @Nested
        @DisplayName("Component Management Tests")
        class ComponentManagementTests {

            @Test
            @DisplayName("Should return player components when player leaves")
            void testReturnPlayerComponentsDetailed() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Use reflection to call returnPlayerComponents
                Method returnComponentsMethod = GameSession.class.getDeclaredMethod("returnPlayerComponents", PlayerId.class);
                returnComponentsMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        returnComponentsMethod.invoke(gameSession, player2Id);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Test
            @DisplayName("Should handle returning components for non-existent player")
            void testReturnPlayerComponentsNonExistentPlayer() throws Exception {
                PlayerId nonExistentPlayer = new PlayerId(UUID.randomUUID(), "nonexistent");

                Method returnComponentsMethod = GameSession.class.getDeclaredMethod("returnPlayerComponents", PlayerId.class);
                returnComponentsMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        returnComponentsMethod.invoke(gameSession, nonExistentPlayer);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        @Nested
        @DisplayName("Adventure Card Controller Tests")
        class AdventureCardControllerTests {

            @Test
            @DisplayName("Should initialize adventure card controller")
            void testAdventureCardControllerInitialization() {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Initially should be null
                assertNull(gameSession.getAdventureCardController());

                // After transitioning to flight, it should remain null until properly initialized
                // (based on the commented code in startFlightPhase)
            }

            @Test
            @DisplayName("Should handle adventure card controller cleanup")
            void testAdventureCardControllerCleanup() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Create a real adventure card controller instead of mock
                GameModel gameModel = gameSession.getGameModel();
                AdventureCardController realController = new AdventureCardController();

                // Set the controller using reflection
                Field controllerField = GameSession.class.getDeclaredField("adventureCardController");
                controllerField.setAccessible(true);
                controllerField.set(gameSession, realController);

                // Verify controller is set
                assertNotNull(gameSession.getAdventureCardController());

                // End the game
                Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
                endGameMethod.setAccessible(true);
                endGameMethod.invoke(gameSession, "Test cleanup");

                // Verify cleanup was called and controller is null
                assertNull(gameSession.getAdventureCardController());
            }
        }

        @Nested
        @DisplayName("Flight Phase Tests")
        class FlightPhaseTests {

            @Test
            @DisplayName("Should transition to flight phase correctly")
            void testTransitionToFlightPhase() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Verify we start in BUILDING phase
                assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

                // Use reflection to call transitionToPhase with FLIGHT
                Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
                transitionMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        transitionMethod.invoke(gameSession, GamePhase.FLIGHT);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                // Verify phase is set to FLIGHT
                assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());
            }

            @Test
            @DisplayName("Should initialize flight phase components when startFlightPhase is called")
            void testStartFlightPhaseInitialization() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Verify we start in BUILDING phase
                assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

                // Get initial state for comparison
                GameModel gameModel = gameSession.getGameModel();

                // Use reflection to call startFlightPhase directly
                Method startFlightMethod = GameSession.class.getDeclaredMethod("startFlightPhase");
                startFlightMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        startFlightMethod.invoke(gameSession);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                // Verify that startFlightPhase doesn't change the current phase
                // (this is the correct behavior - only transitionToPhase should change the phase)
                assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

                // Verify that the flight board was initialized
                // The adventure deck should have started the flight phase
                assertNotNull(gameModel.getAdventureDeck());

                // You can add more specific assertions based on what startFlightPhase should initialize
                // For example, if it initializes player order:
                assertNotNull(gameModel.getFlightBoard());
            }

            @Test
            @DisplayName("Should complete full transition from building to flight")
            void testCompleteBuildingToFlightTransition() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Verify we start in BUILDING phase
                assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

                // Use endBuildingPhase to trigger natural transition
                Method endBuildingMethod = GameSession.class.getDeclaredMethod("endBuildingPhase");
                endBuildingMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        endBuildingMethod.invoke(gameSession);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                // Verify complete transition to FLIGHT
                assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());
            }

            @Test
            @DisplayName("Should handle transition to END phase")
            void testTransitionToEndPhase() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                // Use reflection to call transitionToPhase with END
                Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
                transitionMethod.setAccessible(true);

                assertDoesNotThrow(() -> {
                    try {
                        transitionMethod.invoke(gameSession, GamePhase.END);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                // Verify phase is set to END and game is ended
                assertEquals(GamePhase.END, gameSession.getCurrentPhase());
                assertTrue(gameSession.isEnded());
            }

            @Test
            @DisplayName("Should test all branches of transitionToPhase switch statement")
            void testAllTransitionToPhasesBranches() throws Exception {
                gameSession.addPlayer(player2Id);
                gameSession.setPlayerReady(player2Id, true);
                gameSession.startGame();

                Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
                transitionMethod.setAccessible(true);

                // Test BUILDING transition (already in building, but should work)
                assertDoesNotThrow(() -> {
                    try {
                        transitionMethod.invoke(gameSession, GamePhase.BUILDING);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

                // Test FLIGHT transition
                assertDoesNotThrow(() -> {
                    try {
                        transitionMethod.invoke(gameSession, GamePhase.FLIGHT);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());

                // Test END transition
                assertDoesNotThrow(() -> {
                    try {
                        transitionMethod.invoke(gameSession, GamePhase.END);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                assertEquals(GamePhase.END, gameSession.getCurrentPhase());
                assertTrue(gameSession.isEnded());
            }
        }

        @Test
        @DisplayName("Should handle flight phase initialization")
        void testFlightPhaseInitialization() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Transition to flight phase
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

            assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());

            // Verify phase start time is updated
            Field phaseStartTimeField = GameSession.class.getDeclaredField("phaseStartTime");
            phaseStartTimeField.setAccessible(true);
            long phaseStartTime = (long) phaseStartTimeField.get(gameSession);
            assertTrue(phaseStartTime > 0);
        }
    }

    @Nested
    @DisplayName("Timer Cancellation Tests")
    class TimerCancellationTests {

        @Test
        @DisplayName("Should cancel existing timer when transitioning phases")
        void testTimerCancellation() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Create a real ScheduledFuture for testing
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> testTimer = executor.schedule(() -> {
            }, 1, TimeUnit.HOURS);

            // Set the timer using reflection
            Field timerField = GameSession.class.getDeclaredField("phaseTimer");
            timerField.setAccessible(true);
            timerField.set(gameSession, testTimer);

            assertFalse(testTimer.isCancelled());

            // Transition to flight phase
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

            // Verify timer was cancelled
            assertTrue(testTimer.isCancelled());

            executor.shutdown();
        }

        @Test
        @DisplayName("Should handle timer cancellation during game end")
        void testTimerCancellationOnGameEnd() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Create a real ScheduledFuture for testing
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> testTimer = executor.schedule(() -> {
            }, 1, TimeUnit.HOURS);

            // Set the timer using reflection
            Field timerField = GameSession.class.getDeclaredField("phaseTimer");
            timerField.setAccessible(true);
            timerField.set(gameSession, testTimer);

            assertFalse(testTimer.isCancelled());

            // End the game
            Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
            endGameMethod.setAccessible(true);
            endGameMethod.invoke(gameSession, "Test end");

            // Verify timer was cancelled
            assertTrue(testTimer.isCancelled());

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent player addition")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void testConcurrentPlayerAddition() throws InterruptedException {
            int numThreads = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            AtomicInteger successCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        PlayerId playerId = new PlayerId(UUID.randomUUID(), "player" + threadId);
                        playerRegistry.registerPlayer("client-" + threadId, playerId, "player" + threadId);

                        if (gameSession.addPlayer(playerId)) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await();
            executor.shutdown();

            // Should only allow up to maxPlayers-1 additional players (creator already added)
            assertTrue(successCount.get() <= 3);
            assertTrue(gameSession.getPlayerCount() <= 4);
        }

        @Test
        @DisplayName("Should handle concurrent ready status changes")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void testConcurrentReadyStatusChanges() throws InterruptedException {
            gameSession.addPlayer(player2Id);
            gameSession.addPlayer(player3Id);

            int numThreads = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            for (int i = 0; i < numThreads; i++) {
                final boolean ready = i % 2 == 0;
                final PlayerId playerId = (i % 3 == 0) ? creatorId : ((i % 3 == 1) ? player2Id : player3Id);

                executor.submit(() -> {
                    try {
                        startLatch.await();
                        gameSession.setPlayerReady(playerId, ready);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await();
            executor.shutdown();

            // Should not crash or cause inconsistent state
            assertFalse(gameSession.isEnded());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty player list scenarios")
        void testEmptyPlayerListScenarios() {
            // Remove creator to create empty game
            gameSession.removePlayer(creatorId);

            assertEquals(0, gameSession.getPlayerCount());
            assertTrue(gameSession.isEnded());
            assertFalse(gameSession.areAllPlayersReady());
            assertFalse(gameSession.canStart());
        }

        @Test
        @DisplayName("Should handle game state after multiple transitions")
        void testMultiplePhaseTransitions() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());

            // Transition through all phases
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);

            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);
            assertEquals(GamePhase.FLIGHT, gameSession.getCurrentPhase());

            transitionMethod.invoke(gameSession, GamePhase.END);
            assertEquals(GamePhase.END, gameSession.getCurrentPhase());
            assertTrue(gameSession.isEnded());
        }

//            @Test
//            @DisplayName("Should handle operations on ended game")
//            void testOperationsOnEndedGame() throws Exception {
//                gameSession.addPlayer(player2Id);
//
//                // End the game
//                Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
//                endGameMethod.setAccessible(true);
//                endGameMethod.invoke(gameSession, "Test end");
//
//                assertTrue(gameSession.isEnded());
//                assertFalse(gameSession.canJoin());
//                assertFalse(gameSession.canStart());
//                assertFalse(gameSession.startGame());
//                assertFalse(gameSession.addPlayer(player3Id));
//            }

        @Test
        @DisplayName("Should handle null or invalid player operations")
        void testNullPlayerOperations() {
            assertNull(gameSession.getPlayer(null));

            PlayerId invalidPlayer = new PlayerId(UUID.randomUUID(), "invalid");
            assertNull(gameSession.getPlayer(invalidPlayer));
            assertFalse(gameSession.removePlayer(invalidPlayer));
            assertFalse(gameSession.isCreator(invalidPlayer));
            assertFalse(gameSession.isCreator(null));
        }

        @Test
        @DisplayName("Should handle phase start time tracking")
        void testPhaseStartTimeTracking() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);

            long beforeStart = System.currentTimeMillis();
            gameSession.startGame();
            long afterStart = System.currentTimeMillis();

            Field phaseStartTimeField = GameSession.class.getDeclaredField("phaseStartTime");
            phaseStartTimeField.setAccessible(true);
            long phaseStartTime = (long) phaseStartTimeField.get(gameSession);

            assertTrue(phaseStartTime >= beforeStart);
            assertTrue(phaseStartTime <= afterStart);
        }
    }

    @Nested
    @DisplayName("Timer System Advanced Tests")
    class TimerSystemAdvancedTests {

        @Test
        @DisplayName("Should handle timer flip during different phases")
        void testTimerFlipDifferentPhases() {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);

            // Should fail in SETUP phase
            assertThrows(IllegalStateException.class, () -> {
                timerGameSession.flipBuildingTimer(creatorId.getNickname(), true);
            });

            timerGameSession.startGame();

            // Should work in BUILDING phase
            assertDoesNotThrow(() -> {
                BuildingTimer.FlipResult result = timerGameSession.flipBuildingTimer(creatorId.getNickname(), true);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Should handle timer shutdown during game end")
        void testTimerShutdownOnGameEnd() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            // Verify timer exists
            BuildingTimer timer = timerGameSession.getGameModel().getBuildingTimer();
            assertNotNull(timer);

            // End the game
            Method endGameMethod = GameSession.class.getDeclaredMethod("endGame", String.class);
            endGameMethod.setAccessible(true);
            endGameMethod.invoke(timerGameSession, "Test shutdown");

            assertTrue(timerGameSession.isEnded());
        }

        @Test
        @DisplayName("Should handle all timer event types")
        void testAllTimerEventTypes() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            timerGameSession.addPlayer(player2Id);
            timerGameSession.setPlayerReady(player2Id, true);
            timerGameSession.startGame();

            Method handleTimerEventMethod = GameSession.class.getDeclaredMethod("handleTimerEvent",
                    BuildingTimer.TimerEvent.class, String.class, long.class);
            handleTimerEventMethod.setAccessible(true);

            // Test all timer event types
            for (BuildingTimer.TimerEvent event : BuildingTimer.TimerEvent.values()) {
                assertDoesNotThrow(() -> {
                    try {
                        handleTimerEventMethod.invoke(timerGameSession, event, creatorId.getNickname(), 30000L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        @Test
        @DisplayName("Should handle timer expiration broadcast")
        void testTimerExpirationBroadcast() throws Exception {
            GameSession timerGameSession = new GameSession(
                    "timer-game", "Timer Test Game", creatorId, 4,
                    GameLevel.LEVEL_II, configManager, playerRegistry
            );

            Method broadcastMethod = GameSession.class.getDeclaredMethod("broadcastTimerExpired",
                    BuildingTimer.TimerEvent.class, long.class);
            broadcastMethod.setAccessible(true);

            // Test all expiration events
            assertDoesNotThrow(() -> {
                try {
                    broadcastMethod.invoke(timerGameSession, BuildingTimer.TimerEvent.FIRST_TIMER_EXPIRED, 30000L);
                    broadcastMethod.invoke(timerGameSession, BuildingTimer.TimerEvent.SECOND_TIMER_EXPIRED, 0L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Nested
    @DisplayName("Reflection and Internal State Tests")
    class ReflectionTests {

        @Test
        @DisplayName("Should handle current player index management")
        void testCurrentPlayerIndex() throws Exception {
            Field currentPlayerIndexField = GameSession.class.getDeclaredField("currentPlayerIndex");
            currentPlayerIndexField.setAccessible(true);

            int initialIndex = (int) currentPlayerIndexField.get(gameSession);
            assertEquals(0, initialIndex);

            // Test setting different values
            currentPlayerIndexField.set(gameSession, 1);
            assertEquals(1, (int) currentPlayerIndexField.get(gameSession));
        }

        @Test
        @DisplayName("Should handle lock object access")
        void testLockObject() throws Exception {
            Field lockField = GameSession.class.getDeclaredField("lock");
            lockField.setAccessible(true);

            Object lock = lockField.get(gameSession);
            assertNotNull(lock);

            // Test synchronization
            synchronized (lock) {
                assertTrue(Thread.holdsLock(lock));
            }
        }

        @Test
        @DisplayName("Should handle volatile field access")
        void testVolatileFields() throws Exception {
            Field startedField = GameSession.class.getDeclaredField("started");
            Field endedField = GameSession.class.getDeclaredField("ended");
            Field currentPhaseField = GameSession.class.getDeclaredField("currentPhase");

            startedField.setAccessible(true);
            endedField.setAccessible(true);
            currentPhaseField.setAccessible(true);

            // Test initial values
            assertFalse((Boolean) startedField.get(gameSession));
            assertFalse((Boolean) endedField.get(gameSession));
            assertEquals(GamePhase.SETUP, currentPhaseField.get(gameSession));
        }
    }

    @Nested
    @DisplayName("Logging and Debug Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should handle logging during various operations")
        void testLoggingOperations() {
            // Test logging during player operations
            gameSession.addPlayer(player2Id);
            gameSession.removePlayer(player2Id);

            // Test logging during game operations
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // All operations should complete without logging errors
            assertTrue(gameSession.isStarted());
        }

        @Test
        @DisplayName("Should handle debug output for non-existent players")
        void testDebugOutputForNonExistentPlayers() {
            // This tests the debug output in getPlayer method
            PlayerId nonExistentPlayer = new PlayerId(UUID.randomUUID(), "nonexistent");
            Player result = gameSession.getPlayer(nonExistentPlayer);

            assertNull(result);
            // Debug output should be triggered internally
        }
    }

    @Nested
    @DisplayName("Game Model Integration Tests")
    class GameModelIntegrationTests {

        @Test
        @DisplayName("Should handle game model initialization correctly")
        void testGameModelInitialization() {
            GameModel model = gameSession.getGameModel();

            assertNotNull(model);
            assertEquals(gameSession.getGameId(), model.getGameId());
            assertEquals(gameSession.getGameName(), model.getGameName());
            assertEquals(4, model.getMaxPlayers());

            // Test component and adventure decks
            assertNotNull(model.getComponentDeck());
            assertNotNull(model.getAdventureDeck());
        }

        @Test
        @DisplayName("Should handle game model state changes")
        void testGameModelStateChanges() {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            GameModel model = gameSession.getGameModel();

            // Verify game model reflects session state
            assertEquals(GamePhase.BUILDING, model.getCurrentPhase());
            assertEquals(2, model.getPlayers().size());
        }

        @Test
        @DisplayName("Should handle flight board initialization")
        void testFlightBoardInitialization() throws Exception {
            gameSession.addPlayer(player2Id);
            gameSession.setPlayerReady(player2Id, true);
            gameSession.startGame();

            // Transition to flight phase
            Method transitionMethod = GameSession.class.getDeclaredMethod("transitionToPhase", GamePhase.class);
            transitionMethod.setAccessible(true);
            transitionMethod.invoke(gameSession, GamePhase.FLIGHT);

            GameModel model = gameSession.getGameModel();
            assertNotNull(model.getFlightBoard());
        }
    }
}
}
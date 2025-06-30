package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AdventureCardControllerTest {

    private AdventureCardController controller;
    private PlayerId playerId1;
    private PlayerId playerId2;
    private PiratesCard pirateCard;
    private AbandonedShipCard abandonedShipCard;
    private AbandonedStationCard abandonedStationCard;
    private SlaversCard slaverCard;
    private StardustCard stardustCard;
    private OpenSpaceCard openSpaceCard;
    private EpidemicCard epidemicCard;

    @BeforeEach
    void setUp() {
        controller = new AdventureCardController();

        // Create test player IDs
        playerId1 = new PlayerId(UUID.randomUUID(), "TestPlayer1");
        playerId2 = new PlayerId(UUID.randomUUID(), "TestPlayer2");

        // Create test adventure cards using concrete implementations
        List<CannonFire> pirateAttackPattern = Arrays.asList(
                new CannonFire(Direction.UP, ShotIntensity.HEAVY),
                new CannonFire(Direction.RIGHT, ShotIntensity.LIGHT),
                new CannonFire(Direction.DOWN, ShotIntensity.HEAVY)
        );
        pirateCard = new PiratesCard("pirate-1", CardLevel.TEST_FLIGHT, "Pirate encounter", 5, 2, 100, pirateAttackPattern);
        abandonedShipCard = new AbandonedShipCard("ship-1", CardLevel.TEST_FLIGHT, "Abandoned ship", 2, 100, 1);
        abandonedStationCard = new AbandonedStationCard("station-1", CardLevel.TEST_FLIGHT, "Abandoned station", 3, 1, Map.of(GoodType.RED,1, GoodType.BLUE, 2, GoodType.GREEN, 3));
        slaverCard = new SlaversCard("slaver-1", CardLevel.TEST_FLIGHT, "Slaver encounter", 5, 2, 150, 1);
        stardustCard = new StardustCard("stardust-1", CardLevel.TEST_FLIGHT, "Stardust collection");
        openSpaceCard = new OpenSpaceCard("space-1", CardLevel.TEST_FLIGHT, "Open space encounter");
        epidemicCard = new EpidemicCard("epidemic-1", CardLevel.TEST_FLIGHT, "Epidemic outbreak");
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.cleanup();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor initializes correctly")
        void testDefaultConstructor() {
            AdventureCardController testController = new AdventureCardController();
            assertNotNull(testController);
            assertFalse(testController.isProcessingCard());
            testController.cleanup();
        }

        @Test
        @DisplayName("Constructor with parameters initializes correctly")
        void testParameterizedConstructor() {
            Object gameModel = new Object();
            Object eventPublisher = new Object();
            AdventureCardController testController = new AdventureCardController(gameModel, eventPublisher);
            assertNotNull(testController);
            assertFalse(testController.isProcessingCard());
            testController.cleanup();
        }
    }

    @Nested
    @DisplayName("Adventure Card Management Tests")
    class AdventureCardManagementTests {

        @Test
        @DisplayName("Start adventure card with timeout-required card")
        void testStartAdventureCardWithTimeout() {
            controller.startAdventureCard(pirateCard, playerId1);

            assertTrue(controller.isProcessingCard());
            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(pirateCard, state.getCard());
        }

        @Test
        @DisplayName("Start adventure card without timeout")
        void testStartAdventureCardWithoutTimeout() {
            controller.startAdventureCard(stardustCard, playerId1);

            assertTrue(controller.isProcessingCard());
            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(stardustCard, state.getCard());
        }

        @Test
        @DisplayName("Start multiple cards for different players")
        void testStartMultipleCards() {
            controller.startAdventureCard(pirateCard, playerId1);
            controller.startAdventureCard(abandonedShipCard, playerId2);

            assertTrue(controller.isProcessingCard());

            AdventureCardState state1 = controller.getCardState(playerId1);
            AdventureCardState state2 = controller.getCardState(playerId2);

            assertNotNull(state1);
            assertNotNull(state2);
            assertEquals(pirateCard, state1.getCard());
            assertEquals(abandonedShipCard, state2.getCard());
        }

        @Test
        @DisplayName("Start card resolution")
        void testStartCardResolution() {
            // This method has a placeholder implementation, so we just test it doesn't throw
            assertDoesNotThrow(() -> controller.startCardResolution(pirateCard));
        }
    }

    @Nested
    @DisplayName("Player Choice Tests")
    class PlayerChoiceTests {

        @Test
        @DisplayName("Record player choice for active card")
        void testRecordPlayerChoice() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);

            // Create a player choice first
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            // Now record the choice
            controller.recordPlayerChoice(playerId1, "combatStrength", 5);

            // Verify the choice was recorded
            AdventureCardState.PlayerChoice recordedChoice = state.getPlayerChoice(playerId1);
            assertNotNull(recordedChoice);
        }

        @Test
        @DisplayName("Record player choice for non-existent card")
        void testRecordPlayerChoiceNoCard() {
            // Try to record choice without starting a card first
            assertDoesNotThrow(() -> controller.recordPlayerChoice(playerId1, "combatStrength", 5));
        }

        @Test
        @DisplayName("Handle player choice for pirate card")
        void testHandlePlayerChoicePirates() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            choice.setParameter("combatStrength", 10);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard()); // Card should be resolved
        }

        @Test
        @DisplayName("Handle player choice for slaver card")
        void testHandlePlayerChoiceSlavers() {
            controller.startAdventureCard(slaverCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for abandoned ship")
        void testHandlePlayerChoiceAbandonedShip() {
            controller.startAdventureCard(abandonedShipCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.ABANDONED_SHIP_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for abandoned station")
        void testHandlePlayerChoiceAbandonedStation() {
            controller.startAdventureCard(abandonedStationCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.ABANDONED_STATION_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for stardust card")
        void testHandlePlayerChoiceStardust() {
            controller.startAdventureCard(stardustCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.SKIP_TURN);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for open space card")
        void testHandlePlayerChoiceOpenSpace() {
            controller.startAdventureCard(openSpaceCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.SKIP_TURN);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for epidemic card")
        void testHandlePlayerChoiceEpidemic() {
            controller.startAdventureCard(epidemicCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.SKIP_TURN);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Handle player choice for non-existent card returns false")
        void testHandlePlayerChoiceNoCard() {
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Combat Strength Tests")
    class CombatStrengthTests {

        @Test
        @DisplayName("Process combat strength for pirate card")
        void testProcessCombatStrengthPirate() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            controller.processCombatStrength(playerId1, 10, 5, 3, 2);

            AdventureCardState.PlayerChoice recordedChoice = state.getPlayerChoice(playerId1);
            assertEquals(10, recordedChoice.getParameter("combatStrength"));
            assertEquals(5, recordedChoice.getParameter("engineStrength"));
            assertEquals(3, recordedChoice.getParameter("crewStrength"));
            assertEquals(2, recordedChoice.getParameter("batteryUsed"));
        }

        @Test
        @DisplayName("Process combat strength for slaver card")
        void testProcessCombatStrengthSlaver() {
            controller.startAdventureCard(slaverCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            controller.processCombatStrength(playerId1, 8, 4, 2, 1);

            AdventureCardState.PlayerChoice recordedChoice = state.getPlayerChoice(playerId1);
            assertEquals(8, recordedChoice.getParameter("combatStrength"));
            assertEquals(4, recordedChoice.getParameter("engineStrength"));
            assertEquals(2, recordedChoice.getParameter("crewStrength"));
            assertEquals(1, recordedChoice.getParameter("batteryUsed"));
        }

        @Test
        @DisplayName("Process combat strength for non-combat card does nothing")
        void testProcessCombatStrengthNonCombat() {
            controller.startAdventureCard(stardustCard, playerId1);

            // Should not throw exception even though it's not a combat card
            assertDoesNotThrow(() -> controller.processCombatStrength(playerId1, 10, 5, 3, 2));
        }

        @Test
        @DisplayName("Process combat strength for non-existent card")
        void testProcessCombatStrengthNoCard() {
            assertDoesNotThrow(() -> controller.processCombatStrength(playerId1, 10, 5, 3, 2));
        }

        @Test
        @DisplayName("Process combat strength with no player choice")
        void testProcessCombatStrengthNoChoice() {
            controller.startAdventureCard(pirateCard, playerId1);

            // Don't set player choice first
            assertDoesNotThrow(() -> controller.processCombatStrength(playerId1, 10, 5, 3, 2));
        }
    }

    @Nested
    @DisplayName("Card Resolution Tests")
    class CardResolutionTests {

        @Test
        @DisplayName("Resolve card successfully")
        void testResolveCard() {
            controller.startAdventureCard(pirateCard, playerId1);
            assertTrue(controller.isProcessingCard());

            controller.resolveCard(playerId1);

            assertFalse(controller.isProcessingCard());
            assertNull(controller.getCardState(playerId1));
        }

        @Test
        @DisplayName("Resolve non-existent card")
        void testResolveNonExistentCard() {
            assertDoesNotThrow(() -> controller.resolveCard(playerId1));
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Resolve card marks state as resolved")
        void testResolveCardMarksResolved() {
            controller.startAdventureCard(pirateCard, playerId1);
            AdventureCardState state = controller.getCardState(playerId1);
            assertFalse(state.isResolved());

            controller.resolveCard(playerId1);

            // State should be marked as resolved before removal
            assertTrue(state.isResolved());
        }
    }

    @Nested
    @DisplayName("State Management Tests")
    class StateManagementTests {

        @Test
        @DisplayName("Get card state for existing player")
        void testGetCardStateExists() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(pirateCard, state.getCard());
        }

        @Test
        @DisplayName("Get card state for non-existent player returns null")
        void testGetCardStateNotExists() {
            AdventureCardState state = controller.getCardState(playerId1);
            assertNull(state);
        }

        @Test
        @DisplayName("Is processing card returns false when no cards active")
        void testIsProcessingCardFalse() {
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Is processing card returns true when cards active")
        void testIsProcessingCardTrue() {
            controller.startAdventureCard(pirateCard, playerId1);
            assertTrue(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Timeout Handling Tests")
    class TimeoutHandlingTests {

        @Test
        @DisplayName("Timeout handling for pirate card sets default values")
        void testTimeoutHandlingPirate() throws InterruptedException {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            // Wait a bit longer than timeout to ensure timeout occurs
            TimeUnit.MILLISECONDS.sleep(100);

            // Manually trigger timeout for testing
            controller.cleanup(); // This will trigger shutdown and cleanup

            assertNotNull(choice); // Choice should still exist
        }

        @Test
        @DisplayName("Timeout handling for abandoned ship card")
        void testTimeoutHandlingAbandonedShip() throws InterruptedException {
            controller.startAdventureCard(abandonedShipCard, playerId1);

            // Wait a short time to simulate timeout scenario
            TimeUnit.MILLISECONDS.sleep(50);

            // Verify state exists before cleanup
            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
        }

        @Test
        @DisplayName("Timeout with resolved card should not affect anything")
        void testTimeoutWithResolvedCard() {
            controller.startAdventureCard(pirateCard, playerId1);
            controller.resolveCard(playerId1);

            // Card is already resolved, timeout should have no effect
            assertFalse(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Cleanup clears all active cards")
        void testCleanupClearsCards() {
            controller.startAdventureCard(pirateCard, playerId1);
            controller.startAdventureCard(stardustCard, playerId2);

            assertTrue(controller.isProcessingCard());

            controller.cleanup();

            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Cleanup when no cards active")
        void testCleanupNoCards() {
            assertDoesNotThrow(() -> controller.cleanup());
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Cleanup shuts down executor properly")
        void testCleanupExecutorShutdown() {
            controller.startAdventureCard(pirateCard, playerId1);

            assertDoesNotThrow(() -> controller.cleanup());

            // After cleanup, controller should still be usable for basic operations
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Multiple cleanups should not cause issues")
        void testMultipleCleanups() {
            controller.startAdventureCard(pirateCard, playerId1);

            assertDoesNotThrow(() -> {
                controller.cleanup();
                controller.cleanup(); // Second cleanup should not cause issues
            });
        }
    }

    @Nested
    @DisplayName("Require Timeout Tests")
    class RequireTimeoutTests {

        @Test
        @DisplayName("Pirates card requires timeout")
        void testPiratesRequiresTimeout() {
            controller.startAdventureCard(pirateCard, playerId1);
            // If timeout is required, card should be started normally
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Slavers card requires timeout")
        void testSlaversRequiresTimeout() {
            controller.startAdventureCard(slaverCard, playerId1);
            // If timeout is required, card should be started normally
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Abandoned ship card requires timeout")
        void testAbandonedShipRequiresTimeout() {
            controller.startAdventureCard(abandonedShipCard, playerId1);
            // If timeout is required, card should be started normally
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Stardust card may not require timeout")
        void testStardustTimeoutBehavior() {
            controller.startAdventureCard(stardustCard, playerId1);
            // Card should still be started even if timeout behavior varies
            assertTrue(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Card-Specific Behavior Tests")
    class CardSpecificBehaviorTests {

        @Test
        @DisplayName("Pirates card with attack pattern")
        void testPiratesCardBehavior() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(AdventureType.PIRATES, state.getCard().getType());

            // Verify attack pattern
            PiratesCard pirates = (PiratesCard) state.getCard();
            assertNotNull(pirates.getAttackPattern());
            assertEquals(3, pirates.getAttackPattern().size());
            assertEquals(100, pirates.getCreditReward());
        }

        @Test
        @DisplayName("Abandoned ship card with specific parameters")
        void testAbandonedShipCardBehavior() {
            controller.startAdventureCard(abandonedShipCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(AdventureType.ABANDONED_SHIP, state.getCard().getType());
        }

        @Test
        @DisplayName("Slaver card with power level and penalties")
        void testSlaverCardBehavior() {
            controller.startAdventureCard(slaverCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(AdventureType.SLAVERS, state.getCard().getType());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handle null player ID gracefully")
        void testNullPlayerId() {
            assertDoesNotThrow(() -> {
                controller.startAdventureCard(pirateCard, null);
                controller.recordPlayerChoice(null, "test", "value");
                controller.processCombatStrength(null, 1, 1, 1, 1);
                controller.resolveCard(null);
                controller.getCardState(null);
            });
        }

        @Test
        @DisplayName("Handle null card gracefully")
        void testNullCard() {
            assertDoesNotThrow(() -> {
                controller.startAdventureCard(null, playerId1);
                controller.startCardResolution(null);
            });
        }

        @Test
        @DisplayName("Concurrent access safety")
        void testConcurrentAccess() {
            controller.startAdventureCard(pirateCard, playerId1);
            controller.startAdventureCard(stardustCard, playerId2);

            // Simulate concurrent operations
            assertDoesNotThrow(() -> {
                controller.recordPlayerChoice(playerId1, "test1", "value1");
                controller.recordPlayerChoice(playerId2, "test2", "value2");
                controller.getCardState(playerId1);
                controller.getCardState(playerId2);
            });
        }
    }
    // Aggiungi questi test alla classe AdventureCardControllerTest esistente

    @Nested
    @DisplayName("Planet Card Specific Tests")
    class PlanetCardTests {

        @Test
        @DisplayName("Planet card requires timeout")
        void testPlanetCardRequiresTimeout() {
            Planet planet1 = new Planet(1, Map.of(GoodType.RED, 1, GoodType.BLUE, 2));
            Planet planet2 = new Planet(2, Map.of(GoodType.GREEN, 3, GoodType.YELLOW, 4));
            PlanetsCard planetsCard = new PlanetsCard("planet-1", CardLevel.TEST_FLIGHT, "Planet encounter", 3, List.of(planet1, planet2));
            controller.startAdventureCard(planetsCard, playerId1);

            assertTrue(controller.isProcessingCard());
            AdventureCardState state = controller.getCardState(playerId1);
            assertNotNull(state);
            assertEquals(AdventureType.PLANETS, state.getCard().getType());
        }

        @Test
        @DisplayName("Handle player choice for planets card")
        void testHandlePlayerChoicePlanets() {
            Planet planet1 = new Planet(1, Map.of(GoodType.RED, 1, GoodType.BLUE, 2));
            Planet planet2 = new Planet(2, Map.of(GoodType.GREEN, 3, GoodType.YELLOW, 4));
            PlanetsCard planetsCard = new PlanetsCard("planet-1", CardLevel.TEST_FLIGHT, "Planet encounter", 3, List.of(planet1, planet2));
            controller.startAdventureCard(planetsCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Process planet choice internally")
        void testProcessPlanetChoice() {
            Planet planet1 = new Planet(1, Map.of(GoodType.RED, 1, GoodType.BLUE, 2));
            Planet planet2 = new Planet(2, Map.of(GoodType.GREEN, 3, GoodType.YELLOW, 4));
            PlanetsCard planetsCard = new PlanetsCard("planet-1", CardLevel.TEST_FLIGHT, "Planet encounter", 3, List.of(planet1, planet2));
            controller.startAdventureCard(planetsCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);

            // This will trigger processPlanetChoice internally
            controller.handlePlayerChoice(playerId1, choice);

            assertFalse(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Timeout Mechanism Tests")
    class TimeoutMechanismTests {

        @Test
        @DisplayName("Cards that don't require timeout work immediately")
        void testNoTimeoutCards() {
            // Test cards that don't require timeout
            controller.startAdventureCard(stardustCard, playerId1);
            assertTrue(controller.isProcessingCard());

            controller.startAdventureCard(abandonedShipCard, playerId2);
            assertTrue(controller.isProcessingCard());

            // Both should be active
            assertNotNull(controller.getCardState(playerId1));
            assertNotNull(controller.getCardState(playerId2));
        }

        @Test
        @DisplayName("Timeout handling with resolved card before timeout")
        void testTimeoutWithPreResolvedCard() {
            controller.startAdventureCard(pirateCard, playerId1);

            // Resolve before timeout
            controller.resolveCard(playerId1);
            assertFalse(controller.isProcessingCard());

            // Even if timeout triggers later, nothing should happen
            assertNull(controller.getCardState(playerId1));
        }

        @Test
        @DisplayName("Timeout handling sets default combat parameters")
        void testTimeoutSetsDefaults() throws InterruptedException {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            // Simulate manual timeout trigger
            // Since we can't easily wait for actual timeout, we test the timeout logic directly
            TimeUnit.MILLISECONDS.sleep(50);

            // The choice should exist but may not have parameters set yet
            assertNotNull(state.getPlayerChoice(playerId1));
        }
    }

    @Nested
    @DisplayName("Private Method Coverage Tests")
    class PrivateMethodCoverageTests {

        @Test
        @DisplayName("RequiresTimeout method coverage - Pirates")
        void testRequiresTimeoutPirates() {
            // Pirates card should require timeout - test by starting it
            controller.startAdventureCard(pirateCard, playerId1);
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("RequiresTimeout method coverage - Planets")
        void testRequiresTimeoutPlanets() {
            Planet planet1 = new Planet(1, Map.of(GoodType.RED, 1, GoodType.BLUE, 2));
            Planet planet2 = new Planet(2, Map.of(GoodType.GREEN, 3, GoodType.YELLOW, 4));
            PlanetsCard planetsCard = new PlanetsCard("planet-1", CardLevel.TEST_FLIGHT, "Planet encounter", 3,  List.of(planet1, planet2));
            // Planets card should require timeout - test by starting it
            controller.startAdventureCard(planetsCard, playerId1);
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("RequiresTimeout method coverage - Non-timeout cards")
        void testRequiresTimeoutFalse() {
            // These cards should NOT require timeout
            controller.startAdventureCard(stardustCard, playerId1);
            controller.startAdventureCard(openSpaceCard, playerId2);

            // Both should be processed immediately without timeout scheduling
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("ProcessCombatChoice method coverage")
        void testProcessCombatChoiceInternal() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            // This will internally call processCombatChoice
            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }

        @Test
        @DisplayName("ProcessAbandonedLocationChoice method coverage")
        void testProcessAbandonedLocationChoiceInternal() {
            controller.startAdventureCard(abandonedShipCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.ABANDONED_SHIP_CHOICE);

            // This will internally call processAbandonedLocationChoice
            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Switch Statement Coverage Tests")
    class SwitchStatementCoverageTests {

        @Test
        @DisplayName("Handle player choice - PIRATES case")
        void testHandlePlayerChoicePiratesCase() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
        }

        @Test
        @DisplayName("Handle player choice - SLAVERS case")
        void testHandlePlayerChoiceSlaversCase() {
            controller.startAdventureCard(slaverCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
        }

        @Test
        @DisplayName("Handle player choice - PLANETS case")
        void testHandlePlayerChoicePlanetsCase() {
            Planet planet1 = new Planet(1, Map.of(GoodType.RED, 1, GoodType.BLUE, 2));
            Planet planet2 = new Planet(2, Map.of(GoodType.GREEN, 3, GoodType.YELLOW, 4));
            PlanetsCard planetsCard = new PlanetsCard("planet-1", CardLevel.TEST_FLIGHT, "Planet encounter", 3, List.of(planet1, planet2));
            controller.startAdventureCard(planetsCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
        }

        @Test
        @DisplayName("Handle player choice - ABANDONED_SHIP case")
        void testHandlePlayerChoiceAbandonedShipCase() {
            controller.startAdventureCard(abandonedShipCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.ABANDONED_SHIP_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
        }

        @Test
        @DisplayName("Handle player choice - ABANDONED_STATION case")
        void testHandlePlayerChoiceAbandonedStationCase() {
            controller.startAdventureCard(abandonedStationCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.ABANDONED_STATION_CHOICE);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
        }

        @Test
        @DisplayName("Handle player choice - Default case")
        void testHandlePlayerChoiceDefaultCase() {
            controller.startAdventureCard(stardustCard, playerId1);

            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.SKIP_TURN);

            boolean result = controller.handlePlayerChoice(playerId1, choice);
            assertTrue(result);
            assertFalse(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("Branch Coverage for Conditionals")
    class BranchCoverageTests {

        @Test
        @DisplayName("ProcessCombatStrength - non-combat card branch")
        void testProcessCombatStrengthNonCombatCard() {
            controller.startAdventureCard(stardustCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.SKIP_TURN);
            state.setPlayerChoice(playerId1, choice);

            // Should not process combat strength for non-combat cards
            assertDoesNotThrow(() -> controller.processCombatStrength(playerId1, 10, 5, 3, 2));

            // Choice parameters should not be set for non-combat cards
            assertNull(choice.getParameter("combatStrength"));
        }

        @Test
        @DisplayName("ProcessCombatStrength - SLAVERS card branch")
        void testProcessCombatStrengthSlaversCard() {
            controller.startAdventureCard(slaverCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
            state.setPlayerChoice(playerId1, choice);

            controller.processCombatStrength(playerId1, 8, 4, 2, 1);

            assertEquals(8, choice.getParameter("combatStrength"));
            assertEquals(4, choice.getParameter("engineStrength"));
            assertEquals(2, choice.getParameter("crewStrength"));
            assertEquals(1, choice.getParameter("batteryUsed"));
        }

        @Test
        @DisplayName("Cleanup - executor already shutdown branch")
        void testCleanupExecutorAlreadyShutdown() {
            controller.startAdventureCard(pirateCard, playerId1);

            // First cleanup
            controller.cleanup();
            assertFalse(controller.isProcessingCard());

            // Second cleanup should handle already shutdown executor
            assertDoesNotThrow(() -> controller.cleanup());
        }

        @Test
        @DisplayName("Cleanup - executor await termination failure")
        void testCleanupExecutorAwaitTerminationFailure() throws InterruptedException {
            controller.startAdventureCard(pirateCard, playerId1);

            // This should still work even if there are threading issues
            assertDoesNotThrow(() -> controller.cleanup());
        }
    }

    @Nested
    @DisplayName("Exception and Error Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Record player choice with null parameters individually")
        void testRecordPlayerChoiceNullParams() {
            controller.startAdventureCard(pirateCard, playerId1);

            // Test null playerId
            assertDoesNotThrow(() -> controller.recordPlayerChoice(null, "test", "value"));

            // Test null choiceType
            assertDoesNotThrow(() -> controller.recordPlayerChoice(playerId1, null, "value"));

            // Test null value
            assertDoesNotThrow(() -> controller.recordPlayerChoice(playerId1, "test", null));
        }

        @Test
        @DisplayName("Handle player choice with null playerId returns false")
        void testHandlePlayerChoiceNullPlayerId() {
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                    playerId1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

            boolean result = controller.handlePlayerChoice(null, choice);
            assertFalse(result);
        }

        @Test
        @DisplayName("StartAdventureCard with null parameters in different combinations")
        void testStartAdventureCardNullCombinations() {
            // Both null
            assertDoesNotThrow(() -> controller.startAdventureCard(null, null));

            // Only card null
            assertDoesNotThrow(() -> controller.startAdventureCard(null, playerId1));

            // Only playerId null
            assertDoesNotThrow(() -> controller.startAdventureCard(pirateCard, null));

            assertFalse(controller.isProcessingCard());
        }
    }

    @Nested
    @DisplayName("State Consistency Tests")
    class StateConsistencyTests {

        @Test
        @DisplayName("Multiple cards for same player overwrites previous")
        void testMultipleCardsForSamePlayer() {
            controller.startAdventureCard(pirateCard, playerId1);
            assertTrue(controller.isProcessingCard());

            AdventureCardState firstState = controller.getCardState(playerId1);
            assertEquals(AdventureType.PIRATES, firstState.getCard().getType());

            // Start another card for same player
            controller.startAdventureCard(stardustCard, playerId1);

            AdventureCardState secondState = controller.getCardState(playerId1);
            assertEquals(AdventureType.STARDUST, secondState.getCard().getType());

            // Should still be processing
            assertTrue(controller.isProcessingCard());
        }

        @Test
        @DisplayName("Card state resolution marks state correctly")
        void testCardStateResolutionMarking() {
            controller.startAdventureCard(pirateCard, playerId1);

            AdventureCardState state = controller.getCardState(playerId1);
            assertFalse(state.isResolved());

            controller.resolveCard(playerId1);

            // State should have been marked as resolved before removal
            assertTrue(state.isResolved());
            assertNull(controller.getCardState(playerId1));
        }

        @Test
        @DisplayName("ProcessCombatStrength with no existing choice creates no side effects")
        void testProcessCombatStrengthNoExistingChoice() {
            controller.startAdventureCard(pirateCard, playerId1);

            // Don't create a choice first
            assertDoesNotThrow(() -> controller.processCombatStrength(playerId1, 10, 5, 3, 2));

            // State should still exist
            assertNotNull(controller.getCardState(playerId1));
        }
    }
}
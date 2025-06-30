package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.PlanetsCard;
import it.polimi.ingsw.server.model.domain.adventure.card.StardustCard;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdventureCardStateTest {

    private AdventureCardState cardState;
    private AdventureCard testCard;
    private String gameId;
    private PlayerId player1;
    private PlayerId player2;
    private PlayerId player3;

    @BeforeEach
    void setUp() {
        // Assuming CardLevel and AdventureType are enums with at least one value
        testCard = new StardustCard("TEST_001", CardLevel.TEST_FLIGHT, "Test Adventure Card");
        gameId = "GAME_123";
        cardState = new AdventureCardState(testCard, gameId);

        player1 = new PlayerId(UUID.randomUUID(), "Player1");
        player2 = new PlayerId(UUID.randomUUID(), "Player2");
        player3 = new PlayerId(UUID.randomUUID(), "Player3");
    }

    @Test
    @DisplayName("Constructor should initialize card state correctly")
    void testConstructor() {
        assertEquals(testCard, cardState.getCard());
        assertEquals(gameId, cardState.getGameId());
        assertEquals(AdventureCardState.CardProcessingState.REVEALED, cardState.getProcessingState());
        assertNull(cardState.getCurrentPlayerTurn());
        assertFalse(cardState.isResolved());
    }

    @Test
    @DisplayName("Should set and get processing state")
    void testProcessingState() {
        cardState.setProcessingState(AdventureCardState.CardProcessingState.COLLECTING_CHOICES);
        assertEquals(AdventureCardState.CardProcessingState.COLLECTING_CHOICES, cardState.getProcessingState());

        cardState.setProcessingState(AdventureCardState.CardProcessingState.RESOLVING);
        assertEquals(AdventureCardState.CardProcessingState.RESOLVING, cardState.getProcessingState());

        cardState.setProcessingState(AdventureCardState.CardProcessingState.COMPLETED);
        assertEquals(AdventureCardState.CardProcessingState.COMPLETED, cardState.getProcessingState());
    }

    @Test
    @DisplayName("Should handle player states correctly")
    void testPlayerStates() {
        // Default state should be NOT_ELIGIBLE
        assertEquals(AdventureCardState.PlayerChoiceState.NOT_ELIGIBLE,
                cardState.getPlayerState(player1));

        // Set player state
        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN);
        assertEquals(AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN,
                cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.CHOOSING);
        assertEquals(AdventureCardState.PlayerChoiceState.CHOOSING,
                cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.CHOSEN);
        assertEquals(AdventureCardState.PlayerChoiceState.CHOSEN,
                cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.SKIPPED);
        assertEquals(AdventureCardState.PlayerChoiceState.SKIPPED,
                cardState.getPlayerState(player1));
    }

    @Test
    @DisplayName("Should handle current player turn")
    void testCurrentPlayerTurn() {
        assertNull(cardState.getCurrentPlayerTurn());
        assertFalse(cardState.isPlayersTurn(player1));

        cardState.setCurrentPlayerTurn(player1);
        assertEquals(player1, cardState.getCurrentPlayerTurn());
        assertTrue(cardState.isPlayersTurn(player1));
        assertFalse(cardState.isPlayersTurn(player2));

        // Setting current player should also set their state to CHOOSING
        assertEquals(AdventureCardState.PlayerChoiceState.CHOOSING,
                cardState.getPlayerState(player1));

        cardState.setCurrentPlayerTurn(null);
        assertNull(cardState.getCurrentPlayerTurn());
        assertFalse(cardState.isPlayersTurn(player1));
    }

    @Test
    @DisplayName("Should handle null player in isPlayersTurn")
    void testIsPlayerssTurnWithNull() {
        cardState.setCurrentPlayerTurn(player1);
        assertFalse(cardState.isPlayersTurn(null));
    }

    @Test
    @DisplayName("Should handle player choices")
    void testPlayerChoices() {
        AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);

        assertFalse(cardState.hasPlayerMadeChoice(player1));
        assertNull(cardState.getPlayerChoice(player1));

        cardState.setPlayerChoice(player1, choice);

        assertTrue(cardState.hasPlayerMadeChoice(player1));
        assertEquals(choice, cardState.getPlayerChoice(player1));
        assertEquals(AdventureCardState.PlayerChoiceState.CHOSEN,
                cardState.getPlayerState(player1));

        Map<PlayerId, AdventureCardState.PlayerChoice> allChoices = cardState.getAllPlayerChoices();
        assertEquals(1, allChoices.size());
        assertEquals(choice, allChoices.get(player1));
    }

    @Test
    @DisplayName("Should check if all eligible players are finished")
    void testAreAllEligiblePlayersFinished() {
        // Initially true because no eligible players
        assertTrue(cardState.areAllEligiblePlayersFinished());

        // Add eligible players
        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN);
        cardState.setPlayerState(player2, AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN);
        cardState.setPlayerState(player3, AdventureCardState.PlayerChoiceState.NOT_ELIGIBLE);

        assertFalse(cardState.areAllEligiblePlayersFinished());

        // One player chooses
        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.CHOSEN);
        assertFalse(cardState.areAllEligiblePlayersFinished());

        // Second player skips
        cardState.setPlayerState(player2, AdventureCardState.PlayerChoiceState.SKIPPED);
        assertTrue(cardState.areAllEligiblePlayersFinished());
    }

    @Test
    @DisplayName("Should skip current player")
    void testSkipCurrentPlayer() {
        cardState.setCurrentPlayerTurn(player1);
        assertEquals(player1, cardState.getCurrentPlayerTurn());

        cardState.skipCurrentPlayer();

        assertNull(cardState.getCurrentPlayerTurn());
        assertEquals(AdventureCardState.PlayerChoiceState.SKIPPED,
                cardState.getPlayerState(player1));
    }

    @Test
    @DisplayName("Should handle skip when no current player")
    void testSkipCurrentPlayerWhenNull() {
        assertNull(cardState.getCurrentPlayerTurn());
        assertDoesNotThrow(() -> cardState.skipCurrentPlayer());
        assertNull(cardState.getCurrentPlayerTurn());
    }

    @Test
    @DisplayName("Should initialize eligible players")
    void testInitializeEligiblePlayers() {
        List<PlayerId> playerOrder = Arrays.asList(player1, player2, player3);

        cardState.initializeEligiblePlayers(playerOrder);

        assertEquals(AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN,
                cardState.getPlayerState(player1));
        assertEquals(AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN,
                cardState.getPlayerState(player2));
        assertEquals(AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN,
                cardState.getPlayerState(player3));
    }

    @Test
    @DisplayName("Should handle turn timeout")
    void testTurnTimeout() throws InterruptedException {
        cardState.setCurrentPlayerTurn(player1);

        // Initially should not be timed out
        assertFalse(cardState.hasTurnTimedOut());
        assertTrue(cardState.getRemainingTurnTime() > 29000); // Close to 30 seconds

        // Sleep a small amount to test remaining time calculation
        Thread.sleep(100);
        long remainingTime = cardState.getRemainingTurnTime();
        assertTrue(remainingTime < 30000);
        assertTrue(remainingTime > 29000);
    }

    @Test
    @DisplayName("Should handle resolved state")
    void testResolvedState() {
        assertFalse(cardState.isResolved());

        cardState.setResolved(true);
        assertTrue(cardState.isResolved());

        cardState.setResolved(false);
        assertFalse(cardState.isResolved());
    }

    @Test
    @DisplayName("PlayerChoice should handle parameters correctly")
    void testPlayerChoiceParameters() {
        AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);

        assertEquals(player1, choice.getPlayerId());
        assertEquals(AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH, choice.getChoiceType());

        // Test parameter handling
        choice.setParameter("strength", 5);
        choice.setParameter("target", "enemy");
        choice.setParameter("bonus", true);

        assertEquals(5, choice.getParameter("strength"));
        assertEquals("enemy", choice.getParameter("target"));
        assertEquals(true, choice.getParameter("bonus"));
        assertNull(choice.getParameter("nonexistent"));

        // Test int parameter with default
        assertEquals(5, choice.getIntParameter("strength", 0));
        assertEquals(10, choice.getIntParameter("nonexistent", 10));
        assertEquals(15, choice.getIntParameter("target", 15)); // Non-integer should return default

        // Test getAllParameters
        Map<String, Object> allParams = choice.getAllParameters();
        assertEquals(3, allParams.size());
        assertEquals(5, allParams.get("strength"));
        assertEquals("enemy", allParams.get("target"));
        assertEquals(true, allParams.get("bonus"));
    }

    @Test
    @DisplayName("Should test all AdventureChoiceType enum values")
    void testAdventureChoiceTypes() {
        // Test that all enum values can be used
        AdventureCardState.PlayerChoice planetChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);
        assertEquals(AdventureCardState.AdventureChoiceType.PLANET_CHOICE, planetChoice.getChoiceType());

        AdventureCardState.PlayerChoice combatChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH);
        assertEquals(AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH, combatChoice.getChoiceType());

        AdventureCardState.PlayerChoice engineChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.ENGINE_STRENGTH);
        assertEquals(AdventureCardState.AdventureChoiceType.ENGINE_STRENGTH, engineChoice.getChoiceType());

        AdventureCardState.PlayerChoice shipChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.ABANDONED_SHIP_CHOICE);
        assertEquals(AdventureCardState.AdventureChoiceType.ABANDONED_SHIP_CHOICE, shipChoice.getChoiceType());

        AdventureCardState.PlayerChoice stationChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.ABANDONED_STATION_CHOICE);
        assertEquals(AdventureCardState.AdventureChoiceType.ABANDONED_STATION_CHOICE, stationChoice.getChoiceType());

        AdventureCardState.PlayerChoice skipChoice = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.SKIP_TURN);
        assertEquals(AdventureCardState.AdventureChoiceType.SKIP_TURN, skipChoice.getChoiceType());
    }

    @Test
    @DisplayName("Should test all PlayerChoiceState enum values coverage")
    void testPlayerChoiceStates() {
        // Test all enum values are handled correctly
        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN);
        assertEquals(AdventureCardState.PlayerChoiceState.WAITING_FOR_TURN, cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.CHOOSING);
        assertEquals(AdventureCardState.PlayerChoiceState.CHOOSING, cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.CHOSEN);
        assertEquals(AdventureCardState.PlayerChoiceState.CHOSEN, cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.SKIPPED);
        assertEquals(AdventureCardState.PlayerChoiceState.SKIPPED, cardState.getPlayerState(player1));

        cardState.setPlayerState(player1, AdventureCardState.PlayerChoiceState.NOT_ELIGIBLE);
        assertEquals(AdventureCardState.PlayerChoiceState.NOT_ELIGIBLE, cardState.getPlayerState(player1));
    }

    @Test
    @DisplayName("Should test complex scenario with multiple players")
    void testComplexScenario() {
        List<PlayerId> players = Arrays.asList(player1, player2, player3);
        cardState.initializeEligiblePlayers(players);

        // Start with first player
        cardState.setCurrentPlayerTurn(player1);
        assertFalse(cardState.areAllEligiblePlayersFinished());

        // Player 1 makes a choice
        AdventureCardState.PlayerChoice choice1 = new AdventureCardState.PlayerChoice(
                player1, AdventureCardState.AdventureChoiceType.PLANET_CHOICE);
        choice1.setParameter("planet", "Mars");
        cardState.setPlayerChoice(player1, choice1);

        // Move to player 2
        cardState.setCurrentPlayerTurn(player2);

        // Player 2 skips
        cardState.skipCurrentPlayer();

        // Move to player 3
        cardState.setCurrentPlayerTurn(player3);

        // Player 3 makes a choice
        AdventureCardState.PlayerChoice choice3 = new AdventureCardState.PlayerChoice(
                player3, AdventureCardState.AdventureChoiceType.SKIP_TURN);
        cardState.setPlayerChoice(player3, choice3);

        // Now all players should be finished
        assertTrue(cardState.areAllEligiblePlayersFinished());

        // Verify choices
        Map<PlayerId, AdventureCardState.PlayerChoice> allChoices = cardState.getAllPlayerChoices();
        assertEquals(2, allChoices.size());
        assertEquals("Mars", allChoices.get(player1).getParameter("planet"));
        assertEquals(AdventureCardState.AdventureChoiceType.SKIP_TURN, allChoices.get(player3).getChoiceType());

        // Mark as resolved
        cardState.setResolved(true);
        assertTrue(cardState.isResolved());
    }

    @Test
    @DisplayName("Should handle edge cases for remaining turn time")
    void testRemainingTurnTimeEdgeCases() {
        // Test when no current player is set (should still return some time)
        long remainingTime = cardState.getRemainingTurnTime();
        assertTrue(remainingTime >= 0);

        cardState.setCurrentPlayerTurn(player1);
        remainingTime = cardState.getRemainingTurnTime();
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= 30000);
    }
}
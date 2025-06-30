package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.util.PileIdentifier;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.SlaversCard;
import it.polimi.ingsw.server.model.domain.adventure.card.EpidemicCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class AdventureDeckTest {

    private SlaversCard card1, card2, card7, card8;
    private EpidemicCard card3, card4, card5, card6;
    private PlayerId playerId1, playerId2;
    private PileIdentifier pileId1, pileId2, pileId3;

    @BeforeEach
    void setUp() {
        // Create test cards
        card1 = new SlaversCard("10", CardLevel.LEVEL_II, "description", 4, 5, 6, 7);
        card2 = new SlaversCard("11", CardLevel.LEVEL_II, "description", 4, 5, 6, 7);
        card3 = new EpidemicCard("12", CardLevel.LEVEL_II, "description");
        card4 = new EpidemicCard("13", CardLevel.LEVEL_II, "description");
        card5 = new EpidemicCard("14", CardLevel.LEVEL_II, "description");
        card6 = new EpidemicCard("15", CardLevel.LEVEL_II, "description");
        card7 = new SlaversCard("16", CardLevel.LEVEL_II, "description", 1, 2, 3, 4);
        card8 = new SlaversCard("17", CardLevel.LEVEL_II, "description", 8, 9, 10, 11);

        // Create test players
        playerId1 = new PlayerId(UUID.randomUUID(), "Player1");
        playerId2 = new PlayerId(UUID.randomUUID(), "Player2");

        // Create pile identifiers
        pileId1 = PileIdentifier.BOTTOM_LEFT;
        pileId2 = PileIdentifier.BOTTOM_CENTER;
        pileId3 = PileIdentifier.BOTTOM_RIGHT;
    }

    @Test
    @DisplayName("Constructor - TEST_FLIGHT level with empty uncovered piles")
    void testConstructorTestFlight() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertEquals(GameLevel.TEST_FLIGHT, deck.getGameLevel());
        assertFalse(deck.isFlightPhaseActive());
        assertTrue(deck.getUncoveredPilesView().isEmpty());
        assertEquals(2, deck.getMainFlightDeckView().size());
        assertEquals(0, deck.getPredictablePileIdentifiers().length);
    }

    @Test
    @DisplayName("Constructor - LEVEL_II with uncovered piles")
    void testConstructorLevelII() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertEquals(GameLevel.LEVEL_II, deck.getGameLevel());
        assertFalse(deck.isFlightPhaseActive());
        assertEquals(2, deck.getUncoveredPilesView().size());
        assertEquals(2, deck.getUncoveredPilesView().get(0).size());
        assertEquals(2, deck.getUncoveredPilesView().get(1).size());
        assertEquals(2, deck.getPredictablePileIdentifiers().length);
    }

    @Test
    @DisplayName("Constructor - LEVEL_II with three uncovered piles")
    void testConstructorLevelIIThreePiles() {
        List<AdventureCard> deck1 = Arrays.asList(card1);
        List<AdventureCard> deck2 = Arrays.asList(card2);
        List<AdventureCard> deck3 = Arrays.asList(card3);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2, deck3);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(3, identifiers.length);
        assertEquals(PileIdentifier.BOTTOM_LEFT, identifiers[0]);
        assertEquals(PileIdentifier.BOTTOM_CENTER, identifiers[1]);
        assertEquals(PileIdentifier.BOTTOM_RIGHT, identifiers[2]);
    }

    @Test
    @DisplayName("canPlayerViewPile - TEST_FLIGHT level always returns false")
    void testCanPlayerViewPileTestFlight() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertFalse(deck.canPlayerViewPile(playerId1, pileId1));
    }

    @Test
    @DisplayName("canPlayerViewPile - During flight phase returns false")
    void testCanPlayerViewPileDuringFlightPhase() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        assertFalse(deck.canPlayerViewPile(playerId1, pileId1));
    }

    @Test
    @DisplayName("canPlayerViewPile - Valid pile can be viewed")
    void testCanPlayerViewPileValid() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertTrue(deck.canPlayerViewPile(playerId1, pileId1));
        assertTrue(deck.canPlayerViewPile(playerId1, pileId2));
    }

    @Test
    @DisplayName("canPlayerViewPile - Pile already viewed by another player")
    void testCanPlayerViewPileAlreadyViewed() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Player1 views pile1
        deck.viewPile(playerId1, pileId1);

        // Player2 cannot view the same pile
        assertFalse(deck.canPlayerViewPile(playerId2, pileId1));

        // But Player1 can still view it (already viewing)
        assertTrue(deck.canPlayerViewPile(playerId1, pileId1));
    }

    @Test
    @DisplayName("viewPile - Successful viewing")
    void testViewPileSuccess() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        List<AdventureCard> result = deck.viewPile(playerId1, pileId1);

        assertEquals(2, result.size());
        assertEquals(card1, result.get(0));
        assertEquals(card2, result.get(1));

        // Verify the list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.add(card3));
    }

    @Test
    @DisplayName("viewPile - Re-viewing same pile by same player")
    void testViewPileReviewing() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // First view
        List<AdventureCard> result1 = deck.viewPile(playerId1, pileId1);
        assertEquals(2, result1.size());

        // Second view by same player should work
        List<AdventureCard> result2 = deck.viewPile(playerId1, pileId1);
        assertEquals(2, result2.size());
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("viewPile - During flight phase returns empty list")
    void testViewPileDuringFlightPhase() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        List<AdventureCard> result = deck.viewPile(playerId1, pileId1);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("viewPile - TEST_FLIGHT level returns empty list")
    void testViewPileTestFlight() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        List<AdventureCard> result = deck.viewPile(playerId1, pileId1);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("viewPile - IllegalStateException when pile viewed by another player")
    void testViewPileIllegalState() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Player1 views pile1
        deck.viewPile(playerId1, pileId1);

        // Player2 tries to view the same pile
        assertThrows(IllegalStateException.class, () -> deck.viewPile(playerId2, pileId1));
    }

    @Test
    @DisplayName("stopViewingPile - Successfully stops viewing")
    void testStopViewingPile() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Player1 views pile1
        deck.viewPile(playerId1, pileId1);
        assertFalse(deck.canPlayerViewPile(playerId2, pileId1));

        // Player1 stops viewing
        deck.stopViewingPile(playerId1);

        // Now Player2 can view pile1
        assertTrue(deck.canPlayerViewPile(playerId2, pileId1));
    }

    @Test
    @DisplayName("stopViewingPile - During flight phase does nothing")
    void testStopViewingPileDuringFlightPhase() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        // Should not throw exception
        assertDoesNotThrow(() -> deck.stopViewingPile(playerId1));
    }

    @Test
    @DisplayName("startFlightPhase - TEST_FLIGHT level")
    void testStartFlightPhaseTestFlight() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertFalse(deck.isFlightPhaseActive());
        assertEquals(2, deck.getMainFlightDeckView().size());

        deck.startFlightPhase();

        assertTrue(deck.isFlightPhaseActive());
        assertEquals(2, deck.getMainFlightDeckView().size());
        assertEquals(2, deck.getRemainingCardsInFlightDeck());
    }

    @Test
    @DisplayName("startFlightPhase - LEVEL_II combines all piles")
    void testStartFlightPhaseLevelII() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertFalse(deck.isFlightPhaseActive());
        assertEquals(2, deck.getUncoveredPilesView().size());

        deck.startFlightPhase();

        assertTrue(deck.isFlightPhaseActive());
        assertEquals(6, deck.getMainFlightDeckView().size()); // 2+2+2 cards
        assertEquals(6, deck.getRemainingCardsInFlightDeck());
        assertTrue(deck.getUncoveredPilesView().isEmpty());
    }

    @Test
    @DisplayName("startFlightPhase - Already active does nothing")
    void testStartFlightPhaseAlreadyActive() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        int initialSize = deck.getMainFlightDeckView().size();

        // Call again
        deck.startFlightPhase();

        assertEquals(initialSize, deck.getMainFlightDeckView().size());
        assertTrue(deck.isFlightPhaseActive());
    }

    @Test
    @DisplayName("drawNextCard - Successful drawing")
    void testDrawNextCardSuccess() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        Optional<AdventureCard> card = deck.drawNextCard();
        assertTrue(card.isPresent());
        assertEquals(1, deck.getRemainingCardsInFlightDeck());

        Optional<AdventureCard> card2 = deck.drawNextCard();
        assertTrue(card2.isPresent());
        assertEquals(0, deck.getRemainingCardsInFlightDeck());

        // Different cards should be drawn
        assertNotEquals(card.get(), card2.get());
    }

    @Test
    @DisplayName("drawNextCard - Deck exhausted")
    void testDrawNextCardExhausted() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        // Draw the only card
        Optional<AdventureCard> card = deck.drawNextCard();
        assertTrue(card.isPresent());

        // Try to draw another card
        Optional<AdventureCard> emptyCard = deck.drawNextCard();
        assertFalse(emptyCard.isPresent());
        assertTrue(deck.isExhausted());
    }

    @Test
    @DisplayName("drawNextCard - Not in flight phase")
    void testDrawNextCardNotInFlightPhase() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        Optional<AdventureCard> card = deck.drawNextCard();
        assertFalse(card.isPresent());
    }

    @Test
    @DisplayName("getCurrentCard - Successful retrieval")
    void testGetCurrentCardSuccess() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        Optional<AdventureCard> currentCard = deck.getCurrentCard();
        assertTrue(currentCard.isPresent());

        // Current card should be the same after multiple calls
        Optional<AdventureCard> currentCard2 = deck.getCurrentCard();
        assertEquals(currentCard.get(), currentCard2.get());

        // After drawing, current card should change
        deck.drawNextCard();
        Optional<AdventureCard> nextCurrentCard = deck.getCurrentCard();
        assertNotEquals(currentCard.get(), nextCurrentCard.get());
    }

    @Test
    @DisplayName("getCurrentCard - Deck exhausted")
    void testGetCurrentCardExhausted() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        // Draw the only card
        deck.drawNextCard();

        // No current card available
        Optional<AdventureCard> currentCard = deck.getCurrentCard();
        assertFalse(currentCard.isPresent());
    }

    @Test
    @DisplayName("getCurrentCard - Not in flight phase")
    void testGetCurrentCardNotInFlightPhase() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        Optional<AdventureCard> currentCard = deck.getCurrentCard();
        assertFalse(currentCard.isPresent());
    }

    @Test
    @DisplayName("isExhausted - Various scenarios")
    void testIsExhausted() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Not in flight phase
        assertTrue(deck.isExhausted());

        deck.startFlightPhase();

        // In flight phase with cards
        assertFalse(deck.isExhausted());

        // Draw all cards
        deck.drawNextCard();
        deck.drawNextCard();

        // Now exhausted
        assertTrue(deck.isExhausted());
    }

    @Test
    @DisplayName("getRemainingCardsInFlightDeck - Various scenarios")
    void testGetRemainingCardsInFlightDeck() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Not in flight phase
        assertEquals(0, deck.getRemainingCardsInFlightDeck());

        deck.startFlightPhase();

        // Initial count
        assertEquals(2, deck.getRemainingCardsInFlightDeck());

        // After drawing one card
        deck.drawNextCard();
        assertEquals(1, deck.getRemainingCardsInFlightDeck());

        // After drawing all cards
        deck.drawNextCard();
        assertEquals(0, deck.getRemainingCardsInFlightDeck());
    }

    @Test
    @DisplayName("getPredictablePileIdentifiers - Empty for TEST_FLIGHT")
    void testGetPredictablePileIdentifiersTestFlight() {
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = new ArrayList<>();

        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(0, identifiers.length);
    }

    @Test
    @DisplayName("getPredictablePileIdentifiers - LEVEL_II with one uncovered pile")
    void testGetPredictablePileIdentifiersLevelIIOnePile() {
        List<AdventureCard> deck1 = Arrays.asList(card1);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(1, identifiers.length);
        assertEquals(PileIdentifier.BOTTOM_LEFT, identifiers[0]);
    }

    @Test
    @DisplayName("getPredictablePileIdentifiers - LEVEL_II with two uncovered piles")
    void testGetPredictablePileIdentifiersLevelIITwoPiles() {
        List<AdventureCard> deck1 = Arrays.asList(card1);
        List<AdventureCard> deck2 = Arrays.asList(card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(2, identifiers.length);
        assertEquals(PileIdentifier.BOTTOM_LEFT, identifiers[0]);
        assertEquals(PileIdentifier.BOTTOM_CENTER, identifiers[1]);
    }

    @Test
    @DisplayName("getPredictablePileIdentifiers - LEVEL_II with more than three uncovered piles")
    void testGetPredictablePileIdentifiersLevelIIMoreThanThreePiles() {
        List<AdventureCard> deck1 = Arrays.asList(card1);
        List<AdventureCard> deck2 = Arrays.asList(card2);
        List<AdventureCard> deck3 = Arrays.asList(card3);
        List<AdventureCard> deck4 = Arrays.asList(card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2, deck3, deck4);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(4, identifiers.length);
        assertEquals(PileIdentifier.BOTTOM_LEFT, identifiers[0]);
        assertEquals(PileIdentifier.BOTTOM_CENTER, identifiers[1]);
        assertEquals(PileIdentifier.BOTTOM_RIGHT, identifiers[2]);
        assertEquals(PileIdentifier.UNKNOWN, identifiers[3]);
    }

    @Test
    @DisplayName("getPredictablePileIdentifiers - During flight phase returns empty array")
    void testGetPredictablePileIdentifiersDuringFlightPhase() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        PileIdentifier[] identifiers = deck.getPredictablePileIdentifiers();
        assertEquals(0, identifiers.length);
    }

    @Test
    @DisplayName("canPlayerViewPile - Invalid pile index")
    void testCanPlayerViewPileInvalidIndex() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Try to access pile with index 1 when only pile 0 exists
        assertFalse(deck.canPlayerViewPile(playerId1, pileId2)); // pileId2 has index 1
    }

    @Test
    @DisplayName("canPlayerViewPile - Null parameters")
    void testCanPlayerViewPileNullParameters() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertFalse(deck.canPlayerViewPile(null, pileId1));
        assertFalse(deck.canPlayerViewPile(playerId1, null));
        assertFalse(deck.canPlayerViewPile(null, null));
    }

    @Test
    @DisplayName("viewPile - Null parameters")
    void testViewPileNullParameters() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        List<AdventureCard> result1 = deck.viewPile(null, pileId1);
        assertTrue(result1.isEmpty());

        List<AdventureCard> result2 = deck.viewPile(playerId1, null);
        assertTrue(result2.isEmpty());

        List<AdventureCard> result3 = deck.viewPile(null, null);
        assertTrue(result3.isEmpty());
    }

    @Test
    @DisplayName("stopViewingPile - Player not viewing any pile")
    void testStopViewingPileNotViewing() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Should not throw exception even if player is not viewing anything
        assertDoesNotThrow(() -> deck.stopViewingPile(playerId1));
    }

    @Test
    @DisplayName("stopViewingPile - Null player ID")
    void testStopViewingPileNullPlayerId() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Should not throw exception with null player ID
        assertDoesNotThrow(() -> deck.stopViewingPile(null));
    }

    @Test
    @DisplayName("getUncoveredPilesView - During flight phase returns empty list")
    void testGetUncoveredPilesViewDuringFlightPhase() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);
        deck.startFlightPhase();

        List<List<AdventureCard>> view = deck.getUncoveredPilesView();
        assertTrue(view.isEmpty());
    }

    @Test
    @DisplayName("getUncoveredPilesView - Returns unmodifiable view")
    void testGetUncoveredPilesViewUnmodifiable() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        List<List<AdventureCard>> view = deck.getUncoveredPilesView();

        // The outer list should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> view.add(Arrays.asList(card7)));

        // The inner lists should also be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> view.get(0).add(card7));
    }

    @Test
    @DisplayName("Constructor - Empty initial covered pile")
    void testConstructorEmptyInitialCoveredPile() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> initialCoveredOrUnknownPile = new ArrayList<>();
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        assertEquals(GameLevel.LEVEL_II, deck.getGameLevel());
        assertEquals(1, deck.getUncoveredPilesView().size());
        assertEquals(0, deck.getMainFlightDeckView().size());
    }

    @Test
    @DisplayName("Multiple players viewing different piles")
    void testMultiplePlayersViewingDifferentPiles() {
        List<AdventureCard> deck1 = Arrays.asList(card1, card2);
        List<AdventureCard> deck2 = Arrays.asList(card3, card4);
        List<AdventureCard> deck3 = Arrays.asList(card7, card8);
        List<AdventureCard> initialCoveredOrUnknownPile = Arrays.asList(card5, card6);
        List<List<AdventureCard>> initialUncoveredPiles = Arrays.asList(deck1, deck2, deck3);

        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, initialUncoveredPiles, initialCoveredOrUnknownPile);

        // Player1 views pile1
        List<AdventureCard> result1 = deck.viewPile(playerId1, pileId1);
        assertEquals(2, result1.size());
        assertTrue(deck.canPlayerViewPile(playerId1, pileId1));

        // Player2 views pile2
        List<AdventureCard> result2 = deck.viewPile(playerId2, pileId2);
        assertEquals(2, result2.size());
        assertTrue(deck.canPlayerViewPile(playerId2, pileId2));

        // Both players can still view their respective piles
        assertTrue(deck.canPlayerViewPile(playerId1, pileId1));
        assertTrue(deck.canPlayerViewPile(playerId2, pileId2));

        // But they cannot view each other's piles
        assertFalse(deck.canPlayerViewPile(playerId1, pileId2));
        assertFalse(deck.canPlayerViewPile(playerId2, pileId1));

        // Player1 can view pile3 (not occupied)
        assertTrue(deck.canPlayerViewPile(playerId1, pileId3));
    }
}


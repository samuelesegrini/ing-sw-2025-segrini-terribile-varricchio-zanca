package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
// import it.polimi.ingsw.server.model.domain.adventure.card.TestAdventureCard; // Your mock card
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.util.PileIdentifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

// Mock AdventureCard for testing
class TestAdventureCard extends AdventureCard {
    public TestAdventureCard(String id, CardLevel level) {
        super(id, level, "Desc for " + id, it.polimi.ingsw.server.model.enums.adventure.AdventureType.OPEN_SPACE);
    }
    // Add accept method if AdventureCard is abstract and requires it
    // public boolean accept(AdventureCardVisitor visitor, GameModel state) { return true; }
}


class AdventureDeckTest {

    private PlayerId player1;
    private PlayerId player2;

    @BeforeEach
    void setUp() {
        player1 = new PlayerId(UUID.randomUUID(), "Player1");
        player2 = new PlayerId(UUID.randomUUID(), "Player2");
    }

    private List<AdventureCard> createMockCards(CardLevel level, int count, String prefix) {
        List<AdventureCard> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(new TestAdventureCard(prefix + "_" + level.name() + "_" + i, level));
        }
        return cards;
    }

    // --- TEST FLIGHT SCENARIOS ---
    @Test
    void testTestFlight_Initialization() {
        List<AdventureCard> tfCards = createMockCards(CardLevel.TEST_FLIGHT, 8, "TF");
        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, new ArrayList<>(), tfCards);

        assertFalse(deck.isFlightPhaseActive(), "TestFlight should not start in flight phase automatically");
        assertEquals(0, deck.getUncoveredPilesView().size(), "TestFlight should have no uncovered piles at init.");
        // Main deck for TF is populated directly, but considered "unknownOrTestFlightPile" internally before startFlightPhase
        // After startFlightPhase, it will be in mainFlightDeck.
        // Let's test size after starting flight phase.

        deck.startFlightPhase(); // Explicitly start flight phase
        assertTrue(deck.isFlightPhaseActive());
        assertEquals(8, deck.getMainFlightDeckView().size(), "TestFlight main deck should have all TF cards after start flight.");
        assertEquals(8, deck.getRemainingCardsInFlightDeck());
    }

    @Test
    void testTestFlight_DrawingAllCards() {
        List<AdventureCard> tfCards = createMockCards(CardLevel.TEST_FLIGHT, 3, "TF");
        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, new ArrayList<>(), tfCards);
        deck.startFlightPhase();

        Optional<AdventureCard> card1 = deck.drawNextCard();
        assertTrue(card1.isPresent());
        assertEquals(2, deck.getRemainingCardsInFlightDeck());

        Optional<AdventureCard> card2 = deck.drawNextCard();
        assertTrue(card2.isPresent());
        assertEquals(1, deck.getRemainingCardsInFlightDeck());
        assertNotEquals(card1.get().getId(), card2.get().getId(), "Drawn cards should be different due to shuffle.");

        Optional<AdventureCard> card3 = deck.drawNextCard();
        assertTrue(card3.isPresent());
        assertEquals(0, deck.getRemainingCardsInFlightDeck());
        assertTrue(deck.isExhausted());

        Optional<AdventureCard> card4 = deck.drawNextCard();
        assertFalse(card4.isPresent(), "Should be no more cards to draw.");
        assertTrue(deck.isExhausted());
    }

    @Test
    void testTestFlight_ViewingPilesShouldNotBePossible() {
        List<AdventureCard> tfCards = createMockCards(CardLevel.TEST_FLIGHT, 8, "TF");
        AdventureDeck deck = new AdventureDeck(GameLevel.TEST_FLIGHT, new ArrayList<>(), tfCards);

        assertFalse(deck.canPlayerViewPile(player1, PileIdentifier.BOTTOM_LEFT));
        assertEquals(0, deck.getPredictablePileIdentifiers().length);
        List<AdventureCard> viewed = deck.viewPile(player1, PileIdentifier.BOTTOM_LEFT); // Should ideally throw or return empty
        assertTrue(viewed.isEmpty(), "Viewing specific piles should not be possible in Test Flight setup.");
    }


    // --- LEVEL II SCENARIOS ---
    private AdventureDeck createLevelIIDeck() {
        // Simulating GameConfigurationManager's output
        // 3 predictable piles, each with (e.g.) 2 L2, 1 L1 (TF counts as L1)
        // 1 unknown pile, with (e.g.) 2 L2, 1 L1
        List<List<AdventureCard>> uncoveredPiles = new ArrayList<>();
        for(int i=0; i<3; i++) {
            List<AdventureCard> pile = new ArrayList<>();
            pile.addAll(createMockCards(CardLevel.LEVEL_II, 2, "P"+i));
            pile.addAll(createMockCards(CardLevel.LEVEL_I, 1, "P"+i)); // or TF
            uncoveredPiles.add(pile);
        }

        List<AdventureCard> unknownPile = new ArrayList<>();
        unknownPile.addAll(createMockCards(CardLevel.LEVEL_II, 2, "U"));
        unknownPile.addAll(createMockCards(CardLevel.LEVEL_I, 1, "U"));

        return new AdventureDeck(GameLevel.LEVEL_II, uncoveredPiles, unknownPile);
    }

    @Test
    void testLevelII_Initialization_SetupPhase() {
        AdventureDeck deck = createLevelIIDeck();

        assertFalse(deck.isFlightPhaseActive(), "Level II should start in setup phase.");
        assertEquals(3, deck.getUncoveredPilesView().size(), "Level II should have 3 uncovered piles.");
        assertEquals(3, deck.getUncoveredPilesView().get(0).size(), "Each uncovered pile should have 3 cards.");
        assertEquals(0, deck.getMainFlightDeckView().size(), "Main flight deck should be empty during L2 setup.");
        assertEquals(3, deck.getPredictablePileIdentifiers().length); // BOTTOM_LEFT, _CENTER, _RIGHT
    }

    @Test
    void testLevelII_PileViewing_SetupPhase() {
        AdventureDeck deck = createLevelIIDeck();

        // Player 1 views BOTTOM_LEFT
        assertTrue(deck.canPlayerViewPile(player1, PileIdentifier.BOTTOM_LEFT));
        List<AdventureCard> p1View = deck.viewPile(player1, PileIdentifier.BOTTOM_LEFT);
        assertNotNull(p1View);
        assertEquals(3, p1View.size());

        // Player 2 tries to view BOTTOM_LEFT (should fail)
        assertFalse(deck.canPlayerViewPile(player2, PileIdentifier.BOTTOM_LEFT));
        assertThrows(IllegalStateException.class, () -> {
            deck.viewPile(player2, PileIdentifier.BOTTOM_LEFT);
        }, "Should throw if another player is viewing.");

        // Player 1 stops viewing
        deck.stopViewingPile(player1);
        assertTrue(deck.canPlayerViewPile(player2, PileIdentifier.BOTTOM_LEFT), "Pile should be free after player1 stops viewing.");

        // Player 2 views BOTTOM_CENTER
        List<AdventureCard> p2View = deck.viewPile(player2, PileIdentifier.BOTTOM_CENTER);
        assertEquals(3, p2View.size());

        // Player 1 can still view BOTTOM_LEFT (it's free again)
        assertTrue(deck.canPlayerViewPile(player1, PileIdentifier.BOTTOM_LEFT));
    }

    @Test
    void testLevelII_StartFlightPhase_CombinesAndShufflesPiles() {
        AdventureDeck deck = createLevelIIDeck();
        // Total cards = (3 piles * 3 cards/pile) + (1 unknown pile * 3 cards) = 9 + 3 = 12
        int expectedTotalCards = 12;

        deck.startFlightPhase();

        assertTrue(deck.isFlightPhaseActive());
        assertEquals(0, deck.getUncoveredPilesView().size(), "Uncovered piles should be empty after flight phase starts.");
        assertEquals(expectedTotalCards, deck.getMainFlightDeckView().size(), "Main flight deck should contain all cards.");
        assertEquals(expectedTotalCards, deck.getRemainingCardsInFlightDeck());

        // Check if cards are different after shuffle (probabilistic check)
        // This is hard to test definitively without knowing the exact shuffle.
        // A simple check is that drawing a few cards gives different results.
        List<String> drawnIds = new ArrayList<>();
        for(int i=0; i<Math.min(5, expectedTotalCards); i++) {
            Optional<AdventureCard> card = deck.drawNextCard();
            assertTrue(card.isPresent());
            assertFalse(drawnIds.contains(card.get().getId()), "Cards should be unique after shuffle.");
            drawnIds.add(card.get().getId());
        }
    }

    @Test
    void testLevelII_Drawing_FlightPhase() {
        AdventureDeck deck = createLevelIIDeck();
        int totalCards = 12;
        deck.startFlightPhase();

        for (int i = 0; i < totalCards; i++) {
            assertFalse(deck.isExhausted(), "Deck should not be exhausted yet at card " + i);
            assertEquals(totalCards - i, deck.getRemainingCardsInFlightDeck());
            Optional<AdventureCard> card = deck.drawNextCard();
            assertTrue(card.isPresent(), "Should be able to draw card " + i);
        }

        assertTrue(deck.isExhausted(), "Deck should be exhausted after drawing all cards.");
        assertEquals(0, deck.getRemainingCardsInFlightDeck());
        assertFalse(deck.drawNextCard().isPresent(), "No more cards to draw.");
    }

    @Test
    void testGetCurrentCard_FlightPhase() {
        AdventureDeck deck = createLevelIIDeck();
        deck.startFlightPhase();

        if (deck.getMainFlightDeckView().isEmpty()) {
            assertTrue(deck.getCurrentCard().isEmpty(), "Current card should be empty if deck is empty.");
            return;
        }

        Optional<AdventureCard> current = deck.getCurrentCard();
        assertTrue(current.isPresent());
        AdventureCard firstCard = current.get();

        // Draw it
        Optional<AdventureCard> drawn = deck.drawNextCard();
        assertTrue(drawn.isPresent());
        assertEquals(firstCard.getId(), drawn.get().getId());

        // Get next current
        Optional<AdventureCard> nextCurrent = deck.getCurrentCard();
        if (deck.isExhausted()) {
            assertFalse(nextCurrent.isPresent());
        } else {
            assertTrue(nextCurrent.isPresent());
            assertNotEquals(firstCard.getId(), nextCurrent.get().getId());
        }
    }

    // --- EDGE CASES ---
    @Test
    void testEmptyInitialPiles_LevelII() {
        AdventureDeck deck = new AdventureDeck(GameLevel.LEVEL_II, new ArrayList<>(), new ArrayList<>());
        assertFalse(deck.isFlightPhaseActive());
        assertEquals(0, deck.getUncoveredPilesView().size());

        deck.startFlightPhase();
        assertTrue(deck.isFlightPhaseActive());
        assertEquals(0, deck.getMainFlightDeckView().size());
        assertTrue(deck.isExhausted());
    }

    @Test
    void testDrawingBeforeFlightPhase_LevelII() {
        AdventureDeck deck = createLevelIIDeck();
        assertFalse(deck.drawNextCard().isPresent(), "Cannot draw from main deck before flight phase starts for L2.");
        assertFalse(deck.getCurrentCard().isPresent());
    }

    @Test
    void testViewingPilesAfterFlightPhase_LevelII() {
        AdventureDeck deck = createLevelIIDeck();
        deck.startFlightPhase();

        assertFalse(deck.canPlayerViewPile(player1, PileIdentifier.BOTTOM_LEFT));
        List<AdventureCard> viewed = deck.viewPile(player1, PileIdentifier.BOTTOM_LEFT);
        assertTrue(viewed.isEmpty(), "Viewing specific piles should not be possible after flight phase starts.");
    }

    @Test
    void testPlayerViewingConcurrency_StopViewing() {
        AdventureDeck deck = createLevelIIDeck();
        PileIdentifier pileToView = PileIdentifier.BOTTOM_LEFT;

        // Player 1 views
        deck.viewPile(player1, pileToView);
        assertFalse(deck.canPlayerViewPile(player2, pileToView));

        // Player 1 views another pile (implicitly stops viewing the first)
        // The current AdventureDeck allows a player to "view" only one pile at a time by overwriting.
        deck.viewPile(player1, PileIdentifier.BOTTOM_CENTER);
        assertTrue(deck.canPlayerViewPile(player2, pileToView), "Pile " + pileToView + " should be free now.");

        // Player 2 views the first pile
        deck.viewPile(player2, pileToView);
        assertFalse(deck.canPlayerViewPile(player1, pileToView));

        // Player 2 stops viewing
        deck.stopViewingPile(player2);
        assertTrue(deck.canPlayerViewPile(player1, pileToView));
    }

    @Test
    void testInvalidPileIdentifier_Viewing() {
        AdventureDeck deck = createLevelIIDeck();
        // Assuming PileIdentifier.UNKNOWN is an invalid one for viewing
        assertFalse(deck.canPlayerViewPile(player1, PileIdentifier.UNKNOWN));
        // Depending on implementation, viewPile might throw or return empty for UNKNOWN
        // Current `canPlayerViewPile` checks index < 0 or >= size. UNKNOWN index is -1.
        List<AdventureCard> viewed = deck.viewPile(player1, PileIdentifier.UNKNOWN);
        assertTrue(viewed.isEmpty());


        // Test with an index out of bounds if PileIdentifier could represent that
        PileIdentifier outOfBoundsPile = PileIdentifier.fromIndex(99); // Assuming fromIndex returns something, or this is a custom test one
        if (outOfBoundsPile == PileIdentifier.UNKNOWN && deck.getUncoveredPilesView().size() < 99) {
            assertFalse(deck.canPlayerViewPile(player1, outOfBoundsPile));
            List<AdventureCard> viewedOutOfBounds = deck.viewPile(player1, outOfBoundsPile);
            assertTrue(viewedOutOfBounds.isEmpty());
        }
    }
}
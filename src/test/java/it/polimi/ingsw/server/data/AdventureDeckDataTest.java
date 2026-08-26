package it.polimi.ingsw.server.data;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.GameLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the adventure card data can actually build the decks the manual asks
 * for.
 *
 * <p>The test flight deck is the tighter constraint: manual p.9 requires exactly eight
 * cards bearing the L mark, and those eight are level I cards that also appear in
 * level II flights. Modelling the mark as a third level, which the shipped data used
 * to do, silently shrank the level I pool from 20 cards to 13.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link AdventureCardIdentity},
 * the deck composition of each {@link GameLevel}.
 */
class AdventureDeckDataTest {

    private static GameData data;

    @BeforeAll
    static void loadCatalogue() {
        data = GameDataLoader.loadBundled();
    }

    @Test
    @DisplayName("both card pools hold 20 cards, so a level II deck draws from a full level I pool")
    void bothCardPools_holdTwentyCards() {
        assertEquals(20, data.cardsOfLevel(CardLevel.LEVEL_I).size());
        assertEquals(20, data.cardsOfLevel(CardLevel.LEVEL_II).size());
        assertEquals(40, data.cards().size());
    }

    @Test
    @DisplayName("the test flight deck is exactly the 8 cards marked L, all of them level I cards")
    void testFlightDeck_isEightLevelOneCards() {
        List<AdventureCardIdentity> marked = data.testFlightCards();
        assertEquals(8, marked.size());
        marked.forEach(card -> assertEquals(CardLevel.LEVEL_I, card.level(), card.id()));
        assertTrue(data.cardsOfLevel(CardLevel.LEVEL_I).containsAll(marked),
                "the marked cards must remain part of the level I pool");
    }

    @Test
    @DisplayName("the test flight deck covers the eight adventures described on manual p.12-13")
    void testFlightDeck_coversTheEightPrintedTypes() {
        Set<AdventureCardType> types = data.testFlightCards().stream()
                .map(AdventureCardIdentity::type)
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(Set.of(
                AdventureCardType.PLANETS,
                AdventureCardType.ABANDONED_STATION,
                AdventureCardType.ABANDONED_SHIP,
                AdventureCardType.SMUGGLERS,
                AdventureCardType.OPEN_SPACE,
                AdventureCardType.METEOR_SWARM,
                AdventureCardType.COMBAT_ZONE,
                AdventureCardType.STARDUST), types);
    }

    @Test
    @DisplayName("every card type the specification lists is present in the data")
    void everyCardType_hasAtLeastOneCard() {
        Set<AdventureCardType> present = data.cards().stream()
                .map(AdventureCardIdentity::type)
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(Set.of(AdventureCardType.values()), present,
                "a specified card type has no card, or a card has a type the specification does not list");
    }

    @Test
    @DisplayName("each level's pool is large enough for the deck that level builds")
    void cardPools_areLargeEnoughForTheirDecks() {
        for (GameLevel level : GameLevel.values()) {
            var deck = data.level(level).flightBoard().deck();
            int available = deck.testFlightCardsOnly()
                    ? data.testFlightCards().size()
                    : Integer.MAX_VALUE;
            assertTrue(deck.totalCards() <= available, level + " asks for more marked cards than exist");

            for (CardLevel cardLevel : CardLevel.values()) {
                assertTrue(deck.totalCardsOfLevel(cardLevel) <= data.cardsOfLevel(cardLevel).size(),
                        level + " asks for more " + cardLevel + " cards than the pool holds");
            }
        }
    }

    @Test
    @DisplayName("cards of the types introduced with the complete game never carry the test flight mark")
    void advancedCardTypes_areNotMarkedForTheTestFlight() {
        data.testFlightCards().forEach(card -> assertTrue(card.type().inTestFlight(),
                card.id() + " is marked for the test flight but " + card.type() + " only appears later"));
    }
}

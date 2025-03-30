package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.adventure.card.EpidemicCard;
import it.polimi.ingsw.model.domain.adventure.card.StardustCard;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.util.PileIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class AdventureDeckTest {

    PlayerId playerId1, playerId2;
    Player player1, player2;

    private AdventureDeck adventureDeck;

    private List<List<AdventureCard>> uncoveredPiles;
    private List<AdventureCard> coveredPile;
    private PileIdentifier[] uncoveredPileIdentifiers;
    private PileIdentifier coveredPileIdentifier;

    @BeforeEach
    void setUp() {
        playerId1 = new PlayerId(UUID.randomUUID(), "Samuele");
        playerId2 = new PlayerId(UUID.randomUUID(), "Diego");
        player1 = new Player(playerId1);
        player2 = new Player (playerId2);

        uncoveredPiles = Arrays.asList(
                Arrays.asList(
                        new EpidemicCard("A1", CardLevel.LEVEL_II, "Prima carta", AdventureType.EPIDEMIC),
                        new EpidemicCard("A2", CardLevel.LEVEL_II, "Seconda carta", AdventureType.EPIDEMIC)
                ),
                Arrays.asList(
                        new EpidemicCard("B1", CardLevel.LEVEL_II, "Prima carta", AdventureType.EPIDEMIC),
                        new EpidemicCard("B2", CardLevel.LEVEL_II, "Seconda carta", AdventureType.EPIDEMIC)
                ),
                Arrays.asList(
                        new EpidemicCard("C1", CardLevel.LEVEL_II, "Prima carta", AdventureType.EPIDEMIC),
                        new EpidemicCard("C2", CardLevel.LEVEL_II, "Seconda carta", AdventureType.EPIDEMIC)
                )

        );

        coveredPile = Arrays.asList(
            new EpidemicCard("D1", CardLevel.LEVEL_II, "Carta coperta 1", AdventureType.EPIDEMIC),
                new EpidemicCard("D2", CardLevel.LEVEL_II, "Carta coperta 2", AdventureType.EPIDEMIC)
        );

        adventureDeck = new AdventureDeck(GameLevel.LEVEL_II, uncoveredPiles, coveredPile);

    }

    @Test
    void testGetCurrentCard() {
        Optional<AdventureCard> currentCard = adventureDeck.getCurrentCard();
        assertTrue(currentCard.isPresent());
        assertEquals("D1", currentCard.get().getId());
    }

    @Test
    void testDrawNextCard() {
        Optional<AdventureCard> card1 = adventureDeck.drawNextCard();
        assertTrue(card1.isPresent());
        assertEquals("D1", card1.get().getId());

        Optional<AdventureCard> card2 = adventureDeck.drawNextCard();
        assertTrue(card2.isPresent());
        assertEquals("D2", card2.get().getId());

        Optional<AdventureCard> emptyCard = adventureDeck.drawNextCard();
        assertFalse(emptyCard.isPresent());
    }
    @Test
    void testCanPlayerViewPile() {
        PileIdentifier pileId = PileIdentifier.BOTTOM_LEFT;

        assertTrue(adventureDeck.canPlayerViewPile(playerId1, pileId));
        adventureDeck.viewPile(playerId1, pileId);
        assertFalse(adventureDeck.canPlayerViewPile(playerId2, pileId));
        assertTrue(adventureDeck.canPlayerViewPile(playerId1, pileId));
    }

    @Test
    void testViewPile() {
        PileIdentifier pileId = PileIdentifier.BOTTOM_CENTER;

        List<AdventureCard> cards = adventureDeck.viewPile(playerId1, pileId);
        assertNotNull(cards);
        assertEquals(2, cards.size());
        assertEquals("B1", cards.get(0).getId());
        assertThrows(IllegalStateException.class, () -> adventureDeck.viewPile(playerId2, pileId));
    }

    @Test
    void testStopViewingPile() {
        PileIdentifier pileId = PileIdentifier.BOTTOM_RIGHT;

        adventureDeck.viewPile(playerId1, pileId);
        assertFalse(adventureDeck.canPlayerViewPile(playerId2, pileId));

        adventureDeck.stopViewingPile(playerId1);
        assertTrue(adventureDeck.canPlayerViewPile(playerId2, pileId));
    }
}

package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureDeck;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks reading the flight forecast during building, against manual p.16.
 *
 * <p>Scouting is the mechanism that makes building a decision rather than a scramble:
 * three quarters of the flight can be read in advance, so a player can build for the
 * dangers they know are coming. The fourth pile cannot, so no ship is ever safe.
 *
 * <p>What keeps it from being free is that picking up a pile welds the last tile placed,
 * exactly as reaching for a new tile does, and that no tile can be attached while the
 * cards are in hand. Reading costs building time, which is the whole trade.
 *
 * <p>Components involved: {@link ShipBuilder}, {@link AdventureDeck}.
 */
class ScoutingTest {

    private ShipBuilder builderWithOneTileAttached() {
        ShipBuilder builder = BuildingFixtures.site(2).builder();
        builder.drawFaceDown();
        builder.attach(BuildingFixtures.AHEAD, Rotation.NONE);
        return builder;
    }

    @Nested
    @DisplayName("which piles may be read")
    class WhichPiles {

        @Test
        @DisplayName("three of the four piles may be read and the one at the top of the board may not")
        void threeOfFourPilesAreReadable() {
            AdventureDeck deck = BuildingFixtures.levelTwoDeck();

            assertEquals(4, deck.pileCount());
            assertTrue(deck.isPeekable(0));
            assertTrue(deck.isPeekable(1));
            assertTrue(deck.isPeekable(2));
            assertFalse(deck.isPeekable(3), "the pile at the top of the board stays unknown");
        }

        @Test
        @DisplayName("a test flight deals one pile, which is the unknown one, so there is nothing to read")
        void testFlightHasNothingToRead() {
            ShipBuilder builder = BuildingFixtures.site(0, BuildingFixtures.testFlightDeck()).builder();

            assertFalse(builder.scoutingAllowed());
            builder.drawFaceDown();
            builder.attach(BuildingFixtures.AHEAD, Rotation.NONE);

            assertThrows(IllegalArgumentException.class, () -> builder.scout(0));
        }

        @Test
        @DisplayName("asking for the unknown pile, or for one that does not exist, is refused")
        void unreadablePiles_areRefused() {
            ShipBuilder builder = builderWithOneTileAttached();

            assertThrows(IllegalArgumentException.class, () -> builder.scout(3));
            assertThrows(IllegalArgumentException.class, () -> builder.scout(9));
            assertThrows(IllegalArgumentException.class, () -> builder.scout(-1));
        }

        @Test
        @DisplayName("each level II pile holds one level I card and two level II cards")
        void pileCompositionMatchesTheBoard() {
            List<AdventureCardIdentity> pile = BuildingFixtures.levelTwoDeck().pile(0);

            assertEquals(3, pile.size());
            assertEquals(1, pile.stream().filter(card -> card.level() == CardLevel.LEVEL_I).count());
            assertEquals(2, pile.stream().filter(card -> card.level() == CardLevel.LEVEL_II).count());
        }

        @Test
        @DisplayName("no card is dealt into two piles at once")
        void noCardIsDealtTwice() {
            AdventureDeck deck = BuildingFixtures.levelTwoDeck();
            List<AdventureCardIdentity> all = deck.cards();

            assertEquals(12, all.size());
            assertEquals(12, all.stream().map(AdventureCardIdentity::id).distinct().count());
        }
    }

    @Nested
    @DisplayName("when a player may read")
    class WhenAPlayerMayRead {

        @Test
        @DisplayName("nothing may be read before a first tile is attached")
        void readingBeforeBuildingAnything_isRefused() {
            ShipBuilder builder = BuildingFixtures.site(2).builder();

            assertThrows(IllegalStateException.class, () -> builder.scout(0));
        }

        @Test
        @DisplayName("a player may read as often as they like while still building")
        void readingMayBeRepeated() {
            ShipBuilder builder = builderWithOneTileAttached();

            builder.scout(0);
            builder.putPileBack();
            builder.scout(1);
            builder.putPileBack();
            builder.scout(0);

            assertEquals(Optional.of(0), builder.pileInHand());
        }

        @Test
        @DisplayName("a finished player may read nothing more")
        void finishedPlayerMayNotRead() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.finish();

            assertThrows(IllegalStateException.class, () -> builder.scout(0));
        }

        @Test
        @DisplayName("a pile cannot be read with a tile in hand")
        void readingWithATileInHand_isRefused() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.drawFaceDown();

            assertThrows(IllegalStateException.class, () -> builder.scout(0));
        }
    }

    @Nested
    @DisplayName("what reading costs")
    class WhatItCosts {

        @Test
        @DisplayName("picking up a pile welds the last tile, exactly as reaching for a new one does")
        void readingWeldsTheLooseTile() {
            ShipBuilder builder = builderWithOneTileAttached();
            assertTrue(builder.unweldedCell().isPresent());

            builder.scout(0);

            assertTrue(builder.unweldedCell().isEmpty());
        }

        @Test
        @DisplayName("nothing may be built while the cards are in hand")
        void buildingIsRefusedWhileReading() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.scout(0);

            assertThrows(IllegalStateException.class, builder::drawFaceDown);
            assertThrows(IllegalStateException.class, () -> builder.attach(BuildingFixtures.PORT, Rotation.NONE));
            assertThrows(IllegalStateException.class, () -> builder.adjust(BuildingFixtures.PORT, Rotation.NONE));
            assertThrows(IllegalStateException.class, builder::reserve);
        }

        @Test
        @DisplayName("only one pile may be in hand at a time")
        void onlyOnePileAtATime() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.scout(0);

            assertThrows(IllegalStateException.class, () -> builder.scout(1));
        }

        @Test
        @DisplayName("putting the pile back lets building start again")
        void puttingThePileBackResumesBuilding() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.scout(0);
            builder.putPileBack();

            assertTrue(builder.pileInHand().isEmpty());
            builder.drawFaceDown();
            builder.attach(BuildingFixtures.PORT, Rotation.NONE);

            assertEquals(3, builder.ship().components().size());
        }

        @Test
        @DisplayName("putting back a pile nobody is holding is refused")
        void puttingBackNothing_isRefused() {
            ShipBuilder builder = builderWithOneTileAttached();

            assertThrows(IllegalStateException.class, builder::putPileBack);
        }

        @Test
        @DisplayName("finishing while holding a pile puts it back rather than leaving it in limbo")
        void finishingWhileReading_putsThePileBack() {
            ShipBuilder builder = builderWithOneTileAttached();
            builder.scout(0);

            builder.finish();

            assertTrue(builder.pileInHand().isEmpty());
            assertTrue(builder.hasFinished());
        }
    }
}

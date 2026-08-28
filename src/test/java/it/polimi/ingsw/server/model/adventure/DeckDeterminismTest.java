package it.polimi.ingsw.server.model.adventure;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.board.DeckComposition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That a deck is settled by its seed and nothing else.
 *
 * <p>A saved game is kept as a recipe — the seats, the rules, the seed and every command — and
 * put back by playing it again. That only works if the same seed deals the same cards, and for
 * level II games it did not: the deal walked a {@code Map.copyOf} map, whose iteration order
 * Java deliberately shuffles once per JVM. Restarting the server dealt a different deck, the
 * first recorded answer no longer fitted the question, and the game was set aside as broken
 * about half the time.
 *
 * <p>Nothing running inside one JVM could see it. The order is drawn once and then held for the
 * life of the process, so two decks dealt in the same test run always agreed. What follows
 * instead hands the deal a map that iterates the other way round, which is the same thing the
 * next JVM would have done.
 *
 * <p>Components involved: {@link AdventureDeck}, {@link DeckComposition},
 * {@link AdventureCardIdentity}.
 */
class DeckDeterminismTest {

    private static final long SEED = 20250828L;

    private static List<AdventureCardIdentity> pool(CardLevel level, int howMany) {
        List<AdventureCardIdentity> cards = new ArrayList<>();
        for (int card = 0; card < howMany; card++) {
            cards.add(new AdventureCardIdentity(level + "-" + card,
                    AdventureCardType.OPEN_SPACE, level, false));
        }
        return List.copyOf(cards);
    }

    private static Map<CardLevel, List<AdventureCardIdentity>> pools() {
        return Map.of(CardLevel.LEVEL_I, pool(CardLevel.LEVEL_I, 12),
                CardLevel.LEVEL_II, pool(CardLevel.LEVEL_II, 12));
    }

    @Test
    @DisplayName("every pile takes its levels in a settled order")
    void thePilesAreBuiltInEnumOrder() {
        Map<CardLevel, Integer> perPile = new LinkedHashMap<>();
        perPile.put(CardLevel.LEVEL_II, 2);
        perPile.put(CardLevel.LEVEL_I, 2);
        AdventureDeck deck = AdventureDeck.deal(new DeckComposition(3, perPile, false),
                pools(), new Random(SEED));

        for (int pile = 0; pile < deck.pileCount(); pile++) {
            assertEquals(
                    List.of(CardLevel.LEVEL_I, CardLevel.LEVEL_I,
                            CardLevel.LEVEL_II, CardLevel.LEVEL_II),
                    deck.pile(pile).stream().map(AdventureCardIdentity::level).toList(),
                    "pile " + pile + " was filled in whatever order the composition's map "
                            + "handed its levels over, which is drawn afresh in every JVM");
        }
    }

    @Test
    @DisplayName("and a composition keeps its levels in a settled order")
    void theCompositionIsOrdered() {
        DeckComposition composition = new DeckComposition(3,
                new LinkedHashMap<>(Map.of(CardLevel.LEVEL_II, 2, CardLevel.LEVEL_I, 2)), false);

        assertEquals(List.of(CardLevel.LEVEL_I, CardLevel.LEVEL_II),
                List.copyOf(composition.cardsPerPile().keySet()),
                "a composition that hands its levels out in a different order each run is a "
                        + "deck that cannot be dealt twice the same way");
    }
}

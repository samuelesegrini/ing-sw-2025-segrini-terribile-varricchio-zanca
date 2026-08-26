package it.polimi.ingsw.server.data;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that every adventure card in the game has rules behind it.
 *
 * <p>The card factory grew one type at a time through milestone M3, returning nothing for
 * the types still being written. That was a deliberate hole, and this is the test that
 * closes it: from here on, a card in the data with no implementation is a build failure
 * rather than a card that quietly does nothing when it is turned over.
 *
 * <p>It also checks the reverse direction. A type declared in {@link AdventureCardType}
 * with no card carrying it would mean either a missing card or a type nobody needs, and
 * both are worth knowing about.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link GameData}, {@link AdventureCard}.
 */
class AdventureCardCoverageTest {

    private static final GameData DATA = GameDataLoader.loadBundled();

    @Test
    @DisplayName("every card in the data has its rules built")
    void everyCardIsPlayable() {
        Set<String> unplayable = DATA.cards().stream()
                .map(AdventureCardIdentity::id)
                .filter(id -> DATA.playableCard(id).isEmpty())
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        assertEquals(Set.of(), unplayable, "these cards would do nothing when turned over");
        assertEquals(40, DATA.playableCards().size());
    }

    @Test
    @DisplayName("every card type the specification lists has an implementation")
    void everyTypeIsImplemented() {
        assertEquals(Set.of(AdventureCardType.values()), DATA.implementedCardTypes());
    }

    @Test
    @DisplayName("a card's rules agree with the identity it was built from")
    void rulesAgreeWithTheirIdentity() {
        DATA.cards().forEach(identity -> {
            AdventureCard card = DATA.playableCard(identity.id()).orElseThrow();
            assertEquals(identity, card.identity(), identity.id());
            assertEquals(identity.type(), card.type(), identity.id());
            assertEquals(identity.level(), card.level(), identity.id());
        });
    }

    @Test
    @DisplayName("the test flight deck is playable end to end, since it is drawn from marked cards only")
    void theTestFlightDeckIsPlayable() {
        DATA.testFlightCards().forEach(card ->
                assertTrue(DATA.playableCard(card.id()).isPresent(), card.id()));

        assertEquals(8, DATA.testFlightCards().size());
    }
}

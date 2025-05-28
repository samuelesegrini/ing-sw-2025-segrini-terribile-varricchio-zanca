package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenSpaceCardTest {
    @Test
            void test() {
        OpenSpaceCard card;
        card = new OpenSpaceCard("10", CardLevel.LEVEL_II, "descr iption");
        assertEquals("10", card.getId());
        assertEquals(CardLevel.LEVEL_II, card.getLevel());
        assertEquals("descr iption", card.getDescription());

    }

}
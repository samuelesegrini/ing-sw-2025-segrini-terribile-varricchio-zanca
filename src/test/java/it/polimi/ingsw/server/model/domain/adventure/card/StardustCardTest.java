package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;



class StardustCardTest {

    @Test
    void test() {

        StardustCard card;
        card = new StardustCard("10", CardLevel.LEVEL_II, "description");


        assertEquals("10", card.getId());

        assertEquals("description", card.getDescription());

        assertEquals(CardLevel.LEVEL_II, card.getLevel());


    }

}
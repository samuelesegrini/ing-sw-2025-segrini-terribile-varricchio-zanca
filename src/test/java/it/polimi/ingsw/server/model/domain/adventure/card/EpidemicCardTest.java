package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;

import javax.smartcardio.Card;

import static org.junit.jupiter.api.Assertions.*;

class EpidemicCardTest {
    @Test void setup(){
        EpidemicCard card;
        card = new EpidemicCard("10", CardLevel.TEST_FLIGHT, "description");
        assertEquals("10", card.getId());
        assertEquals("description", card.getDescription());
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel());
    }

}
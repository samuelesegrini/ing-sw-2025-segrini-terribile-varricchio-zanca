package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbandonedShipCardTest {
    @Test
    void setup(){
        AbandonedShipCard ship1;
        ship1 = new AbandonedShipCard("10", CardLevel.TEST_FLIGHT, "description", 3, 4, 5 );
        assertEquals("10", ship1.getId());
        assertEquals(CardLevel.TEST_FLIGHT, ship1.getLevel());
        assertEquals("description", ship1.getDescription());
        assertEquals(3, ship1.getCrewLost());
        assertEquals(4, ship1.getCreditsGained());
        assertEquals(5, ship1.getLostDays());
        assertEquals(false, ship1.isVisited());
        ship1.setVisited();
        assertEquals(true, ship1.isVisited());

    }
}

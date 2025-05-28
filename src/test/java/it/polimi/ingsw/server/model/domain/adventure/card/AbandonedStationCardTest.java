package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbandonedStationCardTest {

    @Test

    void test() {

        AbandonedStationCard card;

        Map<GoodType, Integer> goodQuantities;
        goodQuantities = new HashMap<>();

        goodQuantities.put(GoodType.RED, 3);
        goodQuantities.put(GoodType.YELLOW, 2);
        goodQuantities.put(GoodType.GREEN, 5);
        goodQuantities.put(GoodType.BLUE, 4);

        card = new AbandonedStationCard("10", CardLevel.LEVEL_II, "description", 4, 5, goodQuantities);



        assertEquals("10", card.getId());

        assertEquals("description", card.getDescription());

        assertEquals(CardLevel.LEVEL_II, card.getLevel());

        assertEquals(4, card.getMinCrewRequired());

        assertEquals(5, card.getLostDays());

        assertEquals(goodQuantities, card.getGoodQuantities());

        assertFalse(card.isVisited);

        card.setVisited();

        assertTrue(card.isVisited);




    }


}














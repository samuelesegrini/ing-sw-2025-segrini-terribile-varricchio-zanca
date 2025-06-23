package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmugglersCardTest {

    @Test

    void test(){

        Map<GoodType, Integer> availableGoods;
        availableGoods = new HashMap<>();

        availableGoods.put(GoodType.RED, 1);
        availableGoods.put(GoodType.YELLOW, 3);
        availableGoods.put(GoodType.GREEN, 7);
        availableGoods.put(GoodType.BLUE, 5);

        SmugglersCard card;

        card = new SmugglersCard("10", CardLevel.TEST_FLIGHT, "description", 5, 4, 3, availableGoods);


        assertEquals("10", card.getId());

        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel());

        assertEquals("description", card.getDescription());

        assertEquals(5, card.getPowerLevel());

        assertEquals(4, card.getMovementPenalty());

        assertEquals(3, card.getGoodsLostIfDefeated());

        assertEquals(availableGoods, card.getAvailableGoods());

        assertEquals(1, card.getAvailableGoods().get(GoodType.RED));

        assertEquals(3, card.getAvailableGoods().get(GoodType.YELLOW));

        assertEquals(7, card.getAvailableGoods().get(GoodType.GREEN));

        assertEquals(5, card.getAvailableGoods().get(GoodType.BLUE));

    }


}










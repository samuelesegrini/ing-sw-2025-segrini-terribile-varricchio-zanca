package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanetTest {

    @Test

    void testPlanet() {

        Planet planet;

        Map<GoodType, Integer> goodQuantities;
        goodQuantities = new HashMap<>();
        goodQuantities.put(GoodType.RED, 2);
        goodQuantities.put(GoodType.YELLOW, 3);
        goodQuantities.put(GoodType.GREEN, 4);
        goodQuantities.put(GoodType.BLUE, 5);

        planet = new Planet(3, goodQuantities);


        assertEquals(3, planet.getNumber());

        assertEquals(goodQuantities, planet.getGoodQuantities());

        assertEquals(3, planet.getGoodQuantities().get(GoodType.YELLOW));

        assertEquals(4, planet.getGoodQuantities().get(GoodType.GREEN));

        assertEquals(5, planet.getGoodQuantities().get(GoodType.BLUE));

        assertEquals(2, planet.getGoodQuantities().get(GoodType.RED));

        assertFalse(planet.isVisited());

        planet.setVisited();

        assertTrue(planet.isVisited());

        assertEquals(14, planet.getTotalGoodsQuantity());

        assertEquals(2, planet.getQuantityByType(GoodType.RED));

    }

}








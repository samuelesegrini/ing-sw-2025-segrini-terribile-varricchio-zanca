package it.polimi.ingsw.server.model.domain.adventure.card;


import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;


class PlanetsCardTest {


    @Test
    void test() {

        PlanetsCard card;
        List<Planet> planets;
        planets = new ArrayList<>();
        Planet planet1, planet2;

        Map<GoodType, Integer> goods1, goods2;
        goods1 = new HashMap<>();
        goods1.put(GoodType.RED, 2);
        goods1.put(GoodType.YELLOW, 3);
        goods1.put(GoodType.GREEN, 4);
        goods1.put(GoodType.BLUE, 5);

        goods2 = new HashMap<>();
        goods2.put(GoodType.YELLOW, 6);
        goods2.put(GoodType.GREEN, 7);
        goods2.put(GoodType.BLUE, 8);

        planet1 = new Planet(2, goods1);
        planet2 = new Planet(3, goods2);

        planets.add(planet1);
        planets.add(planet2);

        card = new PlanetsCard("10", CardLevel.LEVEL_II, "description", 3, planets);



        assertEquals("10", card.getId());

        assertEquals("description", card.getDescription());

        assertEquals(3, card.getLostDays());

        assertEquals(2, card.getPlanets().size());

        assertEquals(planets, card.getPlanets());

        assertEquals(planet1, card.getPlanets().get(0));

    }


}
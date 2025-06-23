package it.polimi.ingsw.server.model.domain.flight;


import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;



class RouteTest {

    @Test

    void test(){


        List<Integer> startingPosition;
        startingPosition = new ArrayList<>();

        startingPosition.add(6);
        startingPosition.add(4);
        startingPosition.add(2);
        startingPosition.add(1);


        Map<PlayerOrder, Integer> positionBonus;
        positionBonus = new HashMap<>();


        positionBonus.put(PlayerOrder.FIRST, 7);
        positionBonus.put(PlayerOrder.SECOND, 4);
        positionBonus.put(PlayerOrder.THIRD, 2);
        positionBonus.put(PlayerOrder.FOURTH, 1);



        Map<GoodType, Integer> resourceBonus;
        resourceBonus = new HashMap<>();
        resourceBonus.put(GoodType.RED, 4);
        resourceBonus.put(GoodType.YELLOW, 3);
        resourceBonus.put(GoodType.GREEN, 2);
        resourceBonus.put(GoodType.BLUE, 1);


        RewardSystem rewardSystem;
        rewardSystem = new RewardSystem(GameLevel.LEVEL_II, positionBonus, resourceBonus, 5, 1 );



        Route route;
        route = new Route(GameLevel.LEVEL_II, 20, startingPosition, rewardSystem);



        assertEquals(20, route.getLength());

        assertEquals(startingPosition, route.getStartingPositions());

        assertEquals(rewardSystem, route.getRewardSystem());

        assertEquals(startingPosition, route.getAllAvailableStartingPositions());

        assertEquals(6, route.getFirstAvailableStartingPosition());




    }

}















package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RewardSystemTest {


    @Test

    void test(){

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

        List<Player> finishOrder;
        finishOrder = new ArrayList<>();

        Player player1;
        Player player2;
        Player player3;
        Player player4;

        player1 = new Player(new PlayerId (new UUID(1,4), "manu"), PlayerColor.BLUE);
        player2 = new Player(new PlayerId(new UUID(1,4), "samu"), PlayerColor.GREEN);
        player3 = new Player(new PlayerId(new UUID(1,4), "ale"), PlayerColor.RED);
        player4 = new Player(new PlayerId(new UUID(1,4), "diego"), PlayerColor.YELLOW);


        finishOrder.add(player1);
        finishOrder.add(player2);
        finishOrder.add(player3);
        finishOrder.add(player4);

        assertEquals(7, rewardSystem.calculatePositionBonus(finishOrder, player1));

        assertEquals(4, rewardSystem.calculatePositionBonus(finishOrder, player2));

        assertEquals(2, rewardSystem.calculatePositionBonus(finishOrder, player3));

        assertEquals(1, rewardSystem.calculatePositionBonus(finishOrder, player4));

        Ship ship;
        ship = new Ship(player1, GameLevel.LEVEL_II);


        Map<GoodType, Integer> resources;
        resources = new HashMap<>();

        resources.put(GoodType.RED, 4);
        resources.put(GoodType.YELLOW, 3);


        ship.setResources(resources);

        assertEquals(25, rewardSystem.calculateResourceBonus(ship));

    }

}

















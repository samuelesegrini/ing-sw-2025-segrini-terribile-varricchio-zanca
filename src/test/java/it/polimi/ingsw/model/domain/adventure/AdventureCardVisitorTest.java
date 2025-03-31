package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.domain.adventure.card.AbandonedShipCard;
import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.adventure.card.PlanetsCard;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.domain.flight.FlightBoard;
import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdventureCardVisitorTest {

    private AdventureCardVisitor visitor;
    private GameConfigurationManager configManager;
    private GameModel gameModel;
    private FlightBoard flightBoard;
    private Player player1, player2;
    private PlayerId playerId1, playerId2;
    private Ship ship1, ship2;

    @BeforeEach
    void setUp() {
        visitor = new AdventureCardVisitor();
        configManager = new GameConfigurationManager();

        playerId1 = new PlayerId(UUID.randomUUID(), "Manuela");
        playerId2 = new PlayerId(UUID.randomUUID(), "Diego");

        player1 = new Player(playerId1);
        player2 = new Player(playerId2);

        ship1 = new Ship(player1);
        ship2 = new Ship(player2);
        player1.setShip(ship1);
        player2.setShip(ship2);

        gameModel = new GameModel(GameLevel.TEST_FLIGHT, configManager, 2);
        flightBoard = gameModel.getFlightBoard();
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED,0);
        resources.put(GoodType.BLUE,0);
        resources.put(GoodType.GREEN,0);
        resources.put(GoodType.YELLOW,0);
        ship1.setResources(resources);
        ship2.setResources(resources);

    }
    //ABANDONED SHIP TESTS
    @Test
    void testVisitAbandonedShipCard_PlayerWithEnoughCrew_RepairsShip() {
        ship1.setCrew(3);
        ship2.setCrew(5);
        AbandonedShipCard card = new AbandonedShipCard("X1", CardLevel.TEST_FLIGHT, "Nave abbandonata",
                1, 10, 2);

        boolean result = visitor.visitAbandonedShipCard(card, gameModel);

        assertTrue(result, "La nave dovrebbe essere riparata.");
        assertTrue(card.isVisited(), "La carta dovrebbe essere segnata come visitata.");
        assertEquals(10, player1.getCredits(), "Il giocatore dovrebbe guadagnare 10 crediti.");
        assertEquals(2, player1.getShip().getCrew(), "Il giocatore dovrebbe perdere 1 crew.");
    }

    @Test
    void testVisitAbandonedShipCard_PlayerWithoutEnoughCrew_CannotRepair() {
        AbandonedShipCard card = new AbandonedShipCard("X2", CardLevel.TEST_FLIGHT, "Nave abbandonata",
                6, 10, 2);

        boolean result = visitor.visitAbandonedShipCard(card, gameModel);

        assertFalse(result, "La nave non dovrebbe essere riparata.");
        assertFalse(card.isVisited(), "La carta non dovrebbe essere segnata come visitata.");
        assertEquals(3, player1.getShip().getCrew(), "Il giocatore dovrebbe avere ancora 1 crew.");
        assertEquals(5, player2.getShip().getCrew(), "Il giocatore dovrebbe avere ancora 3 crew.");
    }

    @Test
    void testVisitAbandonedShipCard_OnlyFirstPlayerWithEnoughCrew_RepairsShip() {
        player1.getShip().setCrew(1); // Non abbastanza crew
        player2.getShip().setCrew(3); // Abbastanza crew

        AbandonedShipCard card = new AbandonedShipCard("X3", CardLevel.TEST_FLIGHT, "Nave abbandonata",
                2, 5, 1); // Richiede 2 crew

        boolean result = visitor.visitAbandonedShipCard(card, gameModel);

        assertTrue(result, "Il secondo giocatore dovrebbe riparare la nave.");
        assertTrue(card.isVisited(), "La carta dovrebbe essere segnata come visitata.");
        assertEquals(5, player2.getCredits(), "Il secondo giocatore dovrebbe guadagnare 5 crediti.");
        assertEquals(1, player2.getShip().getCrew(), "Il secondo giocatore dovrebbe perdere 2 crew.");
        assertEquals(1, player1.getShip().getCrew(), "Il primo giocatore non dovrebbe aver perso crew.");
    }

    @Test
    void testVisitAbandonedShipCard_NoPlayerHasEnoughCrew_NobodyRepairs() {
        player1.getShip().setCrew(1);
        player2.getShip().setCrew(1);

        AbandonedShipCard card = new AbandonedShipCard("X4", CardLevel.LEVEL_II, "Nave abbandonata",
                2, 10, 1); // Richiede 2 crew (troppo per entrambi)

        boolean result = visitor.visitAbandonedShipCard(card, gameModel);

        assertFalse(result, "Nessun giocatore dovrebbe riparare la nave.");
        assertFalse(card.isVisited(), "La carta non dovrebbe essere segnata come visitata.");
        assertEquals(1, player1.getShip().getCrew(), "Il primo giocatore dovrebbe avere ancora 1 crew.");
        assertEquals(1, player2.getShip().getCrew(), "Il secondo giocatore dovrebbe avere ancora 1 crew.");
    }

    //PLANETS CARD TESTS
    @Test
    public void testVisitPlanetsCard_PlayerLandsOnPlanet_EnoughCapacity() {
        player1.getFlightData().setPosition(3,10);
        player2.getFlightData().setPosition(7,10);

        ship1.setNormalGoodsCapacity(10);
        ship2.setNormalGoodsCapacity(10);
        ship1.setSpecialGoodsCapacity(10);
        ship2.setSpecialGoodsCapacity(10);

        Planet planetX = new Planet("PlanetX", Map.of(GoodType.RED, 2, GoodType.BLUE, 3));
        Planet planetY = new Planet("PlanetY", Map.of(GoodType.GREEN, 1, GoodType.BLUE, 2, GoodType.YELLOW, 1));
        PlanetsCard planetsCard = new PlanetsCard("P1", CardLevel.TEST_FLIGHT, "Visita pianeti",
                2, List.of(planetX, planetY));

        boolean result = planetsCard.accept(visitor, gameModel);

        assertTrue(result);
        assertTrue(planetX.isVisited());

        assertEquals(1, player1.getFlightData().getPosition());
        assertEquals(2, ship1.getResources().get(GoodType.RED));
        assertEquals(3, ship1.getResources().get(GoodType.BLUE));

        assertEquals(5, player2.getFlightData().getPosition());
        assertEquals(1, ship2.getResources().get(GoodType.GREEN));
        assertEquals(2, ship2.getResources().get(GoodType.BLUE));
        assertEquals(1, ship2.getResources().get(GoodType.YELLOW));
    }

    @Test
    public void testVisitPlanetsCard_PlayerCannotLand_NoSpaceForSpecialGoods() {
        flightBoard.abandonPlayer(player2);
        player1.getFlightData().setPosition(3,10);

        ship1.setSpecialGoodsCapacity(0);
        ship1.setNormalGoodsCapacity(0);

        Planet planetX = new Planet("PlanetX", Map.of(GoodType.RED, 2, GoodType.BLUE, 3));
        PlanetsCard planetsCard = new PlanetsCard("P1", CardLevel.TEST_FLIGHT, "Visita pianeti",
                2, List.of(planetX));

        boolean result = planetsCard.accept(visitor, gameModel);

        assertFalse(result);
        assertFalse(planetX.isVisited());

        assertEquals(3, player1.getFlightData().getPosition());
        assertEquals(0, ship1.getResources().get(GoodType.RED));
        assertEquals(0, ship1.getResources().get(GoodType.RED));
    }
}


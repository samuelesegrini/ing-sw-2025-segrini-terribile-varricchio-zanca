package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.flight.Route;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.*;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.server.model.enums.adventure.PenaltyType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.flight.FlightStatus;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
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
//
//    @BeforeEach
//    void setUp() {
//        visitor = new AdventureCardVisitor();
//        configManager = new GameConfigurationManager();
//
//        playerId1 = new PlayerId(UUID.randomUUID(), "Manuela");
//        playerId2 = new PlayerId(UUID.randomUUID(), "Diego");
//
//        player1 = new Player(playerId1);
//        player2 = new Player(playerId2);
//
//        ship1 = new Ship(player1, GameLevel.LEVEL_II);
//        ship2 = new Ship(player2, GameLevel.LEVEL_II);
//        player1.setShip(ship1);
//        player2.setShip(ship2);
//
//        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
//
//        // Add players to the game model
//        gameModel.addPlayer(playerId1, "Manuela");
//        gameModel.addPlayer(playerId2, "Diego");
//
//        Route routeLevelII = new Route(GameLevel.LEVEL_II, 24, List.of(6,3), null);
//
//        // Create a mock FlightBoard for testing
//        flightBoard = new FlightBoard(GameLevel.LEVEL_II, routeLevelII, 2);
//
//        // Register players with the flight board
//        flightBoard.registerPlayer(player1);
//        flightBoard.registerPlayer(player2);
//
//        // Set the flight board in the game model using reflection to avoid initialization
//        try {
//            java.lang.reflect.Field flightBoardField = GameModel.class.getDeclaredField("flightBoard");
//            flightBoardField.setAccessible(true);
//            flightBoardField.set(gameModel, flightBoard);
//        } catch (Exception e) {
//            fail("Failed to set flight board in game model: " + e.getMessage());
//        }
//    }
//
//    //ABANDONED SHIP TESTS
//    @Test
//    void testVisitAbandonedShipCard_PlayerWithEnoughCrew_RepairsShip() {
//        ship1.setCrew(3);
//        ship2.setCrew(5);
//        AbandonedShipCard card = new AbandonedShipCard("X1", CardLevel.TEST_FLIGHT, "Nave abbandonata",
//                1, 10, 2);
//
//        boolean result = visitor.visitAbandonedShipCard(card, gameModel);
//
//        assertEquals(10, player1.getCredits(), "Il giocatore dovrebbe guadagnare 10 crediti.");
//        assertEquals(2, player1.getShip().getCrew(), "Il giocatore dovrebbe perdere 1 crew.");
//    }
//
//    @Test
//    void testVisitAbandonedShipCard_PlayerWithoutEnoughCrew_CannotRepair() {
//        player1.getShip().setCrew(3);
//        player2.getShip().setCrew(5);
//        AbandonedShipCard card = new AbandonedShipCard("X2", CardLevel.TEST_FLIGHT, "Nave abbandonata",
//                6, 10, 2);
//
//        boolean result = visitor.visitAbandonedShipCard(card, gameModel);
//
//        assertEquals(3, player1.getShip().getCrew(), "Il giocatore dovrebbe avere ancora 1 crew.");
//        assertEquals(5, player2.getShip().getCrew(), "Il giocatore dovrebbe avere ancora 3 crew.");
//    }
//
//    @Test
//    void testVisitAbandonedShipCard_OnlyFirstPlayerWithEnoughCrew_RepairsShip() {
//        player1.getShip().setCrew(1); // Non abbastanza crew
//        player2.getShip().setCrew(3); // Abbastanza crew
//
//        AbandonedShipCard card = new AbandonedShipCard("X3", CardLevel.TEST_FLIGHT, "Nave abbandonata",
//                2, 5, 1); // Richiede 2 crew
//
//        boolean result = visitor.visitAbandonedShipCard(card, gameModel);
//
//        assertEquals(5, player2.getCredits(), "Il secondo giocatore dovrebbe guadagnare 5 crediti.");
//        assertEquals(1, player2.getShip().getCrew(), "Il secondo giocatore dovrebbe perdere 2 crew.");
//        assertEquals(1, player1.getShip().getCrew(), "Il primo giocatore non dovrebbe aver perso crew.");
//    }
//
//    @Test
//    void testVisitAbandonedShipCard_NoPlayerHasEnoughCrew_NobodyRepairs() {
//        player1.getShip().setCrew(1);
//        player2.getShip().setCrew(1);
//
//        AbandonedShipCard card = new AbandonedShipCard("X4", CardLevel.LEVEL_II, "Nave abbandonata",
//                2, 10, 1); // Richiede 2 crew (troppo per entrambi)
//
//        boolean result = visitor.visitAbandonedShipCard(card, gameModel);
//
//        assertEquals(1, player1.getShip().getCrew(), "Il primo giocatore dovrebbe avere ancora 1 crew.");
//        assertEquals(1, player2.getShip().getCrew(), "Il secondo giocatore dovrebbe avere ancora 1 crew.");
//    }
//
//    //PLANETS CARD TESTS
//    @Test
//    public void testVisitPlanetsCard_PlayerLandsOnPlanet_EnoughCapacity() {
//        player1.getFlightData().setPosition(3, 10);
//        player2.getFlightData().setPosition(7, 10);
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Component cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
//        Component cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 3);
//        Component cargoHold3 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 4);
//        Component cargoHold4 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 6);
//
//        ship1.addComponent(cargoHold1, new Position(2, 3));
//        ship1.addComponent(cargoHold2, new Position(3, 3));
//
//        ship2.addComponent(cargoHold3, new Position(3, 2));
//        ship2.addComponent(cargoHold4, new Position(3, 3));
//
//
//        Planet planetX = new Planet(1, Map.of(GoodType.RED, 2, GoodType.BLUE, 3));
//        Planet planetY = new Planet(2, Map.of(GoodType.GREEN, 1, GoodType.BLUE, 2, GoodType.YELLOW, 1));
//        PlanetsCard planetsCard = new PlanetsCard("P1", CardLevel.TEST_FLIGHT, "Visita pianeti",
//                2, List.of(planetX, planetY));
//
//        boolean result = planetsCard.accept(visitor, gameModel);
//
//        assertEquals(1, player1.getFlightData().getPosition());
//        assertEquals(2, ship1.getResources().get(GoodType.RED));
//        assertEquals(3, ship1.getResources().get(GoodType.BLUE));
//
//        assertEquals(5, player2.getFlightData().getPosition());
//        assertEquals(1, ship2.getResources().get(GoodType.GREEN));
//        assertEquals(2, ship2.getResources().get(GoodType.BLUE));
//        assertEquals(1, ship2.getResources().get(GoodType.YELLOW));
//    }
//
//    @Test
//    public void testVisitPlanetsCard_PlayerCannotLand_NoSpaceForSpecialGoods() {
//        flightBoard.abandonPlayer(player2);
//        player1.getFlightData().setPosition(3, 10);
//
//        ship1.setSpecialGoodsCapacity(0);
//        ship1.setNormalGoodsCapacity(0);
//
//        Planet planetX = new Planet(1, Map.of(GoodType.RED, 2, GoodType.BLUE, 3));
//        PlanetsCard planetsCard = new PlanetsCard("P1", CardLevel.TEST_FLIGHT, "Visita pianeti",
//                2, List.of(planetX));
//
//        boolean result = planetsCard.accept(visitor, gameModel);
//
//        assertEquals(3, player1.getFlightData().getPosition());
//        assertEquals(0, ship1.getResources().get(GoodType.RED));
//        assertEquals(0, ship1.getResources().get(GoodType.RED));
//    }
//
//    //OPEN SPACE CARD TESTS
//    @Test
//    void testVisitOpenSpaceCard() {
//        ship1.setEngines(3.0);
//        ship2.setEngines(6.0);
//
//        player1.getFlightData().setPosition(0, 18);
//        player2.getFlightData().setPosition(10, 18);
//
//        OpenSpaceCard openSpaceCard = new OpenSpaceCard("OS1", CardLevel.TEST_FLIGHT, "Spazio aperto");
//        boolean result = openSpaceCard.accept(visitor, gameModel);
//
//        assertEquals(3, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe essere avanzato di 3 posizioni");
//        assertEquals(16, player2.getFlightData().getPosition(), "Il giocatore 2 dovrebbe essere avanzato di 5 posizioni");
//    }
//
//    @Test
//    void testVisitOpenSpaceCard_PlayerWithZeroEngines_AbandonsGame() {
//        ship1.setEngines(0.0);
//        ship2.setEngines(6.0);
//
//        player1.getFlightData().setPosition(0, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        OpenSpaceCard openSpaceCard = new OpenSpaceCard("OS1", CardLevel.TEST_FLIGHT, "Spazio aperto");
//        boolean result = openSpaceCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//
//        assertNotSame(FlightStatus.RACING, flightBoard.getPlayerData(player1).getStatus(), "Il giocatore 1 dovrebbe aver abbandonato la partita");
//        assertEquals(6, player2.getFlightData().getPosition(), "Il giocatore 2 dovrebbe essere avanzato di 5 posizioni");
//    }
//
//    //STARDUST CARD TESTS
//    @Test
//    void testVisitStardustCard() {
//        player1.getFlightData().setPosition(5, 18);
//
//        Map<Direction, ConnectorType> connectors1 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL); //exposed component
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//        Map<Direction, ConnectorType> connectors2 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.SINGLE); //exposed component
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
//        Cannon component2 = new Cannon(ComponentType.CANNON_SINGLE, connectors2);
//        component1.setPosition(new Position(2, 3));
//        component2.setPosition(new Position(2, 4));
//
//        ship1.addComponent(component1, new Position(2, 3));
//        ship1.addComponent(component2, new Position(2, 4));
//
//        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");
//        boolean result = stardustCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(2, ship1.getExposedConnectors());
//        assertEquals(3, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizione");
//    }
//
//    @Test
//    void testVisitStardustCard_NoExposedConnectors() {
//        player1.getFlightData().setPosition(0, 18);
//
//        Map<Direction, ConnectorType> connectors1 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//        Map<Direction, ConnectorType> connectors2 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
//        Cannon component2 = new Cannon(ComponentType.CANNON_SINGLE, connectors2);
//        component1.setPosition(new Position(2, 3));
//        component2.setPosition(new Position(2, 4));
//
//        ship1.addComponent(component1, new Position(2, 3));
//        ship1.addComponent(component2, new Position(2, 4));
//
//        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");
//        boolean result = stardustCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(0, ship1.getExposedConnectors());
//        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe avanzare");
//        ;
//    }
//
//
//    @Test
//    void testVisitStardustCard_ShipsWithoutComponents() {
//        player1.getFlightData().setPosition(0, 18);
//        player2.getFlightData().setPosition(10, 18);
//
//        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");
//
//        boolean result = stardustCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe avanzare");
//        assertEquals(10, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe avanzare");
//    }
//
//    //SLAVERS CARD TESTS
//    @Test
//    void testVisitSlaversCard_PlayerDefeatsSlavers_AcceptsCredits() {
//        player1.getShip().setCannons(5.0); // Potenza sufficiente per sconfiggere gli schiavisti
//        player2.getShip().setCannons(3.0); // Potenza insufficiente per sconfiggere gli schiavisti
//
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        SlaversCard slaversCard = new SlaversCard("S1", CardLevel.LEVEL_II, "Encounter with Slavers",
//                4, 2, 10, 2);
//        boolean result = slaversCard.accept(visitor, gameModel);
//
//        assertEquals(10, player1.getCredits(), "Il giocatore 1 dovrebbe guadagnare 10 crediti");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizioni");
//        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe guadagnare crediti");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
//    }
//
//    @Test
//    void testVisitSlaversCard_NoPlayerDefeatsSlavers() {
//        player1.getShip().setCannons(2.0); // Potenza insufficiente per sconfiggere gli schiavisti
//        player2.getShip().setCannons(4.0); // Pareggio
//
//        player1.getShip().setCrew(4);
//        player2.getShip().setCrew(4);
//
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        SlaversCard slaversCard = new SlaversCard("S2", CardLevel.LEVEL_II, "Encounter with Slavers", 4, 2, 10, 2);
//
//        boolean result = slaversCard.accept(visitor, gameModel);
//
//        assertEquals(0, player1.getCredits(), "Il giocatore 1 non dovrebbe guadagnare crediti");
//        assertEquals(10, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe muoversi");
//        assertEquals(2, player1.getShip().getCrew(), "Il gocatore 1 dovrebbe aver perso 2 crew");
//
//        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe guadagnare crediti");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
//        assertEquals(4, player2.getShip().getCrew(), "Il gocatore 2 non dovrebbe aver perso crew");
//    }
//
//    //SMUGGLERS CARD TESTS
//    @Test
//    void testVisitSmugglersCard_PlayerDefeatsSmugglers() {
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//        player1.getShip().setCannons(5.0); // Potenza sufficiente per sconfiggere i contrabbandieri
//        player2.getShip().setCannons(3.0); // Potenza insufficiente per sconfiggere i contrabbandieri
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Component cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 10);
//        Component cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 10);
//        Component cargoHold3 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 10);
//        Component cargoHold4 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 10);
//
//        ship1.addComponent(cargoHold1, new Position(2, 3));
//        ship1.addComponent(cargoHold2, new Position(3, 3));
//
//        ship2.addComponent(cargoHold3, new Position(3, 2));
//        ship2.addComponent(cargoHold4, new Position(3, 3));
//
//
//        Map<GoodType, Integer> availableGoods = new HashMap<>();
//        availableGoods.put(GoodType.RED, 2);
//        availableGoods.put(GoodType.BLUE, 3);
//
//        SmugglersCard smugglersCard = new SmugglersCard("S1", CardLevel.LEVEL_II, "Encounter with Smugglers",
//                4, 2, 2, availableGoods);
//        boolean result = smugglersCard.accept(visitor, gameModel);
//
//        assertEquals(2, player1.getShip().getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe guadagnare 2 merci rosse");
//        assertEquals(3, player1.getShip().getResources().get(GoodType.BLUE), "Il giocatore 1 dovrebbe guadagnare 3 merci blu");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizioni");
//        assertEquals(0, player2.getShip().getResources().get(GoodType.RED), "Il giocatore 2 non dovrebbe guadagnare merci rosse");
//        assertEquals(0, player2.getShip().getResources().get(GoodType.BLUE), "Il giocatore 2 non dovrebbe guadagnare merci blu");
//    }
//
//    @Test
//    void testVisitSmugglersCard_NoPlayerDefeatsSmugglers() {
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Component cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 10);
//        Component cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 10);
//        Component cannon1 = new Cannon(ComponentType.CANNON_SINGLE, connectors);
//        Component cannon2 = new Cannon(ComponentType.CANNON_SINGLE, connectors);
//
//        Component cargoHold3 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 10);
//        Component cargoHold4 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 10);
//        Cannon cannon3 = new Cannon(ComponentType.CANNON_DOUBLE, connectors);
//        Cannon cannon4 = new Cannon(ComponentType.CANNON_DOUBLE, connectors);
//
//        cannon3.setCharged(true);
//        cannon4.setCharged(true);
//
//        ship1.addComponent(cannon1, new Position(1, 3));
//        ship1.addComponent(cargoHold1, new Position(2, 3));
//        ship1.addComponent(cargoHold2, new Position(3, 3));
//        ship1.addComponent(cannon2, new Position(3, 2));
//
//        ship2.addComponent(cannon4, new Position(3, 1));
//        ship2.addComponent(cargoHold3, new Position(3, 2));
//        ship2.addComponent(cargoHold4, new Position(3, 3));
//        ship2.addComponent(cannon3, new Position(3, 4));
//
//        Map<GoodType, Integer> mutableResources1 = new HashMap<>();
//        mutableResources1.put(GoodType.RED, 1);
//        mutableResources1.put(GoodType.BLUE, 2);
//
//        Map<GoodType, Integer> mutableResources2 = new HashMap<>();
//        mutableResources2.put(GoodType.RED, 2);
//        mutableResources2.put(GoodType.BLUE, 3);
//
//        player1.getShip().addResources(mutableResources1);
//        player2.getShip().addResources(mutableResources2);
//
//        Map<GoodType, Integer> availableGoods = new HashMap<>();
//        availableGoods.put(GoodType.RED, 2);
//        availableGoods.put(GoodType.BLUE, 3);
//
//        System.out.println("Cannon strengh player 1: " + player1.getShip().getCannons());
//        System.out.println("Cannon strengh player 2: " + player2.getShip().getCannons());
//
//        SmugglersCard smugglersCard = new SmugglersCard("S2", CardLevel.LEVEL_II, "Encounter with Smugglers",
//                4, 2, 2, availableGoods);
//
//        boolean result = smugglersCard.accept(visitor, gameModel);
//
//        assertEquals(0, player1.getShip().getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe perdere tutte le merci rosse");
//        assertEquals(1, player1.getShip().getResources().get(GoodType.BLUE), "Il giocatore dovrebbe perdere 1 merce blu");
//        assertEquals(10, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe muoversi");
//
//        assertEquals(2, player2.getShip().getResources().get(GoodType.RED), "Il giocatore 2 dovrebbe mantenere le merci rosse");
//        assertEquals(3, player2.getShip().getResources().get(GoodType.BLUE), "Il giocatore 2 dovrebbe mantenere le merci blu");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
//    }
//
//    //EPIDEMIC CARD TESTS
//    @Test
//    void testVisitEpidemicCard_PlayerWithAdjacentCabin() {
//        player1.getFlightData().setPosition(0, 18);
//        ship1.setCrew(3);
//        Map<Direction, ConnectorType> connectors1 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//        Map<Direction, ConnectorType> connectors2 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.SINGLE);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Map<Direction, ConnectorType> connectors3 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//
//        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
//        Cabin component2 = new Cabin(ComponentType.CABIN_START, connectors2);
//        Cabin component3 = new Cabin(ComponentType.CABIN, connectors3);
//        component1.setPosition(new Position(2, 3));
//        component2.setPosition(new Position(2, 4));
//        component3.setPosition(new Position(1, 4));
//
//        ship1.addComponent(component1, new Position(2, 3));
//        ship1.addComponent(component2, new Position(2, 4));
//        ship1.addComponent(component3, new Position(1, 4));
//
//        EpidemicCard epidemicCard = new EpidemicCard("E1", CardLevel.LEVEL_II, "Epidemic event");
//        boolean result = epidemicCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(0, ship1.getCrew(), "Il giocatore 1 dovrebbe perdere tutto l'equipaggio");
//    }
//
//    @Test
//    void testVisitEpidemicCard_PlayerWithoutAdjacentCabin() {
//        player1.getFlightData().setPosition(0, 18);
//        ship1.setCrew(3);
//        Map<Direction, ConnectorType> connectors1 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//        Map<Direction, ConnectorType> connectors2 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.SINGLE);
//            put(Direction.DOWN, ConnectorType.PLAIN);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Map<Direction, ConnectorType> connectors3 = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.PLAIN);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.PLAIN);
//            put(Direction.LEFT, ConnectorType.PLAIN);
//        }};
//
//        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
//        Engine component2 = new Engine(ComponentType.ENGINE_SINGLE, connectors2, id);
//        Cabin component3 = new Cabin(ComponentType.CABIN, connectors3);
//        component1.setPosition(new Position(2, 3));
//        component2.setPosition(new Position(2, 4));
//        component3.setPosition(new Position(1, 5));
//
//        ship1.addComponent(component1, new Position(2, 3));
//        ship1.addComponent(component2, new Position(2, 4));
//        ship1.addComponent(component3, new Position(1, 5));
//
//        EpidemicCard epidemicCard = new EpidemicCard("E1", CardLevel.LEVEL_II, "Epidemic event");
//        boolean result = epidemicCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(3, ship1.getCrew(), "Il giocatore dovrebbe mantenere tutto l'equipaggio");
//    }
//
//    //ABANDONED STATION TESTS
//    @Test
//    void testVisitAbandonedStationCard_PlayerWithEnoughCrew_LootsStation() {
//        player1.getFlightData().setPosition(10, 18);
//        ship1.setCrew(5);
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Component cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 10);
//        Component cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 10);
//        ship1.addComponent(cargoHold1, new Position(2, 3));
//        ship1.addComponent(cargoHold2, new Position(3, 3));
//
//        Map<GoodType, Integer> availableGoods = new HashMap<>();
//        availableGoods.put(GoodType.RED, 2);
//        availableGoods.put(GoodType.BLUE, 3);
//
//        AbandonedStationCard abandonedStationCard = new AbandonedStationCard("A1", CardLevel.LEVEL_II, "Abandoned Station",
//                3, 2, availableGoods);
//        boolean result = abandonedStationCard.accept(visitor, gameModel);
//
//        assertEquals(2, ship1.getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe ottenere 2 merci rosse");
//        assertEquals(3, ship1.getResources().get(GoodType.BLUE), "Il giocatore 1 dovrebbe ottenere 3 merci blu");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe perdere 2 giorni di volo");
//    }
//
//    @Test
//    void testVisitAbandonedStationCard_PlayerWithoutEnoughCrew_CannotLootStation() {
//        player1.getFlightData().setPosition(0, 18);
//        player2.getFlightData().setPosition(10, 18);
//        ship1.setCrew(2);
//        ship2.setCrew(1);
//        ship1.setNormalGoodsCapacity(10);
//        ship1.setSpecialGoodsCapacity(5);
//
//        Map<GoodType, Integer> availableGoods = new HashMap<>();
//        availableGoods.put(GoodType.RED, 2);
//        availableGoods.put(GoodType.BLUE, 3);
//
//        AbandonedStationCard abandonedStationCard = new AbandonedStationCard("A1", CardLevel.LEVEL_II, "Abandoned Station",
//                3, 2, availableGoods);
//        boolean result = abandonedStationCard.accept(visitor, gameModel);
//
//        assertEquals(0, ship1.getResources().get(GoodType.RED), "Il giocatore 1 non dovrebbe ottenere merci rosse");
//        assertEquals(0, ship1.getResources().get(GoodType.BLUE), "Il giocatore 1 non dovrebbe ottenere merci blu");
//        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe perdere giorni di volo");
//    }
//
//    //METEOR SWARM CARD TESTS
//    @Test
//    void testVisitMeteorSwarmCard_ShipProtectedByShield() {
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Shield shield = new Shield(ComponentType.SHIELD, connectors);
//        shield.setProtectedDirections(Set.of(Direction.UP, Direction.RIGHT));
//        shield.setPosition(new Position(2, 3));
//        ship1.addComponent(shield, new Position(2, 3));
//
//        List<Meteor> meteorPattern = List.of(
//                new Meteor(ShotIntensity.LIGHT, Direction.UP)
//        );
//
//        MeteorSwarmCard meteorSwarmCard = new MeteorSwarmCard("M1", CardLevel.LEVEL_II, "Meteor Swarm", meteorPattern);
//        boolean result = meteorSwarmCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertNotNull(ship1.getBoard()[2][3], "Il componente non dovrebbe essere rimosso");
//    }
//
//    @Test
//    void testVisitMeteorSwarmCard_ShipNotProtected() {
//        player1.getFlightData().setPosition(0, 18);
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Cabin cabin = new Cabin(ComponentType.CABIN, connectors);
//        cabin.setPosition(new Position(2, 3));
//        ship1.addComponent(cabin, new Position(2, 3));
//
//        List<Meteor> meteorPattern = List.of(
//                new Meteor(ShotIntensity.LIGHT, Direction.UP)
//        );
//
//        MeteorSwarmCard meteorSwarmCard = new MeteorSwarmCard("M1", CardLevel.LEVEL_II, "Meteor Swarm", meteorPattern);
//        boolean result = meteorSwarmCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//    }
//
//    @Test
//    void testVisitMeteorSwarmCard_ShipProtectedByCannon() {
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//
//        Cannon cannon = new Cannon(ComponentType.CANNON_SINGLE, connectors);
//        cannon.setPosition(new Position(2, 3));
//        cannon.rotate(); //ora colpo da dx
//        Cabin cabin = new Cabin(ComponentType.CABIN, connectors);
//        cabin.setPosition(new Position(1, 3));
//        Engine engine = new Engine(ComponentType.ENGINE_SINGLE, connectors, id);
//        engine.setPosition(new Position(2, 5));
//        ship1.addComponent(cannon, new Position(2, 3));
//        ship1.addComponent(cabin, new Position(1, 3));
//        ship1.addComponent(engine, new Position(3, 3));
//
//        List<Meteor> meteorPattern = List.of(
//                new Meteor(ShotIntensity.HEAVY, Direction.RIGHT)
//        );
//        MeteorSwarmCard meteorSwarmCard = new MeteorSwarmCard("M1", CardLevel.LEVEL_II, "Meteor Swarm", meteorPattern);
//        boolean result = meteorSwarmCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertNotNull(ship1.getBoard()[2][3], "Il componente non dovrebbe essere rimosso");
//        assertNotNull(ship1.getBoard()[3][3], "Il componente non dovrebbe essere rimosso");
//        assertNotNull(ship1.getBoard()[1][3], "Il componente non dovrebbe essere rimosso");
//    }
//
//    //PIRATE CARD TESTS
//    @Test
//    void testVisitPiratesCard_HeavyShot() {
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        ship1.setCannons(5); // Giocatore 1 ha cannoni sufficienti
//        ship2.setCannons(2); // Giocatore 2 ha cannoni insufficienti (ma ha uno scudo)
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Shield shield = new Shield(ComponentType.SHIELD, connectors);
//        shield.setProtectedDirections(Set.of(Direction.UP, Direction.RIGHT));
//        shield.setPosition(new Position(2, 3));
//        ship2.addComponent(shield, new Position(2, 3));
//
//        List<CannonFire> cannonFires = List.of(
//                new CannonFire(Direction.UP, ShotIntensity.HEAVY),
//                new CannonFire(Direction.RIGHT, ShotIntensity.HEAVY)
//        );
//
//        PiratesCard piratesCard = new PiratesCard("P1", CardLevel.LEVEL_II, "Pirates",
//                3, 2, 5, cannonFires);
//        boolean result = piratesCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(5, player1.getCredits(), "Il giocatore 1 dovrebbe ricevere 5 crediti");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe perdere 2 giorni di volo");
//        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe ricevere crediti");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe perdere giorni di volo");
//    }
//
//    @Test
//    void testVisitPiratesCard_LightShot() {
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//
//        ship1.setCannons(5); // Giocatore 1 ha cannoni sufficienti
//        ship2.setCannons(2); // Giocatore 2 ha cannoni insufficienti (ma ha uno scudo)
//
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Shield shield = new Shield(ComponentType.SHIELD, connectors);
//        shield.setProtectedDirections(Set.of(Direction.UP, Direction.RIGHT));
//        shield.setPosition(new Position(2, 3));
//        ship2.addComponent(shield, new Position(2, 3));
//
//        List<CannonFire> cannonFires = List.of(
//                new CannonFire(Direction.UP, ShotIntensity.LIGHT),
//                new CannonFire(Direction.RIGHT, ShotIntensity.LIGHT)
//        );
//
//        PiratesCard piratesCard = new PiratesCard("P1", CardLevel.LEVEL_II, "Pirates",
//                3, 2, 5, cannonFires);
//        boolean result = piratesCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(5, player1.getCredits(), "Il giocatore 1 dovrebbe ricevere 5 crediti");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe perdere 2 giorni di volo");
//        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe ricevere crediti");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe perdere giorni di volo");
//    }
//
//    //COMBAT ZONE CREDITS TESTS
//    @Test
//    void testVisitCombatZoneCard_CrewLoss_FlightDaysLoss() {
//        player1.getFlightData().setPosition(10, 18);
//        player2.getFlightData().setPosition(0, 18);
//        player1.getShip().setCrew(5);
//        player2.getShip().setCrew(3); //combat loser for check1 is player2
//        player1.getShip().setCannons(3); //combat loser for check2 is player1
//        player2.getShip().setCannons(5);
//
//
//        CombatZoneCard combatZoneCard = new CombatZoneCard("CZ1", CardLevel.LEVEL_II, "Combat Zone");
//        combatZoneCard.addCombatCheck(new CombatCheck(CombatAttributeType.CREW_COUNT, PenaltyType.CREW_LOSS, 2));
//        combatZoneCard.addCombatCheck(new CombatCheck(CombatAttributeType.ENGINE_POWER, PenaltyType.FLIGHT_DAYS_LOSS, 2));
//
//        boolean result = combatZoneCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//        assertEquals(5, player1.getShip().getCrew(), "Il giocatore 1 non dovrebbe perdere membri dell'equipaggio");
//        assertEquals(1, player2.getShip().getCrew(), "Il giocatore 2 dovrebbe perdere 2 membri dell'equipaggio");
//        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe perdere due giorni di volo");
//        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe perdere gorni di volo");
//    }
//    @Test
//    void testVisitCombatZoneCard_CannonFire() {
//        player1.getShip().setCannons(3); //combat loser is player1
//        player2.getShip().setCannons(5);
//        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
//            put(Direction.UP, ConnectorType.UNIVERSAL);
//            put(Direction.DOWN, ConnectorType.UNIVERSAL);
//            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
//            put(Direction.LEFT, ConnectorType.UNIVERSAL);
//        }};
//        Cabin cabin = new Cabin(ComponentType.CABIN, connectors);
//        cabin.setPosition(new Position(2, 3));
//        player2.getShip().addComponent(cabin, new Position(2, 3));
//
//        CannonFire cannonFire = new CannonFire(Direction.UP, ShotIntensity.LIGHT);
//        CombatZoneCard combatZoneCard = new CombatZoneCard("CZ3", CardLevel.LEVEL_II, "Combat Zone");
//        combatZoneCard.addCombatCheck(new CombatCheck(CombatAttributeType.CANNON_STRENGTH, List.of(cannonFire)));
//
//        boolean result = combatZoneCard.accept(visitor, gameModel);
//
//        assertTrue(result, "Il risultato dovrebbe essere true");
//    }
}
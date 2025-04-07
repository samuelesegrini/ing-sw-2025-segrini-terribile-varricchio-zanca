package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.domain.adventure.card.*;
import it.polimi.ingsw.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.domain.flight.FlightBoard;
import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.*;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;
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

        ship1 = new Ship(player1, GameLevel.TEST_FLIGHT);
        ship2 = new Ship(player2, GameLevel.TEST_FLIGHT);
        player1.setShip(ship1);
        player2.setShip(ship2);

        gameModel = new GameModel(GameLevel.TEST_FLIGHT, configManager, 2);
        flightBoard = gameModel.getFlightBoard();
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
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
        player1.getShip().setCrew(3);
        player2.getShip().setCrew(5);
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

    //OPEN SPACE CARD TESTS
    @Test
    void testVisitOpenSpaceCard() {
        ship1.setEngines(3.0);
        ship2.setEngines(6.0);

        player1.getFlightData().setPosition(0, 18);
        player2.getFlightData().setPosition(10, 18);

        OpenSpaceCard openSpaceCard = new OpenSpaceCard("OS1", CardLevel.TEST_FLIGHT, "Spazio aperto");
        boolean result = openSpaceCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");

        assertEquals(3, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe essere avanzato di 3 posizioni");
        assertEquals(16, player2.getFlightData().getPosition(), "Il giocatore 2 dovrebbe essere avanzato di 5 posizioni");
    }

    @Test
    void testVisitOpenSpaceCard_PlayerWithZeroEngines_AbandonsGame() {
        ship1.setEngines(0.0);
        ship2.setEngines(6.0);

        player1.getFlightData().setPosition(0, 18);
        player2.getFlightData().setPosition(0, 18);

        OpenSpaceCard openSpaceCard = new OpenSpaceCard("OS1", CardLevel.TEST_FLIGHT, "Spazio aperto");
        boolean result = openSpaceCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");

        assertFalse(flightBoard.getPlayerData(player1).getStatus() == FlightStatus.RACING, "Il giocatore 1 dovrebbe aver abbandonato la partita");
        assertEquals(6, player2.getFlightData().getPosition(), "Il giocatore 2 dovrebbe essere avanzato di 5 posizioni");
    }

    //STARDUST CARD TESTS
    @Test
    void testVisitStardustCard() {
        player1.getFlightData().setPosition(5, 18);

        Map<Direction, ConnectorType> connectors1 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.UNIVERSAL); //exposed component
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};
        Map<Direction, ConnectorType> connectors2 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.SINGLE); //exposed component
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};

        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
        Cannon component2 = new Cannon(ComponentType.CANNON_SINGLE, connectors2);
        component1.setPosition(new Position(2, 3));
        component2.setPosition(new Position(2, 4));

        ship1.addComponent(component1, new Position(2, 3));
        ship1.addComponent(component2, new Position(2, 4));

        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");
        boolean result = stardustCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertEquals(2, ship1.getExposedConnectors());
        assertEquals(3, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizione");
    }

    @Test
    void testVisitStardustCard_NoExposedConnectors() {
        player1.getFlightData().setPosition(0, 18);

        Map<Direction, ConnectorType> connectors1 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};
        Map<Direction, ConnectorType> connectors2 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};

        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
        Cannon component2 = new Cannon(ComponentType.CANNON_SINGLE, connectors2);
        component1.setPosition(new Position(2, 3));
        component2.setPosition(new Position(2, 4));

        ship1.addComponent(component1, new Position(2, 3));
        ship1.addComponent(component2, new Position(2, 4));

        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");
        boolean result = stardustCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertEquals(0, ship1.getExposedConnectors());
        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe avanzare");;
    }


    @Test
    void testVisitStardustCard_ShipsWithoutComponents() {
        player1.getFlightData().setPosition(0, 18);
        player2.getFlightData().setPosition(10, 18);

        StardustCard stardustCard = new StardustCard("S1", CardLevel.TEST_FLIGHT, "Stardust event");

        boolean result = stardustCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe avanzare");
        assertEquals(10, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe avanzare");
    }

    //SLAVERS CARD TESTS
    @Test
    void testVisitSlaversCard_PlayerDefeatsSlavers_AcceptsCredits() {
        player1.getShip().setCannons(5.0); // Potenza sufficiente per sconfiggere gli schiavisti
        player2.getShip().setCannons(3.0); // Potenza insufficiente per sconfiggere gli schiavisti

        player1.getFlightData().setPosition(10, 18);
        player2.getFlightData().setPosition(0, 18);

        SlaversCard slaversCard = new SlaversCard("S1", CardLevel.LEVEL_II, "Encounter with Slavers",
                4, 2, 10, 2);
        boolean result = slaversCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertTrue(slaversCard.isDefeated(), "Gli schiavisti dovrebbero essere sconfitti");
        assertEquals(10, player1.getCredits(), "Il giocatore 1 dovrebbe guadagnare 10 crediti");
        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizioni");
        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe guadagnare crediti");
        assertEquals(0,player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
    }

    @Test
    void testVisitSlaversCard_NoPlayerDefeatsSlavers() {
        player1.getShip().setCannons(2.0); // Potenza insufficiente per sconfiggere gli schiavisti
        player2.getShip().setCannons(4.0); // Pareggio

        player1.getShip().setCrew(4);
        player2.getShip().setCrew(4);

        player1.getFlightData().setPosition(10, 18);
        player2.getFlightData().setPosition(0, 18);

        SlaversCard slaversCard = new SlaversCard("S2", CardLevel.LEVEL_II, "Encounter with Slavers", 4, 2, 10, 2);

        boolean result = slaversCard.accept(visitor, gameModel);

        assertFalse(result, "Il risultato dovrebbe essere false");
        assertFalse(slaversCard.isDefeated(), "Gli schiavisti non dovrebbero essere sconfitti");

        assertEquals(0, player1.getCredits(), "Il giocatore 1 non dovrebbe guadagnare crediti");
        assertEquals(10,player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe muoversi");
        assertEquals(2, player1.getShip().getCrew(), "Il gocatore 1 dovrebbe aver perso 2 crew");

        assertEquals(0, player2.getCredits(), "Il giocatore 2 non dovrebbe guadagnare crediti");
        assertEquals(0,player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
        assertEquals(4, player2.getShip().getCrew(), "Il gocatore 2 non dovrebbe aver perso crew");
    }

    //SMUGGLERS CARD TESTS
    @Test
    void testVisitSmugglersCard_PlayerDefeatsSmugglers() {
        player1.getShip().setCannons(5.0); // Potenza sufficiente per sconfiggere i contrabbandieri
        player2.getShip().setCannons(3.0); // Potenza insufficiente per sconfiggere i contrabbandieri

        player1.getFlightData().setPosition(10, 18);
        player2.getFlightData().setPosition(0, 18);

        player1.getShip().setSpecialGoodsCapacity(10); //c'è spazio per le merci speciali
        player1.getShip().setNormalGoodsCapacity(10); //c'è spazio per le merci normali
        player1.getShip().setSpecialGoods(0);
        player2.getShip().setNormalGoods(0);

        Map<GoodType, Integer> availableGoods = new HashMap<>();
        availableGoods.put(GoodType.RED, 2);
        availableGoods.put(GoodType.BLUE, 3);

        SmugglersCard smugglersCard = new SmugglersCard("S1", CardLevel.LEVEL_II, "Encounter with Smugglers",
                4, 2,2,  availableGoods);
        boolean result = smugglersCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertTrue(smugglersCard.isDefeated(), "I contrabbandieri dovrebbero essere sconfitti");
        assertEquals(2, player1.getShip().getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe guadagnare 2 merci rosse");
        assertEquals(3, player1.getShip().getResources().get(GoodType.BLUE), "Il giocatore 1 dovrebbe guadagnare 3 merci blu");
        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe tornare indietro di 2 posizioni");
        assertEquals(0, player2.getShip().getResources().get(GoodType.RED), "Il giocatore 2 non dovrebbe guadagnare merci rosse");
        assertEquals(0, player2.getShip().getResources().get(GoodType.BLUE), "Il giocatore 2 non dovrebbe guadagnare merci blu");
    }

    @Test
    void testVisitSmugglersCard_NoPlayerDefeatsSmugglers() {
        player1.getShip().setCannons(2.0); // Potenza insufficiente per sconfiggere i contrabbandieri
        player2.getShip().setCannons(4.0); // Pareggio

        Map<GoodType, Integer> mutableResources1 = new HashMap<>();
        mutableResources1.put(GoodType.RED, 1);
        mutableResources1.put(GoodType.BLUE, 2);

        Map<GoodType, Integer> mutableResources2 = new HashMap<>();
        mutableResources2.put(GoodType.RED, 2);
        mutableResources2.put(GoodType.BLUE, 3);

        player1.getShip().setResources(mutableResources1);
        player2.getShip().setResources(mutableResources2);

        player1.getShip().setSpecialGoods(1);
        player1.getShip().setNormalGoods(2);

        player1.getFlightData().setPosition(10, 18);
        player2.getFlightData().setPosition(0, 18);


        Map<GoodType, Integer> availableGoods = new HashMap<>();
        availableGoods.put(GoodType.RED, 2);
        availableGoods.put(GoodType.BLUE, 3);

        SmugglersCard smugglersCard = new SmugglersCard("S2", CardLevel.LEVEL_II, "Encounter with Smugglers",
                4, 2, 2, availableGoods);

        boolean result = smugglersCard.accept(visitor, gameModel);

        assertFalse(result, "Il risultato dovrebbe essere false");
        assertFalse(smugglersCard.isDefeated(), "I contrabbandieri non dovrebbero essere sconfitti");

        assertNull(player1.getShip().getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe perdere tutte le merci rosse");
        assertEquals(1, player1.getShip().getResources().get(GoodType.BLUE), "Il giocatore dovrebbe perdere 1 merce blu");
        assertEquals(10, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe muoversi");

        assertEquals(2, player2.getShip().getResources().get(GoodType.RED), "Il giocatore 2 dovrebbe mantenere le merci rosse");
        assertEquals(3, player2.getShip().getResources().get(GoodType.BLUE), "Il giocatore 2 dovrebbe mantenere le merci blu");
        assertEquals(0, player2.getFlightData().getPosition(), "Il giocatore 2 non dovrebbe muoversi");
    }
    //EPIDEMIC CARD TESTS
    @Test
    void testVisitEpidemicCard_PlayerWithAdjacentCabin() {
        player1.getFlightData().setPosition(0, 18);
        ship1.setCrew(3);
        Map<Direction, ConnectorType> connectors1 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};
        Map<Direction, ConnectorType> connectors2 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.SINGLE);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        Map<Direction, ConnectorType> connectors3 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};

        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
        Cabin component2 = new Cabin(ComponentType.CABIN_START, connectors2);
        Cabin component3 = new Cabin(ComponentType.CABIN, connectors3);
        component1.setPosition(new Position(2, 3));
        component2.setPosition(new Position(2, 4));
        component3.setPosition(new Position(1, 4));

        ship1.addComponent(component1, new Position(2, 3));
        ship1.addComponent(component2, new Position(2, 4));
        ship1.addComponent(component3, new Position(1, 4));

        EpidemicCard epidemicCard = new EpidemicCard("E1", CardLevel.LEVEL_II, "Epidemic event");
        boolean result = epidemicCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertEquals(0, ship1.getCrew(), "Il giocatore 1 dovrebbe perdere tutto l'equipaggio");
    }

    @Test
    void testVisitEpidemicCard_PlayerWithoutAdjacentCabin() {
        player1.getFlightData().setPosition(0, 18);
        ship1.setCrew(3);
        Map<Direction, ConnectorType> connectors1 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};
        Map<Direction, ConnectorType> connectors2 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.SINGLE);
            put(Direction.DOWN, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        Map<Direction, ConnectorType> connectors3 = new HashMap<>(){{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.PLAIN);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};

        Cabin component1 = new Cabin(ComponentType.CABIN, connectors1);
        Engine component2 = new Engine(ComponentType.ENGINE_SINGLE, connectors2);
        Cabin component3 = new Cabin(ComponentType.CABIN, connectors3);
        component1.setPosition(new Position(2, 3));
        component2.setPosition(new Position(2, 4));
        component3.setPosition(new Position(1, 4));

        ship1.addComponent(component1, new Position(2, 3));
        ship1.addComponent(component2, new Position(2, 4));
        ship1.addComponent(component3, new Position(1, 5));

        EpidemicCard epidemicCard = new EpidemicCard("E1", CardLevel.LEVEL_II, "Epidemic event");
        boolean result = epidemicCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertEquals(3, ship1.getCrew(), "Il giocatore dovrebbe mantenere tutto l'equipaggio");
    }

    //ABANDONED STATION TESTS
    @Test
    void testVisitAbandonedStationCard_PlayerWithEnoughCrew_LootsStation() {
        player1.getFlightData().setPosition(10, 18);
        ship1.setCrew(5);
        ship1.setNormalGoodsCapacity(10);
        ship1.setSpecialGoodsCapacity(5);

        Map<GoodType, Integer> availableGoods = new HashMap<>();
        availableGoods.put(GoodType.RED, 2);
        availableGoods.put(GoodType.BLUE, 3);

        AbandonedStationCard abandonedStationCard = new AbandonedStationCard("A1", CardLevel.LEVEL_II, "Abandoned Station",
                3, 2, availableGoods);
        boolean result = abandonedStationCard.accept(visitor, gameModel);

        assertTrue(result, "Il risultato dovrebbe essere true");
        assertTrue(abandonedStationCard.isVisited(), "La stazione abbandonata dovrebbe essere visitata");
        assertEquals(2, ship1.getResources().get(GoodType.RED), "Il giocatore 1 dovrebbe ottenere 2 merci rosse");
        assertEquals(3, ship1.getResources().get(GoodType.BLUE), "Il giocatore 1 dovrebbe ottenere 3 merci blu");
        assertEquals(8, player1.getFlightData().getPosition(), "Il giocatore 1 dovrebbe perdere 2 giorni di volo");
    }

    @Test
    void testVisitAbandonedStationCard_PlayerWithoutEnoughCrew_CannotLootStation() {
        player1.getFlightData().setPosition(0, 18);
        player2.getFlightData().setPosition(10, 18);
        ship1.setCrew(2);
        ship2.setCrew(1);
        ship1.setNormalGoodsCapacity(10);
        ship1.setSpecialGoodsCapacity(5);

        Map<GoodType, Integer> availableGoods = new HashMap<>();
        availableGoods.put(GoodType.RED, 2);
        availableGoods.put(GoodType.BLUE, 3);

        AbandonedStationCard abandonedStationCard = new AbandonedStationCard("A1", CardLevel.LEVEL_II, "Abandoned Station",
                3, 2, availableGoods);
        boolean result = abandonedStationCard.accept(visitor, gameModel);

        assertFalse(result, "Il risultato dovrebbe essere false");
        assertFalse(abandonedStationCard.isVisited(), "La stazione abbandonata non dovrebbe essere visitata");
        assertEquals(0, ship1.getResources().get(GoodType.RED), "Il giocatore 1 non dovrebbe ottenere merci rosse");
        assertEquals(0,ship1.getResources().get(GoodType.BLUE), "Il giocatore 1 non dovrebbe ottenere merci blu");
        assertEquals(0, player1.getFlightData().getPosition(), "Il giocatore 1 non dovrebbe perdere giorni di volo");
    }

    //METEOR SWARM CARD TESTS
    @Test
    void testVisitMeteorSwarmCard_ShipProtectedByShield() {
        player1.getFlightData().setPosition(0, 18);

        Map<Direction, ConnectorType> connectors = new HashMap<>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        Shield shield = new Shield(ComponentType.SHIELD, connectors);
        shield.setPosition(new Position(2, 3));
        ship1.addComponent(shield, new Position(2, 3));

        List<Meteor> meteorPattern = List.of(
                new Meteor(ShotIntensity.LIGHT, Direction.UP)
        );

        MeteorSwarmCard meteorSwarmCard = new MeteorSwarmCard("M1", CardLevel.LEVEL_II, "Meteor Swarm", meteorPattern);
        boolean result = meteorSwarmCard.accept(visitor, gameModel);

        assertNotNull((ship1.getBoard()[2][3]), "Il componente non dovrebbe essere rimosso");
    }
}


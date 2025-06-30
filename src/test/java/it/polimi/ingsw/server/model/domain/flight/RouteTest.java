package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.enums.*;
import it.polimi.ingsw.server.model.enums.resource.*;
import it.polimi.ingsw.server.model.enums.player.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    private Route route;
    private RewardSystem rewardSystem;
    private List<Integer> startingPositions;
    private final int ROUTE_LENGTH = 50;

    @BeforeEach
    void setUp() {
        // Creo le posizioni di partenza
        startingPositions = Arrays.asList(1, 2, 3, 4);

        // Creo il sistema di ricompense
        Map<PlayerOrder, Integer> positionBonus = new HashMap<>();
        positionBonus.put(PlayerOrder.FIRST, 100);
        positionBonus.put(PlayerOrder.SECOND, 75);
        positionBonus.put(PlayerOrder.THIRD, 50);
        positionBonus.put(PlayerOrder.FOURTH, 25);

        Map<GoodType, Integer> resourceBonus = new HashMap<>();
        resourceBonus.put(GoodType.RED, 10);
        resourceBonus.put(GoodType.BLUE, 8);
        resourceBonus.put(GoodType.YELLOW, 6);
        resourceBonus.put(GoodType.GREEN, 4);

        rewardSystem = new RewardSystem(
                GameLevel.LEVEL_II,
                positionBonus,
                resourceBonus,
                20, // bestLookingShipBonus
                -5  // exposedConnectorsPenalty
        );

        route = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, startingPositions, rewardSystem);
    }

    @Test
    @DisplayName("Test costruttore con parametri validi")
    void testConstructorWithValidParameters() {
        assertNotNull(route);
        assertEquals(ROUTE_LENGTH, route.getLength());
        assertEquals(startingPositions, route.getStartingPositions());
        assertEquals(rewardSystem, route.getRewardSystem());
        assertEquals(startingPositions, route.getAllAvailableStartingPositions());
    }

    @Test
    @DisplayName("Test costruttore con GameLevel TEST_FLIGHT")
    void testConstructorWithTestFlightLevel() {
        RewardSystem testRewardSystem = new RewardSystem(
                GameLevel.TEST_FLIGHT,
                new HashMap<>(),
                new HashMap<>(),
                0,
                0
        );

        Route testRoute = new Route(GameLevel.TEST_FLIGHT, 30, startingPositions, testRewardSystem);

        assertNotNull(testRoute);
        assertEquals(30, testRoute.getLength());
    }

    @Test
    @DisplayName("Test costruttore con lista vuota di posizioni iniziali")
    void testConstructorWithEmptyStartingPositions() {
        List<Integer> emptyPositions = new ArrayList<>();

        Route emptyRoute = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, emptyPositions, rewardSystem);

        assertNotNull(emptyRoute);
        assertTrue(emptyRoute.getStartingPositions().isEmpty());
        assertTrue(emptyRoute.getAllAvailableStartingPositions().isEmpty());
    }

    @Test
    @DisplayName("Test costruttore con lunghezza zero")
    void testConstructorWithZeroLength() {
        Route zeroLengthRoute = new Route(GameLevel.LEVEL_II, 0, startingPositions, rewardSystem);

        assertNotNull(zeroLengthRoute);
        assertEquals(0, zeroLengthRoute.getLength());
    }

    @Test
    @DisplayName("Test getLength")
    void testGetLength() {
        assertEquals(ROUTE_LENGTH, route.getLength());
    }

    @Test
    @DisplayName("Test getStartingPositions")
    void testGetStartingPositions() {
        List<Integer> positions = route.getStartingPositions();

        assertNotNull(positions);
        assertEquals(4, positions.size());
        assertEquals(startingPositions, positions);

        // Verifico che la lista restituita sia immutabile o che modifiche non influenzino l'oggetto originale
        List<Integer> originalPositions = new ArrayList<>(startingPositions);
        positions.clear(); // Tento di modificare la lista restituita

        // La lista originale nell'oggetto route dovrebbe rimanere inalterata
        assertEquals(originalPositions.size(), route.getStartingPositions().size());
    }

    @Test
    @DisplayName("Test getAllAvailableStartingPositions")
    void testGetAllAvailableStartingPositions() {
        List<Integer> availablePositions = route.getAllAvailableStartingPositions();

        assertNotNull(availablePositions);
        assertEquals(startingPositions.size(), availablePositions.size());
        assertEquals(startingPositions, availablePositions);
    }

    @Test
    @DisplayName("Test getRewardSystem")
    void testGetRewardSystem() {
        RewardSystem returnedRewardSystem = route.getRewardSystem();

        assertNotNull(returnedRewardSystem);
        assertEquals(rewardSystem, returnedRewardSystem);
        assertSame(rewardSystem, returnedRewardSystem);
    }

    @Test
    @DisplayName("Test getFirstAvailableStartingPosition con posizioni disponibili")
    void testGetFirstAvailableStartingPositionWithAvailablePositions() {
        // Prima chiamata
        int firstPosition = route.getFirstAvailableStartingPosition();
        assertEquals(1, firstPosition);
        assertEquals(3, route.getAllAvailableStartingPositions().size());
        assertFalse(route.getAllAvailableStartingPositions().contains(1));

        // Seconda chiamata
        int secondPosition = route.getFirstAvailableStartingPosition();
        assertEquals(2, secondPosition);
        assertEquals(2, route.getAllAvailableStartingPositions().size());
        assertFalse(route.getAllAvailableStartingPositions().contains(2));
    }

    @Test
    @DisplayName("Test getFirstAvailableStartingPosition esaurisce tutte le posizioni")
    void testGetFirstAvailableStartingPositionExhaustsAllPositions() {
        List<Integer> retrievedPositions = new ArrayList<>();

        // Prelevo tutte le posizioni disponibili
        for (int i = 0; i < startingPositions.size(); i++) {
            int position = route.getFirstAvailableStartingPosition();
            retrievedPositions.add(position);
            assertEquals(startingPositions.size() - i - 1, route.getAllAvailableStartingPositions().size());
        }

        // Verifico che tutte le posizioni siano state prelevate nell'ordine corretto
        assertEquals(startingPositions, retrievedPositions);
        assertTrue(route.getAllAvailableStartingPositions().isEmpty());
    }

    @Test
    @DisplayName("Test getFirstAvailableStartingPosition con lista vuota solleva eccezione")
    void testGetFirstAvailableStartingPositionWithEmptyListThrowsException() {
        List<Integer> emptyPositions = new ArrayList<>();
        Route emptyRoute = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, emptyPositions, rewardSystem);

        assertThrows(NoSuchElementException.class, emptyRoute::getFirstAvailableStartingPosition);
    }

    @Test
    @DisplayName("Test getFirstAvailableStartingPosition dopo aver esaurito le posizioni solleva eccezione")
    void testGetFirstAvailableStartingPositionAfterExhaustionThrowsException() {
        // Esaurisco tutte le posizioni
        for (int i = 0; i < startingPositions.size(); i++) {
            route.getFirstAvailableStartingPosition();
        }

        // La prossima chiamata dovrebbe sollevare un'eccezione
        assertThrows(NoSuchElementException.class, route::getFirstAvailableStartingPosition);
    }

    @Test
    @DisplayName("Test indipendenza tra getStartingPositions e availableStartingPositions")
    void testIndependenceBetweenStartingPositionsAndAvailablePositions() {
        // Le posizioni iniziali dovrebbero rimanere immutate
        List<Integer> originalStartingPositions = new ArrayList<>(route.getStartingPositions());

        // Prelevo alcune posizioni disponibili
        route.getFirstAvailableStartingPosition();
        route.getFirstAvailableStartingPosition();

        // Le posizioni iniziali dovrebbero rimanere le stesse
        assertEquals(originalStartingPositions, route.getStartingPositions());
        assertEquals(originalStartingPositions.size(), route.getStartingPositions().size());

        // Ma le posizioni disponibili dovrebbero essere cambiate
        assertEquals(originalStartingPositions.size() - 2, route.getAllAvailableStartingPositions().size());
    }

    @Test
    @DisplayName("Test con posizioni iniziali contenenti duplicati")
    void testWithDuplicateStartingPositions() {
        List<Integer> duplicatePositions = Arrays.asList(1, 1, 2, 2);
        Route duplicateRoute = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, duplicatePositions, rewardSystem);

        assertEquals(4, duplicateRoute.getStartingPositions().size());
        assertEquals(4, duplicateRoute.getAllAvailableStartingPositions().size());

        // Dovrei poter prelevare tutte e 4 le posizioni, anche se duplicate
        for (int i = 0; i < 4; i++) {
            int position = duplicateRoute.getFirstAvailableStartingPosition();
            assertTrue(duplicatePositions.contains(position));
        }

        assertTrue(duplicateRoute.getAllAvailableStartingPositions().isEmpty());
    }

    @Test
    @DisplayName("Test con posizioni iniziali in ordine non sequenziale")
    void testWithNonSequentialStartingPositions() {
        List<Integer> nonSequentialPositions = Arrays.asList(10, 5, 1, 20);
        Route nonSequentialRoute = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, nonSequentialPositions, rewardSystem);

        assertEquals(nonSequentialPositions, nonSequentialRoute.getStartingPositions());

        // Il primo elemento prelevato dovrebbe essere 10 (primo nella lista)
        int firstPosition = nonSequentialRoute.getFirstAvailableStartingPosition();
        assertEquals(10, firstPosition);

        // Il secondo dovrebbe essere 5
        int secondPosition = nonSequentialRoute.getFirstAvailableStartingPosition();
        assertEquals(5, secondPosition);
    }

    @Test
    @DisplayName("Test serializzazione - verifica presenza serialVersionUID")
    void testSerializationUID() {
        // Questo test verifica che la classe implementi correttamente Serializable
        assertTrue(java.io.Serializable.class.isAssignableFrom(Route.class));

        // Verifica che serialVersionUID sia definito
        try {
            Route.class.getDeclaredField("serialVersionUID");
        } catch (NoSuchFieldException e) {
            fail("serialVersionUID dovrebbe essere definito per la serializzazione");
        }
    }

    @Test
    @DisplayName("Test con lunghezza negativa")
    void testWithNegativeLength() {
        Route negativeRoute = new Route(GameLevel.LEVEL_II, -10, startingPositions, rewardSystem);

        assertNotNull(negativeRoute);
        assertEquals(-10, negativeRoute.getLength());
    }

    @Test
    @DisplayName("Test immutabilità delle liste - modifiche esterne non dovrebbero influenzare Route")
    void testListImmutability() {
        List<Integer> mutablePositions = new ArrayList<>(Arrays.asList(1, 2, 3));
        Route testRoute = new Route(GameLevel.LEVEL_II, ROUTE_LENGTH, mutablePositions, rewardSystem);

        // Modifico la lista originale dopo la creazione del Route
        mutablePositions.add(4);
        mutablePositions.remove(0);

        // Il Route non dovrebbe essere influenzato dalle modifiche esterne
        assertEquals(3, testRoute.getStartingPositions().size());
        assertEquals(Arrays.asList(1, 2, 3), testRoute.getStartingPositions());
        assertEquals(3, testRoute.getAllAvailableStartingPositions().size());
    }
}
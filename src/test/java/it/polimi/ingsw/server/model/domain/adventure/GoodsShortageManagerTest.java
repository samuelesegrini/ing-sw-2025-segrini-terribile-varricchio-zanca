package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.GoodsShortageManager;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.components.CargoHold;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.domain.general.config.PositionConfig;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GoodsShortageManagerTest {

    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;
    private List<Player> playersOrdered;
    private Planet availablePlanet;
    private Planet visitedPlanet;
    private Planet specialCargoPlanet;
    private Planet highValuePlanet;
    private List<Planet> planets;
    private Map<Direction, ConnectorType> connectors;

    @BeforeEach
    void setUp() {
        // Create universal connectors map for components
        connectors = new HashMap<>(
                Map.of(
                        Direction.UP, ConnectorType.UNIVERSAL,
                        Direction.RIGHT, ConnectorType.UNIVERSAL,
                        Direction.DOWN, ConnectorType.UNIVERSAL,
                        Direction.LEFT, ConnectorType.UNIVERSAL
                )
        );

        // Create players with ships using the provided configuration approach
        player1 = new Player(new PlayerId(UUID.randomUUID(), "Player1"));
        player2 = new Player(new PlayerId(UUID.randomUUID(), "Player2"));
        player3 = new Player(new PlayerId(UUID.randomUUID(), "Player3"));
        player4 = new Player(new PlayerId(UUID.randomUUID(), "Player4"));

        // Create test ship grid config
        ShipGridConfig testConfig = createTestShipGridConfig();

        // Create ships for players with valid config and cargo holds
        Ship ship1 = createShipWithCargo(testConfig);
        Ship ship2 = createShipWithCargo(testConfig);
        Ship ship3 = createShipWithCargo(testConfig);
        Ship ship4 = createShipWithCargo(testConfig);

        player1.setShip(ship1);
        player2.setShip(ship2);
        player3.setShip(ship3);
        player4.setShip(ship4);

        playersOrdered = List.of(player1, player2, player3, player4);

        // Create test planets
        setupTestPlanets();
    }

    private ShipGridConfig createTestShipGridConfig() {
        return new ShipGridConfig(
                "test_ship_image.png",
                5,
                5,
                List.of(),
                List.of()
        );
    }

    private Ship createShipWithCargo(ShipGridConfig config) {
        Ship ship = new Ship(GameLevel.TEST_FLIGHT, config);

        // Add normal cargo hold with capacity 5
        Component cargoHold = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5, "normalCargoHold");
        ship.addComponent(cargoHold, new Position(1, 1));

        // Add special cargo hold with capacity 5
        Component specialCargoHold = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 5, "specialCargoHold");
        ship.addComponent(specialCargoHold, new Position(2, 1));

        return ship;
    }

    private void setupTestPlanets() {
        // Available planet with normal goods (total value: 4, total goods: 3)
        availablePlanet = createPlanetWithGoods(1, Map.of(
                GoodType.BLUE, 2,    // 2 credits
                GoodType.GREEN, 1    // 2 credits
        ), false, false);

        // Already visited planet
        visitedPlanet = createPlanetWithGoods(2, Map.of(
                GoodType.YELLOW, 3   // 9 credits
        ), true, false);

        // Planet requiring special cargo (total value: 9, total goods: 3)
        specialCargoPlanet = createPlanetWithGoods(3, Map.of(
                GoodType.RED, 2,     // 8 credits
                GoodType.BLUE, 1     // 1 credit
        ), false, true);

        // High value planet (total value: 15, total goods: 8)
        highValuePlanet = createPlanetWithGoods(4, Map.of(
                GoodType.BLUE, 3,    // 3 credits
                GoodType.GREEN, 3,   // 6 credits
                GoodType.YELLOW, 2   // 6 credits
        ), false, false);

        planets = List.of(availablePlanet, visitedPlanet, specialCargoPlanet, highValuePlanet);
    }

    private Planet createPlanetWithGoods(int number, Map<GoodType, Integer> goods,
                                         boolean visited, boolean requiresSpecialCargo) {
        // Create a mutable copy of the goods map, ensuring it's never null
        Map<GoodType, Integer> goodsMap = goods != null ? new HashMap<>(goods) : new HashMap<>();

        // If requiresSpecialCargo is true and there are no RED goods, add some RED goods
        // This maintains the test's intent while working with the actual Planet implementation
        if (requiresSpecialCargo && goodsMap.getOrDefault(GoodType.RED, 0) == 0) {
            goodsMap.put(GoodType.RED, 1);
        }

        // Create planet with the correct constructor signature
        Planet planet = new Planet(number, goodsMap);

        if (visited) {
            planet.setVisited();
        }

        return planet;
    }

    @Test
    void testValidatePlanetLanding_Success() {
        // Test successful planet landing validation
        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, availablePlanet);

        assertTrue(result.isSuccess());
        assertEquals("Can land on planet 1", result.getMessage());
    }

    @Test
    void testValidatePlanetLanding_PlanetAlreadyVisited() {
        // Test validation failure for already visited planet
        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, visitedPlanet);

        assertFalse(result.isSuccess());
        assertEquals("Planet 2 has already been claimed by another player", result.getMessage());
    }

    @Test
    void testValidatePlanetLanding_InsufficientCargoCapacity() {
        // Create a planet with excessive normal goods that exceed cargo capacity (>10 total goods)
        Planet overloadedPlanet = createPlanetWithGoods(5, Map.of(
                GoodType.BLUE, 6,    // Normal cargo
                GoodType.GREEN, 6    // Normal cargo - total 12 goods > 10 capacity
        ), false, false);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, overloadedPlanet);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Insufficient cargo capacity"));
        assertTrue(result.getMessage().contains("planet 5"));
        assertTrue(result.getMessage().contains("Needs:"));
    }

    @Test
    void testValidatePlanetLanding_InsufficientSpecialCargoCapacity() {
        // Create a planet requiring special cargo with excessive RED goods (>5 RED goods)
        Planet overloadedSpecialPlanet = createPlanetWithGoods(6, Map.of(
                GoodType.RED, 6      // Exceeds special cargo capacity of 5
        ), false, true);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, overloadedSpecialPlanet);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Insufficient cargo capacity"));
        assertTrue(result.getMessage().contains("(requires special cargo hold for RED goods)"));
    }

    @Test
    void testValidatePlanetLanding_EmptyGoods() {
        // Test planet with no goods
        Planet emptyPlanet = createPlanetWithGoods(7, Map.of(), false, false);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, emptyPlanet);

        assertTrue(result.isSuccess());
        assertEquals("Can land on planet 7", result.getMessage());
    }

    @Test
    void testAnalyzePlanetCompetition_AllPlanetsAvailable() {
        // Test competition analysis when all planets are available
        List<Planet> availablePlanets = List.of(availablePlanet, specialCargoPlanet, highValuePlanet);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(availablePlanets, playersOrdered);

        // Should analyze 3 planets (excluding visited ones in setup)
        assertEquals(3, analysis.size());

        // Check planet 1 analysis
        assertTrue(analysis.containsKey(1));
        GoodsShortageManager.PlanetCompetitionInfo planet1Info = analysis.get(1);
        assertEquals(1, planet1Info.planetNumber);
        assertFalse(planet1Info.requiresSpecialCargo);
        assertEquals(4, planet1Info.eligiblePlayers.size()); // All players can land
        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME, planet1Info.competitionLevel);
    }

    @Test
    void testAnalyzePlanetCompetition_SkipsVisitedPlanets() {
        // Test that visited planets are skipped in analysis
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(planets, playersOrdered);

        // Should not include visited planet (planet 2)
        assertFalse(analysis.containsKey(2));

        // Should include non-visited planets
        assertTrue(analysis.containsKey(1));
        assertTrue(analysis.containsKey(3));
        assertTrue(analysis.containsKey(4));
    }

    @Test
    void testAnalyzePlanetCompetition_SpecialCargoRequirement() {
        // Test analysis for planet requiring special cargo
        List<Planet> specialPlanets = List.of(specialCargoPlanet);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(specialPlanets, playersOrdered);

        GoodsShortageManager.PlanetCompetitionInfo info = analysis.get(3);
        assertNotNull(info);
        assertTrue(info.requiresSpecialCargo);
        assertEquals(3, info.planetNumber);
    }

    @Test
    void testCalculateCompetitionLevel_None() {
        // Test with single eligible player - should be NONE
        List<Player> singlePlayer = List.of(player1);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(availablePlanet), singlePlayer);

        assertEquals(GoodsShortageManager.CompetitionLevel.NONE,
                analysis.get(1).competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_Low() {
        // Test with 2 players and low value planet (value < 6)
        Planet lowValuePlanet = createPlanetWithGoods(8, Map.of(GoodType.BLUE, 3), false, false); // 3 credits
        List<Player> twoPlayers = List.of(player1, player2);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(lowValuePlanet), twoPlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.LOW,
                analysis.get(8).competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_Moderate_TwoPlayersHighValue() {
        // Test with 2 players and high value planet (>=8)
        Planet highValuePlanet = createPlanetWithGoods(9, Map.of(
                GoodType.YELLOW, 3   // 9 credits >= 8
        ), false, false);
        List<Player> twoPlayers = List.of(player1, player2);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(highValuePlanet), twoPlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.MODERATE,
                analysis.get(9).competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_Moderate_ThreePlayersLowValue() {
        // Test with 3 players and low value planet (< 6)
        Planet lowValuePlanet = createPlanetWithGoods(10, Map.of(GoodType.GREEN, 2), false, false); // 4 credits
        List<Player> threePlayers = List.of(player1, player2, player3);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(lowValuePlanet), threePlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.MODERATE,
                analysis.get(10).competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_High() {
        // Test with 3 players and high value planet (>=6)
        Planet highValuePlanet = createPlanetWithGoods(11, Map.of(
                GoodType.GREEN, 3    // 6 credits >= 6
        ), false, false);
        List<Player> threePlayers = List.of(player1, player2, player3);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(highValuePlanet), threePlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.HIGH,
                analysis.get(11).competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_Extreme() {
        // Test with 4+ players - should always be EXTREME
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(availablePlanet), playersOrdered);

        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME,
                analysis.get(1).competitionLevel);
    }

    @Test
    void testGenerateShortageWarnings_NoWarnings() {
        // Test with competition that doesn't generate warnings (NONE, LOW, MODERATE without special cargo)
        Planet lowCompetitionPlanet = createPlanetWithGoods(12, Map.of(GoodType.BLUE, 2), false, false); // 2 credits
        List<Player> twoPlayers = List.of(player1, player2);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(lowCompetitionPlanet), twoPlayers);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void testGenerateShortageWarnings_HighCompetition() {
        // Test HIGH competition warning
        Planet highCompetitionPlanet = createPlanetWithGoods(13, Map.of(
                GoodType.GREEN, 3    // 6 credits, HIGH competition with 3 players
        ), false, false);
        List<Player> threePlayers = List.of(player1, player2, player3);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(highCompetitionPlanet), threePlayers);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("WARNING: Planet 13"));
        assertTrue(warnings.get(0).contains("high competition"));
    }

    @Test
    void testGenerateShortageWarnings_ExtremeCompetition() {
        // Test EXTREME competition warning
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(availablePlanet), playersOrdered);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        assertTrue(warnings.stream().anyMatch(w ->
                w.contains("CRITICAL: Planet 1") && w.contains("extreme competition")));
    }

    @Test
    void testGenerateShortageWarnings_SpecialCargoNotice() {
        // Test special cargo notice with multiple eligible players
        List<Player> multiplePlayers = List.of(player1, player2, player3);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(specialCargoPlanet), multiplePlayers);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        assertTrue(warnings.stream().anyMatch(w ->
                w.contains("NOTICE: Planet 3") && w.contains("special cargo holds for RED goods")));
    }

    @Test
    void testGenerateShortageWarnings_SpecialCargoNoNoticeForSinglePlayer() {
        // Test no special cargo notice when only one player is eligible
        List<Player> singlePlayer = List.of(player1);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(specialCargoPlanet), singlePlayer);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        // Should not generate special cargo notice for single player
        assertTrue(warnings.stream().noneMatch(w -> w.contains("special cargo holds")));
    }

    @Test
    void testGenerateShortageWarnings_MultipleWarnings() {
        // Test multiple warnings generation
        Planet extremePlanet = createPlanetWithGoods(14, Map.of(GoodType.YELLOW, 2), false, false); // 6 credits
        Planet specialPlanet = createPlanetWithGoods(15, Map.of(GoodType.RED, 2), false, true); // RED goods
        List<Planet> testPlanets = List.of(extremePlanet, specialPlanet);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(testPlanets, playersOrdered);

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);

        // Should have both extreme competition and special cargo warnings
        assertTrue(warnings.size() >= 2);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("CRITICAL") && w.contains("extreme competition")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("NOTICE") && w.contains("special cargo")));
    }

    @Test
    void testGoodsValidationResult_GettersAndConstructor() {
        // Test GoodsValidationResult inner class
        GoodsShortageManager.GoodsValidationResult successResult =
                new GoodsShortageManager.GoodsValidationResult(true, "Success message");

        assertTrue(successResult.isSuccess());
        assertEquals("Success message", successResult.getMessage());

        GoodsShortageManager.GoodsValidationResult failureResult =
                new GoodsShortageManager.GoodsValidationResult(false, "Failure message");

        assertFalse(failureResult.isSuccess());
        assertEquals("Failure message", failureResult.getMessage());
    }

    @Test
    void testPlanetCompetitionInfo_FieldsAndInitialization() {
        // Test PlanetCompetitionInfo inner class initialization
        GoodsShortageManager.PlanetCompetitionInfo info =
                new GoodsShortageManager.PlanetCompetitionInfo();

        // Test initial values
        assertEquals(0, info.planetNumber);
        assertEquals(0, info.totalValue);
        assertEquals(0, info.totalGoods);
        assertFalse(info.requiresSpecialCargo);
        assertNull(info.competitionLevel);
        assertNotNull(info.eligiblePlayers);
        assertTrue(info.eligiblePlayers.isEmpty());

        // Test field assignment
        info.planetNumber = 42;
        info.totalValue = 100;
        info.totalGoods = 10;
        info.requiresSpecialCargo = true;
        info.competitionLevel = GoodsShortageManager.CompetitionLevel.HIGH;
        info.eligiblePlayers.add(player1.getPlayerId());

        assertEquals(42, info.planetNumber);
        assertEquals(100, info.totalValue);
        assertEquals(10, info.totalGoods);
        assertTrue(info.requiresSpecialCargo);
        assertEquals(GoodsShortageManager.CompetitionLevel.HIGH, info.competitionLevel);
        assertEquals(1, info.eligiblePlayers.size());
        assertTrue(info.eligiblePlayers.contains(player1.getPlayerId()));
    }

    @Test
    void testCompetitionLevel_EnumValues() {
        // Test all CompetitionLevel enum values
        GoodsShortageManager.CompetitionLevel[] levels = GoodsShortageManager.CompetitionLevel.values();

        assertEquals(5, levels.length);
        assertEquals(GoodsShortageManager.CompetitionLevel.NONE, levels[0]);
        assertEquals(GoodsShortageManager.CompetitionLevel.LOW, levels[1]);
        assertEquals(GoodsShortageManager.CompetitionLevel.MODERATE, levels[2]);
        assertEquals(GoodsShortageManager.CompetitionLevel.HIGH, levels[3]);
        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME, levels[4]);

        // Test valueOf
        assertEquals(GoodsShortageManager.CompetitionLevel.NONE,
                GoodsShortageManager.CompetitionLevel.valueOf("NONE"));
        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME,
                GoodsShortageManager.CompetitionLevel.valueOf("EXTREME"));
    }

    @Test
    void testEdgeCases_EmptyPlayersList() {
        // Test with empty players list
        List<Player> emptyPlayers = List.of();

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(availablePlanet), emptyPlayers);

        GoodsShortageManager.PlanetCompetitionInfo info = analysis.get(1);
        assertNotNull(info);
        assertEquals(0, info.eligiblePlayers.size());
        assertEquals(GoodsShortageManager.CompetitionLevel.NONE, info.competitionLevel);
    }

    @Test
    void testEdgeCases_EmptyPlanetsList() {
        // Test with empty planets list
        List<Planet> emptyPlanets = List.of();

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(emptyPlanets, playersOrdered);

        assertTrue(analysis.isEmpty());

        List<String> warnings = GoodsShortageManager.generateShortageWarnings(analysis);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void testValidatePlanetLanding_MixedCargoRequirements() {
        // Test planet with both normal and special cargo requirements
        Planet mixedCargoPlanet = createPlanetWithGoods(16, Map.of(
                GoodType.RED, 2,     // Special cargo
                GoodType.BLUE, 3,    // Normal cargo
                GoodType.GREEN, 2    // Normal cargo
        ), false, false);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, mixedCargoPlanet);

        // Should succeed as we have both normal (5) and special (5) cargo capacity
        assertTrue(result.isSuccess());
    }

    @Test
    void testValidatePlanetLanding_ExactCapacityLimits() {
        // Test planet that exactly matches cargo capacity limits
        Planet exactCapacityPlanet = createPlanetWithGoods(17, Map.of(
                GoodType.RED, 5,     // Exactly special cargo capacity
                GoodType.BLUE, 5     // Exactly normal cargo capacity
        ), false, false);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, exactCapacityPlanet);

        // Should succeed as it exactly matches our capacity
        assertTrue(result.isSuccess());
    }
    // Aggiungi questi test alla tua classe GoodsShortageManagerTest

    @Test
    void testValidatePlanetLanding_NullPlanet() {
        // Test comportamento con planet null
        assertThrows(NullPointerException.class, () -> {
            GoodsShortageManager.validatePlanetLanding(player1, null);
        });
    }

    @Test
    void testValidatePlanetLanding_NullPlayer() {
        // Test comportamento con player null
        assertThrows(NullPointerException.class, () -> {
            GoodsShortageManager.validatePlanetLanding(null, availablePlanet);
        });
    }

    @Test
    void testValidatePlanetLanding_PlayerWithNoShip() {
        // Test player senza ship
        Player playerWithoutShip = new Player(new PlayerId(UUID.randomUUID(), "NoShipPlayer"));
        // Non settiamo ship al player

        assertThrows(NullPointerException.class, () -> {
            GoodsShortageManager.validatePlanetLanding(playerWithoutShip, availablePlanet);
        });
    }

    @Test
    void testValidatePlanetLanding_DetailedErrorMessage() {
        // Test messaggio di errore dettagliato con tutti i tipi di goods
        Planet complexPlanet = createPlanetWithGoods(20, Map.of(
                GoodType.BLUE, 3,
                GoodType.GREEN, 2,
                GoodType.YELLOW, 1,
                GoodType.RED, 10  // Troppi RED goods per testare messaggio dettagliato
        ), false, true);

        GoodsShortageManager.GoodsValidationResult result =
                GoodsShortageManager.validatePlanetLanding(player1, complexPlanet);

        assertFalse(result.isSuccess());
        String message = result.getMessage();
        assertTrue(message.contains("Insufficient cargo capacity"));
        assertTrue(message.contains("planet 20"));
        assertTrue(message.contains("RED"));
        assertTrue(message.contains("requires special cargo hold"));
    }

    @Test
    void testAnalyzePlanetCompetition_PlayersWithInsufficientCapacity() {
        // Test con players che non hanno capacità sufficiente
        Planet hugePlanet = createPlanetWithGoods(21, Map.of(
                GoodType.BLUE, 20,  // Troppi goods per qualsiasi player
                GoodType.GREEN, 20
        ), false, false);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(hugePlanet), playersOrdered);

        GoodsShortageManager.PlanetCompetitionInfo info = analysis.get(21);
        assertNotNull(info);
        assertEquals(0, info.eligiblePlayers.size()); // Nessun player può atterrare
        assertEquals(GoodsShortageManager.CompetitionLevel.NONE, info.competitionLevel);
    }

    @Test
    void testCalculateCompetitionLevel_BoundaryValues() {
        // Test valori limite per competition level

        // Test con valore esatto 8 per MODERATE (2 players)
        Planet exactValue8 = createPlanetWithGoods(22, Map.of(GoodType.YELLOW, 2, GoodType.GREEN, 1), false, false); // 4+2=6, proviamo con 8
        // Ricreiamo con valore esatto 8
        Planet exactValue8Real = createPlanetWithGoods(22, Map.of(GoodType.RED, 2), false, false); // 2*4=8
        List<Player> twoPlayers = List.of(player1, player2);

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(exactValue8Real), twoPlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.MODERATE, analysis.get(22).competitionLevel);

        // Test con valore esatto 6 per HIGH (3 players)
        Planet exactValue6 = createPlanetWithGoods(23, Map.of(GoodType.GREEN, 3), false, false); // 3*2=6
        List<Player> threePlayers = List.of(player1, player2, player3);

        analysis = GoodsShortageManager.analyzePlanetCompetition(List.of(exactValue6), threePlayers);
        assertEquals(GoodsShortageManager.CompetitionLevel.HIGH, analysis.get(23).competitionLevel);
    }

    @Test
    void testGenerateShortageWarnings_EmptyAnalysis() {
        // Test con analisi vuota
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> emptyAnalysis = new HashMap<>();
        List<String> warnings = GoodsShortageManager.generateShortageWarnings(emptyAnalysis);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void testGenerateShortageWarnings_AllCompetitionLevels() {
        // Test che copre tutti i livelli di competizione
        Planet nonePlanet = createPlanetWithGoods(25, Map.of(GoodType.BLUE, 1), false, false);
        Planet lowPlanet = createPlanetWithGoods(26, Map.of(GoodType.BLUE, 2), false, false);
        Planet moderatePlanet = createPlanetWithGoods(27, Map.of(GoodType.RED, 2), false, false);
        Planet highPlanet = createPlanetWithGoods(28, Map.of(GoodType.GREEN, 3), false, false);
        Planet extremePlanet = createPlanetWithGoods(29, Map.of(GoodType.YELLOW, 2), false, false);

        // Test NONE (1 player)
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis1 =
                GoodsShortageManager.analyzePlanetCompetition(List.of(nonePlanet), List.of(player1));
        assertEquals(GoodsShortageManager.CompetitionLevel.NONE, analysis1.get(25).competitionLevel);

        // Test LOW (2 players, low value)
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis2 =
                GoodsShortageManager.analyzePlanetCompetition(List.of(lowPlanet), List.of(player1, player2));
        assertEquals(GoodsShortageManager.CompetitionLevel.LOW, analysis2.get(26).competitionLevel);

        // Test MODERATE (2 players, high value)
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis3 =
                GoodsShortageManager.analyzePlanetCompetition(List.of(moderatePlanet), List.of(player1, player2));
        assertEquals(GoodsShortageManager.CompetitionLevel.MODERATE, analysis3.get(27).competitionLevel);

        // Test HIGH (3 players, high value)
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis4 =
                GoodsShortageManager.analyzePlanetCompetition(List.of(highPlanet), List.of(player1, player2, player3));
        assertEquals(GoodsShortageManager.CompetitionLevel.HIGH, analysis4.get(28).competitionLevel);

        // Test EXTREME (4 players)
        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis5 =
                GoodsShortageManager.analyzePlanetCompetition(List.of(extremePlanet), playersOrdered);
        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME, analysis5.get(29).competitionLevel);
    }

    @Test
    void testPlanetCompetitionInfo_AllFields() {
        // Test completo di tutti i campi di PlanetCompetitionInfo
        GoodsShortageManager.PlanetCompetitionInfo info = new GoodsShortageManager.PlanetCompetitionInfo();

        // Test valori di default
        assertEquals(0, info.planetNumber);
        assertEquals(0, info.totalValue);
        assertEquals(0, info.totalGoods);
        assertFalse(info.requiresSpecialCargo);
        assertNull(info.competitionLevel);
        assertNotNull(info.eligiblePlayers);
        assertTrue(info.eligiblePlayers.isEmpty());

        // Test modifica di tutti i campi
        info.planetNumber = 100;
        info.totalValue = 500;
        info.totalGoods = 50;
        info.requiresSpecialCargo = true;
        info.competitionLevel = GoodsShortageManager.CompetitionLevel.EXTREME;
        info.eligiblePlayers.add(player1.getPlayerId());
        info.eligiblePlayers.add(player2.getPlayerId());

        assertEquals(100, info.planetNumber);
        assertEquals(500, info.totalValue);
        assertEquals(50, info.totalGoods);
        assertTrue(info.requiresSpecialCargo);
        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME, info.competitionLevel);
        assertEquals(2, info.eligiblePlayers.size());
        assertTrue(info.eligiblePlayers.contains(player1.getPlayerId()));
        assertTrue(info.eligiblePlayers.contains(player2.getPlayerId()));
    }

    @Test
    void testGoodsValidationResult_EdgeCases() {
        // Test con messaggi vuoti e null
        GoodsShortageManager.GoodsValidationResult result1 =
                new GoodsShortageManager.GoodsValidationResult(true, "");
        assertTrue(result1.isSuccess());
        assertEquals("", result1.getMessage());

        GoodsShortageManager.GoodsValidationResult result2 =
                new GoodsShortageManager.GoodsValidationResult(false, null);
        assertFalse(result2.isSuccess());
        assertNull(result2.getMessage());
    }

    @Test
    void testCompetitionLevel_Ordinal() {
        // Test ordine degli enum values
        assertEquals(0, GoodsShortageManager.CompetitionLevel.NONE.ordinal());
        assertEquals(1, GoodsShortageManager.CompetitionLevel.LOW.ordinal());
        assertEquals(2, GoodsShortageManager.CompetitionLevel.MODERATE.ordinal());
        assertEquals(3, GoodsShortageManager.CompetitionLevel.HIGH.ordinal());
        assertEquals(4, GoodsShortageManager.CompetitionLevel.EXTREME.ordinal());
    }

    @Test
    void testAnalyzePlanetCompetition_LargeNumberOfPlayers() {
        // Test con più di 4 players per assicurarsi che EXTREME sia gestito correttamente
        List<Player> manyPlayers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Player p = new Player(new PlayerId(UUID.randomUUID(), "Player" + i));
            ShipGridConfig config = createTestShipGridConfig();
            Ship ship = createShipWithCargo(config);
            p.setShip(ship);
            manyPlayers.add(p);
        }

        Map<Integer, GoodsShortageManager.PlanetCompetitionInfo> analysis =
                GoodsShortageManager.analyzePlanetCompetition(List.of(availablePlanet), manyPlayers);

        assertEquals(GoodsShortageManager.CompetitionLevel.EXTREME, analysis.get(1).competitionLevel);
        assertEquals(10, analysis.get(1).eligiblePlayers.size());
    }
}
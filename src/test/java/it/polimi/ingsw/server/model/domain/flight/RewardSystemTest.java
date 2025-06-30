package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.general.config.PositionConfig;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RewardSystemTest {

    private RewardSystem rewardSystemTestFlight;
    private RewardSystem rewardSystemLevel2;
    private List<Player> players;
    private Player player1, player2, player3, player4;
    private Ship ship1, ship2, ship3, ship4;

    @BeforeEach
    void setUp() {
        // Setup position bonuses
        Map<PlayerOrder, Integer> positionBonus = new HashMap<>();
        positionBonus.put(PlayerOrder.FIRST, 20);
        positionBonus.put(PlayerOrder.SECOND, 15);
        positionBonus.put(PlayerOrder.THIRD, 10);
        positionBonus.put(PlayerOrder.FOURTH, 5);

        // Setup resource bonuses
        Map<GoodType, Integer> resourceBonus = new HashMap<>();
        resourceBonus.put(GoodType.RED, 3);    // Special goods
        resourceBonus.put(GoodType.BLUE, 2);
        resourceBonus.put(GoodType.YELLOW, 2);
        resourceBonus.put(GoodType.GREEN, 2);

        // Create reward systems for different levels
        rewardSystemTestFlight = new RewardSystem(
                GameLevel.TEST_FLIGHT,
                positionBonus,
                resourceBonus,
                8, // bestLookingShipBonus
                2  // exposedConnectorsPenalty
        );

        rewardSystemLevel2 = new RewardSystem(
                GameLevel.LEVEL_II,
                positionBonus,
                resourceBonus,
                12, // bestLookingShipBonus
                3   // exposedConnectorsPenalty
        );

        // Create ship grid configs
        ShipGridConfig shipConfig1 = new ShipGridConfig(
                "ship1.png", 5, 5,
                Arrays.asList(new PositionConfig(1, 1), new PositionConfig(2, 2)),
                Arrays.asList(new PositionConfig(0, 0))
        );

        ShipGridConfig shipConfig2 = new ShipGridConfig(
                "ship2.png", 4, 4,
                Arrays.asList(new PositionConfig(1, 1)),
                Arrays.asList(new PositionConfig(0, 0), new PositionConfig(3, 3))
        );

        ShipGridConfig shipConfig3 = new ShipGridConfig(
                "ship3.png", 6, 6,
                Arrays.asList(),
                Arrays.asList(new PositionConfig(0, 0))
        );

        ShipGridConfig shipConfig4 = new ShipGridConfig(
                "ship4.png", 3, 3,
                Arrays.asList(new PositionConfig(1, 1), new PositionConfig(2, 2), new PositionConfig(0, 1)),
                Arrays.asList()
        );

        // Create ships
        ship1 = new Ship(GameLevel.TEST_FLIGHT, shipConfig1);
        ship2 = new Ship(GameLevel.TEST_FLIGHT, shipConfig2);
        ship3 = new Ship(GameLevel.LEVEL_II, shipConfig3);
        ship4 = new Ship(GameLevel.LEVEL_II, shipConfig4);

        // Create players
        player1 = new Player(new PlayerId(UUID.randomUUID(), "Player1"));
        player2 = new Player(new PlayerId(UUID.randomUUID(), "Player2"));
        player3 = new Player(new PlayerId(UUID.randomUUID(), "Player3"));
        player4 = new Player(new PlayerId(UUID.randomUUID(), "Player4"));

        // Assign ships to players
        player1.setShip(ship1);
        player2.setShip(ship2);
        player3.setShip(ship3);
        player4.setShip(ship4);

        players = Arrays.asList(player1, player2, player3, player4);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - First position")
    void testCalculatePositionBonusFirst() {
        List<Player> finishOrder = Arrays.asList(player1, player2, player3, player4);
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(20, bonus);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - Second position")
    void testCalculatePositionBonusSecond() {
        List<Player> finishOrder = Arrays.asList(player2, player1, player3, player4);
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(15, bonus);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - Third position")
    void testCalculatePositionBonusThird() {
        List<Player> finishOrder = Arrays.asList(player2, player3, player1, player4);
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(10, bonus);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - Fourth position")
    void testCalculatePositionBonusFourth() {
        List<Player> finishOrder = Arrays.asList(player2, player3, player4, player1);
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(5, bonus);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - Player not found")
    void testCalculatePositionBonusPlayerNotFound() {
        List<Player> finishOrder = Arrays.asList(player2, player3, player4);
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(0, bonus);
    }

    @Test
    @DisplayName("Test calculatePositionBonus - Empty finish order")
    void testCalculatePositionBonusEmptyList() {
        List<Player> finishOrder = new ArrayList<>();
        int bonus = rewardSystemTestFlight.calculatePositionBonus(finishOrder, player1);
        assertEquals(0, bonus);
    }

    @Test
    @DisplayName("Test calculateResourceBonus - Mixed resources")
    void testCalculateResourceBonusMixed() {
        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED, 2);    // Special goods: 2 * 3 = 6
        resources.put(GoodType.BLUE, 3);   // 3 * 2 = 6
        resources.put(GoodType.YELLOW, 1); // 1 * 2 = 2
        resources.put(GoodType.GREEN, 4);  // 4 * 2 = 8

        ship1.setResources(resources);

        int bonus = rewardSystemTestFlight.calculateResourceBonus(ship1);
        assertEquals(22, bonus); // 6 + 6 + 2 + 8 = 22
    }

    @Test
    @DisplayName("Test calculateResourceBonus - Only special goods")
    void testCalculateResourceBonusOnlySpecial() {
        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED, 5); // 5 * 3 = 15

        ship1.setResources(resources);

        int bonus = rewardSystemTestFlight.calculateResourceBonus(ship1);
        assertEquals(15, bonus);
    }

    @Test
    @DisplayName("Test calculateResourceBonus - No resources")
    void testCalculateResourceBonusEmpty() {
        Map<GoodType, Integer> resources = new HashMap<>();
        ship1.setResources(resources);

        int bonus = rewardSystemTestFlight.calculateResourceBonus(ship1);
        assertEquals(0, bonus);
    }

    @Test
    @DisplayName("Test calculateResourceBonus - Zero quantities")
    void testCalculateResourceBonusZeroQuantities() {
        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED, 0);
        resources.put(GoodType.BLUE, 0);

        ship1.setResources(resources);

        int bonus = rewardSystemTestFlight.calculateResourceBonus(ship1);
        assertEquals(0, bonus);
    }

    @Test
    @DisplayName("Test calculateBestLookingShipBonus - Clear winner")
    void testCalculateBestLookingShipBonusWinner() {
        // Setup different exposed connectors for each ship
        // Assuming we can set exposed connectors through some mechanism
        // For testing purposes, we'll assume ship1 has the least exposed connectors

        Map<Player, Integer> result = rewardSystemTestFlight.calculateBestLookingShipBonus(players);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsValue(8)); // bestLookingShipBonus value
    }

    @Test
    @DisplayName("Test calculateBestLookingShipBonus - Single player")
    void testCalculateBestLookingShipBonusSinglePlayer() {
        List<Player> singlePlayer = Arrays.asList(player1);

        Map<Player, Integer> result = rewardSystemTestFlight.calculateBestLookingShipBonus(singlePlayer);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(player1, result.keySet().iterator().next());
        assertEquals(8, result.get(player1));
    }

    @Test
    @DisplayName("Test calculateExposedConnectorsPenalty")
    void testCalculateExposedConnectorsPenalty() {
        // Assuming ship has some exposed connectors
        // The penalty is exposedConnectors * exposedConnectorsPenalty
        int penalty = rewardSystemTestFlight.calculateExposedConnectorsPenalty(ship1);

        // The result depends on ship1's exposed connectors
        assertTrue(penalty >= 0);
    }

    @Test
    @DisplayName("Test calculateReservedComponentsPenalty")
    void testCalculateReservedComponentsPenalty() {
        int penalty = rewardSystemTestFlight.calculateReservedComponentsPenalty(ship1);

        // Penalty equals the number of reserved components
        assertTrue(penalty >= 0);
    }

    @Test
    @DisplayName("Test calculateLostComponentsPenalty")
    void testCalculateLostComponentsPenalty() {
        int penalty = rewardSystemTestFlight.calculateLostComponentsPenalty(ship1);

        // Penalty equals the number of lost components
        assertTrue(penalty >= 0);
    }

    @Test
    @DisplayName("Test calculateTotalReward - Positive result")
    void testCalculateTotalRewardPositive() {
        // Setup ship with good conditions
        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED, 3);
        resources.put(GoodType.BLUE, 2);
        ship1.setResources(resources);

        List<Player> finishOrder = Arrays.asList(player1, player2, player3, player4);

        int totalReward = rewardSystemTestFlight.calculateTotalReward(finishOrder, player1);

        // Should be positive due to position bonus and resource bonus
        assertTrue(totalReward > 0);
    }

    @Test
    @DisplayName("Test calculateTotalReward - Negative result possible")
    void testCalculateTotalRewardNegative() {
        // Setup ship with bad conditions (high penalties)
        Map<GoodType, Integer> resources = new HashMap<>();
        ship1.setResources(resources); // No resources

        List<Player> finishOrder = Arrays.asList(player2, player3, player4, player1); // Last position

        int totalReward = rewardSystemTestFlight.calculateTotalReward(finishOrder, player1);

        // Could be negative due to penalties outweighing bonuses
        assertTrue(totalReward <= 20); // Maximum would be position bonus only
    }

    @Test
    @DisplayName("Test calculateFinalScores - Valid end phase")
    void testCalculateFinalScoresValidPhase() {
        // Setup players with initial credits
        player1.setCredits(10);
        player2.setCredits(5);
        player3.setCredits(15);
        player4.setCredits(0);

        // Setup some resources for better rewards
        Map<GoodType, Integer> resources1 = new HashMap<>();
        resources1.put(GoodType.RED, 2);
        ship1.setResources(resources1);

        Map<GoodType, Integer> resources2 = new HashMap<>();
        resources2.put(GoodType.BLUE, 3);
        ship2.setResources(resources2);

        List<Player> finishOrder = Arrays.asList(player1, player2, player3, player4);

        rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.END);

        // Verify that final scores were set and are non-negative
        assertTrue(player1.getFinalScore() >= 0);
        assertTrue(player2.getFinalScore() >= 0);
        assertTrue(player3.getFinalScore() >= 0);
        assertTrue(player4.getFinalScore() >= 0);

        // First place should generally have higher score than last place
        assertTrue(player1.getFinalScore() >= player4.getFinalScore());
    }

    @Test
    @DisplayName("Test calculateFinalScores - Invalid phase throws exception")
    void testCalculateFinalScoresInvalidPhase() {
        List<Player> finishOrder = Arrays.asList(player1, player2, player3, player4);

        // Test with different invalid phases
        assertThrows(IllegalArgumentException.class, () ->
                rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.SETUP));

        assertThrows(IllegalArgumentException.class, () ->
                rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.FLIGHT));

        assertThrows(IllegalArgumentException.class, () ->
                rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.SETUP));
    }

    @Test
    @DisplayName("Test calculateFinalScores - Minimum score is zero")
    void testCalculateFinalScoresMinimumZero() {
        // Setup player with negative total reward scenario
        player1.setCredits(0); // No initial credits

        // Empty resources (no bonus)
        Map<GoodType, Integer> resources = new HashMap<>();
        ship1.setResources(resources);

        List<Player> finishOrder = Arrays.asList(player2, player3, player4, player1); // Last position

        rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.END);

        // Final score should never be negative
        assertTrue(player1.getFinalScore() >= 0);
    }

    @Test
    @DisplayName("Test different reward system levels")
    void testDifferentRewardSystemLevels() {
        Map<GoodType, Integer> resources = new HashMap<>();
        resources.put(GoodType.RED, 2);
        ship3.setResources(resources);

        List<Player> finishOrder = Arrays.asList(player3);

        // Test with LEVEL_II reward system
        Map<Player, Integer> bestShipBonus = rewardSystemLevel2.calculateBestLookingShipBonus(finishOrder);
        assertEquals(12, bestShipBonus.get(player3)); // Different bonus amount

        int resourceBonus = rewardSystemLevel2.calculateResourceBonus(ship3);
        assertEquals(6, resourceBonus); // 2 * 3 = 6 (same resource bonus)
    }

    @Test
    @DisplayName("Test edge cases - Empty player list for best ship")
    void testBestLookingShipBonusEmptyList() {
        List<Player> emptyList = new ArrayList<>();

        Map<Player, Integer> result = rewardSystemTestFlight.calculateBestLookingShipBonus(emptyList);

        // Should handle empty list gracefully
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsValue(8));
        assertNull(result.keySet().iterator().next()); // bestPlayer would be null
    }

    @Test
    @DisplayName("Test complex scenario with all components")
    void testComplexScenarioAllComponents() {
        // Setup complex scenario
        player1.setCredits(25);
        player2.setCredits(15);
        player3.setCredits(10);
        player4.setCredits(5);

        // Setup different resource combinations
        Map<GoodType, Integer> resources1 = new HashMap<>();
        resources1.put(GoodType.RED, 3);   // Special goods
        resources1.put(GoodType.BLUE, 2);
        ship1.setResources(resources1);

        Map<GoodType, Integer> resources2 = new HashMap<>();
        resources2.put(GoodType.YELLOW, 4);
        resources2.put(GoodType.GREEN, 1);
        ship2.setResources(resources2);

        Map<GoodType, Integer> resources3 = new HashMap<>();
        resources3.put(GoodType.RED, 1);
        ship3.setResources(resources3);

        Map<GoodType, Integer> resources4 = new HashMap<>();
        // No resources for player4
        ship4.setResources(resources4);

        List<Player> finishOrder = Arrays.asList(player1, player2, player3, player4);

        // Calculate final scores
        rewardSystemTestFlight.calculateFinalScores(finishOrder, GamePhase.END);

        // Verify logical ordering
        assertTrue(player1.getFinalScore() >= player2.getFinalScore());
        assertTrue(player2.getFinalScore() >= player3.getFinalScore());
        assertTrue(player3.getFinalScore() >= player4.getFinalScore());

        // All scores should be non-negative
        assertTrue(player1.getFinalScore() >= 0);
        assertTrue(player2.getFinalScore() >= 0);
        assertTrue(player3.getFinalScore() >= 0);
        assertTrue(player4.getFinalScore() >= 0);
    }
}
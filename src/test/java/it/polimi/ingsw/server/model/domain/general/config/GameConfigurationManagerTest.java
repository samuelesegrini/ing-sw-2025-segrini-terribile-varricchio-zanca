package it.polimi.ingsw.server.model.domain.general.config;

import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigurationManagerTest {

    private static GameConfigurationManager configManager; // Make it static for BeforeAll

    private static final String FULL_COMPONENTS_JSON = "src/test/resources/json/components.json"; // Or actual path
    private static final String FULL_CARDS_JSON = "src/test/resources/json/adventure_cards.json"; // Or actual path
    private static final String FULL_GAME_CONFIGS_JSON = "src/test/resources/json/game_configurations.json"; // Or actual path

    @BeforeAll // Use @BeforeAll to load configuration once for all tests in this class
    static void setUpAll() throws IOException {
        configManager = new GameConfigurationManager();
        // Check if files exist before attempting to load
        assertTrue(new File(FULL_COMPONENTS_JSON).exists(), "Full components JSON file not found at: " + FULL_COMPONENTS_JSON);
        assertTrue(new File(FULL_CARDS_JSON).exists(), "Full adventure cards JSON file not found at: " + FULL_CARDS_JSON);
        assertTrue(new File(FULL_GAME_CONFIGS_JSON).exists(), "Full game configurations JSON file not found at: " + FULL_GAME_CONFIGS_JSON);

        System.out.println("Loading full configurations for tests...");
        assertDoesNotThrow(() -> {
            configManager.loadAllConfigurations(FULL_COMPONENTS_JSON, FULL_CARDS_JSON, FULL_GAME_CONFIGS_JSON);
        }, "Loading all configurations from full files should not throw an exception.");
        System.out.println("Full configurations loaded.");

        // Basic sanity checks after loading everything
        assertFalse(configManager.getAllComponents().isEmpty(), "Components should be loaded from full file.");
        assertFalse(configManager.getRawCardConfigs().isEmpty(), "Raw card configs should be loaded from full file.");
        assertFalse(configManager.getAllGameConfigs().isEmpty(), "Game configs should be loaded from full file.");
        System.out.println("Loaded " + configManager.getAllComponents().size() + " components.");
        System.out.println("Loaded " + configManager.getRawCardConfigs().size() + " card configurations.");
        System.out.println("Loaded " + configManager.getAllGameConfigs().size() + " game level configurations.");

    }
    // No @BeforeEach is needed if configManager is static and loaded in @BeforeAll

    @Test
    void testOverallLoading_CountsFromFullFiles() {
        // These assertions depend on the content of your *actual* full JSON files.
        // Update these expected numbers based on your files.
        assertEquals(156, configManager.getAllComponents().size(), "Check actual component count in components.json"); // Example count
        assertEquals(40, configManager.getRawCardConfigs().size(), "Check actual card count in adventure_cards.json"); // Example count
        assertEquals(2, configManager.getAllGameConfigs().size(), "Check actual game level count in game_configurations.json"); // TF and L2
    }

    @Test
    void testLoadedComponents_FromFullFile_SpotChecks() {
        // Spot check a few components by their ID (image path)
        Component battery = configManager.getAllComponents().stream()
                .filter(c -> c.getId().equals("/assets/tiles/battery_L-S.jpg"))
                .findFirst().orElse(null);
        assertNotNull(battery, "Specific battery component should be loaded.");
        assertTrue(battery instanceof it.polimi.ingsw.server.model.domain.ship.components.Battery);
        // Add more specific property checks if desired

        Component startingCabinBlue = configManager.getAllComponents().stream()
                .filter(c -> c.getId().equals("/assets/tiles/starting-cabin-blue.jpg"))
                .findFirst().orElse(null);
        assertNotNull(startingCabinBlue, "Starting cabin blue should be loaded.");
        assertTrue(startingCabinBlue instanceof it.polimi.ingsw.server.model.domain.ship.components.Cabin);
        // You might want to check the 'playerColor' property if your Cabin class stores it
        // assertEquals(PlayerColor.BLUE, ((Cabin)startingCabinBlue).getPlayerColor()); // If Cabin has getPlayerColor()
    }

    @Test
    void testLoadedCardConfigs_FromFullFile_SpotChecks() {
        Map<String, CardConfig> cardConfigs = configManager.getRawCardConfigs();

        CardConfig abandonedShipLvl1 = cardConfigs.get("abandoned-ship_lvl1_01");
        assertNotNull(abandonedShipLvl1);
        assertEquals("ABANDONED_SHIP", abandonedShipLvl1.type());
        assertEquals("LEVEL_I", abandonedShipLvl1.properties().get("level"));
        assertEquals(2, ((Number) abandonedShipLvl1.properties().get("crewLost")).intValue());

        CardConfig meteorSwarmLvl2 = cardConfigs.get("meteor-swarm_lvl2_01");
        assertNotNull(meteorSwarmLvl2);
        assertEquals("METEOR_SWARM", meteorSwarmLvl2.type());
        assertTrue(meteorSwarmLvl2.properties().get("meteorPattern") instanceof List);
        assertEquals(5, ((List<?>) meteorSwarmLvl2.properties().get("meteorPattern")).size());
    }

    @Test
    void testLoadedGameConfigs_FromFullFile_SpotChecks() {
        GameConfig tfConfig = configManager.getConfigForLevel(GameLevel.TEST_FLIGHT);
        assertNotNull(tfConfig);
        assertEquals(GameLevel.TEST_FLIGHT, tfConfig.levelEnum());
        assertEquals("18", tfConfig.flightBoardConfig().length()); // From your full game_configurations.json
        assertEquals(5, tfConfig.shipGridConfig().rows()); // From your full game_configurations.json for TF
        assertEquals(4, tfConfig.flightBoardConfig().rewardSystem().positionBonus().get("FIRST"));

        GameConfig l2Config = configManager.getConfigForLevel(GameLevel.LEVEL_II);
        assertNotNull(l2Config);
        assertEquals(GameLevel.LEVEL_II, l2Config.levelEnum());
        assertEquals("24", l2Config.flightBoardConfig().length());
        assertEquals(8, l2Config.flightBoardConfig().rewardSystem().positionBonus().get("FIRST"));
    }

    @Test
    void createComponentDeck_FromFullFile_ContainsAllLoadedComponents() {
        // Assumes loadAllConfigurations was called in @BeforeAll
        ComponentDeck deck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT); // Level doesn't matter for component selection
        assertNotNull(deck);
        assertEquals(configManager.getAllComponents().size(), deck.getDrawPile().size());
    }

    @Test
    void createAdventureDeck_TestFlight_FromFullFile() {
        // Assumes loadAllConfigurations was called in @BeforeAll
        AdventureDeck deck = configManager.createAdventureDeck(GameLevel.TEST_FLIGHT);
        assertNotNull(deck);
        deck.startFlightPhase(); // Make sure mainFlightDeck is populated

        long expectedTfCards = configManager.getRawCardConfigs().values().stream()
                .filter(cc -> "TEST_FLIGHT".equalsIgnoreCase((String)cc.properties().get("level")))
                .count();

        assertEquals(expectedTfCards, deck.getMainFlightDeckView().size(), "Deck size should match number of TEST_FLIGHT cards in full JSON.");
        if (expectedTfCards > 0) {
            assertTrue(deck.getMainFlightDeckView().stream().allMatch(c -> c.getLevel() == CardLevel.TEST_FLIGHT));
        }
    }

    @Test
    void createAdventureDeck_LevelII_FromFullFile_CorrectCardMixAndCount() {
        // Assumes loadAllConfigurations was called in @BeforeAll
        AdventureDeck deck = configManager.createAdventureDeck(GameLevel.LEVEL_II);
        assertNotNull(deck);

        // For Level II, check predictable piles structure if possible (before startFlightPhase)
        // These counts depend on your GameLevel.LEVEL_II enum definition for pile composition
        // and the number of available cards of each level in your full adventure_cards.json
        GameLevel levelII = GameLevel.LEVEL_II; // Use the enum directly
        int expectedPredictablePiles = levelII.getPredictablePileCount();
        assertEquals(expectedPredictablePiles, deck.getUncoveredPilesView().size());

        // Example check for one predictable pile (if rules are 3 cards: 2 L2, 1 L1/TF)
        if (expectedPredictablePiles > 0) {
            // This is complex to assert exactly without knowing the exact count of L1, L2, TF cards
            // in your full JSON and how the drawing for piles handles shortages.
            // A simpler check might be the total number of cards after combining.
        }

        deck.startFlightPhase(); // Combines all piles

        long expectedL1Cards = configManager.getRawCardConfigs().values().stream()
                .filter(cc -> "LEVEL_I".equalsIgnoreCase((String)cc.properties().get("level")))
                .count();
        long expectedL2Cards = configManager.getRawCardConfigs().values().stream()
                .filter(cc -> "LEVEL_II".equalsIgnoreCase((String)cc.properties().get("level")))
                .count();
        long expectedTfCardsForL2Deck = configManager.getRawCardConfigs().values().stream()
                .filter(cc -> "TEST_FLIGHT".equalsIgnoreCase((String)cc.properties().get("level")))
                .count();
        // Level II deck includes L1, L2, and TF cards (as TF can be part of L1 piles)
        long totalExpectedCardsInL2Deck = expectedL1Cards + expectedL2Cards + expectedTfCardsForL2Deck;

        assertEquals(totalExpectedCardsInL2Deck, deck.getMainFlightDeckView().size(),
                "Total cards in Level II deck should match sum of L1, L2, TF from full JSON.");
    }

    @Test
    void getRouteForLevel_FromFullFile_TestFlight() {
        it.polimi.ingsw.server.model.domain.flight.Route tfRoute = configManager.getRouteForLevel(GameLevel.TEST_FLIGHT);
        assertNotNull(tfRoute);
        assertEquals(18, Integer.parseInt(configManager.getConfigForLevel(GameLevel.TEST_FLIGHT).flightBoardConfig().length()));
        assertEquals(tfRoute.getLength(), Integer.parseInt(configManager.getConfigForLevel(GameLevel.TEST_FLIGHT).flightBoardConfig().length()));
        assertEquals(4, tfRoute.getStartingPositions().size());
        assertNotNull(tfRoute.getRewardSystem());

        // Create ONE PlayerId and ONE Player instance
        UUID testPlayerUUID = UUID.randomUUID();
        PlayerId thePlayerId = new PlayerId(testPlayerUUID, "p1");
        Player thePlayer = new Player(thePlayerId);

        // Debugging PlayerId equality
        PlayerId anotherIdWithSameUUID = new PlayerId(testPlayerUUID, "p1_diff_nick"); // Same UUID, diff nick
        System.out.println("thePlayerId.equals(anotherIdWithSameUUID): " + thePlayerId.equals(anotherIdWithSameUUID)); // Should be TRUE if equals by UUID
        System.out.println("thePlayerId.hashCode() == anotherIdWithSameUUID.hashCode(): " + (thePlayerId.hashCode() == anotherIdWithSameUUID.hashCode())); // Should be TRUE

        List<Player> finishOrderList = List.of(thePlayer); // List contains thePlayer
        int calculatedPosition = finishOrderList.indexOf(thePlayer); // Searching for thePlayer in a list containing it
        System.out.println("finishOrderList.indexOf(thePlayer) directly: " + calculatedPosition); // MUST be 0

        // If the above prints 0, then the issue is not with indexOf.
        // Then check the map lookup:
        PlayerOrder orderForPos0 = PlayerOrder.values()[0]; // This is PlayerOrder.FIRST
        System.out.println("Order for position 0: " + orderForPos0);
        // tfRoute.getRewardSystem().getPositionBonusMapDirectly() needs to be added to RewardSystem for this print
        // System.out.println("TF RewardSystem positionBonusMap: " + ((RewardSystem)tfRoute.getRewardSystem()).getPositionBonusMapDirectly());


        assertEquals(4, tfRoute.getRewardSystem().calculatePositionBonus(
                finishOrderList,
                thePlayer
        ), "Position bonus for first player in TF should be 4.");
    }
}
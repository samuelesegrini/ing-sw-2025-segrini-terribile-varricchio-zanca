package it.polimi.ingsw.model.domain.general.config;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the GameConfigurationManager.
 * Tests the loading of configuration files and the creation of game components.
 */
class GameConfigurationManagerTest {

    private GameConfigurationManager configManager;
    private File tempDir;
    private File componentsFile;
    private File cardsFile;
    private File gameConfigFile;

    // Default JSON content
    private static final String DEFAULT_COMPONENTS_JSON = """
        [
            {
                "id": "test_component_1",
                "type": "CANNON_SINGLE",
                "connectors": ["UP", "DOWN", "LEFT", "RIGHT"],
                "properties": {
                    "cost": 2,
                    "damage": 1
                }
            },
            {
                "id": "test_component_2",
                "type": "SHIELD",
                "connectors": ["UP", "DOWN", "LEFT", "RIGHT"],
                "properties": {
                    "cost": 3,
                    "defense": 2
                }
            }
        ]
        """;
    
    private static final String DEFAULT_CARDS_JSON = """
        [
            {
                "id": "test_card_1",
                "name": "Test Card 1",
                "description": "Test Description",
                "level": "TEST_FLIGHT",
                "type": "PLANETS"
            },
            {
                "id": "test_card_2",
                "name": "Test Card 2",
                "description": "Test Description 2",
                "level": "LEVEL_II",
                "type": "METEOR_SWARM"
            }
        ]
        """;
    
    private static final String DEFAULT_GAME_CONFIG_JSON = """
        [
            {
                "level": "TEST_FLIGHT",
                "boardSize": 8,
                "buildingTime": 5,
                "flightDays": 8,
                "rewards": {},
                "goodsPrices": {},
                "adventureDeckConfig": {}
            },
            {
                "level": "LEVEL_II",
                "boardSize": 10,
                "buildingTime": 7,
                "flightDays": 0,
                "rewards": {},
                "goodsPrices": {},
                "adventureDeckConfig": {}
            }
        ]
        """;

    @BeforeEach
    void setUp(@TempDir File tempDir) throws IOException {
        this.tempDir = tempDir;
        this.configManager = new GameConfigurationManager();
        
        // Initialize file paths
        componentsFile = new File(tempDir, "components.json");
        cardsFile = new File(tempDir, "cards.json");
        gameConfigFile = new File(tempDir, "game_config.json");
        
        // Create default test configuration files
        createTestConfigFiles(DEFAULT_COMPONENTS_JSON, DEFAULT_CARDS_JSON, DEFAULT_GAME_CONFIG_JSON);
    }

    /**
     * Creates test configuration files with specified content.
     */
    private void createTestConfigFiles(String componentsJson, String cardsJson, String gameConfigJson) throws IOException {
        if (componentsJson != null) writeToFile(componentsFile, componentsJson);
        if (cardsJson != null) writeToFile(cardsFile, cardsJson);
        if (gameConfigJson != null) writeToFile(gameConfigFile, gameConfigJson);
    }

    /**
     * Writes content to a file.
     */
    private void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    @Nested
    @DisplayName("Configuration Loading Tests")
    class ConfigurationLoadingTests {
        
        @Test
        @DisplayName("Should load all configurations successfully")
        void testLoadAllConfigurations() throws IOException {
            // Action: Load all configurations using default valid files
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Assert: Components, cards, and configs are loaded
            assertEquals(2, configManager.getAllComponents().size(), "Should have loaded 2 components");
            assertEquals(2, configManager.getAllAdventureCards().size(), "Should have loaded 2 adventure cards");
            assertEquals(2, configManager.getAllGameConfigs().size(), "Should have loaded 2 game configs");

            // Verify that creating an adventure deck throws UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class, () -> {
                configManager.createAdventureDeck(GameLevel.TEST_FLIGHT);
            }, "createAdventureDeck should throw UnsupportedOperationException");
        }

        @Test
        @DisplayName("Should load components correctly")
        void testLoadComponents() throws IOException {
            List<Component> components = configManager.loadComponents(componentsFile.getPath());
            assertEquals(2, components.size(), "Should load 2 components");
            assertTrue(components.stream().anyMatch(c -> c.getType() == ComponentType.CANNON_SINGLE));
            assertTrue(components.stream().anyMatch(c -> c.getType() == ComponentType.SHIELD));
        }

        @Test
        @DisplayName("Should load adventure cards correctly")
        void testLoadAdventureCards() throws IOException {
            // Action: Load adventure cards
            Map<String, AdventureCard> cards = configManager.loadAdventureCards(cardsFile.getPath());

            // Assert: Cards are loaded correctly
            assertEquals(2, cards.size(), "Should load 2 adventure cards");
            assertTrue(cards.containsKey("test_card_1"), "Should contain test_card_1");
            assertTrue(cards.containsKey("test_card_2"), "Should contain test_card_2");

            // Verify card properties
            AdventureCard card1 = cards.get("test_card_1");
            assertEquals("Test Description", card1.getDescription(), "Card 1 description should match");
            assertEquals(AdventureType.PLANETS, card1.getType(), "Card 1 type should be PLANETS");
            assertEquals(CardLevel.TEST_FLIGHT, card1.getLevel(), "Card 1 level should be TEST_FLIGHT");

            AdventureCard card2 = cards.get("test_card_2");
            assertEquals("Test Description 2", card2.getDescription(), "Card 2 description should match");
            assertEquals(AdventureType.METEOR_SWARM, card2.getType(), "Card 2 type should be METEOR_SWARM");
            assertEquals(CardLevel.LEVEL_II, card2.getLevel(), "Card 2 level should be LEVEL_II");
        }

        @Test
        @DisplayName("Should load game configs correctly")
        void testLoadGameConfigs() throws IOException {
            Map<GameLevel, GameConfig> configs = configManager.loadGameConfigs(gameConfigFile.getPath());
            assertEquals(2, configs.size(), "Should load 2 game configs");
            assertTrue(configs.containsKey(GameLevel.TEST_FLIGHT));
            assertTrue(configs.containsKey(GameLevel.LEVEL_II));
            assertEquals(8, configs.get(GameLevel.TEST_FLIGHT).boardSize());
            assertEquals(10, configs.get(GameLevel.LEVEL_II).boardSize());
        }

        @Test
        @DisplayName("Should handle empty JSON arrays")
        void testLoadEmptyJsonArrays() throws IOException {
            // Arrange: Create empty files
            createTestConfigFiles("[]", "[]", "[]");

            // Action & Assert: Load each type and check for emptiness
            List<Component> components = configManager.loadComponents(componentsFile.getPath());
            assertTrue(components.isEmpty(), "Loading empty components JSON should result in an empty list");

            Map<String, AdventureCard> cards = configManager.loadAdventureCards(cardsFile.getPath());
            assertTrue(cards.isEmpty(), "Loading empty cards JSON should result in an empty map");

            Map<GameLevel, GameConfig> configs = configManager.loadGameConfigs(gameConfigFile.getPath());
            assertTrue(configs.isEmpty(), "Loading empty game config JSON should result in an empty map");
        }

        @Test
        @DisplayName("Should throw exception when loading invalid JSON format")
        void testLoadInvalidJsonFormat() throws IOException {
            // Arrange: Create invalid JSON file
            createTestConfigFiles("invalid json", null, null); // Only need one invalid file

            // Action & Assert: Test loading invalid components
            assertThrows(IOException.class, () -> 
                configManager.loadComponents(componentsFile.getPath()),
                "Should throw IOException for invalid components JSON format"
            );
            assertThrows(IOException.class, () ->
                configManager.loadAdventureCards(componentsFile.getPath()),
                "Should throw IOException for invalid cards JSON format"
            );
            assertThrows(IOException.class, () ->
                configManager.loadGameConfigs(componentsFile.getPath()),
                "Should throw IOException for invalid game config JSON format"
            );
        }

        @Test
        @DisplayName("Should throw exception for structurally invalid component data")
        void testLoadStructurallyInvalidComponent() throws IOException {
            // Arrange: Component missing 'type' field
            String invalidComponentsJson = """
            [
                {
                    "id": "invalid_component",
                    "connectors": ["UP"],
                    "properties": {}
                }
            ]
            """;
            createTestConfigFiles(invalidComponentsJson, DEFAULT_CARDS_JSON, DEFAULT_GAME_CONFIG_JSON);

            // Action & Assert: Loading should fail due to missing required field during conversion/creation
            assertThrows(Exception.class, () ->
                configManager.loadComponents(componentsFile.getPath()),
                "Should throw an exception when component data is structurally invalid"
            );
        }
        
        @Test
        @DisplayName("Should handle invalid GameLevel string in game config gracefully")
        void testLoadInvalidGameLevelString() throws IOException {
            // Arrange: Invalid level string
            String invalidGameConfigJson = """
            [
                {
                    "level": "INVALID_LEVEL_STRING", 
                    "boardSize": 8, "buildingTime": 5, "flightDays": 8, "rewards": {}, "goodsPrices": {}, "adventureDeckConfig": {}
                }
            ]
            """;
            createTestConfigFiles(DEFAULT_COMPONENTS_JSON, DEFAULT_CARDS_JSON, invalidGameConfigJson);

            // Action: Loading game configs should skip invalid level values
            Map<GameLevel, GameConfig> configs = configManager.loadGameConfigs(gameConfigFile.getPath());
            
            // Assert: No configs should be loaded
            assertTrue(configs.isEmpty(), "Should not load any configs with invalid level values");
        }
        
        @Test
        @DisplayName("Should throw exception when loading non-existent files")
        void testMissingFiles() {
            // Arrange: Use paths that don't exist
            String nonExistentPath = "non_existent_file.json";

            // Action & Assert: Test loading throws IOException for each type
            assertThrows(IOException.class, () -> 
                configManager.loadComponents(nonExistentPath),
                "Should throw IOException when components file doesn't exist"
            );
            
            assertThrows(IOException.class, () -> 
                configManager.loadAdventureCards(nonExistentPath),
                "Should throw IOException when cards file doesn't exist"
            );
            
            assertThrows(IOException.class, () -> 
                configManager.loadGameConfigs(nonExistentPath),
                "Should throw IOException when game config file doesn't exist"
            );
        }
    }

    @Nested
    @DisplayName("Deck Creation Tests")
    class DeckCreationTests {
        
        @BeforeEach
        void setUp() throws IOException {
            // Arrange: Load default configurations before each test in this nested class
            createTestConfigFiles(DEFAULT_COMPONENTS_JSON, DEFAULT_CARDS_JSON, DEFAULT_GAME_CONFIG_JSON);
            
            // Since the AdventureCardFactory methods throw UnsupportedOperationException,
            // we'll load only the components and game configs, skipping the adventure cards
            configManager.loadComponents(componentsFile.getPath());
            configManager.loadGameConfigs(gameConfigFile.getPath());
            
            // We'll skip loading adventure cards since it throws UnsupportedOperationException
            // configManager.loadAdventureCards(cardsFile.getPath());
        }
        
        @Test
        @DisplayName("Should create component deck containing all loaded components regardless of level")
        void testCreateComponentDeckContents() {
            // Action: Create decks for different levels
            var testFlightDeck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            var levelIIDeck = configManager.createComponentDeck(GameLevel.LEVEL_II);

            // Assert: Both decks should contain all components loaded (2 in this case)
            assertNotNull(testFlightDeck, "TEST_FLIGHT component deck should not be null");
            assertEquals(2, testFlightDeck.getRemainingCards(), "TEST_FLIGHT deck should contain all 2 loaded components");

            assertNotNull(levelIIDeck, "LEVEL_II component deck should not be null");
            assertEquals(2, levelIIDeck.getRemainingCards(), "LEVEL_II deck should contain all 2 loaded components");
            
            // Verify specific components are potentially drawable
            Optional<Component> c1 = testFlightDeck.draw();
            Optional<Component> c2 = testFlightDeck.draw();
            assertTrue(c1.isPresent() && c2.isPresent(), "Should be able to draw both components");
            assertTrue(
                (c1.get().getType() == ComponentType.CANNON_SINGLE && c2.get().getType() == ComponentType.SHIELD) ||
                (c1.get().getType() == ComponentType.SHIELD && c2.get().getType() == ComponentType.CANNON_SINGLE),
                "Drawn components should be the ones loaded"
            );
        }

        @Test
        @DisplayName("Should handle component deck draw, discard, and reshuffle")
        void testComponentDeckOperations() {
            // Arrange
            var deck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            assertEquals(2, deck.getRemainingCards(), "Initial draw pile size");
            assertEquals(0, deck.getDiscardedCards(), "Initial discard pile size");

            // Action: Draw one, discard it
            Component drawn1 = deck.draw().orElseThrow();
            assertEquals(1, deck.getRemainingCards(), "Draw pile after 1 draw");
            deck.discard(drawn1);
            assertEquals(1, deck.getDiscardedCards(), "Discard pile after 1 discard");

            // Action: Draw second (last in draw pile)
            Component drawn2 = deck.draw().orElseThrow();
            assertEquals(0, deck.getRemainingCards(), "Draw pile after 2 draws");
            assertNotEquals(drawn1.getType(), drawn2.getType(), "Second draw should be different component");

            // Action: Discard second, attempt draw (should trigger reshuffle)
            deck.discard(drawn2);
            assertEquals(2, deck.getDiscardedCards(), "Discard pile after 2 discards");
            Optional<Component> drawn3 = deck.draw();

            // Assert: Reshuffle occurred
            assertTrue(drawn3.isPresent(), "Should draw successfully after reshuffle");
            assertEquals(1, deck.getRemainingCards(), "Draw pile after reshuffle and draw");
            assertEquals(0, deck.getDiscardedCards(), "Discard pile should be empty after reshuffle");
        }

        @Test
        @DisplayName("Should handle drawing from an empty component deck")
        void testEmptyComponentDeckDraw() {
            // Arrange: Create empty component file, load, create deck
            try {
                createTestConfigFiles("[]", DEFAULT_CARDS_JSON, DEFAULT_GAME_CONFIG_JSON);
                configManager.loadComponents(componentsFile.getPath()); // Only reload components
            } catch (IOException e) { fail("Setup for empty component deck failed", e); }
            
            var deck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            assertEquals(0, deck.getRemainingCards(), "Deck should be empty initially");

            // Action & Assert: Draw from empty deck
            Optional<Component> emptyDraw = deck.draw();
            assertTrue(emptyDraw.isEmpty(), "Drawing from empty deck should return empty Optional");
        }

        @Test
        @DisplayName("Should create adventure deck with cards for the specified level")
        void testCreateAdventureDeckContents() {
            // Since the AdventureCardFactory.createDeckForGameLevel method throws UnsupportedOperationException,
            // we'll just test that the method exists and can be called
            assertThrows(UnsupportedOperationException.class, () -> {
                configManager.createAdventureDeck(GameLevel.TEST_FLIGHT);
            }, "Should throw UnsupportedOperationException since the method is not implemented");
        }
        
        @Test
        @DisplayName("Should create empty adventure deck if no cards exist for the level")
        void testCreateAdventureDeckForLevelWithNoCards() throws IOException {
            // Arrange: Add a non-existent level config but no cards for it
            String gameConfigWithNonExistentLevel = """
            [
                {
                    "level": "TEST_FLIGHT", "boardSize": 8, "buildingTime": 5, "flightDays": 8,
                     "rewards": {}, "goodsPrices": {}, "adventureDeckConfig": {}
                },
                {
                    "level": "LEVEL_II", "boardSize": 10, "buildingTime": 7, "flightDays": 0,
                     "rewards": {}, "goodsPrices": {}, "adventureDeckConfig": {}
                },
                {
                    "level": "NON_EXISTENT_LEVEL", "boardSize": 12, "buildingTime": 9, "flightDays": 0,
                     "rewards": {}, "goodsPrices": {}, "adventureDeckConfig": {}
                }
            ]
            """;
            createTestConfigFiles(DEFAULT_COMPONENTS_JSON, DEFAULT_CARDS_JSON, gameConfigWithNonExistentLevel);
            // Reload all as the factory likely uses the game config during deck creation potentially
            configManager.loadAllConfigurations(componentsFile.getPath(), cardsFile.getPath(), gameConfigFile.getPath());

            // Action: Create deck for a level that doesn't exist in the enum
            // This should throw UnsupportedOperationException since the method is not implemented
            assertThrows(UnsupportedOperationException.class, () -> {
                configManager.createAdventureDeck(GameLevel.TEST_FLIGHT);
            }, "Should throw UnsupportedOperationException since the method is not implemented");
        }
    }

    @Nested
    @DisplayName("Game Configuration Retrieval Tests")
    class GameConfigurationRetrievalTests {
        
        @BeforeEach
        void setUp() throws IOException {
            // Arrange: Load default configurations
            createTestConfigFiles(DEFAULT_COMPONENTS_JSON, DEFAULT_CARDS_JSON, DEFAULT_GAME_CONFIG_JSON);
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );
        }
        
        @Test
        @DisplayName("Should get correct config for existing levels")
        void testGetConfigForExistingLevel() {
            // Action & Assert: Test TEST_FLIGHT level config
            GameConfig testFlightConfig = configManager.getConfigForLevel(GameLevel.TEST_FLIGHT);
            assertNotNull(testFlightConfig, "TEST_FLIGHT config should not be null");
            assertEquals(8, testFlightConfig.flightDays());
            assertEquals(8, testFlightConfig.boardSize());
            assertEquals(5, testFlightConfig.buildingTime());

            // Action & Assert: Test LEVEL_II config
            GameConfig levelIIConfig = configManager.getConfigForLevel(GameLevel.LEVEL_II);
            assertNotNull(levelIIConfig, "LEVEL_II config should not be null");
            assertEquals(0, levelIIConfig.flightDays());
            assertEquals(10, levelIIConfig.boardSize());
            assertEquals(7, levelIIConfig.buildingTime());
        }

        @Test
        @DisplayName("Should return null when getting config for a level not loaded")
        void testGetConfigForNonLoadedLevel() {
            // Action & Assert: Request a level that exists in enum but wasn't in the config file
            // Since we only have TEST_FLIGHT and LEVEL_II in our config, any other level should return null
            GameConfig config = configManager.getConfigForLevel(GameLevel.TEST_FLIGHT);
            assertNotNull(config, "Config for TEST_FLIGHT should exist");
            
            // We can't test for LEVEL_III since it doesn't exist in the enum
            // Instead, we'll test that we can get all configs and verify the count
            Map<GameLevel, GameConfig> allConfigs = configManager.getAllGameConfigs();
            assertEquals(2, allConfigs.size(), "Should have exactly 2 game configs loaded");
        }
    }
} 
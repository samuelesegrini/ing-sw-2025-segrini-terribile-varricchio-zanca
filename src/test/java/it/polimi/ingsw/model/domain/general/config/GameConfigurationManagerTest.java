package it.polimi.ingsw.model.domain.general.config;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigurationManagerTest {

    private GameConfigurationManager configManager;
    private Path tempDir;
    private File componentsFile;
    private File cardsFile;
    private File gameConfigFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        this.tempDir = tempDir;
        this.configManager = new GameConfigurationManager();
        
        // Create test configuration files
        createTestConfigFiles();
    }

    private void createTestConfigFiles() throws IOException {
        // Create components.json
        componentsFile = tempDir.resolve("components.json").toFile();
        String componentsJson = """
            [
                {
                    "id": "test_component_1",
                    "name": "Test Component 1",
                    "type": "CANNON_SINGLE",
                    "cost": 2,
                    "damage": 1
                },
                {
                    "id": "test_component_2",
                    "name": "Test Component 2",
                    "type": "SHIELD",
                    "cost": 3,
                    "defense": 2
                }
            ]
            """;
        java.nio.file.Files.writeString(componentsFile.toPath(), componentsJson);

        // Create cards.json
        cardsFile = tempDir.resolve("cards.json").toFile();
        String cardsJson = """
            [
                {
                    "id": "test_card_1",
                    "name": "Test Card 1",
                    "description": "Test Description",
                    "level": "TEST_FLIGHT",
                    "type": "EVENT"
                },
                {
                    "id": "test_card_2",
                    "name": "Test Card 2",
                    "description": "Test Description 2",
                    "level": "LEVEL_II",
                    "type": "ENEMY"
                }
            ]
            """;
        java.nio.file.Files.writeString(cardsFile.toPath(), cardsJson);

        // Create game_config.json
        gameConfigFile = tempDir.resolve("game_config.json").toFile();
        String gameConfigJson = """
            [
                {
                    "level": "TEST_FLIGHT",
                    "maxPlayers": 2,
                    "startingResources": 3,
                    "maxTurns": 10
                },
                {
                    "level": "LEVEL_II",
                    "maxPlayers": 4,
                    "startingResources": 2,
                    "maxTurns": 8
                }
            ]
            """;
        java.nio.file.Files.writeString(gameConfigFile.toPath(), gameConfigJson);
    }

    @Nested
    @DisplayName("Configuration Loading Tests")
    class ConfigurationLoadingTests {
        @Test
        @DisplayName("Should load all configurations successfully")
        void testLoadAllConfigurations() throws IOException {
            // Load all configurations
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Verify components were loaded using the individual load method
            List<Component> components = configManager.loadComponents(componentsFile.getPath());
            assertFalse(components.isEmpty(), "Components should not be empty");
            assertEquals(2, components.size(), "Should load exactly 2 components");
            assertEquals(ComponentType.CANNON_SINGLE, components.get(0).getType());
            assertEquals(ComponentType.SHIELD, components.get(1).getType());

            // Verify cards were loaded using the individual load method
            Map<String, AdventureCard> cards = configManager.loadAdventureCards(cardsFile.getPath());
            assertFalse(cards.isEmpty(), "Cards should not be empty");
            assertEquals(2, cards.size(), "Should load exactly 2 cards");
            assertTrue(cards.containsKey("test_card_1"));
            assertTrue(cards.containsKey("test_card_2"));

            // Verify game configs were loaded using the individual load method
            Map<GameLevel, GameConfig> configs = configManager.loadGameConfigs(gameConfigFile.getPath());
            assertFalse(configs.isEmpty(), "Game configs should not be empty");
            assertEquals(2, configs.size(), "Should load exactly 2 game configs");
            assertTrue(configs.containsKey(GameLevel.TEST_FLIGHT));
            assertTrue(configs.containsKey(GameLevel.LEVEL_II));
        }

        @Test
        @DisplayName("Should throw exception when loading invalid JSON")
        void testLoadInvalidJson() {
            // Create invalid JSON file
            File invalidFile = tempDir.resolve("invalid.json").toFile();
            try {
                java.nio.file.Files.writeString(invalidFile.toPath(), "invalid json content");
            } catch (IOException e) {
                fail("Failed to create invalid JSON file");
            }

            // Test loading invalid components
            assertThrows(IOException.class, () -> 
                configManager.loadComponents(invalidFile.getPath()),
                "Should throw IOException when loading invalid components JSON"
            );

            // Test loading invalid cards
            assertThrows(IOException.class, () -> 
                configManager.loadAdventureCards(invalidFile.getPath()),
                "Should throw IOException when loading invalid cards JSON"
            );

            // Test loading invalid game config
            assertThrows(IOException.class, () -> 
                configManager.loadGameConfigs(invalidFile.getPath()),
                "Should throw IOException when loading invalid game config JSON"
            );
        }
    }

    @Nested
    @DisplayName("Deck Creation Tests")
    class DeckCreationTests {
        @Test
        @DisplayName("Should create component deck with correct components")
        void testCreateComponentDeck() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Create and verify component deck
            var deck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            assertNotNull(deck, "Component deck should not be null");
            assertEquals(2, deck.getRemainingCards(), "Draw pile should contain exactly 2 components");
            assertEquals(0, deck.getDiscardedCards(), "Discard pile should be empty initially");

            // Test drawing and discarding components
            Optional<Component> drawnComponent = deck.draw();
            assertTrue(drawnComponent.isPresent(), "Should be able to draw a component");
            assertEquals(1, deck.getRemainingCards(), "Should have one less card in draw pile");
            
            Component component = drawnComponent.get();
            deck.discard(component);
            assertEquals(1, deck.getDiscardedCards(), "Should have one card in discard pile");
            
            // Test reshuffling when draw pile is empty
            Optional<Component> secondDraw = deck.draw();
            assertTrue(secondDraw.isPresent(), "Should be able to draw from reshuffled deck");
            assertEquals(0, deck.getDiscardedCards(), "Discard pile should be empty after reshuffling");
        }

        @Test
        @DisplayName("Should create component deck for different levels")
        void testCreateComponentDeckForDifferentLevels() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Create decks for both levels
            var testFlightDeck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            var levelIIDeck = configManager.createComponentDeck(GameLevel.LEVEL_II);

            // Verify both decks are created successfully
            assertNotNull(testFlightDeck, "TEST_FLIGHT deck should not be null");
            assertNotNull(levelIIDeck, "LEVEL_II deck should not be null");
            
            // Verify both decks contain the same number of components
            assertEquals(testFlightDeck.getRemainingCards(), levelIIDeck.getRemainingCards(),
                "Both decks should have the same number of components");
        }

        @Test
        @DisplayName("Should handle empty deck correctly")
        void testEmptyDeck() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Create deck
            var deck = configManager.createComponentDeck(GameLevel.TEST_FLIGHT);
            
            // Draw all components
            for (int i = 0; i < 2; i++) {
                Optional<Component> drawn = deck.draw();
                assertTrue(drawn.isPresent(), "Should be able to draw component " + i);
                deck.discard(drawn.get());
            }
            
            // Try to draw when both piles are empty
            Optional<Component> emptyDraw = deck.draw();
            assertTrue(emptyDraw.isEmpty(), "Should not be able to draw from empty deck");
        }

        @Test
        @DisplayName("Should create adventure deck with correct cards")
        void testCreateAdventureDeck() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Create and verify adventure deck
            var deck = configManager.createAdventureDeck(GameLevel.TEST_FLIGHT);
            assertNotNull(deck, "Adventure deck should not be null");
            
            // Draw and verify cards are for the correct level
            Optional<AdventureCard> drawnCard = deck.drawNextCard();
            assertTrue(drawnCard.isPresent(), "Should be able to draw a card");
            assertEquals(GameLevel.TEST_FLIGHT, drawnCard.get().getLevel(), 
                "Card should be for TEST_FLIGHT level");
        }
    }

    @Nested
    @DisplayName("Game Configuration Tests")
    class GameConfigurationTests {
        @Test
        @DisplayName("Should get correct config for different levels")
        void testGetConfigForLevel() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Test TEST_FLIGHT level config
            GameConfig testFlightConfig = configManager.getConfigForLevel(GameLevel.TEST_FLIGHT);
            assertNotNull(testFlightConfig, "TEST_FLIGHT level config should not be null");
            assertEquals(8, testFlightConfig.flightDays());

            // Test LEVEL_II config
            GameConfig levelIIConfig = configManager.getConfigForLevel(GameLevel.LEVEL_II);
            assertNotNull(levelIIConfig, "LEVEL_II config should not be null");
            assertEquals(0, levelIIConfig.flightDays());
        }

        @Test
        @DisplayName("Should return null for non-existent level")
        void testGetConfigForNonExistentLevel() throws IOException {
            // Load configurations first
            configManager.loadAllConfigurations(
                componentsFile.getPath(),
                cardsFile.getPath(),
                gameConfigFile.getPath()
            );

            // Since we only have TEST_FLIGHT and LEVEL_II, any other level should return null
            GameConfig config = configManager.getConfigForLevel(GameLevel.TEST_FLIGHT);
            assertNotNull(config, "Config for non-existent level should be null");
        }
    }
} 
package it.polimi.ingsw.server.model.domain.general.loader;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.MeteorSwarmCard;
import it.polimi.ingsw.server.model.domain.adventure.card.PlanetsCard;
import it.polimi.ingsw.server.model.domain.general.config.CardConfig;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AdventureCardFactory class that is responsible for creating
 * different types of adventure cards used in the game.
 */
class AdventureCardFactoryTest {

    private AdventureCardFactory factory;
    private CardConfig planetConfig;
    private CardConfig meteorSwarmConfig;
    private CardConfig unknownConfig;
    private CardConfig invalidLevelConfig;

    @BeforeEach
    void setUp() {
        factory = new AdventureCardFactory();
        
        // Create test configurations
        Map<String, Object> planetProperties = new HashMap<>();
        planetProperties.put("level", "TEST_FLIGHT");
        planetConfig = new CardConfig("planet1", "PLANETS", "Test Planet", planetProperties);

        Map<String, Object> meteorProperties = new HashMap<>();
        meteorProperties.put("level", "TEST_FLIGHT");
        meteorSwarmConfig = new CardConfig("meteor1", "METEOR_SWARM", "Test Meteor Swarm", meteorProperties);

        Map<String, Object> unknownProperties = new HashMap<>();
        unknownConfig = new CardConfig("unknown1", "UNKNOWN", "Test Unknown", unknownProperties);
        
        Map<String, Object> invalidLevelProperties = new HashMap<>();
        invalidLevelProperties.put("level", "INVALID_LEVEL");
        invalidLevelConfig = new CardConfig("invalid1", "PLANETS", "Invalid Level", invalidLevelProperties);
    }

    @Test
    void testCreateCardWithUnknownType() {
        // Test that creating a card with an unknown type throws IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class, 
                () -> factory.createCard(unknownConfig));
        
        // Verify the exception message
        assertTrue(exception.getMessage().contains("No creator registered for card type: UNKNOWN"));
    }

    @Test
    void testCreatePlanetCard() {
        // Test that creating a planet card works
        AdventureCard card = factory.createCard(planetConfig);
        
        assertNotNull(card);
        assertTrue(card instanceof PlanetsCard);
        assertEquals("planet1", card.getId());
        assertEquals("Test Planet", card.getDescription());
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel());
        assertEquals(AdventureType.PLANETS, card.getType());
        
        // Check that the card contains planets
        PlanetsCard planetCard = (PlanetsCard) card;
        assertNotNull(planetCard.getPlanets());
        assertFalse(planetCard.getPlanets().isEmpty());
    }

    @Test
    void testCreateMeteorSwarmCard() {
        // Test that creating a meteor swarm card works
        AdventureCard card = factory.createCard(meteorSwarmConfig);
        
        assertNotNull(card);
        assertTrue(card instanceof MeteorSwarmCard);
        assertEquals("meteor1", card.getId());
        assertEquals("Test Meteor Swarm", card.getDescription());
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel());
        assertEquals(AdventureType.METEOR_SWARM, card.getType());
        
        // Check that the card contains meteors
        MeteorSwarmCard meteorCard = (MeteorSwarmCard) card;
        assertNotNull(meteorCard.getMeteorPattern());
        assertFalse(meteorCard.getMeteorPattern().isEmpty());
    }

    @Test
    void testCreateCardWithInvalidLevel() {
        // Test that creating a card with an invalid level uses default level
        AdventureCard card = factory.createCard(invalidLevelConfig);
        
        assertNotNull(card);
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel(), 
                "Should use default level TEST_FLIGHT when level is invalid");
    }
    
    @Test
    void testCreateCardWithNullProperties() {
        // Test creating a card with null properties
        CardConfig configWithNullProps = new CardConfig("test1", "PLANETS", "Test Card", null);
        
        AdventureCard card = factory.createCard(configWithNullProps);
        assertNotNull(card);
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel(), 
                "Should use default level TEST_FLIGHT when properties are null");
    }

    @Test
    void testCreateDeckForGameLevel() {
        // Test that creating a deck for any game level throws UnsupportedOperationException
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> 
            factory.createDeckForGameLevel(GameLevel.TEST_FLIGHT));
        
        assertEquals("createDeckForGameLevel is not yet implemented", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(GameLevel.class)
    void testCreatePileForGameLevel(GameLevel gameLevel) {
        // Test that creating a pile for each game level returns a pile with the correct size
        List<AdventureCard> pile = factory.createPileForGameLevel(gameLevel);
        
        assertNotNull(pile);
        assertEquals(gameLevel.getCardPerPile(), pile.size(), 
                "Pile size should match the cardsPerPile setting for the game level");
        
        // Verify that all cards have the correct level
        for (AdventureCard card : pile) {
            CardLevel expectedCardLevel = null;
            switch (gameLevel) {
                case TEST_FLIGHT:
                    expectedCardLevel = CardLevel.TEST_FLIGHT;
                    break;
                case LEVEL_II:
                    expectedCardLevel = CardLevel.LEVEL_II;
                    break;
                default:
                    fail("Unexpected game level: " + gameLevel);
            }
            
            assertEquals(expectedCardLevel, card.getLevel(),
                    "Card level should match the corresponding level for the game level");
        }
    }
    
    @Test
    void testPileContainsMixOfCardTypes() {
        // Create a pile with enough cards to ensure we get both types
        List<AdventureCard> pile = factory.createPileForGameLevel(GameLevel.TEST_FLIGHT);
        
        // Count the different card types
        long planetCardCount = pile.stream()
                .filter(card -> card.getType() == AdventureType.PLANETS)
                .count();
                
        long meteorCardCount = pile.stream()
                .filter(card -> card.getType() == AdventureType.METEOR_SWARM)
                .count();
        
        // The pile should contain a mix of both types (this is probabilistic but very likely)
        // If we have at least 8 cards, the probability of getting only one type is very low
        assertTrue(planetCardCount > 0, "Pile should contain at least one planet card");
        assertTrue(meteorCardCount > 0, "Pile should contain at least one meteor swarm card");
    }

    @Test
    void testRegisterCreator() {
        // Test that we can register a new creator
        AdventureCardCreator mockCreator = config -> {
            throw new UnsupportedOperationException("Mock creator");
        };
        
        factory.registerCreator("TEST", mockCreator);
        
        // Verify that the creator was registered by attempting to use it
        CardConfig testConfig = new CardConfig("test1", "TEST", "Test Card", new HashMap<>());
        Exception exception = assertThrows(UnsupportedOperationException.class, 
                () -> factory.createCard(testConfig));
        
        assertEquals("Mock creator", exception.getMessage());
    }
    
    @Test
    void testRegisterCreatorWithNullValues() {
        // Test registering with null values
        assertThrows(NullPointerException.class, 
                () -> factory.registerCreator(null, config -> null));
        
        assertThrows(NullPointerException.class, 
                () -> factory.registerCreator("NULL_CREATOR", null));
    }
    
    @Test
    void testCardIdUniqueness() {
        // Create a pile and verify IDs are unique
        // This indirectly tests the createRandomCard method since createPileForGameLevel uses it
        List<AdventureCard> pile = factory.createPileForGameLevel(GameLevel.TEST_FLIGHT);
        
        // Extract all IDs
        Set<String> uniqueIds = new HashSet<>();
        for (AdventureCard card : pile) {
            uniqueIds.add(card.getId());
        }
        
        // Verify all IDs are unique
        assertEquals(pile.size(), uniqueIds.size(), "All card IDs should be unique");
    }
} 
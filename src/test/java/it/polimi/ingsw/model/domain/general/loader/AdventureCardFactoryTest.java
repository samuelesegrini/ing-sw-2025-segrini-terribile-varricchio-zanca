package it.polimi.ingsw.model.domain.general.loader;

import it.polimi.ingsw.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.general.config.CardConfig;
import it.polimi.ingsw.model.enums.GameLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdventureCardFactoryTest {

    private AdventureCardFactory factory;
    private CardConfig planetConfig;
    private CardConfig meteorSwarmConfig;
    private CardConfig unknownConfig;

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
    }

    @Test
    void testCreateCardWithUnknownType() {
        // Test that creating a card with an unknown type throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> factory.createCard(unknownConfig));
    }

    @Test
    void testCreatePlanetCard() {
        // Test that creating a planet card works
        AdventureCard card = factory.createCard(planetConfig);
        assertNotNull(card);
        assertEquals("planet1", card.getId());
    }

    @Test
    void testCreateMeteorSwarmCard() {
        // Test that creating a meteor swarm card works
        AdventureCard card = factory.createCard(meteorSwarmConfig);
        assertNotNull(card);
        assertEquals("meteor1", card.getId());
    }

    @Test
    void testCreateDeckForGameLevel() {
        // Test that creating a deck for any game level throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> 
            factory.createDeckForGameLevel(GameLevel.TEST_FLIGHT));
    }

    @Test
    void testCreatePileForGameLevel() {
        // Test that creating a pile for a game level returns a non-empty list
        List<AdventureCard> pile = factory.createPileForGameLevel(GameLevel.TEST_FLIGHT);
        assertNotNull(pile);
        assertFalse(pile.isEmpty());
        assertEquals(GameLevel.TEST_FLIGHT.getCardPerPile(), pile.size());
        
        // Verify that all cards in the pile are valid
        for (AdventureCard card : pile) {
            assertNotNull(card);
            assertNotNull(card.getId());
            assertTrue(card.getId().startsWith("card_"));
        }
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
        assertThrows(UnsupportedOperationException.class, () -> factory.createCard(testConfig));
    }
} 
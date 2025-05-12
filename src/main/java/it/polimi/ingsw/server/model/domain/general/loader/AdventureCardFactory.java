package it.polimi.ingsw.server.model.domain.general.loader;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.PlanetsCard;
import it.polimi.ingsw.server.model.domain.adventure.card.MeteorSwarmCard;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.general.config.CardConfig;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Factory class responsible for creating different types of adventure cards
 */
public class AdventureCardFactory {
    private final Map<String, AdventureCardCreator> creators;
    private final Map<String, AdventureCard> cardRegistry;
    private final Random random;

    public AdventureCardFactory() {
        this.creators = new HashMap<>();
        this.cardRegistry = new HashMap<>();
        this.random = new Random();
        initializeCreators();
    }

    /**
     * Registers a creator for a specific card type
     */
    public void registerCreator(String type, AdventureCardCreator creator) {
        creators.put(type, creator);
    }

    /**
     * Creates an adventure card from the given configuration
     */
    public AdventureCard createCard(CardConfig config) {
        AdventureCardCreator creator = creators.get(config.type());
        if (creator == null) {
            throw new IllegalArgumentException("No creator registered for card type: " + config.type());
        }
        return creator.create(config);
    }

    /**
     * Creates a planet card
     */
    private AdventureCard createPlanetCard(CardConfig config) {
        // Create a list of planets
        List<Planet> planets = new ArrayList<>();
        
        // Create a planet with some goods
        Map<GoodType, Integer> goodQuantities = new HashMap<>();
        goodQuantities.put(GoodType.RED, 2);
        goodQuantities.put(GoodType.BLUE, 1);
        planets.add(new Planet("Planet 1", goodQuantities));
        
        // Create another planet with different goods
        goodQuantities = new HashMap<>();
        goodQuantities.put(GoodType.GREEN, 2);
        goodQuantities.put(GoodType.YELLOW, 1);
        planets.add(new Planet("Planet 2", goodQuantities));
        
        // Get the card level from properties
        CardLevel level = CardLevel.TEST_FLIGHT;
        if (config.properties() != null && config.properties().containsKey("level")) {
            String levelStr = (String) config.properties().get("level");
            try {
                level = CardLevel.valueOf(levelStr);
            } catch (IllegalArgumentException e) {
                // Use default level if invalid
            }
        }
        
        // Create and return the PlanetsCard
        return new PlanetsCard(
            config.id(),
            level,
            config.description(),
            2, // Lost days when landing on a planet
            planets
        );
    }

    /**
     * Creates a meteor swarm card
     */
    private AdventureCard createMeteorSwarmCard(CardConfig config) {
        // Create a list of meteors
        List<Meteor> meteors = new ArrayList<>();
        
        // Add some meteors with different intensities and approach directions
        meteors.add(new Meteor(ShotIntensity.LIGHT, Direction.UP));
        meteors.add(new Meteor(ShotIntensity.HEAVY, Direction.DOWN));
        meteors.add(new Meteor(ShotIntensity.HEAVY, Direction.LEFT));
        
        // Get the card level from properties
        CardLevel level = CardLevel.TEST_FLIGHT;
        if (config.properties() != null && config.properties().containsKey("level")) {
            String levelStr = (String) config.properties().get("level");
            try {
                level = CardLevel.valueOf(levelStr);
            } catch (IllegalArgumentException e) {
                // Use default level if invalid
            }
        }
        
        // Create and return the MeteorSwarmCard
        return new MeteorSwarmCard(
            config.id(),
            level,
            config.description(),
            meteors
        );
    }

    /**
     * Creates a deck for the specified game level
     * 
     * @param gameLevel The game level to create a deck for
     * @return A new AdventureDeck with empty piles
     * @throws UnsupportedOperationException since this method is not yet implemented
     */
    public AdventureDeck createDeckForGameLevel(GameLevel gameLevel) {
        // This method is not yet implemented
        throw new UnsupportedOperationException("createDeckForGameLevel is not yet implemented");
    }

    /**
     * Creates a pile of cards for the specified game level
     */
    public List<AdventureCard> createPileForGameLevel(GameLevel gameLevel) {
        List<AdventureCard> pile = new ArrayList<>();
        int cardsPerPile = gameLevel.getCardPerPile();
        
        // Create cards for the pile based on the game level
        for (int i = 0; i < cardsPerPile; i++) {
            AdventureCard card = createRandomCard(gameLevel);
            pile.add(card);
        }
        
        return pile;
    }
    
    /**
     * Creates a random card for the specified game level
     */
    private AdventureCard createRandomCard(GameLevel gameLevel) {
        // Map game level to card level
        CardLevel cardLevel;
        switch (gameLevel) {
            case TEST_FLIGHT:
                cardLevel = CardLevel.TEST_FLIGHT;
                break;
            case LEVEL_II:
                cardLevel = CardLevel.LEVEL_II;
                break;
            default:
                cardLevel = CardLevel.TEST_FLIGHT;
                break;
        }
        
        // Generate a unique ID
        String id = "card_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        
        // Randomly choose a card type
        String[] cardTypes = {"PLANETS", "METEOR_SWARM"};
        String cardType = cardTypes[random.nextInt(cardTypes.length)];
        
        // Create a card config
        Map<String, Object> properties = new HashMap<>();
        properties.put("level", cardLevel.name());
        
        CardConfig config = new CardConfig(id, cardType, "Random " + cardType + " card", properties);
        
        // Create and return the card
        return createCard(config);
    }

    private void initializeCreators() {
        // Register creators for different card types
        registerCreator("PLANETS", this::createPlanetCard);
        registerCreator("METEOR_SWARM", this::createMeteorSwarmCard);
        // Add more card type creators as needed
    }
} 
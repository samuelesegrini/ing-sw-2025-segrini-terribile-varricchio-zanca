package it.polimi.ingsw.model.domain.general.loader;

import it.polimi.ingsw.model.domain.general.loader.AdventureCardCreator;
import it.polimi.ingsw.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.general.config.CardConfig;
import it.polimi.ingsw.model.enums.GameLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class responsible for creating different types of adventure cards
 */
public class AdventureCardFactory {
    private final Map<String, AdventureCardCreator> creators;

    public AdventureCardFactory() {
        this.creators = new HashMap<>();
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
    private AdventureCard createPlanetCard() {
        // Implementation will depend on your specific card types and requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Creates a meteor swarm card
     */
    private AdventureCard createMeteorSwarmCard() {
        // Implementation will depend on your specific card types and requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Creates a deck for the specified game level
     */
    public AdventureDeck createDeckForGameLevel(GameLevel gameLevel) {
        // Implementation will depend on your game level requirements
        // This should create appropriate piles of cards based on the game level
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Creates a pile of cards for the specified game level
     */
    public List<AdventureCard> createPileForGameLevel(GameLevel gameLevel) {
        // Implementation will depend on your game level requirements
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void initializeCreators() {
        // Register creators for different card types
        registerCreator("PLANET", config -> createPlanetCard());
        registerCreator("METEOR_SWARM", config -> createMeteorSwarmCard());
        // Add more card type creators as needed
    }
} 
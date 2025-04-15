package it.polimi.ingsw.model.domain.general.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.general.loader.AdventureCardFactory;
import it.polimi.ingsw.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.domain.general.ComponentDeck;
import it.polimi.ingsw.model.domain.general.loader.ComponentFactory;
import it.polimi.ingsw.model.enums.GameLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Manager class responsible for loading and parsing game configurations from JSON files
 */
public class GameConfigurationManager {
    private final ComponentFactory componentFactory;
    private final AdventureCardFactory cardFactory;
    private final ObjectMapper jsonMapper;
    private final List<Component> allComponents;
    private final Map<String, AdventureCard> cardRegistry;
    private final Map<GameLevel, GameConfig> levelConfigs;

    /**
     * Creates a new GameConfigurationManager with default factories
     */
    public GameConfigurationManager() {
        this.componentFactory = new ComponentFactory();
        this.cardFactory = new AdventureCardFactory();
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.allComponents = new ArrayList<>();
        this.cardRegistry = new HashMap<>();
        this.levelConfigs = new HashMap<>();
    }

    /**
     * Creates a new GameConfigurationManager with custom factories
     * 
     * @param componentFactory The factory for creating components
     * @param cardFactory The factory for creating adventure cards
     */
    public GameConfigurationManager(ComponentFactory componentFactory, AdventureCardFactory cardFactory) {
        this.componentFactory = componentFactory;
        this.cardFactory = cardFactory;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.allComponents = new ArrayList<>();
        this.cardRegistry = new HashMap<>();
        this.levelConfigs = new HashMap<>();
    }

    /**
     * Loads all configurations from the specified paths
     * 
     * @param componentsPath Path to the components configuration file
     * @param cardsPath Path to the cards configuration file
     * @param gameConfigPath Path to the game configuration file
     * @throws IOException If an I/O error occurs
     */
    public void loadAllConfigurations(String componentsPath, String cardsPath, String gameConfigPath) throws IOException {
        loadComponents(componentsPath);
        loadAdventureCards(cardsPath);
        loadGameConfigs(gameConfigPath);
    }

    /**
     * Loads components from a JSON file
     * 
     * @param jsonPath Path to the JSON file
     * @return List of loaded components
     * @throws IOException If an I/O error occurs
     */
    public List<Component> loadComponents(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        List<Component> components = new ArrayList<>();
        
        // Clear the allComponents list before loading new components
        allComponents.clear();
        
        for (JsonNode node : root) {
            ComponentConfig config = parseComponentConfig(node);
            Component component = componentFactory.createComponent(config);
            components.add(component);
            allComponents.add(component);
        }
        
        return components;
    }

    /**
     * Loads adventure cards from a JSON file
     * 
     * @param jsonPath Path to the JSON file
     * @return Map of card IDs to adventure cards
     * @throws IOException If an I/O error occurs
     */
    public Map<String, AdventureCard> loadAdventureCards(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        Map<String, AdventureCard> cards = new HashMap<>();
        
        for (JsonNode node : root) {
            CardConfig config = parseCardConfig(node);
            AdventureCard card = cardFactory.createCard(config);
            cards.put(card.getId(), card);
            cardRegistry.put(card.getId(), card);
        }
        
        return cards;
    }

    /**
     * Loads game configurations for different levels
     * 
     * @param jsonPath Path to the JSON file
     * @return Map of game levels to configurations
     * @throws IOException If an I/O error occurs
     */
    public Map<GameLevel, GameConfig> loadGameConfigs(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        
        for (JsonNode node : root) {
            GameConfig config = parseGameConfig(node);
            try {
                GameLevel level = GameLevel.valueOf(node.get("level").asText());
                levelConfigs.put(level, config);
            } catch (IllegalArgumentException e) {
                // Skip invalid level values
                System.err.println("Warning: Skipping game config with invalid level: " + node.get("level").asText());
            }
        }
        
        return levelConfigs;
    }

    /**
     * Reads a JSON file and returns its root node
     * 
     * @param jsonPath Path to the JSON file
     * @return Root JsonNode of the file
     * @throws IOException If an I/O error occurs
     */
    private JsonNode readJsonFile(String jsonPath) throws IOException {
        try (InputStream inputStream = new FileInputStream(new File(jsonPath))) {
            return jsonMapper.readTree(inputStream);
        }
    }

    /**
     * Parses a component configuration from a JSON node
     * 
     * @param jsonNode The JSON node to parse
     * @return ComponentConfig object
     */
    public ComponentConfig parseComponentConfig(JsonNode jsonNode) {
        return jsonMapper.convertValue(jsonNode, ComponentConfig.class);
    }

    /**
     * Parses a card configuration from a JSON node
     * 
     * @param jsonNode The JSON node to parse
     * @return CardConfig object
     */
    public CardConfig parseCardConfig(JsonNode jsonNode) {
        // Create a map for properties
        Map<String, Object> properties = new HashMap<>();
        
        // Add level to properties if it exists
        if (jsonNode.has("level")) {
            properties.put("level", jsonNode.get("level").asText());
        }
        
        // Create the CardConfig with the properties map
        return new CardConfig(
            jsonNode.get("id").asText(),
            jsonNode.get("type").asText(),
            jsonNode.get("description").asText(),
            properties
        );
    }

    /**
     * Parses a game configuration from a JSON node
     * 
     * @param jsonNode The JSON node to parse
     * @return GameConfig object
     */
    public GameConfig parseGameConfig(JsonNode jsonNode) {
        return jsonMapper.convertValue(jsonNode, GameConfig.class);
    }

    /**
     * Creates a component deck for the specified game level
     * 
     * @param level The game level
     * @return A new ComponentDeck
     */
    public ComponentDeck createComponentDeck(GameLevel level) {
        return new ComponentDeck(new ArrayList<>(allComponents));
    }

    /**
     * Creates an adventure deck for the specified game level
     * 
     * @param level The game level
     * @return A new AdventureDeck
     */
    public AdventureDeck createAdventureDeck(GameLevel level) {
        return cardFactory.createDeckForGameLevel(level);
    }

    /**
     * Gets the game configuration for the specified level
     * 
     * @param level The game level
     * @return The game configuration, or null if not found
     */
    public GameConfig getConfigForLevel(GameLevel level) {
        return levelConfigs.get(level);
    }

    /**
     * Gets all loaded components
     * 
     * @return List of all components
     */
    public List<Component> getAllComponents() {
        return new ArrayList<>(allComponents);
    }

    /**
     * Gets all loaded adventure cards
     * 
     * @return Map of card IDs to adventure cards
     */
    public Map<String, AdventureCard> getAllAdventureCards() {
        return new HashMap<>(cardRegistry);
    }

    /**
     * Gets all loaded game configurations
     * 
     * @return Map of game levels to configurations
     */
    public Map<GameLevel, GameConfig> getAllGameConfigs() {
        return new HashMap<>(levelConfigs);
    }
} 
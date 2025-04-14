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
import java.io.IOException;
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
     * Loads all configurations from the specified paths
     */
    public void loadAllConfigurations(String componentsPath, String cardsPath, String gameConfigPath) throws IOException {
        List<Component> components = loadComponents(componentsPath);
        loadAdventureCards(cardsPath);
        loadGameConfigs(gameConfigPath);
    }

    /**
     * Loads components from a JSON file
     */
    public List<Component> loadComponents(String jsonPath) throws IOException {
        JsonNode root = jsonMapper.readTree(new File(jsonPath));
        List<Component> components = new ArrayList<>();
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
     */
    public Map<String, AdventureCard> loadAdventureCards(String jsonPath) throws IOException {
        JsonNode root = jsonMapper.readTree(new File(jsonPath));
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
     */
    public Map<GameLevel, GameConfig> loadGameConfigs(String jsonPath) throws IOException {
        JsonNode root = jsonMapper.readTree(new File(jsonPath));
        for (JsonNode node : root) {
            GameConfig config = parseGameConfig(node);
            GameLevel level = GameLevel.valueOf(node.get("level").asText());
            levelConfigs.put(level, config);
        }
        return levelConfigs;
    }

    /**
     * Parses a component configuration from a JSON node
     */
    public ComponentConfig parseComponentConfig(JsonNode jsonNode) {
        return jsonMapper.convertValue(jsonNode, ComponentConfig.class);
    }

    /**
     * Parses a card configuration from a JSON node
     */
    public CardConfig parseCardConfig(JsonNode jsonNode) {
        return jsonMapper.convertValue(jsonNode, CardConfig.class);
    }

    /**
     * Parses a game configuration from a JSON node
     */
    public GameConfig parseGameConfig(JsonNode jsonNode) {
        return jsonMapper.convertValue(jsonNode, GameConfig.class);
    }

    /**
     * Creates a component deck for the specified game level
     */
    public ComponentDeck createComponentDeck(GameLevel level) {
        GameConfig config = levelConfigs.get(level);
        return new ComponentDeck(new ArrayList<>(allComponents));
    }

    /**
     * Creates an adventure deck for the specified game level
     */
    public AdventureDeck createAdventureDeck(GameLevel level) {
        return cardFactory.createDeckForGameLevel(level);
    }

    /**
     * Gets the game configuration for the specified level
     */
    public GameConfig getConfigForLevel(GameLevel level) {
        return levelConfigs.get(level);
    }
} 
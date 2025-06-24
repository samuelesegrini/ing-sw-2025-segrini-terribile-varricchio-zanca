package it.polimi.ingsw.server.model.domain.general.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.flight.RewardSystem;
import it.polimi.ingsw.server.model.domain.flight.Route;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.general.loader.AdventureCardFactory;
import it.polimi.ingsw.server.model.domain.general.loader.ComponentFactory;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.Direction;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedList; // Import LinkedList
import java.util.stream.Collectors;

public class GameConfigurationManager {
    private final ComponentFactory componentFactory;
    private final AdventureCardFactory cardFactory;
    private final ObjectMapper jsonMapper;
    private final List<Component> allComponents;
    private final Map<String, CardConfig> rawCardConfigs;
    private final Map<GameLevel, GameConfig> levelConfigs;

    public GameConfigurationManager() {
        this.componentFactory = new ComponentFactory();
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.jsonMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);


        this.allComponents = new ArrayList<>();
        this.rawCardConfigs = new HashMap<>();
        this.levelConfigs = new HashMap<>();
        this.cardFactory = new AdventureCardFactory(this.rawCardConfigs);
    }

    public GameConfigurationManager(ComponentFactory componentFactory, AdventureCardFactory customCardFactory) {
        this.componentFactory = componentFactory;
        this.cardFactory = customCardFactory;
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.jsonMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        this.allComponents = new ArrayList<>();
        this.rawCardConfigs = new HashMap<>();
        this.levelConfigs = new HashMap<>();
    }

    public void loadAllConfigurations(String componentsPath, String cardsPath, String gameConfigPath) throws IOException {
        System.out.println("Loading game configurations...");
        
        loadComponents(componentsPath);
        loadAdventureCardConfigs(cardsPath);
        loadGameConfigs(gameConfigPath);
        
        System.out.println("All configurations loaded successfully!");
    }

    public List<Component> loadComponents(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        allComponents.clear();
        List<Component> loadedComponents = new ArrayList<>();

        for (JsonNode node : root) {
            ComponentConfig config = parseComponentConfig(node);
            Component component = componentFactory.createComponent(config);
            if (component != null) {
                allComponents.add(component);
                loadedComponents.add(component);
            } else {
                System.err.println("Warning: Failed to create component for config ID: " + (config != null ? config.id() : "null config"));
            }
        }
        return loadedComponents;
    }

    public Map<String, CardConfig> loadAdventureCardConfigs(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        rawCardConfigs.clear();

        for (JsonNode node : root) {
            CardConfig config = parseCardConfig(node);
            rawCardConfigs.put(config.id(), config);
        }
        return new HashMap<>(rawCardConfigs);
    }

    public Map<GameLevel, GameConfig> loadGameConfigs(String jsonPath) throws IOException {
        JsonNode root = readJsonFile(jsonPath);
        JsonNode levelsArrayNode = root.get("levels");
        levelConfigs.clear();

        if (levelsArrayNode != null && levelsArrayNode.isArray()) {
            for (JsonNode levelNode : levelsArrayNode) {
                try {
                    GameConfig config = parseGameConfig(levelNode);
                    levelConfigs.put(config.levelEnum(), config);
                    System.out.println("Loaded configuration for: " + config.levelEnum());
                } catch (Exception e) {
                    String levelStr = levelNode.has("level") ? levelNode.get("level").asText() : "LEVEL_FIELD_MISSING";
                    System.err.println("Warning: Skipping game config for level '" + levelStr + "' due to parsing error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Warning: 'levels' array not found or not an array in " + jsonPath);
        }        
        System.out.println("Successfully loaded " + levelConfigs.size() + " game configurations: " + levelConfigs.keySet());

        return new HashMap<>(levelConfigs);
    }

    private JsonNode readJsonFile(String jsonPath) throws IOException {
        // Try classpath resource first
        InputStream inputStream = getClass().getResourceAsStream(jsonPath);
        if (inputStream != null) {
            try (InputStream is = inputStream) {
                return jsonMapper.readTree(is);
            }
        }
        
        // Fallback to file system path
        File file = new File(jsonPath);
        if (!file.exists()) {
            throw new IOException("Resource not found in classpath or file system: " + jsonPath);
        }
        
        try (InputStream fileStream = new FileInputStream(file)) {
            return jsonMapper.readTree(fileStream);
        }
    }

    public ComponentConfig parseComponentConfig(JsonNode jsonNode) {
        String id = jsonNode.has("image") ? jsonNode.get("image").asText() : "unknown_component_" + System.nanoTime();
        String type = jsonNode.get("type").asText();

        List<String> connectorList = new ArrayList<>();
        Map<String, Object> properties = new HashMap<>();

        if (jsonNode.has("connectors")) {
            JsonNode connectorsNode = jsonNode.get("connectors");
            if (connectorsNode.isObject()) {
                // Handle object format: {"UP": "SINGLE", "RIGHT": "PLAIN", ...}
                Iterator<Map.Entry<String, JsonNode>> cFields = connectorsNode.fields();
                while (cFields.hasNext()) {
                    Map.Entry<String, JsonNode> cField = cFields.next();
                    connectorList.add(cField.getKey().toUpperCase() + ":" + cField.getValue().asText().toUpperCase());
                }
            } else if (connectorsNode.isArray()) {
                // Handle array format: ["PLAIN", "PLAIN", "PLAIN", "SINGLE"] for [UP, RIGHT, DOWN, LEFT]
                Direction[] directions = {Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT};
                for (int i = 0; i < Math.min(connectorsNode.size(), directions.length); i++) {
                    String connectorType = connectorsNode.get(i).asText().toUpperCase();
                    connectorList.add(directions[i].name() + ":" + connectorType);
                }
            } else {
                System.err.println("Warning: Unknown connectors format for component " + id);
            }
        }

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();

            if (key.endsWith(" connector") && !key.equals("connectors")) {
                String direction = key.replace(" connector", "").trim().toUpperCase();
                try {
                    Direction.valueOf(direction);
                    connectorList.add(direction + ":" + valueNode.asText().toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid connector direction '" + direction + "' in component " + id);
                }
            } else if (!key.equals("image") && !key.equals("type") && !key.equals("connectors")) {
                if (valueNode.isInt()) properties.put(key, valueNode.asInt());
                else if (valueNode.isLong()) properties.put(key, valueNode.asLong());
                else if (valueNode.isDouble()) properties.put(key, valueNode.asDouble());
                else if (valueNode.isBoolean()) properties.put(key, valueNode.asBoolean());
                else if (valueNode.isTextual()) properties.put(key, valueNode.asText());
                else {
                    try {
                        properties.put(key, jsonMapper.convertValue(valueNode, Object.class));
                    } catch (Exception e) {
                        System.err.println("Warning: Could not convert complex property " + key + " for component " + id + ". Storing as text. Error: " + e.getMessage());
                        properties.put(key, valueNode.asText());
                    }
                }
            }
        }
        return new ComponentConfig(id, type, connectorList, properties);
    }

    public CardConfig parseCardConfig(JsonNode jsonNode) {
        String id = jsonNode.get("id").asText();
        String type = jsonNode.get("type").asText();
        String description = jsonNode.get("description").asText();

        Map<String, Object> properties = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (!key.equals("id") && !key.equals("type") && !key.equals("description")) {
                JsonNode valueNode = field.getValue();
                Object value;
                if (valueNode.isBoolean()) value = valueNode.asBoolean();
                else if (valueNode.isInt()) value = valueNode.asInt();
                else if (valueNode.isLong()) value = valueNode.asLong();
                else if (valueNode.isDouble()) value = valueNode.asDouble();
                else if (valueNode.isTextual()) value = valueNode.asText();
                else {
                    value = jsonMapper.convertValue(valueNode, Object.class);
                }
                properties.put(key, value);
            }
        }
        return new CardConfig(id, type, description, properties);
    }

    public GameConfig parseGameConfig(JsonNode levelNode) {
        GameLevel levelEnum = GameLevel.valueOf(levelNode.get("level").asText().toUpperCase());

        JsonNode fbNode = levelNode.get("flightBoardConfig");
        Map<String, Integer> posBonus = jsonMapper.convertValue(fbNode.get("rewardSystem").get("positionBonus"), new TypeReference<Map<String, Integer>>() {});
        Map<String, Integer> resBonus = jsonMapper.convertValue(fbNode.get("rewardSystem").get("resourceBonus"), new TypeReference<Map<String, Integer>>() {});
        RewardSystemConfig rsConfig = new RewardSystemConfig(
                posBonus, resBonus,
                fbNode.get("rewardSystem").get("bestLookingShipBonus").asInt(),
                fbNode.get("rewardSystem").get("exposedConnectorsPenalty").asInt()
        );
        FlightBoardConfig flightBoardConfig = new FlightBoardConfig(
                fbNode.get("image").asText(),
                fbNode.get("length").asText(),
                jsonMapper.convertValue(fbNode.get("startingPositions"), new TypeReference<List<Integer>>() {}),
                rsConfig
        );

        JsonNode sgNode = levelNode.get("shipGridConfig");
        ShipGridConfig shipGridConfig = new ShipGridConfig(
                sgNode.get("image").asText(),
                sgNode.get("rows").asInt(),
                sgNode.get("cols").asInt(),
                jsonMapper.convertValue(sgNode.get("reservedComponentsPositions"), new TypeReference<List<PositionConfig>>() {}),
                jsonMapper.convertValue(sgNode.get("forbiddenPositions"), new TypeReference<List<PositionConfig>>() {})
        );

        return new GameConfig(levelEnum, flightBoardConfig, shipGridConfig);
    }

    public ComponentDeck createComponentDeck(GameLevel level) {
        return new ComponentDeck(new ArrayList<>(allComponents));
    }

    // Helper method within GCM to draw cards for pile formation, respecting L1 including TF
    // This method modifies the `availableCardsMap` by removing drawn cards.
    private AdventureCard drawCardForPileFormation(CardLevel definedNeededLevel, Map<CardLevel, LinkedList<AdventureCard>> availableCardsMap) {
        if (definedNeededLevel == CardLevel.LEVEL_I) {
            // Try to draw a LEVEL_I card first
            LinkedList<AdventureCard> l1s = availableCardsMap.get(CardLevel.LEVEL_I);
            if (l1s != null && !l1s.isEmpty()) {
                return l1s.removeFirst();
            }
            // If no LEVEL_I cards, try to draw a TEST_FLIGHT card (as per rule "L1 including TF")
            LinkedList<AdventureCard> tfs = availableCardsMap.get(CardLevel.TEST_FLIGHT);
            if (tfs != null && !tfs.isEmpty()) {
                return tfs.removeFirst();
            }
        } else { // For any other specific level (e.g., LEVEL_II, TEST_FLIGHT if explicitly requested)
            LinkedList<AdventureCard> specificLevelCards = availableCardsMap.get(definedNeededLevel);
            if (specificLevelCards != null && !specificLevelCards.isEmpty()) {
                return specificLevelCards.removeFirst();
            }
        }
        return null; // No card of the required type(s) available
    }


    public AdventureDeck createAdventureDeck(GameLevel gameLevel) {
        System.out.println("[GCM.createAdventureDeck] Entered for GameLevel: " + gameLevel);
        Map<CardLevel, List<AdventureCard>> allCardsByCardLevelSource = cardFactory.getAllCardsByLevel();

        // Use LinkedLists for efficient removal from source availableCards
        Map<CardLevel, LinkedList<AdventureCard>> availableCards = new HashMap<>();
        allCardsByCardLevelSource.forEach((level, list) -> {
            LinkedList<AdventureCard> linkedList = new LinkedList<>(list);
            Collections.shuffle(linkedList); // Shuffle each source list once
            availableCards.put(level, linkedList);
        });

        System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Initial TF cards available: " + availableCards.getOrDefault(CardLevel.TEST_FLIGHT, new LinkedList<>()).size());
        System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Initial L1 cards available: " + availableCards.getOrDefault(CardLevel.LEVEL_I, new LinkedList<>()).size());
        System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Initial L2 cards available: " + availableCards.getOrDefault(CardLevel.LEVEL_II, new LinkedList<>()).size());


        List<List<AdventureCard>> uncoveredPilesForDeck = new ArrayList<>();
        LinkedList<AdventureCard> unknownPileExplicitCards = new LinkedList<>();
        LinkedList<AdventureCard> remainingCardsPool = new LinkedList<>(); // For all other cards not in specific piles

        if (gameLevel == GameLevel.TEST_FLIGHT) {
            LinkedList<AdventureCard> tfDeck = availableCards.getOrDefault(CardLevel.TEST_FLIGHT, new LinkedList<>());
            remainingCardsPool.addAll(tfDeck); // All TF cards go into the main pool for TF level
            tfDeck.clear(); // Mark as "used" from available
            System.out.println("[GCM.createAdventureDeck for TF] Size of remainingCardsPool (all TF cards): " + remainingCardsPool.size());
        } else { // For LEVEL_II (and potentially LEVEL_III)
            // Populate the predictable (uncovered) piles
            Map<CardLevel, Integer> predictablePileComp = gameLevel.getCardsPerPredictablePileComposition();
            System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Forming " + gameLevel.getPredictablePileCount() + " predictable piles with composition: " + predictablePileComp);
            for (int i = 0; i < gameLevel.getPredictablePileCount(); i++) {
                List<AdventureCard> currentPredictablePile = new ArrayList<>();
                for (Map.Entry<CardLevel, Integer> entry : predictablePileComp.entrySet()) {
                    CardLevel definedNeededLevel = entry.getKey();
                    int countForThisLevelInPile = entry.getValue();
                    for (int k = 0; k < countForThisLevelInPile; k++) {
                        AdventureCard cardToAdd = drawCardForPileFormation(definedNeededLevel, availableCards);
                        if (cardToAdd != null) {
                            currentPredictablePile.add(cardToAdd);
                        } else {
                            System.err.println("Warning: [GCM] Not enough cards to fulfill composition for " + definedNeededLevel + " in predictable pile " + (i+1));
                        }
                    }
                }
                uncoveredPilesForDeck.add(currentPredictablePile);
                System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Formed predictable pile " + (i+1) + " with " + currentPredictablePile.size() + " cards.");
            }

            // Populate the "unknown" top pile
            Map<CardLevel, Integer> unknownPileComp = gameLevel.getCardsForUnknownPileComposition();
            System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Forming unknown pile with composition: " + unknownPileComp);
            for (Map.Entry<CardLevel, Integer> entry : unknownPileComp.entrySet()) {
                CardLevel definedNeededLevel = entry.getKey();
                int countForThisLevelInPile = entry.getValue();
                for (int k = 0; k < countForThisLevelInPile; k++) {
                    AdventureCard cardToAdd = drawCardForPileFormation(definedNeededLevel, availableCards);
                    if (cardToAdd != null) {
                        unknownPileExplicitCards.add(cardToAdd);
                    } else {
                        System.err.println("Warning: [GCM] Not enough cards to fulfill composition for " + definedNeededLevel + " in unknown pile.");
                    }
                }
            }
            System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Formed unknown pile with " + unknownPileExplicitCards.size() + " cards.");

            // Collect ALL REMAINING cards of relevant levels (TF, L1, L2 for a LEVEL_II game)
            // that were not used in the specific piles above.
            System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Collecting remaining cards...");
            System.out.println("  Remaining TF: " + availableCards.getOrDefault(CardLevel.TEST_FLIGHT, new LinkedList<>()).size());
            System.out.println("  Remaining L1: " + availableCards.getOrDefault(CardLevel.LEVEL_I, new LinkedList<>()).size());
            System.out.println("  Remaining L2: " + availableCards.getOrDefault(CardLevel.LEVEL_II, new LinkedList<>()).size());

            remainingCardsPool.addAll(availableCards.getOrDefault(CardLevel.TEST_FLIGHT, new LinkedList<>()));
            remainingCardsPool.addAll(availableCards.getOrDefault(CardLevel.LEVEL_I, new LinkedList<>()));
            remainingCardsPool.addAll(availableCards.getOrDefault(CardLevel.LEVEL_II, new LinkedList<>()));
            // Clear from availableCards as they are now accounted for
            availableCards.getOrDefault(CardLevel.TEST_FLIGHT, new LinkedList<>()).clear();
            availableCards.getOrDefault(CardLevel.LEVEL_I, new LinkedList<>()).clear();
            availableCards.getOrDefault(CardLevel.LEVEL_II, new LinkedList<>()).clear();
            System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Size of remainingCardsPool collected: " + remainingCardsPool.size());
        }

        // Prepare the 'initialCoveredOrUnknownPile' for the AdventureDeck constructor
        LinkedList<AdventureCard> initialCoveredForDeckConstructor = new LinkedList<>();
        if (gameLevel == GameLevel.TEST_FLIGHT) {
            initialCoveredForDeckConstructor.addAll(remainingCardsPool); // This contains all TF cards
        } else {
            initialCoveredForDeckConstructor.addAll(unknownPileExplicitCards); // The specifically formed unknown pile
            initialCoveredForDeckConstructor.addAll(remainingCardsPool); // Plus all other cards not used in specific piles
        }
        System.out.println("[GCM.createAdventureDeck " + gameLevel + "] Size of initialCoveredForDeckConstructor being passed to AdventureDeck: " + initialCoveredForDeckConstructor.size());

        return new AdventureDeck(gameLevel, uncoveredPilesForDeck, initialCoveredForDeckConstructor);
    }


    public GameConfig getConfigForLevel(GameLevel level) {
        GameConfig cfg = levelConfigs.get(level);
        if (cfg == null) {
            System.err.println("FATAL: No game configuration found for level: " + level);
            System.err.println("Available levels in configuration: " + levelConfigs.keySet());
            throw new IllegalStateException("Configuration for game level " + level + " not found.");
        }
        return cfg;
    }

    public Route getRouteForLevel(GameLevel level) {
        GameConfig gameCfg = getConfigForLevel(level);
        FlightBoardConfig fbCfg = gameCfg.flightBoardConfig();
        RewardSystemConfig rsCfg = fbCfg.rewardSystem();

        Map<PlayerOrder, Integer> positionBonusMap = rsCfg.positionBonus().entrySet().stream()
                .collect(Collectors.toMap(e -> PlayerOrder.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));
        Map<GoodType, Integer> resourceBonusMap = rsCfg.resourceBonus().entrySet().stream()
                .collect(Collectors.toMap(e -> GoodType.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));

        System.out.println("[GCM.getRouteForLevel] Creating RewardSystem for " + level + ". PositionBonusMap: " + positionBonusMap);


        RewardSystem rewardSystem = new RewardSystem(level, positionBonusMap, resourceBonusMap,
                rsCfg.bestLookingShipBonus(), rsCfg.exposedConnectorsPenalty());

        return new Route(level, Integer.parseInt(fbCfg.length()), new ArrayList<>(fbCfg.startingPositions()), rewardSystem);
    }

    public List<Component> getAllComponents() {
        return new ArrayList<>(allComponents);
    }

    public Map<String, CardConfig> getRawCardConfigs() {
        return new HashMap<>(rawCardConfigs);
    }

    public Map<GameLevel, GameConfig> getAllGameConfigs() {
        return new HashMap<>(levelConfigs);
    }
}
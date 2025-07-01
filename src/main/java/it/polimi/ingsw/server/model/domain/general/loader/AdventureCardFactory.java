package it.polimi.ingsw.server.model.domain.general.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.general.config.CardConfig;
import it.polimi.ingsw.server.model.enums.adventure.*;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

public class AdventureCardFactory {
    private final Map<String, AdventureCardCreator> creators;
    private final Map<String, CardConfig> rawCardConfigs; // Stores all raw CardConfig objects loaded
    private final ObjectMapper objectMapper; // For converting complex properties

    public AdventureCardFactory(Map<String, CardConfig> rawCardConfigs) {
        this.creators = new HashMap<>();
        this.rawCardConfigs = rawCardConfigs; // Should be populated by GameConfigurationManager
        this.objectMapper = new ObjectMapper();
        // Configure objectMapper to be tolerant of unknown properties if necessary for nested objects
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initializeCreators();
    }

    /**
     * Registers a creator for a specific card type (adventure type).
     * The type string is converted to uppercase to ensure consistent map keys.
     *
     * @param type    The string representation of the adventure type (e.g., "ABANDONED_SHIP").
     * @param creator The functional interface implementing the card creation logic.
     */
    public void registerCreator(String type, AdventureCardCreator creator) {
        creators.put(type.toUpperCase(), creator);
    }

    /**
     * Creates an AdventureCard instance from its CardConfig.
     * Uses the registered creator for the card's type.
     *
     * @param config The CardConfig object containing the card's data.
     * @return The created AdventureCard, or null if no creator is found or an error occurs.
     */
    public AdventureCard createCard(CardConfig config) {
        if (config == null || config.type() == null) {
            System.err.println("Error: CardConfig or its type is null.");
            return null;
        }
        AdventureCardCreator creator = creators.get(config.type().toUpperCase());
        if (creator == null) {
            System.err.println("Warning: No creator registered for card type: " + config.type() + " (ID: " + config.id() + ").");
            return null;
        }
        try {
            return creator.create(config);
        } catch (Exception e) {
            System.err.println("Error creating card ID " + config.id() + " of type " + config.type() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prepares lists of all available AdventureCards, categorized by their CardLevel.
     * This method iterates through all raw configurations, creates the cards, and sorts them.
     *
     * @return A map where keys are CardLevels and values are lists of corresponding AdventureCards.
     */
    public Map<CardLevel, List<AdventureCard>> getAllCardsByLevel() {
        Map<CardLevel, List<AdventureCard>> cardsByLevel = new HashMap<>();
        for (CardLevel cl : CardLevel.values()) {
            cardsByLevel.put(cl, new ArrayList<>());
        }

        if (rawCardConfigs == null || rawCardConfigs.isEmpty()) {
            System.err.println("Error: Raw card configurations are empty or not loaded in AdventureCardFactory.");
            return cardsByLevel; // Return empty categorized lists
        }

        for (CardConfig config : rawCardConfigs.values()) {
            AdventureCard card = createCard(config); // createCard handles null config.properties().get("level")
            if (card != null) {
                cardsByLevel.get(card.getLevel()).add(card);
            }
        }
        return cardsByLevel;
    }

    private void initializeCreators() {
        // Register creators using AdventureType enum for consistency if possible,
        // otherwise, use the exact strings from JSON `type` field.
        registerCreator(AdventureType.ABANDONED_SHIP.toString(), this::createAbandonedShipCard);
        registerCreator(AdventureType.ABANDONED_STATION.toString(), this::createAbandonedStationCard);
        registerCreator(AdventureType.COMBAT_ZONE.toString(), this::createCombatZoneCard); // JSON type is "WAR_ZONE"
        registerCreator(AdventureType.EPIDEMIC.toString(), this::createEpidemicCard);
        registerCreator(AdventureType.METEOR_SWARM.toString(), this::createMeteorSwarmCard);
        registerCreator(AdventureType.OPEN_SPACE.toString(), this::createOpenSpaceCard);
        registerCreator(AdventureType.PIRATES.toString(), this::createPiratesCard);
        registerCreator(AdventureType.PLANETS.toString(), this::createPlanetsCard);
        registerCreator(AdventureType.SLAVERS.toString(), this::createSlaversCard);
        registerCreator(AdventureType.SMUGGLERS.toString(), this::createSmugglersCard);
        registerCreator(AdventureType.STARDUST.toString(), this::createStardustCard);
    }

    // Helper to safely get CardLevel from properties
    private CardLevel getCardLevelFromConfig(CardConfig config, String cardIdForLogging) {
        Object levelObj = config.properties().get("level");
        if (levelObj == null) {
            System.err.println("Warning: Card " + cardIdForLogging + " is missing 'level' property. Defaulting to TEST_FLIGHT.");
            return CardLevel.TEST_FLIGHT; // Default or throw
        }
        String levelStr = levelObj.toString();
        try {
            return CardLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Card " + cardIdForLogging + " has invalid level string: '" + levelStr + "'. Defaulting to TEST_FLIGHT.");
            return CardLevel.TEST_FLIGHT; // Default or throw
        }
    }

    // --- Creator Methods for each specific card type ---

    private AbandonedShipCard createAbandonedShipCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        return new AbandonedShipCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("crewLost")).intValue(),
                ((Number) props.get("creditsGained")).intValue(),
                ((Number) props.get("lostDays")).intValue()
        );
    }

    private AbandonedStationCard createAbandonedStationCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        Map<String, Integer> rawGoods = objectMapper.convertValue(props.get("goodQuantities"), new TypeReference<Map<String, Integer>>() {});
        Map<GoodType, Integer> goodQuantities = rawGoods.entrySet().stream()
                .collect(Collectors.toMap(e -> GoodType.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));
        return new AbandonedStationCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("minCrewRequired")).intValue(),
                ((Number) props.get("lostDays")).intValue(),
                goodQuantities
        );
    }

    private CombatZoneCard createCombatZoneCard(CardConfig config) {
        CombatZoneCard card = new CombatZoneCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description()
        );
        List<Map<String, Object>> rawChecks = objectMapper.convertValue(
                config.properties().get("combatChecks"),
                new TypeReference<List<Map<String, Object>>>() {}
        );

        for (Map<String, Object> rawCheck : rawChecks) {
            CombatAttributeType attribute = CombatAttributeType.valueOf(((String) rawCheck.get("attribute")).toUpperCase());
            PenaltyType penaltyType = PenaltyType.valueOf(((String) rawCheck.get("penaltyType")).toUpperCase());
            CombatCheck combatCheck;

            if (penaltyType == PenaltyType.CANNON_FIRE) {
                List<Map<String, String>> rawFires = objectMapper.convertValue(
                        rawCheck.get("cannonFires"),
                        new TypeReference<List<Map<String, String>>>() {}
                );
                List<CannonFire> cannonFires = rawFires.stream().map(rf -> new CannonFire(
                        Direction.valueOf(rf.get("approach").toUpperCase()),
                        ShotIntensity.valueOf(rf.get("intensity").toUpperCase())
                )).collect(Collectors.toList());
                combatCheck = new CombatCheck(attribute, cannonFires);
            } else {
                int penaltyValue = ((Number) rawCheck.get("penaltyValue")).intValue();
                combatCheck = new CombatCheck(attribute, penaltyType, penaltyValue);
            }
            card.addCombatCheck(combatCheck);
        }
        return card;
    }

    private EpidemicCard createEpidemicCard(CardConfig config) {
        return new EpidemicCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description()
        );
    }

    private MeteorSwarmCard createMeteorSwarmCard(CardConfig config) {
        List<Map<String, String>> rawPattern = objectMapper.convertValue(
                config.properties().get("meteorPattern"),
                new TypeReference<List<Map<String, String>>>() {}
        );
        List<Meteor> meteorPattern = rawPattern.stream().map(m -> new Meteor(
                ShotIntensity.valueOf(m.get("intensity").toUpperCase()),
                Direction.valueOf(m.get("approach").toUpperCase())
        )).collect(Collectors.toList());
        return new MeteorSwarmCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                meteorPattern
        );
    }

    private OpenSpaceCard createOpenSpaceCard(CardConfig config) {
        return new OpenSpaceCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description()
        );
    }

    private PiratesCard createPiratesCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        List<Map<String, String>> rawAttackPattern = objectMapper.convertValue(
                props.get("attackPattern"),
                new TypeReference<List<Map<String, String>>>() {}
        );
        List<CannonFire> attackPattern = rawAttackPattern.stream().map(ap -> new CannonFire(
                Direction.valueOf(ap.get("approach").toUpperCase()),
                ShotIntensity.valueOf(ap.get("intensity").toUpperCase())
        )).collect(Collectors.toList());
        return new PiratesCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("powerLevel")).intValue(),
                ((Number) props.get("movementPenalty")).intValue(),
                ((Number) props.get("creditReward")).intValue(),
                attackPattern
        );
    }

    private PlanetsCard createPlanetsCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        List<Map<String, Object>> rawPlanets = objectMapper.convertValue(
                props.get("planets"),
                new TypeReference<List<Map<String, Object>>>() {}
        );
        List<Planet> planets = rawPlanets.stream().map(rp -> {
            Map<String, Integer> rawGoods = objectMapper.convertValue(
                    rp.get("goodQuantities"),
                    new TypeReference<Map<String, Integer>>() {}
            );
            Map<GoodType, Integer> goodQuantities = rawGoods.entrySet().stream()
                    .collect(Collectors.toMap(e -> GoodType.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));
            return new Planet( // Assuming Planet constructor takes (int number, Map<GoodType, Integer> goodQuantities)
                    ((Number) rp.get("number")).intValue(),
                    goodQuantities
            );
        }).collect(Collectors.toList());
        return new PlanetsCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("lostDays")).intValue(),
                planets
        );
    }

    private SlaversCard createSlaversCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        return new SlaversCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("powerLevel")).intValue(),
                ((Number) props.get("movementPenalty")).intValue(),
                ((Number) props.get("creditReward")).intValue(),
                ((Number) props.get("crewLossAmount")).intValue()
        );
    }

    private SmugglersCard createSmugglersCard(CardConfig config) {
        Map<String, Object> props = config.properties();
        Map<String, Integer> rawGoods = objectMapper.convertValue(
                props.get("availableGoods"),
                new TypeReference<Map<String, Integer>>() {}
        );
        Map<GoodType, Integer> availableGoods = rawGoods.entrySet().stream()
                .collect(Collectors.toMap(e -> GoodType.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));
        return new SmugglersCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description(),
                ((Number) props.get("powerLevel")).intValue(),
                ((Number) props.get("movementPenalty")).intValue(),
                ((Number) props.get("goodsLostIfDefeated")).intValue(),
                availableGoods
        );
    }

    private StardustCard createStardustCard(CardConfig config) {
        return new StardustCard(
                config.id(),
                getCardLevelFromConfig(config, config.id()),
                config.description()
        );
    }
}
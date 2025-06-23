package it.polimi.ingsw.server.model.domain.general.loader;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.domain.general.config.CardConfig;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.PenaltyType;
import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AdventureCardFactoryTest {

    private Map<String, CardConfig> rawCardConfigs;
    private AdventureCardFactory factory;

    @BeforeEach
    void setUp() {
        rawCardConfigs = new HashMap<>();
        // It's important that the factory is initialized with the map it will use
        factory = new AdventureCardFactory(rawCardConfigs);
    }

    private CardConfig createSimpleCardConfig(String id, String type, CardLevel level) {
        Map<String, Object> props = new HashMap<>();
        props.put("level", level.name()); // Crucial for categorization
        // Add minimal required properties for the type if its creator expects them
        // For a generic test, just the level might be enough to test categorization.
        // For specific card type tests, more props are needed.
        return new CardConfig(id, type, "Description for " + id, props);
    }

    @Test
    void testCreateCard_UnknownType() {
        CardConfig config = createSimpleCardConfig("unknown-01", "NON_EXISTENT_TYPE", CardLevel.TEST_FLIGHT);
        rawCardConfigs.put(config.id(), config); // Add to map factory uses

        AdventureCard card = factory.createCard(config);
        assertNull(card, "Should return null or throw for an unknown card type if no default creator.");
        // Current factory prints an error and returns null for unknown types.
    }

    @Test
    void testCreateCard_NullConfigOrType() {
        assertNull(factory.createCard(null), "Should handle null config gracefully.");
        CardConfig configNoType = new CardConfig("id1", null, "desc", new HashMap<>());
        assertNull(factory.createCard(configNoType), "Should handle null type in config gracefully.");
    }

    @Test
    void testGetAllCardsByLevel_EmptyConfigs() {
        Map<CardLevel, List<AdventureCard>> categorizedCards = factory.getAllCardsByLevel();
        assertNotNull(categorizedCards);
        for (CardLevel cl : CardLevel.values()) {
            assertTrue(categorizedCards.containsKey(cl), "Should contain an entry for each CardLevel.");
            assertTrue(categorizedCards.get(cl).isEmpty(), "Lists should be empty if no configs.");
        }
    }

    @Test
    void testGetAllCardsByLevel_PopulatesCorrectly() {
        // Add some configs to the rawCardConfigs map
        CardConfig tfCard1 = createSimpleCardConfig("tf-01", AdventureType.OPEN_SPACE.toString(), CardLevel.TEST_FLIGHT);
        CardConfig l1Card1 = createSimpleCardConfig("l1-01", AdventureType.PLANETS.toString(), CardLevel.LEVEL_I);
        CardConfig l1Card2 = createSimpleCardConfig("l1-02", AdventureType.ABANDONED_SHIP.toString(), CardLevel.LEVEL_I);
        CardConfig l2Card1 = createSimpleCardConfig("l2-01", AdventureType.PIRATES.toString(), CardLevel.LEVEL_II);

        // Minimal properties for specific types to avoid NPEs in their creators
        // For OPEN_SPACE (simple, no extra mandatory props in JSON for basic card)
        // For PLANETS
        Map<String, Object> planetsProps = new HashMap<>(l1Card1.properties());
        planetsProps.put("lostDays", 1);
        planetsProps.put("planets", new ArrayList<>()); // Empty list of planets for test
        l1Card1 = new CardConfig(l1Card1.id(), l1Card1.type(), l1Card1.description(), planetsProps);

        // For ABANDONED_SHIP
        Map<String, Object> asProps = new HashMap<>(l1Card2.properties());
        asProps.put("crewLost", 1);
        asProps.put("creditsGained", 1);
        asProps.put("lostDays", 1);
        l1Card2 = new CardConfig(l1Card2.id(), l1Card2.type(), l1Card2.description(), asProps);

        // For PIRATES
        Map<String, Object> piratesProps = new HashMap<>(l2Card1.properties());
        piratesProps.put("powerLevel", 1);
        piratesProps.put("movementPenalty", 1);
        piratesProps.put("creditReward", 1);
        piratesProps.put("attackPattern", new ArrayList<>()); // Empty attack pattern
        l2Card1 = new CardConfig(l2Card1.id(), l2Card1.type(), l2Card1.description(), piratesProps);


        rawCardConfigs.put(tfCard1.id(), tfCard1);
        rawCardConfigs.put(l1Card1.id(), l1Card1);
        rawCardConfigs.put(l1Card2.id(), l1Card2);
        rawCardConfigs.put(l2Card1.id(), l2Card1);

        Map<CardLevel, List<AdventureCard>> categorizedCards = factory.getAllCardsByLevel();

        assertEquals(1, categorizedCards.get(CardLevel.TEST_FLIGHT).size());
        assertTrue(categorizedCards.get(CardLevel.TEST_FLIGHT).stream().anyMatch(c -> c.getId().equals("tf-01")));

        assertEquals(2, categorizedCards.get(CardLevel.LEVEL_I).size());
        assertTrue(categorizedCards.get(CardLevel.LEVEL_I).stream().anyMatch(c -> c.getId().equals("l1-01")));
        assertTrue(categorizedCards.get(CardLevel.LEVEL_I).stream().anyMatch(c -> c.getId().equals("l1-02")));

        assertEquals(1, categorizedCards.get(CardLevel.LEVEL_II).size());
        assertTrue(categorizedCards.get(CardLevel.LEVEL_II).stream().anyMatch(c -> c.getId().equals("l2-01")));
    }

    @Test
    void testGetAllCardsByLevel_HandlesMissingLevelPropertyGracefully() {
        // Card config without "level" in properties
        Map<String, Object> propsNoLevel = new HashMap<>();
        // No props.put("level", ...);
        CardConfig noLevelCard = new CardConfig("no-level-01", AdventureType.OPEN_SPACE.toString(), "Desc", propsNoLevel);
        rawCardConfigs.put(noLevelCard.id(), noLevelCard);

        // Card config with invalid "level" string
        Map<String, Object> propsInvalidLevel = new HashMap<>();
        propsInvalidLevel.put("level", "LEVEL_XYZ");
        CardConfig invalidLevelCard = new CardConfig("invalid-level-01", AdventureType.OPEN_SPACE.toString(), "Desc", propsInvalidLevel);
        rawCardConfigs.put(invalidLevelCard.id(), invalidLevelCard);

        // Add one valid card to ensure the map is processed
        CardConfig validCard = createSimpleCardConfig("valid-01", AdventureType.OPEN_SPACE.toString(), CardLevel.TEST_FLIGHT);
        rawCardConfigs.put(validCard.id(), validCard);


        Map<CardLevel, List<AdventureCard>> categorizedCards = factory.getAllCardsByLevel();

        // The factory's createCard -> getCardLevelFromConfig defaults to TEST_FLIGHT for missing/invalid levels
        // So these cards might end up in TEST_FLIGHT or be skipped depending on strictness.
        // Current getCardLevelFromConfig defaults to TEST_FLIGHT and prints a warning.
        // So, we expect 3 cards in TEST_FLIGHT list if they were created successfully despite level issues.

        // If createCard returns null for these due to strict error handling in getCardLevelFromConfig (if it were to throw)
        // then only 'valid-01' would be present.
        // Given current `getCardLevelFromConfig`, it defaults to TEST_FLIGHT.
        // And `createCard` will use this default.

        assertEquals(3, categorizedCards.get(CardLevel.TEST_FLIGHT).size(),
                "Cards with missing/invalid level should default to TEST_FLIGHT and be created, or createCard should return null and they are skipped.");

        boolean noLevelCardFound = false;
        boolean invalidLevelCardFound = false;
        boolean validCardFound = false;

        for(AdventureCard card : categorizedCards.get(CardLevel.TEST_FLIGHT)) {
            if(card.getId().equals("no-level-01")) noLevelCardFound = true;
            if(card.getId().equals("invalid-level-01")) invalidLevelCardFound = true;
            if(card.getId().equals("valid-01")) validCardFound = true;
        }
        assertTrue(noLevelCardFound, "Card with no level property should have defaulted and been created.");
        assertTrue(invalidLevelCardFound, "Card with invalid level string should have defaulted and been created.");
        assertTrue(validCardFound, "Valid card should be present.");


        assertEquals(0, categorizedCards.get(CardLevel.LEVEL_I).size());
        assertEquals(0, categorizedCards.get(CardLevel.LEVEL_II).size());
    }


    // --- Tests for specific card type creation ---
    // These tests verify that the factory correctly calls the specific creator methods
    // and that those methods parse properties correctly.

    @Test
    void testCreateAbandonedShipCard_CorrectProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("level", "LEVEL_I");
        props.put("crewLost", 2);
        props.put("creditsGained", 5);
        props.put("lostDays", 1);
        CardConfig config = new CardConfig("as-01", AdventureType.ABANDONED_SHIP.toString(), "Abandoned Ship Desc", props);
        rawCardConfigs.put(config.id(), config);

        AdventureCard card = factory.createCard(config);
        assertNotNull(card);
        assertTrue(card instanceof AbandonedShipCard);
        AbandonedShipCard asc = (AbandonedShipCard) card;
        assertEquals(CardLevel.LEVEL_I, asc.getLevel());
        assertEquals(2, asc.getCrewLost());
        assertEquals(5, asc.getCreditsGained());
        assertEquals(1, asc.getLostDays());
    }

    @Test
    void testCreatePlanetsCard_CorrectProperties() {
        Map<String, Object> planet1Goods = new HashMap<>();
        planet1Goods.put(GoodType.RED.name(), 1);
        planet1Goods.put(GoodType.BLUE.name(), 2);
        Map<String, Object> planet1Data = new HashMap<>();
        planet1Data.put("number", 1);
        planet1Data.put("goodQuantities", planet1Goods);
        planet1Data.put("visited", false); // Though not used by Planet constructor from CardConfig

        List<Map<String, Object>> planetsData = new ArrayList<>();
        planetsData.add(planet1Data);

        Map<String, Object> props = new HashMap<>();
        props.put("level", "LEVEL_II");
        props.put("lostDays", 3);
        props.put("planets", planetsData);
        CardConfig config = new CardConfig("pl-01", AdventureType.PLANETS.toString(), "Planets Desc", props);
        rawCardConfigs.put(config.id(), config);

        AdventureCard card = factory.createCard(config);
        assertNotNull(card);
        assertTrue(card instanceof PlanetsCard);
        PlanetsCard pc = (PlanetsCard) card;
        assertEquals(CardLevel.LEVEL_II, pc.getLevel());
        assertEquals(3, pc.getLostDays());
        assertNotNull(pc.getPlanets());
        assertEquals(1, pc.getPlanets().size());
        assertEquals(1, pc.getPlanets().get(0).getNumber()); // Corrected: Planet has getNumber()
        assertEquals(2, pc.getPlanets().get(0).getGoodQuantities().get(GoodType.BLUE));
    }

    @Test
    void testCreateCombatZoneCard_CorrectProperties() {
        // Cannon Fire definition for a check
        List<Map<String, String>> cannonFiresData = new ArrayList<>();
        Map<String, String> fire1 = new HashMap<>();
        fire1.put("approach", Direction.UP.name());
        fire1.put("intensity", ShotIntensity.LIGHT.name());
        cannonFiresData.add(fire1);

        // CombatCheck 1: Cannon Fire
        Map<String, Object> check1Data = new HashMap<>();
        check1Data.put("attribute", CombatAttributeType.CANNON_STRENGTH.name());
        check1Data.put("penaltyType", PenaltyType.CANNON_FIRE.name());
        check1Data.put("cannonFires", cannonFiresData);

        // CombatCheck 2: Crew Loss
        Map<String, Object> check2Data = new HashMap<>();
        check2Data.put("attribute", CombatAttributeType.CREW_COUNT.name());
        check2Data.put("penaltyType", PenaltyType.CREW_LOSS.name());
        check2Data.put("penaltyValue", 3);

        List<Map<String, Object>> combatChecksData = new ArrayList<>();
        combatChecksData.add(check1Data);
        combatChecksData.add(check2Data);

        Map<String, Object> props = new HashMap<>();
        props.put("level", "LEVEL_II");
        props.put("combatChecks", combatChecksData);
        CardConfig config = new CardConfig("cz-01", AdventureType.WAR_ZONE.toString(), "Combat Zone Desc", props);
        rawCardConfigs.put(config.id(), config);

        AdventureCard card = factory.createCard(config);
        assertNotNull(card);
        assertTrue(card instanceof CombatZoneCard);
        CombatZoneCard czc = (CombatZoneCard) card;

        assertEquals(CardLevel.LEVEL_II, czc.getLevel());
        assertNotNull(czc.getCombatChecks());
        assertEquals(2, czc.getCombatChecks().size());

        // Validate first combat check (Cannon Fire)
        CombatCheck cc1 = czc.getCombatChecks().get(0);
        assertEquals(CombatAttributeType.CANNON_STRENGTH, cc1.getAttribute());
        assertEquals(PenaltyType.CANNON_FIRE, cc1.getPenaltyType());
        assertNotNull(cc1.getCannonFires());
        assertEquals(1, cc1.getCannonFires().size());
        assertEquals(Direction.UP, cc1.getCannonFires().get(0).getApproach());
        assertEquals(ShotIntensity.LIGHT, cc1.getCannonFires().get(0).getIntensity());

        // Validate second combat check (Crew Loss)
        CombatCheck cc2 = czc.getCombatChecks().get(1);
        assertEquals(CombatAttributeType.CREW_COUNT, cc2.getAttribute());
        assertEquals(PenaltyType.CREW_LOSS, cc2.getPenaltyType());
        assertEquals(3, cc2.getPenaltyValue());
    }

    // Add more tests for each specific card type, ensuring all their unique properties are parsed correctly.
    // For example, for MeteorSwarmCard, test the meteorPattern.
    // For PiratesCard, test powerLevel, creditReward, and attackPattern.
}
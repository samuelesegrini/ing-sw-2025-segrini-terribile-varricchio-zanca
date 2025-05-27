package it.polimi.ingsw.server.model.domain.general.loader;

import it.polimi.ingsw.server.model.domain.general.config.ComponentConfig;
import it.polimi.ingsw.server.model.domain.ship.components.*;
import it.polimi.ingsw.server.model.enums.crew.AlienColor;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentFactory {
    private final Map<String, ComponentCreator> creators;

    public ComponentFactory() {
        this.creators = new HashMap<>();
        initializeCreators();
    }

    public void registerCreator(String type, ComponentCreator creator) {
        creators.put(type.toUpperCase(), creator);
    }

    public Component createComponent(ComponentConfig config) {
        ComponentCreator creator = creators.get(config.type().toUpperCase());
        if (creator == null) {
            System.err.println("Warning: No creator registered for component type: " + config.type() + " (ID: " + config.id() + "). Skipping component.");
            return null; // Or throw an exception
        }
        try {
            return creator.create(config);
        } catch (Exception e) {
            System.err.println("Error creating component ID " + config.id() + " of type " + config.type() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Map<Direction, ConnectorType> parseConnectors(List<String> connectorStrings) {
        Map<Direction, ConnectorType> connectors = new HashMap<>();
        if (connectorStrings == null) return connectors;

        for (String cs : connectorStrings) {
            String[] parts = cs.split(":");
            if (parts.length == 2) {
                try {
                    Direction dir = Direction.valueOf(parts[0].trim().toUpperCase());
                    ConnectorType ct = ConnectorType.valueOf(parts[1].trim().toUpperCase());
                    connectors.put(dir, ct);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid connector string part in '" + cs + "': " + e.getMessage());
                }
            } else {
                System.err.println("Warning: Malformed connector string: '" + cs + "'");
            }
        }
        return connectors;
    }

    private void initializeCreators() {
        registerCreator("BATTERY", this::createBattery);
        registerCreator("CABIN", this::createCabin);
        registerCreator("CABIN_START", this::createCabin);
        registerCreator("CANNON_SINGLE", this::createCannonSingle);
        registerCreator("CANNON_DOUBLE", this::createCannonDouble);
        registerCreator("CARGO_HOLD", this::createCargoHold);
        registerCreator("CARGO_HOLD_SPECIAL", this::createCargoHoldSpecial);
        registerCreator("ENGINE_SINGLE", this::createEngineSingle);
        registerCreator("ENGINE_DOUBLE", this::createEngineDouble);
        registerCreator("LIFE_SUPPORT_BROWN", this::createLifeSupportBrown);
        registerCreator("LIFE_SUPPORT_PURPLE", this::createLifeSupportPurple);
        registerCreator("SHIELD", this::createShield);
        registerCreator("STRUCTURAL", this::createStructural);
    }

    private Battery createBattery(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        int maxBatteries = Integer.parseInt((String) config.properties().get("maxBatteries"));
        // Assuming ComponentType.BATTERY matches the "BATTERY" string from JSON
        return new Battery(ComponentType.BATTERY, connectors, maxBatteries, config.id());
    }

    private Cabin createCabin(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        ComponentType type = ComponentType.CABIN; // Default
        // Check if it's a starting cabin by playerColor property
        if (config.properties().containsKey("playerColor")) {
            // This is a starting cabin. Your Cabin class needs to handle this,
            // or you need a StartingCabin subclass. For now, assuming Cabin handles it.
            // PlayerColor color = PlayerColor.valueOf(((String) config.properties().get("playerColor")).toUpperCase());
            // Cabin cabin = new Cabin(type, connectors);
            // cabin.setPlayerColor(color); // Example if Cabin has this setter
            type = ComponentType.CABIN_START; // Or set a flag
        }
        return new Cabin(type, connectors, config.id()); // Pass PlayerColor if Cabin constructor supports it
    }

    private Cannon createCannonSingle(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        return new Cannon(ComponentType.CANNON_SINGLE, connectors, config.id());
    }

    private Cannon createCannonDouble(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        return new Cannon(ComponentType.CANNON_DOUBLE, connectors, config.id());
    }

    private CargoHold createCargoHold(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        int capacity = Integer.parseInt((String) config.properties().get("capacity"));
        String id = config.id();
        return new CargoHold(ComponentType.CARGO_HOLD, connectors, capacity, id);
    }

    private CargoHold createCargoHoldSpecial(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        int capacity = Integer.parseInt((String) config.properties().get("capacity"));
        String id = config.id();
        return new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, capacity, id);
    }

    private Engine createEngineSingle(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        String id = config.id();
        return new Engine(ComponentType.ENGINE_SINGLE, connectors, id);
    }

    private Engine createEngineDouble(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        String id = config.id();
        return new Engine(ComponentType.ENGINE_DOUBLE, connectors, id);
    }

    private LifeSupportSystem createLifeSupportBrown(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        LifeSupportSystem lss = new LifeSupportSystem(ComponentType.LIFE_SUPPORT_BROWN, connectors, config.id());
        lss.setSupportedAlienColor(AlienColor.ALIEN_BROWN);
        return lss;
    }

    private LifeSupportSystem createLifeSupportPurple(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        LifeSupportSystem lss = new LifeSupportSystem(ComponentType.LIFE_SUPPORT_PURPLE, connectors, config.id());
        lss.setSupportedAlienColor(AlienColor.ALIEN_PURPLE);
        return lss;
    }

    private Shield createShield(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        return new Shield(ComponentType.SHIELD, connectors, config.id());
    }

    private StructuralModule createStructural(ComponentConfig config) {
        Map<Direction, ConnectorType> connectors = parseConnectors(config.connectors());
        return new StructuralModule(ComponentType.STRUCTURAL, connectors, config.id());
    }
}
package it.polimi.ingsw.common;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.io.Serializable;
import java.util.Map;

/**
 * Complete component data transfer object for server-to-client communication.
 * Contains all information needed to create a ComponentInstance on the client side.
 */
public class ComponentData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final String id;
    public final ComponentType type;
    public final Map<Direction, ConnectorType> connectors;
    public final Direction defaultDirection;
    
    public ComponentData(String id, ComponentType type, Map<Direction, ConnectorType> connectors, Direction defaultDirection) {
        this.id = id;
        this.type = type;
        this.connectors = connectors;
        this.defaultDirection = defaultDirection;
    }
    
    public ComponentData(String id, ComponentType type, Map<Direction, ConnectorType> connectors) {
        this(id, type, connectors, Direction.UP);
    }
    
    public String getId() { return id; }
    public ComponentType getType() { return type; }
    public Map<Direction, ConnectorType> getConnectors() { return connectors; }
    public Direction getDefaultDirection() { return defaultDirection; }
}
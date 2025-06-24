package it.polimi.ingsw.client.core.state;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents a complete component instance on the client side.
 * Contains all information needed for proper UI display including unique ID, 
 * component type, connectors, image path, and current orientation.
 */
public class ComponentInstance {
    private final String id;
    private final ComponentType type;
    private final String imagePath;
    private final Map<Direction, ConnectorType> baseConnectors; // Original connector configuration
    private Direction currentDirection; // Current rotation state
    private Map<Direction, ConnectorType> currentConnectors; // Current connector state after rotation
    
    /**
     * Create a ComponentInstance with full component information
     */
    public ComponentInstance(String id, ComponentType type, Map<Direction, ConnectorType> connectors) {
        this.id = id;
        this.type = type;
        this.imagePath = id; // ID is the image path
        this.baseConnectors = new HashMap<>(connectors);
        this.currentDirection = Direction.UP;
        this.currentConnectors = new HashMap<>(connectors);
    }
    
    public String getId() {
        return id;
    }
    
    public ComponentType getType() {
        return type;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    /**
     * Get the image path for this component.
     */
    public String getImageResourcePath() {
        return imagePath;
    }
    
    public Direction getCurrentDirection() {
        return currentDirection;
    }
    
    /**
     * Get the current connector configuration (after any rotations)
     */
    public Map<Direction, ConnectorType> getCurrentConnectors() {
        return new HashMap<>(currentConnectors);
    }
    
    /**
     * Get the connector type at a specific direction in current orientation
     */
    public ConnectorType getConnectorAt(Direction direction) {
        return currentConnectors.get(direction);
    }
    
    /**
     * Get the original base connector configuration (before any rotations)
     */
    public Map<Direction, ConnectorType> getBaseConnectors() {
        return new HashMap<>(baseConnectors);
    }
    
    /**
     * Rotate the component 90 degrees clockwise
     */
    public void rotate() {
        currentDirection = currentDirection.rotateClockwise();
        
        Map<Direction, ConnectorType> temp = new HashMap<>(currentConnectors);
        for (Direction d : Direction.values()) {
            currentConnectors.put(d, temp.get(d.rotateCounterClockwise()));
        }
    }
    
    /**
     * Set the component to a specific direction (handles multiple rotations)
     */
    public void setDirection(Direction targetDirection) {
        // Reset to base configuration
        currentDirection = Direction.UP;
        currentConnectors = new HashMap<>(baseConnectors);
        
        // Rotate to target direction
        while (currentDirection != targetDirection) {
            rotate();
        }
    }
    
    /**
     * Check if this component can connect to another component at the given direction
     */
    public boolean canConnectTo(ComponentInstance other, Direction direction) {
        ConnectorType myConnector = getConnectorAt(direction);
        ConnectorType otherConnector = other.getConnectorAt(direction.getOpposite());
        
        if (myConnector == null || otherConnector == null) {
            return false;
        }
        
        return myConnector.canConnectTo(otherConnector);
    }
    
    /**
     * Get display symbol for TUI (based on component type)
     */
    public String getDisplaySymbol() {
        return switch (type) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> "E";
            case CANNON_SINGLE, CANNON_DOUBLE -> "C";
            case CABIN, CABIN_START -> "R";
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> "G";
            case BATTERY -> "B";
            case SHIELD -> "S";
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> "L";
            case STRUCTURAL -> "T";
        };
    }
    
    /**
     * Get formatted display name for UI
     */
    public String getDisplayName() {
        return type.name().toLowerCase().replace("_", " ");
    }
    
    /**
     * Create a copy of this component instance with a new ID (for duplication)
     */
    public ComponentInstance copy(String newId) {
        ComponentInstance copy = new ComponentInstance(newId, this.type, this.baseConnectors);
        copy.setDirection(this.currentDirection);
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ComponentInstance that = (ComponentInstance) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("ComponentInstance{id='%s', type=%s, direction=%s}", 
            id, type, currentDirection);
    }
}
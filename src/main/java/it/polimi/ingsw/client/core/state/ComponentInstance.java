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

    // ANSI escape codes for colors
    final String RESET = "\u001B[0m";

    // Background colors
    final String BG_BLACK = "\u001B[48;2;0;0;0m";           // Pure black
    final String BG_WHITE = "\u001B[48;2;255;255;255m";     // Pure white
    final String BG_RED = "\u001B[48;2;255;0;0m";           // Pure red
    final String BG_BLUE = "\u001B[48;2;0;0;255m";          // Pure blue
    final String BG_GREEN = "\u001B[48;2;0;255;0m";         // Pure green
    final String BG_YELLOW = "\u001B[48;2;255;255;0m";      // Pure yellow
    final String BG_PURPLE = "\u001B[48;2;128;0;128m";      // Medium purple
    final String BG_BROWN = "\u001B[48;2;139;69;19m";       // Saddle brown
    final String BG_VIOLET = "\u001B[48;2;138;43;226m";     // Blue violet
    final String BG_CYAN = "\u001B[48;2;0;255;255m";        // Pure cyan

    final String BG_BRIGHT_BLACK = "\u001B[48;2;128;128;128m";   // Bright black (gray)
    final String BG_BRIGHT_WHITE = "\u001B[48;2;255;255;255m";   // Bright white
    final String BG_BRIGHT_RED = "\u001B[48;2;255;99;71m";       // Bright red (tomato)
    final String BG_BRIGHT_BLUE = "\u001B[48;2;0;191;255m";      // Deep sky blue
    final String BG_BRIGHT_GREEN = "\u001B[48;2;0;255;127m";     // Spring green
    final String BG_BRIGHT_YELLOW = "\u001B[48;2;255;255;102m";  // Bright yellow
    final String BG_BRIGHT_PURPLE = "\u001B[48;2;255;0;255m";    // Bright purple (magenta)
    final String BG_BRIGHT_BROWN = "\u001B[48;2;205;133;63m";    // Bright brown (peru)
    final String BG_BRIGHT_VIOLET = "\u001B[48;2;147;112;219m";  // Bright violet
    final String BG_BRIGHT_CYAN = "\u001B[48;2;0;255;255m";      // Bright cyan

    final String BG_DARK_BLACK = "\u001B[48;2;0;0;0m";           // Pure black
    final String BG_DARK_WHITE = "\u001B[48;2;169;169;169m";     // Dark gray
    final String BG_DARK_RED = "\u001B[48;2;139;0;0m";           // Dark red
    final String BG_DARK_BLUE = "\u001B[48;2;0;0;139m";          // Dark blue
    final String BG_DARK_GREEN = "\u001B[48;2;0;100;0m";         // Dark green
    final String BG_DARK_YELLOW = "\u001B[48;2;184;134;11m";     // Dark goldenrod
    final String BG_DARK_PURPLE = "\u001B[48;2;75;0;130m";       // Indigo
    final String BG_DARK_BROWN = "\u001B[48;2;101;67;33m";       // Dark brown
    final String BG_DARK_VIOLET = "\u001B[48;2;75;0;130m";       // Dark violet
    final String BG_DARK_CYAN = "\u001B[48;2;0;139;139m";        // Dark cyan

    // Text colors
    final String FG_BLACK = "\u001B[38;2;0;0;0m";           // Pure black
    final String FG_WHITE = "\u001B[38;2;255;255;255m";     // Pure white
    final String FG_RED = "\u001B[38;2;255;0;0m";           // Pure red
    final String FG_BLUE = "\u001B[38;2;0;0;255m";          // Pure blue
    final String FG_GREEN = "\u001B[38;2;0;255;0m";         // Pure green
    final String FG_YELLOW = "\u001B[38;2;255;255;0m";      // Pure yellow
    final String FG_PURPLE = "\u001B[38;2;128;0;128m";      // Medium purple
    final String FG_BROWN = "\u001B[38;2;139;69;19m";       // Saddle brown
    final String FG_VIOLET = "\u001B[38;2;138;43;226m";     // Blue violet
    final String FG_CYAN = "\u001B[38;2;0;255;255m";        // Pure cyan

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
//        return switch (type) {
//            case ENGINE_SINGLE, ENGINE_DOUBLE -> "E";
//            case CANNON_SINGLE, CANNON_DOUBLE -> "C";
//            case CABIN, CABIN_START -> "R";
//            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> "G";
//            case BATTERY -> "B";
//            case SHIELD -> "S";
//            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> "L";
//            case STRUCTURAL -> "T";
//        };

        return switch (type) {
            case BATTERY -> "🔋";
            case CABIN -> "⛺️";
            case CABIN_START -> BG_YELLOW + "⛺️" + RESET;
            case CANNON_SINGLE -> "🔫";
            case CANNON_DOUBLE -> BG_BRIGHT_GREEN + "🔫" + RESET;
            case CARGO_HOLD -> "📦️";
            case CARGO_HOLD_SPECIAL -> BG_RED + "📦️" + RESET;
            case ENGINE_SINGLE -> "🚀";
            case ENGINE_DOUBLE -> BG_BRIGHT_GREEN + "🚀" + RESET;
            case LIFE_SUPPORT_BROWN -> BG_BROWN + "🫁️" + RESET;
            case LIFE_SUPPORT_PURPLE -> BG_PURPLE + "🫁️" + RESET;
            case SHIELD -> "🛡️";
            case STRUCTURAL -> "🔗️";
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
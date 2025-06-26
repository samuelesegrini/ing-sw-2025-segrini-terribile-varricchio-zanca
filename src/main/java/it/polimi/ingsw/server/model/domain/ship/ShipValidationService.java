package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.*;

/**
 * Comprehensive ship validation service that handles both basic placement validation
 * and advanced structural integrity checks according to Galaxy Trucker rules.
 */
public class ShipValidationService {
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        }
        
        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, List.of(error), Collections.emptyList());
        }
        
        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors, Collections.emptyList());
        }
        
        public static ValidationResult withWarnings(List<String> warnings) {
            return new ValidationResult(true, Collections.emptyList(), warnings);
        }
    }
    
    /**
     * Validates component placement at a specific position.
     * This is used for real-time validation during ship building.
     * @param ship The ship to validate placement on
     * @param component The component to place
     * @param position The position to place the component
     * @return ValidationResult indicating if placement is valid
     */
    public static ValidationResult validateComponentPlacement(Ship ship, Component component, Position position) {
        List<String> errors = new ArrayList<>();
        
        // Check basic placement requirements
        if (position == null) {
            errors.add("Position cannot be null");
            return ValidationResult.invalid(errors);
        }
        
        if (component == null) {
            errors.add("Component cannot be null");
            return ValidationResult.invalid(errors);
        }
        
        // Check grid bounds
        Component[][] board = ship.getBoard();
        int row = position.getRow();
        int col = position.getCol();
        
        if (row < 0 || row >= board.length || col < 0 || col >= board[0].length) {
            errors.add("Position is outside ship grid bounds");
            return ValidationResult.invalid(errors);
        }
        
        // Check forbidden positions
        if (ship.forbiddenPositions.contains(position)) {
            errors.add("Cannot place component on forbidden position");
            return ValidationResult.invalid(errors);
        }
        
        // Check if position is already occupied
        if (board[row][col] != null) {
            errors.add("Position is already occupied by another component");
            return ValidationResult.invalid(errors);
        }
        
        // Check component-specific placement rules
        ValidationResult componentSpecificResult = validateComponentSpecificRules(ship, component, position);
        if (!componentSpecificResult.isValid()) {
            return componentSpecificResult;
        }
        
        // Check connectivity requirements
        ValidationResult connectivityResult = validateConnectivity(ship, component, position);
        if (!connectivityResult.isValid()) {
            return connectivityResult;
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates component-specific placement rules (e.g., engine exhaust, cannon placement).
     */
    private static ValidationResult validateComponentSpecificRules(Ship ship, Component component, Position position) {
        List<String> errors = new ArrayList<>();
        Component[][] board = ship.getBoard();
        
        switch (component.getType()) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> {
                // Engines must have exhaust pointing DOWN and no component behind exhaust
                Direction exhaust = component.getDirection().getOpposite();
                if (exhaust != Direction.DOWN) {
                    errors.add("Engine exhaust must point down (outside the ship)");
                }
                
                Position exhaustPosition = position.offsetBy(exhaust);
                if (isValidPosition(board, exhaustPosition) && board[exhaustPosition.getRow()][exhaustPosition.getCol()] != null) {
                    errors.add("Engine exhaust is blocked by another component");
                }
            }
            case CANNON_SINGLE, CANNON_DOUBLE -> {
                // Cannons must have clear firing line
                Direction firingDirection = component.getDirection();
                Position firingPosition = position.offsetBy(firingDirection);
                if (isValidPosition(board, firingPosition) && board[firingPosition.getRow()][firingPosition.getCol()] != null) {
                    errors.add("Cannon firing line is blocked by another component");
                }
            }
            case CABIN_START -> {
                // Starting cabin must be placed at the designated starting position
                // This is level-specific and should be validated against ship configuration
                if (!isStartingCabinPosition(ship, position)) {
                    errors.add("Starting cabin must be placed at the designated starting position");
                }
            }
        }
        
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }
    
    /**
     * Validates that a component will be properly connected when placed.
     */
    private static ValidationResult validateConnectivity(Ship ship, Component component, Position position) {
        List<String> errors = new ArrayList<>();
        Component[][] board = ship.getBoard();
        boolean hasValidConnection = false;
        
        // Check all four directions for valid connections
        for (Direction direction : Direction.values()) {
            Position neighborPos = position.offsetBy(direction);
            
            if (isValidPosition(board, neighborPos)) {
                Component neighbor = board[neighborPos.getRow()][neighborPos.getCol()];
                
                if (neighbor != null) {
                    ConnectorType myConnector = component.getConnectorAt(direction);
                    ConnectorType neighborConnector = neighbor.getConnectorAt(direction.getOpposite());
                    
                    if (myConnector != null && neighborConnector != null) {
                        if (myConnector.canConnectTo(neighborConnector)) {
                            hasValidConnection = true;
                        } else if (myConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN) {
                            errors.add(String.format("Incompatible connectors: %s cannot connect to %s",
                                myConnector, neighborConnector));
                        }
                    }
                }
            }
        }
        
        // First component (starting cabin) doesn't need connections
        if (!hasValidConnection && !isFirstComponent(ship)) {
            errors.add("Component must connect to at least one existing component");
        }
        
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }
    
    /**
     * Performs comprehensive ship validation for end-of-building-phase checks.
     * This includes structural integrity, rule compliance, and optimization suggestions.
     */
    public static ValidationResult validateCompleteShip(Ship ship, GamePhase currentPhase) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Basic structural validation
        ValidationResult structuralResult = validateShipStructure(ship);
        errors.addAll(structuralResult.getErrors());
        warnings.addAll(structuralResult.getWarnings());
        
        // Connection validation
        ValidationResult connectionResult = validateAllConnections(ship);
        errors.addAll(connectionResult.getErrors());
        warnings.addAll(connectionResult.getWarnings());
        
        // Rule compliance validation
        ValidationResult rulesResult = validateGameRules(ship, currentPhase);
        errors.addAll(rulesResult.getErrors());
        warnings.addAll(rulesResult.getWarnings());
        
        // Performance optimization suggestions
        if (errors.isEmpty()) {
            warnings.addAll(generateOptimizationSuggestions(ship));
        }
        
        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }
    
    /**
     * Validates the overall structure of the ship.
     */
    private static ValidationResult validateShipStructure(Ship ship) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        Component[][] board = ship.getBoard();
        boolean hasStartingCabin = false;
        int componentCount = 0;
        
        // Check for starting cabin and count components
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                if (component != null) {
                    componentCount++;
                    if (component.getType() == ComponentType.CABIN_START) {
                        hasStartingCabin = true;
                    }
                }
            }
        }
        
        if (!hasStartingCabin) {
            errors.add("Ship must have a starting cabin");
        }
        
        if (componentCount == 0) {
            errors.add("Ship cannot be empty");
        } else if (componentCount == 1 && hasStartingCabin) {
            warnings.add("Ship only has a starting cabin - consider adding more components");
        }
        
        // Check ship connectivity (all components must be connected)
        if (componentCount > 1) {
            Set<Position> connectedComponents = findConnectedComponents(ship);
            if (connectedComponents.size() != componentCount) {
                errors.add("All ship components must be connected");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Validates all component connections in the ship.
     */
    private static ValidationResult validateAllConnections(Ship ship) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        Component[][] board = ship.getBoard();
        
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                if (component != null) {
                    Position position = new Position(row, col);
                    component.setPosition(position);
                    
                    if (!ship.isCorrectlyConnected(component)) {
                        errors.add(String.format("Component at (%d,%d) has invalid connections", row, col));
                    }
                    
                    // Check for placement errors
                    List<Component> placementErrors = ship.checkPlacingErrors();
                    if (!placementErrors.isEmpty()) {
                        errors.add(String.format("Component at (%d,%d) violates placement rules", row, col));
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Validates game rule compliance.
     */
    private static ValidationResult validateGameRules(Ship ship, GamePhase currentPhase) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Update ship statistics
        ship.updateStats();
        
        // Validate minimum requirements (if any)
        if (ship.getCannons() == 0) {
            warnings.add("Ship has no cannons - you'll be defenseless in combat");
        }
        
        if (ship.getEngines() == 0) {
            errors.add("Ship must have at least one engine to move");
        }
        
        if (ship.getBatteries() == 0 && (ship.getCannons() > 0 || ship.getEngines() > 1)) {
            warnings.add("Ship needs batteries to power cannons and extra engines");
        }
        
        // Check cargo capacity
        int totalCargo = ship.calculateNormalGoodsCapacity() + ship.calculateSpecialGoodsCapacity();
        if (totalCargo == 0) {
            warnings.add("Ship has no cargo capacity - you won't be able to carry goods");
        }
        
        // Flight phase specific validations
        if (currentPhase == GamePhase.FLIGHT) {
            // During flight, structural integrity is more critical
            int exposedConnectors = ship.getExposedConnectors();
            if (exposedConnectors > 5) {
                warnings.add(String.format("Ship has %d exposed connectors (high vulnerability)", exposedConnectors));
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Generates optimization suggestions for the ship.
     */
    private static List<String> generateOptimizationSuggestions(Ship ship) {
        List<String> suggestions = new ArrayList<>();
        
        ship.updateStats();
        
        // Balance suggestions
        double engineToCannon = ship.getEngines() / Math.max(1, ship.getCannons());
        if (engineToCannon > 3) {
            suggestions.add("Consider adding more cannons to balance your engines");
        } else if (engineToCannon < 0.5) {
            suggestions.add("Consider adding more engines to balance your cannons");
        }
        
        // Battery suggestions
        double totalPowerNeeds = ship.getCannons() + Math.max(0, ship.getEngines() - 1);
        if (ship.getBatteries() < totalPowerNeeds) {
            suggestions.add(String.format("Consider adding %d more batteries for optimal power", 
                (int)(totalPowerNeeds - ship.getBatteries())));
        }
        
        // Crew suggestions
        int cabinCount = ship.countAllAdjacentCabins();
        if (cabinCount < 3) {
            suggestions.add("Consider adding more connected cabins for crew capacity");
        }
        
        // Defensive suggestions
        if (ship.getExposedConnectors() > 3) {
            suggestions.add("Consider minimizing exposed connectors to reduce vulnerability");
        }
        
        return suggestions;
    }
    
    // Helper methods
    
    private static boolean isValidPosition(Component[][] board, Position position) {
        int row = position.getRow();
        int col = position.getCol();
        return row >= 0 && row < board.length && col >= 0 && col < board[0].length;
    }
    
    private static boolean isStartingCabinPosition(Ship ship, Position position) {
        // This should check against the ship's level-specific configuration
        // For now, using a simple check - starting cabin typically goes in the center
        return position.getRow() == 2 && position.getCol() == 3;
    }
    
    private static boolean isFirstComponent(Ship ship) {
        Component[][] board = ship.getBoard();
        int componentCount = 0;
        
        for (Component[] row : board) {
            for (Component component : row) {
                if (component != null) {
                    componentCount++;
                }
            }
        }
        
        return componentCount == 0;
    }
    
    private static Set<Position> findConnectedComponents(Ship ship) {
        Component[][] board = ship.getBoard();
        Set<Position> visited = new HashSet<>();
        Set<Position> connected = new HashSet<>();
        
        // Find the first component to start flood fill
        Position start = null;
        for (int row = 0; row < board.length && start == null; row++) {
            for (int col = 0; col < board[0].length && start == null; col++) {
                if (board[row][col] != null) {
                    start = new Position(row, col);
                }
            }
        }
        
        if (start != null) {
            floodFillConnectedComponents(ship, start, visited, connected);
        }
        
        return connected;
    }
    
    private static void floodFillConnectedComponents(Ship ship, Position position, Set<Position> visited, Set<Position> connected) {
        if (visited.contains(position)) {
            return;
        }
        
        Component[][] board = ship.getBoard();
        int row = position.getRow();
        int col = position.getCol();
        
        if (!isValidPosition(board, position) || board[row][col] == null) {
            return;
        }
        
        visited.add(position);
        connected.add(position);
        
        Component current = board[row][col];
        current.setPosition(position);
        
        // Check all adjacent positions for connected components
        for (Direction direction : Direction.values()) {
            Position neighbor = position.offsetBy(direction);
            if (isValidPosition(board, neighbor) && board[neighbor.getRow()][neighbor.getCol()] != null) {
                Component neighborComponent = board[neighbor.getRow()][neighbor.getCol()];
                neighborComponent.setPosition(neighbor);
                
                // Check if components are properly connected
                ConnectorType myConnector = current.getConnectorAt(direction);
                ConnectorType neighborConnector = neighborComponent.getConnectorAt(direction.getOpposite());
                
                if (myConnector != null && neighborConnector != null && 
                    myConnector.canConnectTo(neighborConnector)) {
                    floodFillConnectedComponents(ship, neighbor, visited, connected);
                }
            }
        }
    }
    
    /**
     * Enhanced Galaxy Trucker specific validation with detailed feedback.
     */
    public static ValidationResult validateGalaxyTruckerRules(Ship ship, GamePhase currentPhase) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> tips = new ArrayList<>();
        
        // Run comprehensive validation
        ValidationResult basicResult = validateCompleteShip(ship, currentPhase);
        errors.addAll(basicResult.getErrors());
        warnings.addAll(basicResult.getWarnings());
        
        // Galaxy Trucker specific rules
        validatePowerSystem(ship, errors, warnings, tips);
        validateDefenseSystems(ship, errors, warnings, tips);
        validateCargoSystems(ship, errors, warnings, tips);
        validateCrewSystems(ship, errors, warnings, tips);
        validateStructuralIntegrity(ship, errors, warnings, tips);
        
        // Add tips as warnings for user feedback
        warnings.addAll(tips);
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    private static void validatePowerSystem(Ship ship, List<String> errors, List<String> warnings, List<String> tips) {
        int batteries = ship.getBatteries();
        int cannons = (int)ship.getCannons();
        double engines = ship.getEngines();
        
        // Power requirements calculation
        int cannonPowerNeeds = cannons; // Each cannon needs 1 battery to fire
        int enginePowerNeeds = Math.max(0, (int)engines - 1); // First engine is free
        int totalPowerNeeds = cannonPowerNeeds + enginePowerNeeds;
        
        if (batteries < totalPowerNeeds) {
            errors.add(String.format("Insufficient batteries: Need %d but only have %d (Cannons=%d, Extra Engines=%d)", 
                      totalPowerNeeds, batteries, cannonPowerNeeds, enginePowerNeeds));
        } else if (batteries == totalPowerNeeds) {
            warnings.add("Power system at capacity - no room for boosting strength during encounters");
        } else {
            tips.add(String.format("Power system healthy: %d batteries for %d needs (%d spare for boosting)", 
                    batteries, totalPowerNeeds, batteries - totalPowerNeeds));
        }
        
        // Battery placement tips
        if (batteries > 0 && cannons > 0) {
            tips.add("💡 Place batteries near cannons for efficient power distribution");
        }
    }
    
    private static void validateDefenseSystems(Ship ship, List<String> errors, List<String> warnings, List<String> tips) {
        int cannons = (int)ship.getCannons();
        int exposedConnectors = ship.getExposedConnectors();
        
        // Combat readiness
        if (cannons == 0) {
            warnings.add("🚨 No cannons - you'll be defenseless against pirates and meteors");
        } else if (cannons < 2) {
            warnings.add("⚠️ Only 1 cannon - consider adding more for stronger combat");
        } else {
            tips.add(String.format("✅ Combat ready with %d cannons", cannons));
        }
        
        // Vulnerability assessment
        if (exposedConnectors > 8) {
            errors.add(String.format("🚨 CRITICAL: %d exposed connectors (high risk of catastrophic damage)", exposedConnectors));
        } else if (exposedConnectors > 5) {
            warnings.add(String.format("⚠️ %d exposed connectors - vulnerable to stardust penalties", exposedConnectors));
        } else if (exposedConnectors > 2) {
            tips.add(String.format("⚠️ %d exposed connectors - acceptable but could be better", exposedConnectors));
        } else {
            tips.add(String.format("✅ Excellent protection: only %d exposed connectors", exposedConnectors));
        }
        
        // Shield analysis
        int shields = countShields(ship);
        if (shields == 0) {
            warnings.add("No shields - meteors will cause direct damage");
        } else {
            tips.add(String.format("✅ %d shields for meteor protection", shields));
        }
    }
    
    private static void validateCargoSystems(Ship ship, List<String> errors, List<String> warnings, List<String> tips) {
        int normalCargo = ship.calculateNormalGoodsCapacity();
        int specialCargo = ship.calculateSpecialGoodsCapacity();
        int totalCargo = normalCargo + specialCargo;
        
        if (totalCargo == 0) {
            warnings.add("🚨 No cargo capacity - you can't collect any goods for profit");
        } else if (totalCargo < 4) {
            warnings.add("⚠️ Limited cargo capacity - you'll miss profit opportunities");
        } else if (totalCargo < 8) {
            tips.add(String.format("📦 Moderate cargo: %d total capacity (%d normal, %d special)", 
                    totalCargo, normalCargo, specialCargo));
        } else {
            tips.add(String.format("✅ Excellent cargo: %d total capacity (%d normal, %d special)", 
                    totalCargo, normalCargo, specialCargo));
        }
        
        // Interior protection tip
        tips.add("💡 Place cargo holds in ship interior for better protection");
    }
    
    private static void validateCrewSystems(Ship ship, List<String> errors, List<String> warnings, List<String> tips) {
        int connectedCabins = ship.countAllAdjacentCabins();
        int totalCrew = ship.getCrew(); // Current crew count
        
        if (connectedCabins == 0) {
            errors.add("🚨 No crew cabins - ship cannot operate");
        } else if (connectedCabins < 3) {
            warnings.add("⚠️ Limited crew capacity - you'll miss exploration opportunities");
        } else {
            tips.add(String.format("✅ Good crew capacity: %d connected cabins", connectedCabins));
        }
        
        // Life support check
        if (totalCrew > connectedCabins) {
            errors.add(String.format("🚨 Life support failure: %d crew but only %d cabin capacity", 
                      totalCrew, connectedCabins));
        }
        
        tips.add("💡 Connected cabins provide life support - keep them linked!");
    }
    
    private static void validateStructuralIntegrity(Ship ship, List<String> errors, List<String> warnings, List<String> tips) {
        // Check for structural weak points
        Component[][] board = ship.getBoard();
        List<String> structuralIssues = new ArrayList<>();
        
        // Check for isolated components that might break off
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                if (component != null) {
                    int connections = countValidConnections(board, row, col);
                    if (connections <= 1 && !isStartingCabin(component)) {
                        structuralIssues.add(String.format("Component at (%d,%d) has only %d connection(s)", 
                                           row, col, connections));
                    }
                }
            }
        }
        
        if (!structuralIssues.isEmpty()) {
            warnings.add("⚠️ Structural weak points detected:");
            warnings.addAll(structuralIssues);
            tips.add("💡 Components with single connections may break off during flight");
        } else {
            tips.add("✅ Strong structural integrity - all components well-connected");
        }
        
        // Balance assessment
        double engines = ship.getEngines();
        int cannons = (int)ship.getCannons();
        double ratio = cannons > 0 ? engines / cannons : engines;
        
        if (ratio > 4) {
            tips.add("⚖️ Engine-heavy design - great for racing, weak in combat");
        } else if (ratio < 0.5) {
            tips.add("⚖️ Combat-heavy design - strong fighter, may lag behind");
        } else {
            tips.add("⚖️ Balanced design - good mix of speed and firepower");
        }
    }
    
    private static int countShields(Ship ship) {
        Component[][] board = ship.getBoard();
        int shieldCount = 0;
        
        for (Component[] row : board) {
            for (Component component : row) {
                if (component != null && component.getType() == ComponentType.SHIELD) {
                    shieldCount++;
                }
            }
        }
        
        return shieldCount;
    }
    
    private static int countValidConnections(Component[][] board, int row, int col) {
        Component component = board[row][col];
        if (component == null) return 0;
        
        int connections = 0;
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        
        for (int i = 0; i < 4; i++) {
            int newRow = row + dRow[i];
            int newCol = col + dCol[i];
            
            if (newRow >= 0 && newRow < board.length && newCol >= 0 && newCol < board[0].length) {
                Component neighbor = board[newRow][newCol];
                if (neighbor != null) {
                    ConnectorType myConnector = component.getConnectorAt(directions[i]);
                    ConnectorType neighborConnector = neighbor.getConnectorAt(directions[i].getOpposite());
                    
                    if (myConnector != null && neighborConnector != null && 
                        myConnector.canConnectTo(neighborConnector)) {
                        connections++;
                    }
                }
            }
        }
        
        return connections;
    }
    
    private static boolean isStartingCabin(Component component) {
        return component.getType() == ComponentType.CABIN_START;
    }
    
    /**
     * Gets the Direction opposite to the given direction.
     */
    private static Direction getOppositeDirection(Direction dir) {
        return switch (dir) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }
}
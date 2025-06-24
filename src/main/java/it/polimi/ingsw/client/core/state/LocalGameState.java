package it.polimi.ingsw.client.core.state;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalGameState {
    private static LocalGameState instance = new LocalGameState();
    
    // Basic game state
    private String localPlayerId;
    private String currentGameId;
    private GamePhase currentPhase = GamePhase.SETUP;
    
    // Ship building state - 2D array with server-driven dimensions
    private ComponentInstance[][] shipGrid;
    private int gridRows = 5; // Default, will be updated from server
    private int gridCols = 7; // Default, will be updated from server
    private final List<ComponentInstance> availableTiles = Collections.synchronizedList(new ArrayList<>());
    private final List<ComponentInstance> heldTiles = Collections.synchronizedList(new ArrayList<>());
    private final List<ComponentInstance> faceUpJunkyardTiles = Collections.synchronizedList(new ArrayList<>());
    private final Set<Position> forbiddenPositions = new HashSet<>();
    private ShipGridConfig shipGridConfig; // Server-provided configuration
    private boolean buildingTimerFlipped = false;
    private long buildingTimeRemaining = 0;
    private long buildingPhaseStartTime = 0;
    private boolean shipValidated = false;
    private final List<String> validationErrors = Collections.synchronizedList(new ArrayList<>());
    
    // Player state
    private final Map<String, Boolean> playerReadyStatus = new ConcurrentHashMap<>();
    private final Map<String, Map<Position, ComponentInstance>> otherPlayersShips = new ConcurrentHashMap<>();
    private final List<GameInfo> availableGames = Collections.synchronizedList(new ArrayList<>());
    
    // Ship statistics cache
    private int totalEngines = 0;
    private int totalCannons = 0;
    private int totalCrew = 0;
    private int totalCargo = 0;
    private int totalBatteries = 0;
    private int totalShields = 0;
    
    private LocalGameState() {
        initializeShipGrid();
        initializeForbiddenPositions();
    }
    
    public static LocalGameState getInstance() { 
        return instance; 
    }
    
    // Basic getters
    public String getLocalPlayerId() { 
        return localPlayerId; 
    }
    
    public String getCurrentGameId() { 
        return currentGameId; 
    }
    
    public void setLocalPlayerId(String localPlayerId) {
        this.localPlayerId = localPlayerId;
    }
    
    public void setCurrentGameId(String currentGameId) {
        this.currentGameId = currentGameId;
    }
    
    // Game phase management
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
    
    public void setCurrentPhase(GamePhase phase) {
        this.currentPhase = phase;
        if (phase == GamePhase.BUILDING) {
            buildingPhaseStartTime = System.currentTimeMillis();
        }
    }
    
    public boolean isBuildingPhaseActive() {
        return currentPhase == GamePhase.BUILDING;
    }
    
    // Ship grid management
    public void placeTile(ComponentInstance component, Position position, int rotation) {
        if (component != null && position != null && !forbiddenPositions.contains(position)) {
            int row = position.getRow();
            int col = position.getCol();
            if (isValidGridPosition(row, col)) {
                shipGrid[row][col] = component;
                updateShipStatistics();
            }
        }
    }

    /**
     * Places a tile at the specified position with rotation.
     * This is used when receiving tile placement updates from the server.
     * @param playerId The player placing the tile
     * @param tileId The component tile ID (image path)
     * @param row Grid row position
     * @param col Grid column position  
     * @param rotation Number of 90-degree clockwise rotations
     */
    public void placeTile(String playerId, String tileId, int row, int col, int rotation) {
        Position position = new Position(row, col);
        
        // Find the component instance in available tiles or held tiles
        ComponentInstance component = findComponentById(tileId);
        if (component != null) {
            // Apply rotation
            for (int i = 0; i < rotation; i++) {
                component.rotate();
            }
            
            // Place the component
            placeTile(component, position, rotation);
            
            // Remove from available/held tiles since it's now placed
            removeAvailableTileById(tileId);
            removeHeldTileById(tileId);
        }
    }
    
    public void removeTile(Position position) {
        if (position != null) {
            int row = position.getRow();
            int col = position.getCol();
            if (isValidGridPosition(row, col)) {
                shipGrid[row][col] = null;
                updateShipStatistics();
            }
        }
    }
    
    public ComponentInstance getComponentAt(Position position) {
        if (position != null) {
            int row = position.getRow();
            int col = position.getCol();
            if (isValidGridPosition(row, col)) {
                return shipGrid[row][col];
            }
        }
        return null;
    }
    
    public ComponentInstance getComponentAt(int row, int col) {
        if (isValidGridPosition(row, col)) {
            return shipGrid[row][col];
        }
        return null;
    }
    
    // Get ship grid as 2D array of ComponentInstances
    public ComponentInstance[][] getShipGrid() {
        ComponentInstance[][] result = new ComponentInstance[gridRows][gridCols];
        for (int row = 0; row < gridRows; row++) {
            System.arraycopy(shipGrid[row], 0, result[row], 0, gridCols);
        }
        return result;
    }
    
    public boolean canPlaceComponent(ComponentInstance component, Position position) {
        if (component == null || position == null) {
            return false;
        }
        
        // Check if position is within server-defined grid bounds
        if (position.getRow() < 0 || position.getRow() >= gridRows || 
            position.getCol() < 0 || position.getCol() >= gridCols) {
            return false;
        }
        
        // Check if position is forbidden
        if (forbiddenPositions.contains(position)) {
            return false;
        }
        
        // Check if position is already occupied
        int row = position.getRow();
        int col = position.getCol();
        if (!isValidGridPosition(row, col) || shipGrid[row][col] != null) {
            return false;
        }
        
        // Basic connectivity check - component must connect to existing ship (except first component)
        if (hasAnyComponents() && !hasAdjacentComponent(position)) {
            return false;
        }
        
        return true;
    }
    
    public Set<Position> getForbiddenPositions() {
        return new HashSet<>(forbiddenPositions);
    }
    
    public void setForbiddenPositions(Set<Position> newForbiddenPositions) {
        forbiddenPositions.clear();
        if (newForbiddenPositions != null) {
            forbiddenPositions.addAll(newForbiddenPositions);
        }
    }
    
    public void clearForbiddenPositions() {
        forbiddenPositions.clear();
    }
    
    public void addForbiddenPosition(Position position) {
        forbiddenPositions.add(position);
    }
    
    // Ship grid configuration management
    public void setShipGridConfig(ShipGridConfig config) {
        this.shipGridConfig = config;
        if (config != null) {
            updateGridDimensions(config.rows(), config.cols());
        }
    }
    
    public ShipGridConfig getShipGridConfig() {
        return shipGridConfig;
    }
    
    public String getShipGridBackgroundImage() {
        return shipGridConfig != null ? shipGridConfig.image() : null;
    }
    
    public int getGridRows() {
        return gridRows;
    }
    
    public int getGridCols() {
        return gridCols;
    }
    
    // Component inventory management
    public void addAvailableTile(ComponentInstance component) {
        if (component != null) {
            availableTiles.add(component);
        }
    }

    /**
     * Adds an available tile with complete component data from server.
     * @param componentId The component ID (image path)
     * @param componentType The type of component
     * @param connectors The exact connector configuration from server
     */
    public void addAvailableTile(String componentId, ComponentType componentType, Map<Direction, ConnectorType> connectors) {
        ComponentInstance component = new ComponentInstance(componentId, componentType, connectors);
        addAvailableTile(component);
    }
    
    public void removeAvailableTile(ComponentInstance component) {
        availableTiles.remove(component);
    }
    
    public void removeAvailableTileById(String componentId) {
        availableTiles.removeIf(component -> component.getId().equals(componentId));
    }
    
    public List<ComponentInstance> getAvailableTiles() {
        return new ArrayList<>(availableTiles);
    }
    
    public void addHeldTile(ComponentInstance component) {
        if (component != null && heldTiles.size() < 2) { // Max 2 reserved components per rules
            heldTiles.add(component);
        }
    }

    /**
     * Adds a held (reserved) tile with complete component data from server.
     * @param componentId The component ID (image path)
     * @param componentType The type of component
     * @param connectors The exact connector configuration from server
     */
    public void addHeldTile(String componentId, ComponentType componentType, Map<Direction, ConnectorType> connectors) {
        ComponentInstance component = new ComponentInstance(componentId, componentType, connectors);
        addHeldTile(component);
    }
    
    public void removeHeldTile(ComponentInstance component) {
        heldTiles.remove(component);
    }
    
    public void removeHeldTileById(String componentId) {
        heldTiles.removeIf(component -> component.getId().equals(componentId));
    }
    
    public List<ComponentInstance> getHeldTiles() {
        return new ArrayList<>(heldTiles);
    }
    
    public boolean canReserveMoreTiles() {
        return heldTiles.size() < 2;
    }
    
    // Face-up junkyard tile management
    public List<ComponentInstance> getFaceUpJunkyardTiles() {
        return new ArrayList<>(faceUpJunkyardTiles);
    }
    
    public void setFaceUpJunkyardTiles(List<ComponentInstance> tiles) {
        faceUpJunkyardTiles.clear();
        if (tiles != null) {
            faceUpJunkyardTiles.addAll(tiles);
        }
    }
    
    public void addFaceUpJunkyardTile(ComponentInstance tile) {
        if (tile != null) {
            faceUpJunkyardTiles.add(tile);
        }
    }

    /**
     * Adds a face-up junkyard tile (returned by players, visible to all).
     * @param componentId The component ID (image path)
     * @param componentType The type of component
     * @param connectors The exact connector configuration from server
     */
    public void addFaceUpJunkyardTile(String componentId, ComponentType componentType, Map<Direction, ConnectorType> connectors) {
        ComponentInstance tile = new ComponentInstance(componentId, componentType, connectors);
        addFaceUpJunkyardTile(tile);
    }
    
    public void removeFaceUpJunkyardTile(ComponentInstance tile) {
        faceUpJunkyardTiles.remove(tile);
    }
    
    public void removeFaceUpJunkyardTileById(String componentId) {
        faceUpJunkyardTiles.removeIf(tile -> tile.getId().equals(componentId));
    }
    
    // Building timer management
    public void updateBuildingTimer(long timeRemaining) {
        this.buildingTimeRemaining = timeRemaining;
    }
    
    public long getBuildingTimeRemaining() {
        return buildingTimeRemaining;
    }
    
    public void setBuildingTimerFlipped(boolean flipped) {
        this.buildingTimerFlipped = flipped;
    }
    
    public boolean isBuildingTimerFlipped() {
        return buildingTimerFlipped;
    }
    
    public long getBuildingPhaseStartTime() {
        return buildingPhaseStartTime;
    }
    
    // Ship validation
    public void setShipValidation(boolean valid, List<String> errors) {
        this.shipValidated = valid;
        this.validationErrors.clear();
        if (errors != null) {
            this.validationErrors.addAll(errors);
        }
    }
    
    public boolean isShipValidated() {
        return shipValidated;
    }
    
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    // Ship statistics
    public int getShipStats(ComponentStatType statType) {
        return switch (statType) {
            case ENGINES -> totalEngines;
            case CANNONS -> totalCannons;
            case CREW -> totalCrew;
            case CARGO -> totalCargo;
            case BATTERIES -> totalBatteries;
            case SHIELDS -> totalShields;
        };
    }
    
    public Map<ComponentStatType, Integer> getAllShipStats() {
        Map<ComponentStatType, Integer> stats = new HashMap<>();
        for (ComponentStatType statType : ComponentStatType.values()) {
            stats.put(statType, getShipStats(statType));
        }
        return stats;
    }
    
    // Player management
    public void removePlayer(String playerId) {
        playerReadyStatus.remove(playerId);
    }
    
    public void setPlayerReady(String playerId, boolean ready) {
        playerReadyStatus.put(playerId, ready);
    }
    
    public boolean isPlayerReady(String playerId) {
        return playerReadyStatus.getOrDefault(playerId, false);
    }
    
    public Map<String, Boolean> getPlayerReadyStatus() {
        return new HashMap<>(playerReadyStatus);
    }
    
    // Other players' ship management
    public void updateOtherPlayerShip(String playerId, Map<Position, ComponentInstance> shipGrid) {
        if (playerId != null && !playerId.equals(localPlayerId)) {
            otherPlayersShips.put(playerId, new ConcurrentHashMap<>(shipGrid));
        }
    }
    
    public Map<Position, ComponentInstance> getOtherPlayerShip(String playerId) {
        Map<Position, ComponentInstance> ship = otherPlayersShips.get(playerId);
        return ship != null ? new HashMap<>(ship) : new HashMap<>();
    }
    
    public Map<String, Map<Position, ComponentInstance>> getAllOtherPlayersShips() {
        Map<String, Map<Position, ComponentInstance>> result = new HashMap<>();
        for (Map.Entry<String, Map<Position, ComponentInstance>> entry : otherPlayersShips.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return result;
    }
    
    public void removeOtherPlayer(String playerId) {
        playerReadyStatus.remove(playerId);
        otherPlayersShips.remove(playerId);
    }
    
    // Game lobby management
    public void addAvailableGame(GameInfo gameInfo) {
        availableGames.add(gameInfo);
    }
    
    public void removeAvailableGame(String gameId) {
        availableGames.removeIf(game -> game.getGameId().equals(gameId));
    }
    
    public List<GameInfo> getAvailableGames() {
        return new ArrayList<>(availableGames);
    }
    
    // Damage management
    public void damageShip(String playerId, int row, int col) {
        Position position = new Position(row, col);
        removeTile(position);
    }
    
    public void damageShip(Position position) {
        removeTile(position);
    }
    
    // Reset methods
    public void resetShipBuildingState() {
        // Clear 2D array with current dimensions
        if (shipGrid != null) {
            for (int row = 0; row < gridRows; row++) {
                for (int col = 0; col < gridCols; col++) {
                    shipGrid[row][col] = null;
                }
            }
        }
        availableTiles.clear();
        heldTiles.clear();
        buildingTimerFlipped = false;
        buildingTimeRemaining = 0;
        buildingPhaseStartTime = 0;
        shipValidated = false;
        validationErrors.clear();
        updateShipStatistics();
    }
    
    public void resetGameState() {
        resetShipBuildingState();
        currentPhase = GamePhase.SETUP;
        playerReadyStatus.clear();
        availableGames.clear();
    }
    
    // Private helper methods
    /**
     * Initializes forbidden positions. This will be overridden by server configuration.
     * Only used as fallback for client-side validation before server data arrives.
     */
    private void initializeForbiddenPositions() {
        // Clear any existing forbidden positions
        forbiddenPositions.clear();
        // Server will provide the actual forbidden positions for the game level
        // This method is kept for backward compatibility but should not be used
        // in production as all configuration comes from server
    }
    
    private boolean isValidGridPosition(int row, int col) {
        return row >= 0 && row < gridRows && col >= 0 && col < gridCols;
    }
    
    private void initializeShipGrid() {
        shipGrid = new ComponentInstance[gridRows][gridCols];
    }
    
    private void updateGridDimensions(int rows, int cols) {
        if (rows > 0 && cols > 0 && (rows != gridRows || cols != gridCols)) {
            gridRows = rows;
            gridCols = cols;
            
            // Recreate ship grid with new dimensions
            ComponentInstance[][] oldGrid = shipGrid;
            shipGrid = new ComponentInstance[gridRows][gridCols];
            
            // Copy existing components if any
            if (oldGrid != null) {
                int minRows = Math.min(gridRows, oldGrid.length);
                int minCols = Math.min(gridCols, oldGrid.length > 0 ? oldGrid[0].length : 0);
                for (int row = 0; row < minRows; row++) {
                    for (int col = 0; col < minCols; col++) {
                        shipGrid[row][col] = oldGrid[row][col];
                    }
                }
            }
        }
    }
    
    private void updateShipStatistics() {
        totalEngines = 0;
        totalCannons = 0;
        totalCrew = 0;
        totalCargo = 0;
        totalBatteries = 0;
        totalShields = 0;
        
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                ComponentInstance componentInstance = shipGrid[row][col];
                if (componentInstance == null) continue;
                
                ComponentType component = componentInstance.getType();
                switch (component) {
                    case ENGINE_SINGLE -> totalEngines += 1;
                    case ENGINE_DOUBLE -> totalEngines += 2;
                    case CANNON_SINGLE -> totalCannons += 1;
                    case CANNON_DOUBLE -> totalCannons += 2;
                    case CABIN, CABIN_START -> totalCrew += 1;
                    case CARGO_HOLD, CARGO_HOLD_SPECIAL -> totalCargo += 1;
                    case BATTERY -> totalBatteries += 1;
                    case SHIELD -> totalShields += 1;
                    case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> totalCrew += 1; // Life support counts as crew capacity
                    case STRUCTURAL -> { /* No stats contribution */ }
                }
            }
        }
    }
    
    // Helper methods
    
    /**
     * Finds a component by its ID in available tiles, held tiles, or face-up junkyard tiles.
     * @param componentId The component ID to search for
     * @return The ComponentInstance if found, null otherwise
     */
    private ComponentInstance findComponentById(String componentId) {
        // Check available tiles
        for (ComponentInstance component : availableTiles) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        
        // Check held tiles
        for (ComponentInstance component : heldTiles) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        
        // Check face-up junkyard tiles
        for (ComponentInstance component : faceUpJunkyardTiles) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        
        return null;
    }
    
    
    /**
     * Checks if the ship has any components placed.
     */
    private boolean hasAnyComponents() {
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (shipGrid[row][col] != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if a position has at least one adjacent component for connectivity.
     */
    private boolean hasAdjacentComponent(Position position) {
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        
        for (Direction direction : directions) {
            Position adjacent = position.offsetBy(direction);
            int adjRow = adjacent.getRow();
            int adjCol = adjacent.getCol();
            
            if (isValidGridPosition(adjRow, adjCol) && shipGrid[adjRow][adjCol] != null) {
                return true;
            }
        }
        
        return false;
    }

    // Nested enum for ship statistics
    public enum ComponentStatType {
        ENGINES,
        CANNONS,
        CREW,
        CARGO,
        BATTERIES,
        SHIELDS
    }
}
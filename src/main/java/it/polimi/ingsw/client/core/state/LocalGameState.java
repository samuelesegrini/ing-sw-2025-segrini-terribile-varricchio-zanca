package it.polimi.ingsw.client.core.state;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalGameState {
    private static LocalGameState instance = new LocalGameState();
    
    // Basic game state
    private String localPlayerId;
    private String currentGameId;
    private GamePhase currentPhase = GamePhase.SETUP;
    
    // Ship building state
    private final Map<Position, ComponentType> shipGrid = new ConcurrentHashMap<>();
    private final List<ComponentType> availableTiles = Collections.synchronizedList(new ArrayList<>());
    private final List<ComponentType> heldTiles = Collections.synchronizedList(new ArrayList<>());
    private final Set<Position> forbiddenPositions = new HashSet<>();
    private boolean buildingTimerFlipped = false;
    private long buildingTimeRemaining = 0;
    private long buildingPhaseStartTime = 0;
    private boolean shipValidated = false;
    private final List<String> validationErrors = Collections.synchronizedList(new ArrayList<>());
    
    // Player state
    private final Map<String, Boolean> playerReadyStatus = new ConcurrentHashMap<>();
    private final List<GameInfo> availableGames = Collections.synchronizedList(new ArrayList<>());
    
    // Ship statistics cache
    private int totalEngines = 0;
    private int totalCannons = 0;
    private int totalCrew = 0;
    private int totalCargo = 0;
    private int totalBatteries = 0;
    private int totalShields = 0;
    
    private LocalGameState() {
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
    public void placeTile(ComponentType component, Position position, int rotation) {
        if (component != null && position != null && !forbiddenPositions.contains(position)) {
            shipGrid.put(position, component);
            updateShipStatistics();
        }
    }
    
    public void placeTile(String playerId, String tileId, int row, int col, int rotation) {
        // Legacy method - convert to new format if needed
        Position position = new Position(row, col);
        ComponentType componentType = parseComponentType(tileId);
        if (componentType != null) {
            placeTile(componentType, position, rotation);
        }
    }
    
    public void removeTile(Position position) {
        if (position != null) {
            shipGrid.remove(position);
            updateShipStatistics();
        }
    }
    
    public ComponentType getComponentAt(Position position) {
        return shipGrid.get(position);
    }
    
    public ComponentType getComponentAt(int row, int col) {
        return getComponentAt(new Position(row, col));
    }
    
    public Map<Position, ComponentType> getShipGrid() {
        return new HashMap<>(shipGrid);
    }
    
    public boolean canPlaceComponent(ComponentType component, Position position) {
        if (component == null || position == null) {
            return false;
        }
        
        // Check if position is within bounds (5x7 grid)
        if (position.getRow() < 0 || position.getRow() >= 5 || 
            position.getCol() < 0 || position.getCol() >= 7) {
            return false;
        }
        
        // Check if position is forbidden
        if (forbiddenPositions.contains(position)) {
            return false;
        }
        
        // Check if position is already occupied
        return !shipGrid.containsKey(position);
    }
    
    public Set<Position> getForbiddenPositions() {
        return new HashSet<>(forbiddenPositions);
    }
    
    // Component inventory management
    public void addAvailableTile(ComponentType component) {
        if (component != null) {
            availableTiles.add(component);
        }
    }
    
    public void removeAvailableTile(ComponentType component) {
        availableTiles.remove(component);
    }
    
    public List<ComponentType> getAvailableTiles() {
        return new ArrayList<>(availableTiles);
    }
    
    public void addHeldTile(ComponentType component) {
        if (component != null && heldTiles.size() < 2) { // Max 2 reserved components per rules
            heldTiles.add(component);
        }
    }
    
    public void removeHeldTile(ComponentType component) {
        heldTiles.remove(component);
    }
    
    public List<ComponentType> getHeldTiles() {
        return new ArrayList<>(heldTiles);
    }
    
    public boolean canReserveMoreTiles() {
        return heldTiles.size() < 2;
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
        shipGrid.clear();
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
    private void initializeForbiddenPositions() {
        // Based on game rules - corner positions are forbidden in Test Flight
        forbiddenPositions.add(new Position(0, 0));
        forbiddenPositions.add(new Position(0, 6));
        forbiddenPositions.add(new Position(4, 0));
        forbiddenPositions.add(new Position(4, 6));
        // Starting cabin position (2,3) - cannot be changed
        forbiddenPositions.add(new Position(2, 3));
    }
    
    private void updateShipStatistics() {
        totalEngines = 0;
        totalCannons = 0;
        totalCrew = 0;
        totalCargo = 0;
        totalBatteries = 0;
        totalShields = 0;
        
        for (ComponentType component : shipGrid.values()) {
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
    
    private ComponentType parseComponentType(String tileId) {
        // Helper method to convert string IDs to ComponentType enum
        // This would need to be implemented based on how tile IDs are structured
        try {
            return ComponentType.valueOf(tileId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
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
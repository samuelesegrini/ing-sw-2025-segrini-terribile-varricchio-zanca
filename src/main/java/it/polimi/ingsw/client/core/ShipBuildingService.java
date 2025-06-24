package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.common.ComponentData;
import it.polimi.ingsw.common.message.request.*;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Map;

/**
 * Service class that connects the ship building logic to the UI.
 * Provides a clean interface for the GuiShipBuildingView to interact with the game state
 * and server communication.
 */
public class ShipBuildingService {
    private final ClientController controller;
    private final LocalGameState gameState;
    private final PropertyChangeSupport pcs;
    
    // UI-bindable properties
    private ComponentInstance currentHeldComponent;
    private boolean canTakeComponent = true;
    private boolean canReserveComponent = true;
    private long buildingTimeRemaining;
    private boolean timerActive;
    
    public ShipBuildingService(ClientController controller) {
        this.controller = controller;
        this.gameState = LocalGameState.getInstance();
        this.pcs = new PropertyChangeSupport(this);
        
        // Listen to game state changes
        setupGameStateListeners();
    }
    
    // Property change support for UI binding
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    // Component management methods
    
    /**
     * Takes a face-down component from the central pile
     */
    public void takeFaceDownComponent() {
        if (!canTakeComponent()) {
            return;
        }
        
        TakeTileRequest request = new TakeTileRequest();
        controller.sendRequest(request);
        
        // Temporarily disable taking components to prevent spam
        setCanTakeComponent(false);
    }
    
    /**
     * Takes a specific face-up component
     * @param componentInstance The face-up component to take
     */
    public void takeFaceUpComponent(ComponentInstance componentInstance) {
        if (!canTakeComponent() || componentInstance == null) {
            return;
        }
        
        RequestFaceUpTileRequest request = new RequestFaceUpTileRequest(componentInstance.getId());
        controller.sendRequest(request);
        
        setCanTakeComponent(false);
    }
    
    /**
     * Returns the currently held component to the face-up pile
     */
    public void returnHeldComponent() {
        if (currentHeldComponent == null) {
            return;
        }
        
        ReturnTileRequest request = new ReturnTileRequest(currentHeldComponent.getId());
        controller.sendRequest(request);
        
        setCurrentHeldComponent(null);
    }
    
    /**
     * Reserves the currently held component
     */
    public void reserveHeldComponent() {
        if (currentHeldComponent == null || !canReserveComponent()) {
            return;
        }
        
        // Component reservation is handled automatically by the server
        // Move component from held to reserved in local state
        gameState.removeHeldTile(currentHeldComponent);
        gameState.addHeldTile(currentHeldComponent); // This goes to reserved area
        
        setCurrentHeldComponent(null);
        updateReservationCapacity();
    }
    
    /**
     * Places the currently held component on the ship grid
     * @param position The position to place the component
     * @param rotation Number of 90-degree clockwise rotations
     */
    public void placeHeldComponent(Position position, int rotation) {
        if (currentHeldComponent == null || !canPlaceComponent(currentHeldComponent, position)) {
            return;
        }
        
        // Apply rotation to component
        ComponentInstance rotatedComponent = currentHeldComponent;
        for (int i = 0; i < rotation % 4; i++) {
            rotatedComponent.rotate();
        }
        
        PlaceTileRequest request = new PlaceTileRequest(
            rotatedComponent.getId(),
            position.getRow(),
            position.getCol(),
            rotation
        );
        controller.sendRequest(request);
        
        // Optimistically update local state
        gameState.placeTile(rotatedComponent, position, rotation);
        gameState.removeHeldTile(currentHeldComponent);
        
        setCurrentHeldComponent(null);
        firePropertyChange("shipGridUpdated", null, gameState.getShipGrid());
    }
    
    /**
     * Removes a component from the ship grid
     * @param position The position to remove the component from
     */
    public void removeComponentFromShip(Position position) {
        ComponentInstance component = gameState.getComponentAt(position);
        if (component == null) {
            return;
        }
        
        // Send request to server
        ReturnTileRequest request = new ReturnTileRequest(component.getId());
        controller.sendRequest(request);
        
        // Update local state
        gameState.removeTile(position);
        firePropertyChange("shipGridUpdated", null, gameState.getShipGrid());
    }
    
    /**
     * Validates ship and requests server validation
     */
    public void validateShip() {
        ValidateShipRequest request = new ValidateShipRequest();
        controller.sendRequest(request);
    }
    
    /**
     * Flips the building timer (Level II games only)
     */
    public void flipBuildingTimer() {
        if (!timerActive) {
            return;
        }
        
        FlipBuildingTimerRequest request = new FlipBuildingTimerRequest();
        controller.sendRequest(request);
    }
    
    // Validation methods
    
    /**
     * Checks if a component can be placed at the given position
     */
    public boolean canPlaceComponent(ComponentInstance component, Position position) {
        return gameState.canPlaceComponent(component, position);
    }
    
    /**
     * Checks if the player can take more components
     */
    public boolean canTakeComponent() {
        return canTakeComponent && gameState.getHeldTiles().isEmpty();
    }
    
    /**
     * Checks if the player can reserve more components
     */
    public boolean canReserveComponent() {
        return canReserveComponent && gameState.canReserveMoreTiles();
    }
    
    // State getters for UI binding
    
    public ComponentInstance getCurrentHeldComponent() {
        return currentHeldComponent;
    }
    
    public List<ComponentInstance> getAvailableTiles() {
        return gameState.getAvailableTiles();
    }
    
    public List<ComponentInstance> getFaceUpJunkyardTiles() {
        return gameState.getFaceUpJunkyardTiles();
    }
    
    public List<ComponentInstance> getReservedTiles() {
        return gameState.getHeldTiles(); // Reserved tiles are stored as held tiles
    }
    
    public ComponentInstance[][] getShipGrid() {
        return gameState.getShipGrid();
    }
    
    public long getBuildingTimeRemaining() {
        return buildingTimeRemaining;
    }
    
    public boolean isTimerActive() {
        return timerActive;
    }
    
    public Map<LocalGameState.ComponentStatType, Integer> getShipStats() {
        return gameState.getAllShipStats();
    }
    
    public List<String> getShipValidationErrors() {
        return gameState.getValidationErrors();
    }
    
    public boolean isShipValidated() {
        return gameState.isShipValidated();
    }
    
    // Internal methods
    
    private void setupGameStateListeners() {
        // Listen to model changes and update local properties
        if (controller.getModel() != null) {
            controller.getModel().addPropertyChangeListener(evt -> {
                switch (evt.getPropertyName()) {
                    case "heldTiles" -> updateCurrentHeldComponent();
                    case "buildingTimeRemaining" -> updateBuildingTimer();
                    case "availableTiles" -> firePropertyChange("availableTiles", evt.getOldValue(), evt.getNewValue());
                    case "shipGridUpdated" -> firePropertyChange("shipGrid", evt.getOldValue(), evt.getNewValue());
                    case "shipValidated" -> firePropertyChange("shipValidated", evt.getOldValue(), evt.getNewValue());
                }
            });
        }
    }
    
    private void updateCurrentHeldComponent() {
        List<ComponentInstance> heldTiles = gameState.getHeldTiles();
        ComponentInstance newHeld = heldTiles.isEmpty() ? null : heldTiles.get(0);
        
        if (newHeld != currentHeldComponent) {
            ComponentInstance oldHeld = currentHeldComponent;
            currentHeldComponent = newHeld;
            firePropertyChange("currentHeldComponent", oldHeld, newHeld);
            
            // Update can take component status
            setCanTakeComponent(newHeld == null);
        }
    }
    
    private void updateBuildingTimer() {
        long newTime = gameState.getBuildingTimeRemaining();
        boolean newActive = gameState.isBuildingTimerFlipped();
        
        if (newTime != buildingTimeRemaining) {
            long oldTime = buildingTimeRemaining;
            buildingTimeRemaining = newTime;
            firePropertyChange("buildingTimeRemaining", oldTime, newTime);
        }
        
        if (newActive != timerActive) {
            boolean oldActive = timerActive;
            timerActive = newActive;
            firePropertyChange("timerActive", oldActive, newActive);
        }
    }
    
    private void updateReservationCapacity() {
        boolean newCanReserve = gameState.canReserveMoreTiles();
        if (newCanReserve != canReserveComponent) {
            boolean oldCanReserve = canReserveComponent;
            canReserveComponent = newCanReserve;
            firePropertyChange("canReserveComponent", oldCanReserve, newCanReserve);
        }
    }
    
    private void setCurrentHeldComponent(ComponentInstance component) {
        ComponentInstance old = this.currentHeldComponent;
        this.currentHeldComponent = component;
        firePropertyChange("currentHeldComponent", old, component);
    }
    
    private void setCanTakeComponent(boolean canTake) {
        boolean old = this.canTakeComponent;
        this.canTakeComponent = canTake;
        firePropertyChange("canTakeComponent", old, canTake);
    }
    
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    /**
     * Creates a ComponentData object from server component information.
     * This is used when the server sends component data to the client.
     */
    public static ComponentData createComponentData(String id, ComponentType type, 
                                                   Map<Direction, ConnectorType> connectors) {
        return new ComponentData(id, type, connectors);
    }
    
    /**
     * Creates a ComponentData object with default direction.
     */
    public static ComponentData createComponentData(String id, ComponentType type, 
                                                   Map<Direction, ConnectorType> connectors, 
                                                   Direction defaultDirection) {
        return new ComponentData(id, type, connectors, defaultDirection);
    }
}
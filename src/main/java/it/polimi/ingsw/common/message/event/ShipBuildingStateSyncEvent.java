package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.common.ComponentData;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Event sent to a player to synchronize the full building phase state.
 * Used at the start of the building phase and on reconnection.
 */
public class ShipBuildingStateSyncEvent extends AbstractEvent {
    private final Map<Position, ComponentData> shipGrid;
    private final List<ComponentData> availableTiles;
    private final List<ComponentData> heldTiles;
    private final Set<Position> forbiddenPositions;
    private final long buildingTimeRemaining;
    private final boolean timerFlipped;

    public ShipBuildingStateSyncEvent(String gameId,
                                      String playerId,
                                      Map<Position, ComponentData> shipGrid,
                                      List<ComponentData> availableTiles,
                                      List<ComponentData> heldTiles,
                                      Set<Position> forbiddenPositions,
                                      long buildingTimeRemaining,
                                      boolean timerFlipped) {
        super(EventType.SHIP_BUILDING_STATE_SYNC, gameId, playerId);
        this.shipGrid = shipGrid;
        this.availableTiles = availableTiles;
        this.heldTiles = heldTiles;
        this.forbiddenPositions = forbiddenPositions;
        this.buildingTimeRemaining = buildingTimeRemaining;
        this.timerFlipped = timerFlipped;
    }

    public Map<Position, ComponentData> getShipGrid() {
        return shipGrid;
    }

    public List<ComponentData> getAvailableTiles() {
        return availableTiles;
    }

    public List<ComponentData> getHeldTiles() {
        return heldTiles;
    }

    public Set<Position> getForbiddenPositions() {
        return forbiddenPositions;
    }

    public long getBuildingTimeRemaining() {
        return buildingTimeRemaining;
    }

    public boolean isTimerFlipped() {
        return timerFlipped;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Get local game state instance
        var localState = it.polimi.ingsw.client.core.state.LocalGameState.getInstance();
        
        // Sync ship grid - create ComponentInstance from complete server data
        localState.resetShipBuildingState();
        for (Map.Entry<Position, ComponentData> entry : shipGrid.entrySet()) {
            ComponentData componentData = entry.getValue();
            ComponentInstance component = new ComponentInstance(
                componentData.getId(),
                componentData.getType(),
                componentData.getConnectors()
            );
            component.setDirection(componentData.getDefaultDirection());
            localState.placeTile(component, entry.getKey(), 0);
        }
        
        // Sync available tiles - create ComponentInstance from complete server data
        for (ComponentData componentData : availableTiles) {
            localState.addAvailableTile(
                componentData.getId(),
                componentData.getType(),
                componentData.getConnectors()
            );
        }
        
        // Sync held tiles - create ComponentInstance from complete server data
        for (ComponentData componentData : heldTiles) {
            localState.addHeldTile(
                componentData.getId(),
                componentData.getType(),
                componentData.getConnectors()
            );
        }
        
        // Sync timer state
        localState.updateBuildingTimer(buildingTimeRemaining);
        localState.setBuildingTimerFlipped(timerFlipped);
        
        // Sync forbidden positions (only if server provides explicit positions)
        System.out.println("=== SHIP BUILDING STATE SYNC FORBIDDEN POSITIONS ===");
        System.out.println("ShipBuildingStateSyncEvent forbidden positions: " + forbiddenPositions);
        if (forbiddenPositions != null && !forbiddenPositions.isEmpty()) {
            System.out.println("Setting " + forbiddenPositions.size() + " forbidden positions from ShipBuildingStateSyncEvent:");
            for (Position pos : forbiddenPositions) {
                System.out.println("  - Position(" + pos.getRow() + ", " + pos.getCol() + ")");
            }
            localState.setForbiddenPositions(forbiddenPositions);
            // Trigger UI refresh to show forbidden squares
            context.getController().getModel().firePropertyChange("shipGridConfig", null, null);
        } else {
            System.out.println("No forbidden positions in ShipBuildingStateSyncEvent - keeping existing positions");
        }
        System.out.println("=== END SHIP BUILDING STATE SYNC FORBIDDEN POSITIONS ===");
        // If server forbidden positions are empty/null, keep the ones extracted from ShipGridConfig
        
        // Update UI if available
        if (context.getNotificationService() != null) {
            context.getNotificationService().showNotification(
                new it.polimi.ingsw.client.ui.Notification(
                    "State Sync",
                    "Building state synchronized",
                    it.polimi.ingsw.client.ui.NotificationType.INFO
                )
            );
        }
        
        // Log for debugging
        System.out.println("Synchronized building state: " + 
            shipGrid.size() + " components, " + 
            availableTiles.size() + " available tiles, " + 
            heldTiles.size() + " held tiles");
    }
} 
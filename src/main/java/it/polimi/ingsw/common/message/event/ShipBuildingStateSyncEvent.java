package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Event sent to a player to synchronize the full building phase state.
 * Used at the start of the building phase and on reconnection.
 */
public class ShipBuildingStateSyncEvent extends AbstractEvent {
    private final Map<Position, ComponentType> shipGrid;
    private final List<ComponentType> availableTiles;
    private final List<ComponentType> heldTiles;
    private final Set<Position> forbiddenPositions;
    private final long buildingTimeRemaining;
    private final boolean timerFlipped;

    public ShipBuildingStateSyncEvent(String gameId,
                                      String playerId,
                                      Map<Position, ComponentType> shipGrid,
                                      List<ComponentType> availableTiles,
                                      List<ComponentType> heldTiles,
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

    public Map<Position, ComponentType> getShipGrid() {
        return shipGrid;
    }

    public List<ComponentType> getAvailableTiles() {
        return availableTiles;
    }

    public List<ComponentType> getHeldTiles() {
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
        // Implementation to be added on the client side
    }
} 
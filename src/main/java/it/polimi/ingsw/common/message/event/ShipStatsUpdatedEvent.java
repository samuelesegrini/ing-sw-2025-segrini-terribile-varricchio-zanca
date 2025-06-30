package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;

import java.util.logging.Logger;

/**
 * Event broadcast when a ship's stats are recalculated.
 * Updates the UI to show current ship capabilities.
 */
public class ShipStatsUpdatedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ShipStatsUpdatedEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final Ship ship;
    private final int engines;
    private final int cannons;
    private final int crew;
    private final int shields;
    private final int cargoHolds;
    private final int batteries;
    private final boolean structurallyValid;

    public ShipStatsUpdatedEvent(String gameId, PlayerId playerId, String playerNickname, Ship ship) {
        super(EventType.SHIP_STATS_UPDATED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.ship = ship;
        
        // Capture current stats
        this.engines = (int) ship.getEngines();
        this.cannons = (int) ship.getCannons();
        this.crew = ship.getCrew();
        this.shields = ship.getShields();
        this.cargoHolds = ship.getCargoHolds();
        this.batteries = ship.getBatteries();
        this.structurallyValid = ship.isStructurallyValid();
        
        LOGGER.fine("ShipStatsUpdatedEvent created for player: " + playerNickname + 
                   ", engines: " + engines + ", cannons: " + cannons + ", crew: " + crew);
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public Ship getShip() {
        return ship;
    }

    public int getEngines() {
        return engines;
    }

    public int getCannons() {
        return cannons;
    }

    public int getCrew() {
        return crew;
    }

    public int getShields() {
        return shields;
    }

    public int getCargoHolds() {
        return cargoHolds;
    }

    public int getBatteries() {
        return batteries;
    }

    public boolean isStructurallyValid() {
        return structurallyValid;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all players in the game to show ship stats
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Update ship stats
        clientState.updateShipStats(playerId.toString(), ship);
        clientState.incrementStateVersion();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling ShipStatsUpdatedEvent for player: " + playerNickname + 
                       ", engines: " + engines + ", crew: " + crew);
            
            // UI updates will be handled by the refreshCurrentViewOnly() call in updateClientState
        });
    }
}
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.common.ComponentData;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.client.ClientModel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

/**
 * Broadcast to all players in a game when the lobby is full and the game starts.
 * Signals the transition to the Ship Building phase.
 */
public class GameStartedEvent extends AbstractEvent {
    private final List<PlayerInfo> players;
    private final ShipGridConfig shipGridConfig; // Provides board layout info
    private final Map<String, Map<Position, ComponentData>> playerShipGrids; // playerId -> shipGrid (Position->ComponentData)
    private final Map<String, List<ComponentData>> playerAvailableTiles; // playerId -> availableTiles
    private final Map<String, List<ComponentData>> playerHeldTiles; // playerId -> heldTiles
    private final Map<String, Set<Position>> playerForbiddenPositions; // playerId -> forbiddenPositions
    private final Map<String, Long> playerBuildingTimeRemaining; // playerId -> time
    private final Map<String, Boolean> playerTimerFlipped; // playerId -> timer flipped

    public GameStartedEvent(String gameId, List<PlayerInfo> players, ShipGridConfig shipGridConfig,
                           Map<String, Map<Position, ComponentData>> playerShipGrids,
                           Map<String, List<ComponentData>> playerAvailableTiles,
                           Map<String, List<ComponentData>> playerHeldTiles,
                           Map<String, Set<Position>> playerForbiddenPositions,
                           Map<String, Long> playerBuildingTimeRemaining,
                           Map<String, Boolean> playerTimerFlipped) {
        super(EventType.GAME_STARTED, gameId, null);
        this.players = List.copyOf(players);
        this.shipGridConfig = shipGridConfig;
        this.playerShipGrids = playerShipGrids;
        this.playerAvailableTiles = playerAvailableTiles;
        this.playerHeldTiles = playerHeldTiles;
        this.playerForbiddenPositions = playerForbiddenPositions;
        this.playerBuildingTimeRemaining = playerBuildingTimeRemaining;
        this.playerTimerFlipped = playerTimerFlipped;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public ShipGridConfig getShipGridConfig() {
        return shipGridConfig;
    }

    public Map<String, Map<Position, ComponentData>> getPlayerShipGrids() { return playerShipGrids; }
    public Map<String, List<ComponentData>> getPlayerAvailableTiles() { return playerAvailableTiles; }
    public Map<String, List<ComponentData>> getPlayerHeldTiles() { return playerHeldTiles; }
    public Map<String, Set<Position>> getPlayerForbiddenPositions() { return playerForbiddenPositions; }
    public Map<String, Long> getPlayerBuildingTimeRemaining() { return playerBuildingTimeRemaining; }
    public Map<String, Boolean> getPlayerTimerFlipped() { return playerTimerFlipped; }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update model state to reflect game start and begin building phase
            if (context.getController() != null && context.getController().getModel() != null) {
                ClientModel model = context.getController().getModel();
                model.setPlayersInLobby(players);
                model.setCurrentView(ClientModel.ViewState.GAME);
            }

            // Update local game state for the building phase
            String localPlayerId = context.getController() != null ? context.getController().getPlayerId() : null;
            if (localPlayerId != null) {
                LocalGameState gameState = context.getGameState();
                gameState.resetShipBuildingState();
                gameState.setCurrentPhase(GamePhase.BUILDING);

                // Set ship grid configuration from server
                gameState.setShipGridConfig(shipGridConfig);

                // Set ship grid from server data
                Map<Position, ComponentData> gridMap = playerShipGrids.get(localPlayerId);
                if (gridMap != null) {
                    for (Map.Entry<Position, ComponentData> entry : gridMap.entrySet()) {
                        ComponentData componentData = entry.getValue();
                        ComponentInstance component = new ComponentInstance(
                            componentData.getId(),
                            componentData.getType(),
                            componentData.getConnectors()
                        );
                        component.setDirection(componentData.getDefaultDirection());
                        gameState.placeTile(component, entry.getKey(), 0);
                    }
                }

                // Set available tiles with complete component data
                List<ComponentData> available = playerAvailableTiles.get(localPlayerId);
                if (available != null) {
                    for (ComponentData componentData : available) {
                        gameState.addAvailableTile(
                            componentData.getId(),
                            componentData.getType(),
                            componentData.getConnectors()
                        );
                    }
                }

                // Set held tiles with complete component data
                List<ComponentData> held = playerHeldTiles.get(localPlayerId);
                if (held != null) {
                    for (ComponentData componentData : held) {
                        gameState.addHeldTile(
                            componentData.getId(),
                            componentData.getType(),
                            componentData.getConnectors()
                        );
                    }
                }

                // Set forbidden positions from server configuration
                Set<Position> forbidden = playerForbiddenPositions.get(localPlayerId);
                if (forbidden != null) {
                    gameState.setForbiddenPositions(forbidden);
                }

                // Set timer and timer flipped
                Long time = playerBuildingTimeRemaining.get(localPlayerId);
                if (time != null) {
                    gameState.updateBuildingTimer(time);
                }
                Boolean flipped = playerTimerFlipped.get(localPlayerId);
                if (flipped != null) {
                    gameState.setBuildingTimerFlipped(flipped);
                }
            }

            // Show notification about game start
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                                "Game Started",
                                "The building phase has begun! Build your ship before time runs out.",
                                NotificationType.INFO
                        )
                );
            }

            // Fire property change for UI updates
            if (context.getController() != null && context.getController().getModel() != null) {
                context.getController().getModel().firePropertyChange("gameStarted", false, true);
                context.getController().getModel().firePropertyChange("shipGridConfig", null, shipGridConfig);
                context.getController().getModel().firePropertyChange("shipGridUpdated", null, null);
                context.getController().getModel().firePropertyChange("buildingTimeRemaining", null, null);
                context.getController().getModel().firePropertyChange("buildingTimerFlipped", null, null);
                context.getController().getModel().firePropertyChange("availableTiles", null, null);
                context.getController().getModel().firePropertyChange("heldTiles", null, null);
            }
        });
    }
}
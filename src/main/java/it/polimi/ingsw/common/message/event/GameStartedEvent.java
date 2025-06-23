package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.client.ClientModel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.polimi.ingsw.client.core.state.LocalGameState;

/**
 * Broadcast to all players in a game when the lobby is full and the game starts.
 * Signals the transition to the Ship Building phase.
 */
public class GameStartedEvent extends AbstractEvent {
    private final List<PlayerInfo> players;
    private final ShipGridConfig shipGridConfig; // Provides board layout info
    private final Map<String, Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerShipGrids; // playerId -> shipGrid (Position->ComponentType)
    private final Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerAvailableTiles; // playerId -> availableTiles
    private final Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerHeldTiles; // playerId -> heldTiles
    private final Map<String, Set<it.polimi.ingsw.server.model.domain.ship.Position>> playerForbiddenPositions; // playerId -> forbiddenPositions
    private final Map<String, Long> playerBuildingTimeRemaining; // playerId -> time
    private final Map<String, Boolean> playerTimerFlipped; // playerId -> timer flipped

    public GameStartedEvent(String gameId, List<PlayerInfo> players, ShipGridConfig shipGridConfig,
                           Map<String, Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerShipGrids,
                           Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerAvailableTiles,
                           Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerHeldTiles,
                           Map<String, Set<it.polimi.ingsw.server.model.domain.ship.Position>> playerForbiddenPositions,
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

    public Map<String, Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType>> getPlayerShipGrids() { return playerShipGrids; }
    public Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> getPlayerAvailableTiles() { return playerAvailableTiles; }
    public Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> getPlayerHeldTiles() { return playerHeldTiles; }
    public Map<String, Set<it.polimi.ingsw.server.model.domain.ship.Position>> getPlayerForbiddenPositions() { return playerForbiddenPositions; }
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
                gameState.setCurrentPhase(it.polimi.ingsw.server.model.enums.GamePhase.BUILDING);

                // Set ship grid
                Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType> grid =
                        playerShipGrids.get(localPlayerId);
                if (grid != null) {
                    for (Map.Entry<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType> entry : grid.entrySet()) {
                        gameState.placeTile(entry.getValue(), entry.getKey(), 0);
                    }
                }

                // Set available tiles
                List<it.polimi.ingsw.server.model.enums.ship.ComponentType> available = playerAvailableTiles.get(localPlayerId);
                if (available != null) {
                    for (it.polimi.ingsw.server.model.enums.ship.ComponentType comp : available) {
                        gameState.addAvailableTile(comp);
                    }
                }

                // Set held tiles
                List<it.polimi.ingsw.server.model.enums.ship.ComponentType> held = playerHeldTiles.get(localPlayerId);
                if (held != null) {
                    for (it.polimi.ingsw.server.model.enums.ship.ComponentType comp : held) {
                        gameState.addHeldTile(comp);
                    }
                }

                // Set forbidden positions
                Set<it.polimi.ingsw.server.model.domain.ship.Position> forbidden = playerForbiddenPositions.get(localPlayerId);
                if (forbidden != null) {
                    gameState.getForbiddenPositions().clear();
                    gameState.getForbiddenPositions().addAll(forbidden);
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
                                it.polimi.ingsw.client.ui.NotificationType.INFO
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
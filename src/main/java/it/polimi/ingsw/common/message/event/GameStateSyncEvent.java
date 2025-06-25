package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.general.GameModel;

/**
 * Event broadcast periodically to synchronize client state with server.
 * Contains the complete authoritative game state.
 */
public class GameStateSyncEvent extends AbstractEvent {
    private final GameModel gameModel;
    private final long stateVersion;

    public GameStateSyncEvent(String gameId, GameModel gameModel, long stateVersion) {
        super(EventType.GAME_STATE_SYNC, gameId, null);
        this.gameModel = gameModel;
        this.stateVersion = stateVersion;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // SIMPLIFIED: Direct model replacement for state synchronization
            
            // NEW: Simple direct GameModel usage via ClientState
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
                // State version tracking can be added to ClientState if needed
            } else {
                // LEGACY: Fallback to LocalGameState
                if (context.getClientState() != null) {
                    // Note: syncWithGameModel and setStateVersion methods don't exist in current LocalGameState
                    // This is legacy code that needs to be replaced
                }
            }

            // SIMPLIFIED: Single property change notification
            if (context.getController() != null && context.getController().getClientState() != null) {
                context.getController().getClientState().firePropertyChange("gameStateSync", null, gameModel);
            }
        });
    }
}
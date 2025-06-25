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
            // Simple direct GameModel replacement for state synchronization
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
                // UI refreshes automatically via ClientState.refreshCurrentView()
                // State version tracking can be added to ClientState if needed for optimization
            }
        });
    }
}
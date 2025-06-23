package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ClientModel;

/**
 * Event broadcast when a player's ready status changes in the game lobby.
 * Updates the lobby UI to show which players are ready to start the game.
 */
public class PlayerReadyChangedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final boolean ready;

    public PlayerReadyChangedEvent(String gameId, String playerId, String playerNickname, boolean ready) {
        super(EventType.PLAYER_READY_CHANGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.ready = ready;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client (they already have the response)
        String playerId = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerId)) {
            return false;
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update model state if this is the current game
            if (context.getController() != null && context.getController().getModel() != null) {
                ClientModel model = context.getController().getModel();
                if (gameId.equals(model.getCurrentGameId())) {
                    // Update player ready status in the model
                    model.setPlayerReadyStatus(playerId, ready);
                }
            }

            // Show notification for other players (not the one who changed status)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                String message = ready 
                    ? playerNickname + " is ready to start"
                    : playerNickname + " is no longer ready";
                    
                context.getNotificationService().showInfo(
                        "Player Status",
                        message
                );
            }

            // Update lobby UI if available
            // if (context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().updatePlayerReadyStatus(playerId, ready);
            //     // context.getGameUI().refreshLobbyView();
            // }
        });
    }
}
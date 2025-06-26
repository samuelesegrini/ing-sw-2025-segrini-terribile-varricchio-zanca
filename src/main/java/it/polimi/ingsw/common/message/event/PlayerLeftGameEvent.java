package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a player leaves a game lobby or active game.
 * Updates the player list and notifies remaining players.
 */
public class PlayerLeftGameEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerLeftGameEvent.class.getName());
    private final PlayerId playerId;
    private final String playerNickname;

    public PlayerLeftGameEvent(String gameId, PlayerId playerId, String playerNickname) {
        super(EventType.PLAYER_LEFT_GAME, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        LOGGER.fine("PlayerLeftGameEvent instantiated for game: " + gameId + ", player: " + playerNickname);
    }
    

    public PlayerId getPlayerId() {
        return playerId;
    }
    

    public String getPlayerNickname() {
        return playerNickname;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the leaving player (they already have the response)
        PlayerId playerId = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerId)) {
            LOGGER.finer("EVENT FILTERING - PlayerLeftGameEvent NOT sent to leaving player: " + clientId);
            return false;
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            ClientState clientState = context.getClientState();
            
            // Update lobby state by removing the leaving player
            if (clientState != null && clientState.getCurrentGameLobby() != null) {
                // Remove player from current game lobby
                try {
                    it.polimi.ingsw.server.model.domain.general.GameModel currentGame = clientState.getCurrentGameLobby();
                    PlayerId leavingPlayerId = playerId;
                    
                    // Remove player from the game model
                    boolean removed = currentGame.removePlayer(leavingPlayerId);
                    if (removed) {
                        // Trigger UI refresh after player removal
                        clientState.setCurrentGameLobby(currentGame);
                        LOGGER.fine("Removed player " + playerNickname + " from lobby");
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to remove player from lobby: " + e.getMessage());
                }
            }
            
            // If this is the local player leaving, should navigate back to lobby
            if (context.isLocalPlayer(playerId)) {
                // Local player left the game - navigate back to LOBBY
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.LOBBY, "Left game: " + getGameId());
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.LOBBY);
                        LOGGER.severe("Failed to navigate to LOBBY after leaving game - Reason: " + reason);
                    }
                } else {
                    LOGGER.severe("ViewNavigator not available - cannot navigate to LOBBY after leaving game");
                }
                
                // Clear current game lobby for local player
                clientState.setCurrentGameLobby(null);
            } else {
                // Only show notification for remaining players (not the leaving player)
                if (context.getNotificationService() != null) {
                    context.getNotificationService().showWarning(
                            "Player Left",
                            playerNickname + " left the game"
                    );
                }
            }
        });
    }
}
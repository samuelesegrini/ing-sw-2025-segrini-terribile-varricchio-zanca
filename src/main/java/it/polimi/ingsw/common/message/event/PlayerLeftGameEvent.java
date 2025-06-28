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
        // Send to ALL players, including the leaving player (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - PlayerLeftGameEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including leaving player)");
        return shouldSend;
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
            
            // Handle state updates for ALL players - this is the single source of truth
            boolean isLocalPlayer = context.isLocalPlayer(playerId);
            
            if (isLocalPlayer) {
                // Local player left the game - clear state and navigate back to lobby
                if (clientState != null) {
                    clientState.setGameModel(null);
                    clientState.setPlayersInLobby(new java.util.ArrayList<>());
                    clientState.setCurrentGameLobby(null);
                }
                
                // Navigate back to LOBBY
                if (context.getController() != null && 
                    context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.LOBBY, "Left game successfully");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.LOBBY);
                        LOGGER.severe("Failed to navigate to LOBBY after leaving game - Reason: " + reason);
                        // Fallback navigation
                        if (clientState != null) {
                            clientState.setCurrentView(ClientState.ViewState.LOBBY);
                        }
                    }
                } else {
                    LOGGER.severe("ViewNavigator not available - using direct navigation fallback");
                    if (clientState != null) {
                        clientState.setCurrentView(ClientState.ViewState.LOBBY);
                    }
                }
            }
            
            // Show notification for all players
            if (context.getNotificationService() != null) {
                String message;
                if (isLocalPlayer) {
                    message = "You have left the game";
                    LOGGER.fine("Displaying 'Left Game' notification for local player");
                } else {
                    message = playerNickname + " left the game";
                    LOGGER.fine("Displaying 'Player Left' notification for other player: " + message);
                }
                
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        isLocalPlayer ? "Left Game" : "Player Left",
                        message,
                        isLocalPlayer ? it.polimi.ingsw.client.ui.NotificationType.INFO : it.polimi.ingsw.client.ui.NotificationType.WARNING
                    )
                );
            }
        });
    }
}
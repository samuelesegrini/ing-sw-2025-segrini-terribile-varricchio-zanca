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

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param playerNickname the player nickname
     */

    public PlayerLeftGameEvent(String gameId, PlayerId playerId, String playerNickname) {
        super(EventType.PLAYER_LEFT_GAME, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        LOGGER.fine("PlayerLeftGameEvent instantiated for game: " + gameId + ", player: " + playerNickname);
    }

    /**
     *
     * @return  the player ID
     */

    public PlayerId getPlayerId() {
        return playerId;
    }

    /**
     *
     * @return the player nickname
     */
    

    public String getPlayerNickname() {
        return playerNickname;
    }

    /**
     *
     * @param clientId The client to check
     * @param context The filter context
     *
     */

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to ALL players, including the leaving player (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - PlayerLeftGameEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including leaving player)");
        return shouldSend;
    }

    /**
     *
     * @param clientState The client state to update
     */

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Update lobby state by removing the leaving player
        if (clientState.getCurrentGameLobby() != null) {
            try {
                it.polimi.ingsw.server.model.domain.general.GameModel currentGame = clientState.getCurrentGameLobby();
                // Remove player from the game model
                boolean removed = currentGame.removePlayer(playerId);
                if (removed) {
                    clientState.setCurrentGameLobby(currentGame);
                    clientState.incrementStateVersion();
                    LOGGER.fine("Removed player " + playerNickname + " from lobby");
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to remove player from lobby: " + e.getMessage());
            }
        }
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        context.runOnUIThread(() -> {
            ClientState clientState = context.getClientState();
            boolean isLocalPlayer = context.isLocalPlayer(playerId);
            
            // Then handle UI updates
            context.getController().getUI().onPlayerLeftGameEvent(this);
            
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
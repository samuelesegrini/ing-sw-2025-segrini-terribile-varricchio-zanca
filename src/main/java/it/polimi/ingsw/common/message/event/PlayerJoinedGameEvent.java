package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event broadcast when a player joins a game lobby.
 * Updates the lobby UI and notifies other players.
 * Contains the full GameModel as the single source of truth for state updates.
 */
public class PlayerJoinedGameEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerJoinedGameEvent.class.getName());
    private final String gameId;
    private final PlayerId playerId;
    private final String playerNickname;
    private final int currentPlayerCount;
    private final GameModel gameModel;

    public PlayerJoinedGameEvent(String gameId, PlayerId playerId, String playerNickname,
                                 int currentPlayerCount, GameModel gameModel) {
        super(EventType.PLAYER_JOINED_GAME, gameId, playerId);
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.currentPlayerCount = currentPlayerCount;
        
        this.gameModel = gameModel;
        
        LOGGER.fine("PlayerJoinedGameEvent instantiated for game: " + gameId + ", player: " + playerNickname);
    }

    

    public PlayerId getPlayerId() {
        return playerId;
    }
    

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Event updates client state with complete game lobby model
        if (gameModel != null) {
            clientState.updateGameLobbyState(gameModel);
            clientState.setPlayersInLobby(gameModel.getPlayers());
            clientState.incrementStateVersion();
        }
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        // Then handle UI updates
        context.runOnUIThread(() -> {
            ClientState clientState = context.getClientState();
            
            // Handle navigation for local player
            if (context.isLocalPlayer(playerId) && gameModel != null) {
                // Navigate to GAME_LOBBY
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.GAME_LOBBY, "Joined game: " + gameModel.getGameName());
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.GAME_LOBBY);
                        LOGGER.severe("Failed to navigate to GAME_LOBBY after joining game: " + reason);
                    }
                }
            }
            
            // Direct UI notification via newUI
            context.getNewUI().onPlayerJoinedGameEvent(this);
            
            // Show notifications
            if (context.getNotificationService() != null) {
                if (context.isLocalPlayer(playerId)) {
                    context.getNotificationService().showNotification(new Notification(
                            "Joined Game",
                            "Successfully joined " + (gameModel != null ? gameModel.getGameName() : "game"),
                            NotificationType.SUCCESS
                    ));
                } else {
                    context.getNotificationService().showNotification(new Notification(
                            "Player Joined",
                            playerNickname + " joined the game (" + currentPlayerCount + " players)",
                            NotificationType.INFO
                    ));
                }
            }
        });
    }
}

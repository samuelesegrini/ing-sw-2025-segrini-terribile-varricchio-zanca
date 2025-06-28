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
    public void handleOnClient(ClientEventContext context) {
        ClientState clientState = context.getClientState();
        if (gameId != null && clientState != null) {
            LOGGER.fine("PlayerJoined: " + playerNickname + " -> " + gameId + " (" + currentPlayerCount + " players)");
        }
        
        context.runOnUIThread(() -> {
            // Update client state with the full GameModel if available (single source of truth)
            if (clientState != null && gameModel != null) {
                // For the requesting player, set the full lobby state and navigate
                if (context.isLocalPlayer(playerId)) {
                    clientState.setCurrentGameLobby(gameModel);
                    clientState.setPlayersInLobby(gameModel.getPlayers());
                    
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
                } else if (gameId.equals(clientState.getCurrentGameId())) {
                    // For other players in the same lobby, update the lobby state
                    clientState.setCurrentGameLobby(gameModel);
                    clientState.setPlayersInLobby(gameModel.getPlayers());
                }
            }
            
            // Show notifications
            if (context.isLocalPlayer(playerId)) {
                // Success notification for joining player
                if (context.getNotificationService() != null) {
                    context.getNotificationService().showNotification(new Notification(
                            "Joined Game",
                            "Successfully joined " + (gameModel != null ? gameModel.getGameName() : "game"),
                            NotificationType.SUCCESS
                    ));
                }
            } else if (context.getNotificationService() != null) {
                // Info notification for other players
                context.getNotificationService().showNotification(new Notification(
                        "Player Joined",
                        playerNickname + " joined the game (" + currentPlayerCount + " players)",
                        NotificationType.INFO
                ));
            }
        });
    }
}

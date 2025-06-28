package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event broadcast when a game is created.
 * Contains the full GameModel as the single source of truth for state updates.
 */
public class GameCreatedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameCreatedEvent.class.getName());
    private final String gameId;
    private final PlayerId creatorId;
    private final String creatorNickname;
    private final int maxPlayers;
    private final GameLevel gameLevel;
    private final String gameName;
    private final GameModel gameModel;

    public GameCreatedEvent(String gameId, PlayerId creatorId, String creatorNickname,
                            int maxPlayers, GameLevel gameLevel, String gameName, GameModel gameModel) {
        super(EventType.GAME_CREATED, null, creatorId);
        this.gameId = gameId;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
        this.gameName = gameName;
        this.gameModel = gameModel;
        LOGGER.fine("GameCreatedEvent instantiated for game: " + gameId + " by " + creatorNickname);
    }
    
    // Legacy constructor for backward compatibility
    public GameCreatedEvent(String gameId, PlayerId creatorId, String creatorNickname,
                            int maxPlayers, GameLevel gameLevel, String gameName) {
        this(gameId, creatorId, creatorNickname, maxPlayers, gameLevel, gameName, null);
    }
    

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.info("GameCreatedEvent received on client. Creator ID: " + creatorId + ". Is this the local player? " + context.isLocalPlayer(creatorId));

        context.runOnUIThread(() -> {
            // Update client state with the full GameModel if available (single source of truth)
            if (context.getClientState() != null && gameModel != null && context.isLocalPlayer(creatorId)) {
                // For the creator, set the full lobby state and navigate
                context.getClientState().setCurrentGameLobby(gameModel);
                context.getClientState().setPlayersInLobby(gameModel.getPlayers());
                
                // Navigate to GAME_LOBBY
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.GAME_LOBBY, "Game created: " + gameName);
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.GAME_LOBBY);
                        LOGGER.severe("Failed to navigate to GAME_LOBBY after creating game: " + reason);
                    }
                }
            }
            
            // Show notifications
            if (context.getNotificationService() != null) {
                if (context.isLocalPlayer(creatorId)) {
                    // Success notification for creator
                    context.getNotificationService().showNotification(new Notification(
                            "Game Created",
                            "Successfully created game: " + gameName,
                            NotificationType.SUCCESS
                    ));
                } else {
                    // Info notification for other players
                    String gameDesc = gameName != null ? "'" + gameName + "'" : "a new game";
                    context.getNotificationService().showNotification(new Notification(
                            "Game Created",
                            creatorNickname + " created " + gameDesc,
                            NotificationType.INFO
                    ));
                }
            }
        });
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        PlayerId playerId = context.getPlayerIdForClient(clientId);
        if (playerId == null) {
            return false;
        }

        // Always send to the creator of the game
        if (this.creatorId.equals(playerId)) {
            return true;
        }

        // For other players in the lobby, always send the notification
        // Since this is a global lobby event, send to all authenticated clients
        return true;
    }

    public String getGameName() {
        return  gameName;
    }
    public PlayerId getCreatorId() {
        return creatorId;
    }
    
    public String getCreatorNickname() {
        return  creatorNickname;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public GameLevel getGameLevel() {
        return gameLevel;
    }

    @Override
    public String getGameId() {
        // Return null so it gets sent to ALL clients, not just clients in this game
        return null;
    }
    
    public String getCreatedGameId() {
        return gameId;
    }
}
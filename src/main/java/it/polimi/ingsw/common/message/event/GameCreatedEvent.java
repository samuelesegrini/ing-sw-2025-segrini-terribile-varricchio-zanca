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
 */
public class GameCreatedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameCreatedEvent.class.getName());
    private final String gameId;
    private final PlayerId creatorId;
    private final String creatorNickname;
    private final int maxPlayers;
    private final GameLevel gameLevel;
    private final String gameName;

    public GameCreatedEvent(String gameId, PlayerId creatorId, String creatorNickname,
                            int maxPlayers, GameLevel gameLevel, String gameName) {
        super(EventType.GAME_CREATED, null, creatorId);
        this.gameId = gameId;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
        this.gameName = gameName;
        LOGGER.fine("GameCreatedEvent instantiated for game: " + gameId + " by " + creatorNickname);
    }
    

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.info("GameCreatedEvent received on client. Creator ID: " + creatorId + ". Is this the local player? " + context.isLocalPlayer(creatorId));

        // Create game info for the new game  
        Player creatorInfo = new Player(creatorId);
        creatorInfo.setReady(true);
        
        // Note: GameModel constructor requires (GameLevel, GameConfigurationManager, int maxPlayers)
        // We can't create a full GameModel here, so we'll update the ClientState differently

        // Notify all players about the new game creation (but don't handle creator navigation)
        // Creator navigation is handled by CreateGameResponse
        context.runOnUIThread(() -> {
            if (context.getNotificationService() != null) {
                if (context.isLocalPlayer(creatorId)) {
                    // For creator: just a confirmation that the event was received
                    // (CreateGameResponse already handled the navigation)
                    LOGGER.info("GameCreatedEvent received for creator - CreateGameResponse should have handled navigation");
                } else {
                    // For other players: show notification about the new game
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
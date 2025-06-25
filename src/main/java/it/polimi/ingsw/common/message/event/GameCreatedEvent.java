package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
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
    private final String creatorId;
    private final String creatorNickname;
    private final int maxPlayers;
    private final GameLevel gameLevel;
    private final String gameName;

    public GameCreatedEvent(String gameId, String creatorId, String creatorNickname,
                            int maxPlayers, GameLevel gameLevel, String gameName) {
        super(EventType.GAME_CREATED, null, creatorId);
        this.gameId = gameId;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
        this.gameName = gameName;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.log(Level.SEVERE, "GameCreatedEvent received on client. Creator ID: " + creatorId + ". Is this the local player? " + context.isLocalPlayer(creatorId));

        // Create game info for the new game
        Player creatorInfo = new Player(creatorId, creatorNickname, true);
        GameModel gameInfo = new GameModel(gameId, gameName, creatorId, maxPlayers, 1, gameLevel,
                GamePhase.SETUP, Collections.singletonList(creatorInfo));

        // Add game to available games list for all recipients
        context.getController().getModel().addAvailableGame(gameInfo);

        // If this client is the creator, transition to game lobby
        if (context.isLocalPlayer(creatorId)) {
            LOGGER.log(Level.SEVERE, "This is the creator's client. Transitioning to game lobby " + gameId);
            
            // Set current game and transition to game lobby
            context.getController().getModel().setCurrentGame(gameInfo);
            context.getController().getModel().setCurrentView(ClientState.ViewState.GAME_LOBBY);

        } else {
            // For other players, show a notification
            context.runOnUIThread(() -> {
                if (context.getNotificationService() != null) {
                    String gameDesc = gameName != null ? "'" + gameName + "'" : "a new game";
                    context.getNotificationService().showNotification(new Notification(
                            "Game Created",
                            creatorNickname + " created " + gameDesc,
                            NotificationType.INFO
                    ));
                }
            });
        }
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        String playerId = context.getPlayerIdForClient(clientId);
        if (playerId == null) {
            return false;
        }

        // Always send to the creator of the game
        if (playerId.equals(this.creatorId)) {
            return true;
        }

        // For other players, send if they are not in this game (i.e., in the lobby)
        return !context.isClientInGame(clientId, gameId);
    }

    public String getGameName() {
        return  gameName;
    }
    public  String getCreatorId() {
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
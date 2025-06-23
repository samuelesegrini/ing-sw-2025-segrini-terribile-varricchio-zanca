package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.enums.GameLevel;

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
        // Add game to available games list
        PlayerInfo creatorInfo = new PlayerInfo(creatorId, creatorNickname, true);
        GameInfo gameInfo = new GameInfo(gameId, gameName, maxPlayers, 1, gameLevel,
                Collections.singletonList(creatorInfo));

        if(context.getGameState() != null){
            context.getGameState().addAvailableGame(gameInfo);
        }

        LOGGER.log(Level.SEVERE, "GameCreatedEvent received on client. Creator ID: " + creatorId + ". Is this the local player? " + context.isLocalPlayer(creatorId));

        // If this client is the creator, automatically join the game.
        if (context.isLocalPlayer(creatorId)) {
            LOGGER.log(Level.SEVERE, "This is the creator's client. Attempting to auto-join game " + gameId);
            context.getController().joinGame(gameId);
        } else {
            // Otherwise, just show a notification to other players.
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

    public String getGameId() {
        return gameId;
    }
}
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.client.ClientModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event broadcast when a player joins a game lobby.
 * Updates the lobby UI and notifies other players.
 */
public class PlayerJoinedGameEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerJoinedGameEvent.class.getName());
    private final String gameId;
    private final String playerId;
    private final String playerNickname;
    private final int currentPlayerCount;

    public PlayerJoinedGameEvent(String gameId, String playerId, String playerNickname,
                                 int currentPlayerCount) {
        super(EventType.PLAYER_JOINED_GAME, gameId, playerId);
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.currentPlayerCount = currentPlayerCount;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        if (gameId != null) {
            List<GameInfo> availableGames = context.getController().getModel().getAvailableGames();
            for (int i = 0; i < availableGames.size(); i++) {
                GameInfo game = availableGames.get(i);
                if (game.getGameId().equals(gameId)) {
                    GameInfo updatedGame = new GameInfo(
                        game.getGameId(),
                        game.getGameName(),
                        game.getCreatorId(),
                        game.getMaxPlayers(),
                        currentPlayerCount,
                        game.getGameLevel(),
                        game.getCurrentPhase(),
                        game.getPlayers()
                    );
                    
                    List<GameInfo> updatedGames = new ArrayList<>(availableGames);
                    updatedGames.set(i, updatedGame);
                    context.getController().getModel().setAvailableGames(updatedGames);
                    LOGGER.info("PlayerJoined: " + playerNickname + " -> " + gameId + " (" + currentPlayerCount + " players)");
                    break;
                }
            }
        }
        
        context.runOnUIThread(() -> {
            // Only show notification for other players (not the joining player)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                context.getNotificationService().showInfo(
                        "Player Joined",
                        playerNickname + " joined the game (" + currentPlayerCount + " players)"
                );
            }
        });
    }
}

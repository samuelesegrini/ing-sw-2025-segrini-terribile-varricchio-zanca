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
 */
public class PlayerJoinedGameEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerJoinedGameEvent.class.getName());
    private final String gameId;
    private final PlayerId playerId;
    private final String playerNickname;
    private final int currentPlayerCount;

    public PlayerJoinedGameEvent(String gameId, PlayerId playerId, String playerNickname,
                                 int currentPlayerCount) {
        super(EventType.PLAYER_JOINED_GAME, gameId, playerId);
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.currentPlayerCount = currentPlayerCount;
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

    @Override
    public void handleOnClient(ClientEventContext context) {
        ClientState clientState = context.getClientState();
        if (gameId != null && clientState != null) {
            LOGGER.info("PlayerJoined: " + playerNickname + " -> " + gameId + " (" + currentPlayerCount + " players)");
        }
        
        context.runOnUIThread(() -> {
            // Update lobby player list if we're in the same game
            if (clientState != null && gameId.equals(clientState.getCurrentGameId())) {
                // Trigger UI refresh to update player list in lobby
                GameModel currentLobby = clientState.getCurrentGameLobby();
                if (currentLobby != null) {
                    // Force refresh by updating the current game lobby reference
                    clientState.setCurrentGameLobby(currentLobby);
                }
            }
            
            // Only show notification for other players (not the joining player)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                context.getNotificationService().showNotification(new Notification(
                        "Player Joined",
                        playerNickname + " joined the game (" + currentPlayerCount + " players)",
                        NotificationType.INFO
                ));
            }
        });
    }
}

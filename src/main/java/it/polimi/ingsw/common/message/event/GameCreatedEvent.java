package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.enums.GameLevel;

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
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Update games list - add new game to available games (for all clients)
        if (clientState.getAvailableGames() != null) {
            // Create GameInfo for the new game
            it.polimi.ingsw.common.model.GameInfo newGameInfo = new it.polimi.ingsw.common.model.GameInfo(
                gameId, gameName, gameLevel, 1, maxPlayers, 
                it.polimi.ingsw.server.model.enums.GamePhase.SETUP, false, true);
            
            // Add to available games list
            java.util.List<it.polimi.ingsw.common.model.GameInfo> updatedGames = new java.util.ArrayList<>(clientState.getAvailableGames());
            updatedGames.add(newGameInfo);
            clientState.setAvailableGames(updatedGames);
        }
        
        // Only set lobby state if this is the creator's client
        // (This check will be done in handleOnClient method)
        
        clientState.incrementStateVersion();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.info("GameCreatedEvent received on client. Creator ID: " + creatorId + ". Is this the local player? " + context.isLocalPlayer(creatorId));

        // First update client state
        updateClientState(context.getClientState());
        
        // For the creator, also set the full lobby state
        if (context.isLocalPlayer(creatorId) && gameModel != null) {
            context.getClientState().setCurrentGameLobby(gameModel);
            context.getClientState().setPlayersInLobby(gameModel.getPlayers());
        }
        
        context.runOnUIThread(() -> {
            // Then handle UI updates
            context.getController().getUI().onGameCreatedEvent(this);
            
            // Navigate to GAME_LOBBY for creator
            if (context.isLocalPlayer(creatorId)) {
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
package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.UUID;

/**
 * Response to game creation request.
 */
public class CreateGameResponse extends AbstractResponse {
    private final String gameId;
    private final String gameName;
    private final int maxPlayers;
    private final it.polimi.ingsw.server.model.enums.GameLevel gameLevel;
    private final it.polimi.ingsw.server.model.domain.general.GameModel gameModel;

    public CreateGameResponse(UUID correlationId, String gameId, String gameName, 
                            int maxPlayers, GameLevel gameLevel, 
                            it.polimi.ingsw.server.model.domain.general.GameModel gameModel) {
        super(correlationId);
        this.gameId = gameId;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
        this.gameModel = gameModel;
    }

    public String getGameId() {
        return gameId;
    }

    public it.polimi.ingsw.server.model.domain.general.GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Game created successfully - handle creator's navigation and state
        if (context.getClientState() != null) {
            // Set the current game lobby for the creator (who is automatically added to the game)
            context.getClientState().setCurrentGameLobby(gameModel);
            if (gameModel != null) {
                context.getClientState().setPlayersInLobby(gameModel.getPlayers());
            }
            
            // Navigate creator to GAME_LOBBY using proper ViewNavigator
            if (context.getController() == null) {
                throw new IllegalStateException("Controller not available - cannot navigate to GAME_LOBBY after game creation");
            }
            
            if (context.getController().getUIContext() != null && 
                context.getController().getUIContext().getViewNavigator() != null) {
                
                boolean success = context.getController().getUIContext().getViewNavigator()
                    .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME_LOBBY, 
                               "Game created: " + gameName);
                
                if (!success) {
                    String reason = context.getController().getUIContext().getViewNavigator()
                        .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME_LOBBY);
                    throw new IllegalStateException("Failed to navigate to GAME_LOBBY after game creation: " + reason);
                }
                
                java.util.logging.Logger.getLogger(CreateGameResponse.class.getName())
                    .info("Creator navigated to GAME_LOBBY after successful game creation: " + gameName);
            } else {
                throw new IllegalStateException("ViewNavigator not available - cannot navigate to GAME_LOBBY after game creation");
            }
        }
        
        // Show success notification
        context.showNotification("Game Created", 
            "Successfully created game: " + gameName, 
            NotificationType.SUCCESS);
    }

}
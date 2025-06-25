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

    public CreateGameResponse(UUID correlationId, String gameId, String gameName, 
                            int maxPlayers, GameLevel gameLevel) {
        super(correlationId);
        this.gameId = gameId;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Game created successfully - handle creator's navigation and state
        if (context.getClientState() != null) {
            // Set the current game lobby - create a minimal GameModel for the lobby
            try {
                // Create a minimal GameModel for the lobby state
                it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager configManager = 
                    new it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager();
                it.polimi.ingsw.server.model.domain.general.GameModel gameLobby = 
                    new it.polimi.ingsw.server.model.domain.general.GameModel(gameLevel, configManager, maxPlayers);
                    
                context.getClientState().setCurrentGameLobby(gameLobby);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(CreateGameResponse.class.getName())
                    .warning("Failed to create GameModel for lobby: " + e.getMessage());
                // Continue without setting the game lobby - navigation might still work
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
package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.UUID;

/**
 * Response to start game request.
 * Returns the updated GameModel to the requesting host.
 */
public class StartGameResponse extends AbstractResponse {
    private final String gameId;
    private final GameModel gameModel;

    public StartGameResponse(UUID correlationId, String gameId, GameModel gameModel) {
        super(correlationId);
        this.gameId = gameId;
        this.gameModel = gameModel;
        
        // Debug: Log what GameModel is being stored in the response
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StartGameResponse.class.getName());
        if (gameModel != null) {
            logger.info("📦 RESPONSE CONSTRUCTOR - StartGameResponse created with GameModel: " + 
                       gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + 
                       " phase (Hash: " + System.identityHashCode(gameModel) + ")");
        }
    }

    public String getGameId() {
        return gameId;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StartGameResponse.class.getName());
        logger.info("🎯 START GAME RESPONSE - Processing StartGameResponse for gameId: " + gameId);
        
        // Update the GameModel in client state
        if (context.getClientState() != null && gameModel != null) {
            logger.info("📝 SETTING GAMEMODEL - Via StartGameResponse with " + 
                       gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + " phase");
            context.getClientState().setGameModel(gameModel);
            
            // Navigate to GAME view for the building phase
            if (context.getController().getUIContext() != null && 
                context.getController().getUIContext().getViewNavigator() != null) {
                
                boolean success = context.getController().getUIContext().getViewNavigator()
                    .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME, 
                               "Game started - entering building phase");
                
                if (!success) {
                    String reason = context.getController().getUIContext().getViewNavigator()
                        .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME);
                    logger.severe("Failed to navigate to GAME after game started - Reason: " + reason);
                }
            } else {
                logger.severe("ViewNavigator not available - cannot navigate to GAME after game started");
            }
        }
        
        // Show success notification
        context.showNotification("Game Started", 
            "Game has started! Building phase begins now.", 
            NotificationType.SUCCESS);
    }
}
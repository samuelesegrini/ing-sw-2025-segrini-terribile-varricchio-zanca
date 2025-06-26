package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Broadcast to all players in a game when the lobby is full and the game starts.
 * Signals the transition to the Ship Building phase.
 * 
 * SIMPLIFIED VERSION: Carries full GameModel instead of complex individual state.
 */
public class GameStartedEvent extends AbstractEvent {
    private final GameModel gameModel; // Full server game model

    public GameStartedEvent(String gameId, GameModel gameModel, PlayerId requesterId) {
        super(EventType.GAME_STARTED, gameId, requesterId);
        this.gameModel = gameModel;
        
        // Debug: Log what GameModel is being stored in the event
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GameStartedEvent.class.getName());
        if (gameModel != null) {
            logger.info("📦 EVENT CONSTRUCTOR - GameStartedEvent created with GameModel: " + 
                       gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + 
                       " phase (Hash: " + System.identityHashCode(gameModel) + ")");
        }
    }
    
    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client (they get StartGameResponse instead)
        PlayerId playerId = context.getPlayerIdForClient(clientId);
        if (this.sourcePlayerId != null && this.sourcePlayerId.equals(playerId)) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GameStartedEvent.class.getName());
            logger.info("🎯 EVENT FILTERING - GameStartedEvent NOT sent to requester: " + clientId);
            return false;
        }
        
        // Use default game filtering for other clients
        boolean shouldSend = super.shouldSendTo(clientId, context);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GameStartedEvent.class.getName());
        logger.info("🎯 EVENT FILTERING - GameStartedEvent shouldSendTo clientId: " + clientId + " = " + shouldSend);
        return shouldSend;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GameStartedEvent.class.getName());
            logger.info("🎮 GAME STARTED EVENT - Processing for client, GameModel has " + 
                       (gameModel != null ? gameModel.getPlayers().size() + " players" : "null GameModel"));
            
            // Simple Direct Model Architecture: Update ClientState directly
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                logger.info("📝 SETTING GAMEMODEL - Via GameStartedEvent with " + 
                           gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + " phase");
                clientState.setGameModel(gameModel);
                
                // Use ViewNavigator for proper navigation instead of direct state manipulation
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.GAME, "Game started - entering building phase");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.GAME);
                        java.util.logging.Logger.getLogger(GameStartedEvent.class.getName())
                            .severe("Failed to navigate to GAME after game started - Reason: " + reason);
                    }
                } else {
                    java.util.logging.Logger.getLogger(GameStartedEvent.class.getName())
                        .severe("ViewNavigator not available - cannot navigate to GAME after game started");
                }
            }

            // Show notification about game start
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                        new Notification(
                                "Game Started",
                                "The building phase has begun! Build your ship before time runs out.",
                                NotificationType.INFO
                        )
                );
            }
        });
    }
}
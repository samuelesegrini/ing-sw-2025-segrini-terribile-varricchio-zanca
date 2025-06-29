package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Broadcast to all players in a game when the lobby is full and the game starts.
 * Signals the transition to the Ship Building phase.
 * 
 * SIMPLIFIED VERSION: Carries full GameModel instead of complex individual state.
 */
public class GameStartedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameStartedEvent.class.getName());
    private final GameModel gameModel; // Full server game model

    public GameStartedEvent(String gameId, GameModel gameModel, PlayerId requesterId) {
        super(EventType.GAME_STARTED, gameId, requesterId);
        this.gameModel = gameModel;
        
        // Debug: Log what GameModel is being stored in the event
        if (gameModel != null) {
            LOGGER.fine("EVENT CONSTRUCTOR - GameStartedEvent created with GameModel: " +
                       gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() +
                       " phase (Hash: " + System.identityHashCode(gameModel) + ")");
        }
    }
    
    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // MODIFIED: Send to ALL players in the game, including the requester
        // Both response AND event provide consistent state updates
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - GameStartedEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("GAME STARTED EVENT - Processing for client, GameModel has " +
                       (gameModel != null ? gameModel.getPlayers().size() + " players" : "null GameModel"));
            
            // Simple Direct Model Architecture: Update ClientState directly
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                LOGGER.fine("SETTING GAMEMODEL - Via GameStartedEvent with " +
                           gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + " phase");
                clientState.setGameModel(gameModel);
                
                // Use ViewNavigator for proper navigation instead of direct state manipulation
                // TODO: NON ENTRA NELL'IF, FIXA
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.BUILDING, "Game started - entering building phase");
                    
                    if (!success) {
                        LOGGER.severe("Failed to navigate to BUILDING after game started - Reason: " + context.getController().getUIContext().getViewNavigator().getNavigationFailureReason(ClientState.ViewState.BUILDING));
                    }
                } else {
                    LOGGER.severe("ViewNavigator not available - cannot navigate to BUILDING after game started");
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
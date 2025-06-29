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
            LOGGER.info("EVENT CONSTRUCTOR - GameStartedEvent created with GameModel: " +
                       gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() +
                       " phase (Hash: " + System.identityHashCode(gameModel) + ")");
            
            // Log each player in the event constructor
            for (it.polimi.ingsw.server.model.domain.player.Player p : gameModel.getPlayers()) {
                LOGGER.info("EVENT CONSTRUCTOR PLAYER: " + p.getId() + " - " + p.getNickname());
            }
        } else {
            LOGGER.severe("EVENT CONSTRUCTOR - GameModel is null!");
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
        LOGGER.info("DEBUG: GameStartedEvent.handleOnClient() called for gameId: " + getGameId());
        LOGGER.info("DEBUG: Context details - playerId: " + (context.getClientState() != null ? context.getClientState().getPlayerId() : "null"));
        
        context.runOnUIThread(() -> {
            LOGGER.info("DEBUG: Running on UI thread - GameModel has " +
                       (gameModel != null ? gameModel.getPlayers().size() + " players" : "null GameModel"));
            
            // Debug: Log each player received on client side
            if (gameModel != null) {
                for (it.polimi.ingsw.server.model.domain.player.Player p : gameModel.getPlayers()) {
                    LOGGER.info("DEBUG: CLIENT RECEIVED PLAYER: " + p.getId() + " - " + p.getNickname());
                }
            }
            
            // Simple Direct Model Architecture: Update ClientState directly
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                LOGGER.info("DEBUG: ClientState found, current GameModel: " + 
                           (clientState.getGameModel() != null ? "exists" : "null"));
                LOGGER.info("DEBUG: About to call clientState.setGameModel() with " +
                           gameModel.getPlayers().size() + " players in " + gameModel.getCurrentPhase() + " phase");
                
                clientState.setGameModel(gameModel);
                
                LOGGER.info("DEBUG: After setGameModel() - ClientState GameModel: " + 
                           (clientState.getGameModel() != null ? "exists with " + clientState.getGameModel().getPlayers().size() + " players" : "null"));
            } else {
                LOGGER.severe("DEBUG: ClientState is null! Cannot update GameModel");
            }

                // Use ViewNavigator for proper navigation instead of direct state manipulation
                LOGGER.info("DEBUG: Checking navigation components...");
                LOGGER.info("DEBUG: Controller: " + (context.getController() != null ? "exists" : "null"));
                
                if (context.getController() != null) {
                    LOGGER.info("DEBUG: UIContext: " + (context.getController().getUIContext() != null ? "exists" : "null"));
                    
                    if (context.getController().getUIContext() != null) {
                        LOGGER.info("DEBUG: ViewNavigator: " + (context.getController().getUIContext().getViewNavigator() != null ? "exists" : "null"));
                    }
                }
                
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    LOGGER.info("DEBUG: Attempting navigation to BUILDING phase...");
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.BUILDING, "Game started - entering building phase");
                    
                    LOGGER.info("DEBUG: Navigation result: " + success);
                    if (!success) {
                        LOGGER.severe("DEBUG: Failed to navigate to BUILDING after game started - Reason: " + context.getController().getUIContext().getViewNavigator().getNavigationFailureReason(ClientState.ViewState.BUILDING));
                    }
                } else {
                    LOGGER.severe("DEBUG: ViewNavigator not available - cannot navigate to BUILDING after game started");
                    LOGGER.severe("DEBUG: Controller null: " + (context.getController() == null));
                    LOGGER.severe("DEBUG: UIContext null: " + (context.getController() != null && context.getController().getUIContext() == null));
                    LOGGER.severe("DEBUG: ViewNavigator null: " + (context.getController() != null && context.getController().getUIContext() != null && context.getController().getUIContext().getViewNavigator() == null));
                }

            // Show notification about game start
            LOGGER.info("DEBUG: Checking notification service...");

            if (context.getNotificationService() != null) {
                LOGGER.info("DEBUG: Showing game started notification");
                context.getNotificationService().showNotification(
                        new Notification(
                                "Game Started",
                                "The building phase has begun! Build your ship before time runs out.",
                                NotificationType.INFO
                        )
                );
            } else {
                LOGGER.warning("DEBUG: NotificationService is null - cannot show notification");
            }
        });
    }
}
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Broadcast when the game transitions from one major phase to another (e.g., BUILDING -> FLIGHT).
 */
public class PhaseChangedEvent extends AbstractEvent {
    private final GamePhase newPhase;
    private final long durationInSeconds;

    public PhaseChangedEvent(String gameId, GamePhase newPhase, long durationInSeconds) {
        super(EventType.PHASE_CHANGED, gameId, null);
        this.newPhase = newPhase;
        this.durationInSeconds = durationInSeconds;
    }

    public GamePhase getNewPhase() {
        return newPhase;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update model state with new game phase
            if (context.getClientState() != null && context.getClientState().getGameModel() != null) {
                // Update the game model's current phase
                context.getClientState().getGameModel().setCurrentPhase(newPhase);
                
                // Trigger UI refresh after phase change
                context.getClientState().setGameModel(context.getClientState().getGameModel());
                
                java.util.logging.Logger.getLogger(PhaseChangedEvent.class.getName())
                    .info("Phase changed to " + newPhase + " (duration: " + durationInSeconds + "s)");
            }

            // Show phase transition notification
            if (context.getNotificationService() != null) {
                String phaseMessage = getPhaseTransitionMessage(newPhase);
                context.getNotificationService().showInfo(
                        "Phase Changed",
                        phaseMessage + (durationInSeconds > 0 ? " (" + durationInSeconds + "s)" : "")
                );
            }

            // For END phase, might want to navigate to a results view in the future
            // For now, just ensure we're in the GAME view to see the phase change
            if (newPhase == GamePhase.BUILDING || newPhase == GamePhase.FLIGHT) {
                // Ensure we're in GAME view to see the phase UI
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null &&
                    context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME, 
                                   "Phase changed to " + newPhase);
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME);
                        java.util.logging.Logger.getLogger(PhaseChangedEvent.class.getName())
                            .severe("Failed to navigate to GAME after phase change - Reason: " + reason);
                    }
                }
            }
        });
    }

    /**
     * Gets a user-friendly message for the phase transition.
     * @param phase The new game phase
     * @return User-friendly phase description
     */
    private String getPhaseTransitionMessage(GamePhase phase) {
        return switch (phase) {
            case BUILDING -> "Ship building phase has begun!";
            case FLIGHT -> "Flight phase started - prepare for adventure!";
            case END -> "Game finished!";
            default -> "Game phase changed to " + phase;
        };
    }
}
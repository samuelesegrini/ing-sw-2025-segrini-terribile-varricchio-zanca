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
            if (context.getGameState() != null) {
                // context.getGameState().setCurrentPhase(newPhase);
                // context.getGameState().setPhaseDuration(durationInSeconds);
            }

            // Show phase transition notification
            if (context.getNotificationService() != null) {
                String phaseMessage = getPhaseTransitionMessage(newPhase);
                context.getNotificationService().showInfo(
                        "Phase Changed",
                        phaseMessage + (durationInSeconds > 0 ? " (" + durationInSeconds + "s)" : "")
                );
            }

            // Transition UI to new phase if available
            // if (context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().transitionToPhase(newPhase, durationInSeconds);
            // }
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
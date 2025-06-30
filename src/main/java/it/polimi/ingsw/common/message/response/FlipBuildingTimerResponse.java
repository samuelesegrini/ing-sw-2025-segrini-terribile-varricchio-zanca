package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;

import java.util.UUID;

/**
 * Response to flip building timer request.
 * Confirms whether the timer was successfully flipped and provides current stage information.
 */
public class FlipBuildingTimerResponse extends AbstractResponse {

    private final long timeRemaining;
    private final BuildingTimer.TimerStage currentStage;
    private final String message;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param success whether it was successful or not
     * @param message The message
     * @param currentStage The current stage
     * @param timeRemaining The amount of time remaining
     */

    public FlipBuildingTimerResponse(UUID correlationId, boolean success, String message, 
                                   BuildingTimer.TimerStage currentStage, long timeRemaining) {
        super(correlationId, success, message);
        this.message = message;
        this.currentStage = currentStage;
        this.timeRemaining = timeRemaining;
    }

    /**
     *
     * @return The amount of time remaining
     */

    public long getTimeRemaining() {
        return timeRemaining;
    }

    /**
     *
     * @return The current stage
     */

    public BuildingTimer.TimerStage getCurrentStage() {
        return currentStage;
    }

    /**
     *
     * @return The message
     */

    public String getMessage() {
        return message;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update timer display
            Object timerView = context.getTimerView();
            if (timerView != null) {
                try {
                    // Use reflection to call updateStage if the method exists
                    java.lang.reflect.Method updateMethod = timerView.getClass().getMethod(
                        "updateStage", 
                        it.polimi.ingsw.server.model.domain.general.BuildingTimer.TimerStage.class,
                        long.class,
                        int.class
                    );
                    updateMethod.invoke(timerView, currentStage, timeRemaining, 0);
                } catch (Exception e) {
                    // Silently ignore if the method doesn't exist or fails
                }
            }

            // Show stage-specific notification
            String notificationTitle = "Timer Updated";
            String notificationMessage = getStageNotificationMessage();
            NotificationType type = getNotificationType();

            context.showNotification(notificationTitle, notificationMessage, type);
        } else {
            // Show error notification
            context.showNotification(
                    "Timer Flip Failed",
                    message != null ? message : "Failed to flip building timer",
                    NotificationType.ERROR
            );
        }

        context.getController().getUI().onFlipBuildingTimerResponse(new GenericSuccessResponse(getCorrelationId()));
    }

    /**
     *
     * @return the current stage of the timer as a string: "First/Second timer started/expired" or "Building phase ended!"
     */

    private String getStageNotificationMessage() {
        return switch (currentStage) {
            case FIRST_TIMER -> "First timer started! " + (timeRemaining / 1000) + "s remaining";
            case SECOND_TIMER -> "Second timer started! " + (timeRemaining / 1000) + "s remaining";
            case BUILDING_ENDED -> "Building phase ended!";
            case FIRST_EXPIRED -> "First timer expired - waiting for flip";
            case SECOND_EXPIRED -> "Second timer expired - finished players can end building";
            default -> "Timer updated";
        };
    }

    /**
     *
     * @return the notification type (info/warning/critical)
     */

    private NotificationType getNotificationType() {
        return switch (currentStage) {
            case FIRST_TIMER -> NotificationType.INFO;
            case SECOND_TIMER -> NotificationType.WARNING;
            case BUILDING_ENDED -> NotificationType.CRITICAL;
            default -> NotificationType.INFO;
        };
    }
}
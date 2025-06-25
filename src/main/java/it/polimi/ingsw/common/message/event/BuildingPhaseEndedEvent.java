package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.Player;

import java.util.List;
import java.util.Map;

/**
 * Event broadcast when the ship building phase ends.
 * Contains validation results for all players and signals transition to flight phase.
 */
public class BuildingPhaseEndedEvent extends AbstractEvent {
    private final Map<String, ShipValidationResult> validationResults;
    private final List<Player> flightOrder;
    private final long buildingTimeElapsed;

    public BuildingPhaseEndedEvent(String gameId, Map<String, ShipValidationResult> validationResults,
                                   List<Player> flightOrder, long buildingTimeElapsed) {
        super(EventType.BUILDING_PHASE_COMPLETED, gameId, null);
        this.validationResults = validationResults;
        this.flightOrder = flightOrder;
        this.buildingTimeElapsed = buildingTimeElapsed;
    }

    public Map<String, ShipValidationResult> getValidationResults() {
        return validationResults;
    }

    public List<Player> getFlightOrder() {
        return flightOrder;
    }

    public long getBuildingTimeElapsed() {
        return buildingTimeElapsed;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update client state to end building phase
            ClientState clientState = context.getClientState();
            if (clientState != null && clientState.getCurrentGame() != null) {
                clientState.getCurrentGame().setCurrentPhase(it.polimi.ingsw.server.model.enums.GamePhase.FLIGHT);
                
                // Update local validation results
                String localPlayerId = clientState.getPlayerId();
                ShipValidationResult localResult = validationResults.get(localPlayerId);
                if (localResult != null) {
                    // Store validation result in client state if needed
                    // clientState.setShipValidation(localResult.isValid(), localResult.getErrors());
                }
            }

            // Show phase transition notification
            if (context.getNotificationService() != null) {

                String message = "Building phase complete! ";
                if (localResult != null) {
                    if (localResult.isValid()) {
                        message += "Your ship is ready for flight.";
                    } else {
                        message += "Your ship has " + localResult.getErrorCount() + " construction errors.";
                    }
                }

                context.getNotificationService().showNotification(new Notification(
                        "Building Phase Complete",
                        message,
                        localResult != null && localResult.isValid() ? NotificationType.INFO : NotificationType.WARNING
                ));
            }

            // Fire property change for UI transition if supported
            // Note: In Simple Direct Model Architecture, UI components refresh automatically
            // when ClientState is updated. Property change events are not needed.
        });
    }

    /**
     * Represents the validation result for a player's ship.
     */
    public static class ShipValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final int penaltyCredits;

        public ShipValidationResult(boolean valid, List<String> errors, int penaltyCredits) {
            this.valid = valid;
            this.errors = errors;
            this.penaltyCredits = penaltyCredits;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getErrorCount() {
            return errors != null ? errors.size() : 0;
        }

        public int getPenaltyCredits() {
            return penaltyCredits;
        }
    }
}
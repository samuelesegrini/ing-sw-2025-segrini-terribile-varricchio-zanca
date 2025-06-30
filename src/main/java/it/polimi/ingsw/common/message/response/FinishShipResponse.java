package it.polimi.ingsw.common.message.response;

import java.util.List;

/**
 * Response to a FinishShipRequest.
 * Contains the result of ship validation, component corrections, and starting position assignment.
 */
public class FinishShipResponse extends Response {
    private final ShipFinishResult result;

    public FinishShipResponse(boolean success, ShipFinishResult result) {
        super(success, success ? "Ship finished successfully" : result.getErrorMessage());
        this.result = result;
    }

    public ShipFinishResult getResult() {
        return result;
    }

    /**
     * Result of the ship finishing process.
     */
    public static class ShipFinishResult {
        private final boolean success;
        private final boolean shipWasValid;
        private final List<String> validationErrors;
        private final List<String> removedComponents;
        private final int startingPosition;
        private final int flightOrder;
        private final String errorMessage;

        // Success result
        public ShipFinishResult(boolean shipWasValid, List<String> removedComponents, 
                               int startingPosition, int flightOrder) {
            this.success = true;
            this.shipWasValid = shipWasValid;
            this.validationErrors = List.of();
            this.removedComponents = removedComponents != null ? removedComponents : List.of();
            this.startingPosition = startingPosition;
            this.flightOrder = flightOrder;
            this.errorMessage = null;
        }

        // Failure result
        public ShipFinishResult(String errorMessage, List<String> validationErrors) {
            this.success = false;
            this.shipWasValid = false;
            this.validationErrors = validationErrors != null ? validationErrors : List.of();
            this.removedComponents = List.of();
            this.startingPosition = -1;
            this.flightOrder = -1;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean wasShipValid() {
            return shipWasValid;
        }

        public List<String> getValidationErrors() {
            return validationErrors;
        }

        public List<String> getRemovedComponents() {
            return removedComponents;
        }

        public int getStartingPosition() {
            return startingPosition;
        }

        public int getFlightOrder() {
            return flightOrder;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean hadComponentsRemoved() {
            return !removedComponents.isEmpty();
        }
    }
}
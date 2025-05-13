package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ShipBoardDTO;
import it.polimi.ingsw.common.dto.ValidationErrorDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server to a player indicating the result of a ship component placement
 * or a full ship validation.
 */
public class ShipValidationResultEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String playerId; // Player whose ship was validated
    private final boolean isValid;
    private final List<ValidationErrorDTO> errors; // Empty if isValid is true
    private final ShipBoardDTO updatedShipBoard; // Current state of the ship board (even if invalid)

    public ShipValidationResultEvent(String playerId, boolean isValid, List<ValidationErrorDTO> errors, ShipBoardDTO updatedShipBoard) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.isValid = isValid;
        this.errors = new ArrayList<>(Objects.requireNonNull(errors, "errors list cannot be null"));
        this.updatedShipBoard = Objects.requireNonNull(updatedShipBoard, "updatedShipBoard cannot be null");

        if (isValid && !errors.isEmpty()) {
            throw new IllegalArgumentException("If ship is valid, errors list must be empty.");
        }
        if (!isValid && errors.isEmpty()) {
            throw new IllegalArgumentException("If ship is invalid, errors list must not be empty.");
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<ValidationErrorDTO> getErrors() {
        return new ArrayList<>(errors); // Defensive copy
    }

    public ShipBoardDTO getUpdatedShipBoard() {
        return updatedShipBoard; // ShipBoardDTO is a record, its fields are final. Its lists are copied on construction/access.
    }

    @Override
    public String toString() {
        return "ShipValidationResultEvent{" +
                "playerId='" + playerId + '\'' +
                ", isValid=" + isValid +
                ", errorsCount=" + errors.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
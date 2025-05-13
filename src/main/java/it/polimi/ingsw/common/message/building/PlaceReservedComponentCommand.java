package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.PositionDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.server.model.enums.ship.Direction; // Assuming common access

import java.util.Objects;

/**
 * Command sent by a client to place a previously reserved component onto their ship board.
 */
public class PlaceReservedComponentCommand extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String reservedComponentInstanceId; // ID of the component instance from the reserved slot
    private final PositionDTO position;
    private final Direction orientation;

    public PlaceReservedComponentCommand(String reservedComponentInstanceId, PositionDTO position, Direction orientation) {
        super();
        this.reservedComponentInstanceId = Objects.requireNonNull(reservedComponentInstanceId, "reservedComponentInstanceId cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
    }

    public String getReservedComponentInstanceId() {
        return reservedComponentInstanceId;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public Direction getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return "PlaceReservedComponentCommand{" +
                "reservedComponentInstanceId='" + reservedComponentInstanceId + '\'' +
                ", position=" + position +
                ", orientation=" + orientation +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
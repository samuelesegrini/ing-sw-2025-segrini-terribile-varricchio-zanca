package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.dto.PositionDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.server.model.enums.ship.Direction; // Assuming common access

import java.util.Objects;

/**
 * Command sent by a client to place a component they are holding onto their ship board.
 */
public class PlaceComponentCommand extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String componentInstanceId; // ID of the component instance the player is trying to place
    private final PositionDTO position;
    private final Direction orientation; // The desired final orientation on the board

    public PlaceComponentCommand(String componentInstanceId, PositionDTO position, Direction orientation) {
        super();
        this.componentInstanceId = Objects.requireNonNull(componentInstanceId, "componentInstanceId cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
    }

    public String getComponentInstanceId() {
        return componentInstanceId;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public Direction getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return "PlaceComponentCommand{" +
                "componentInstanceId='" + componentInstanceId + '\'' +
                ", position=" + position +
                ", orientation=" + orientation +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
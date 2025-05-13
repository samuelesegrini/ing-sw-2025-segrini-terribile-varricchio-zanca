package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.dto.PositionDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.server.model.enums.ship.Direction; // Assuming common access

import java.util.Objects;

/**
 * Event sent by the server to confirm that a component has been successfully placed
 * on a player's ship board. This is typically broadcast to all players in the session
 * if they can view each other's ships, or just to the acting player.
 */
public class ComponentPlacedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final ComponentDTO component; // The component that was placed
    private final PositionDTO position;   // Position where it was placed
    private final Direction orientation;  // Orientation it was placed with

    public ComponentPlacedEvent(String playerId, ComponentDTO component, PositionDTO position, Direction orientation) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.component = Objects.requireNonNull(component, "component cannot be null");
        this.position = Objects.requireNonNull(position, "position cannot be null");
        this.orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
    }

    public String getPlayerId() {
        return playerId;
    }

    public ComponentDTO getComponent() {
        return component;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public Direction getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return "ComponentPlacedEvent{" +
                "playerId='" + playerId + '\'' +
                ", component=" + component.componentType() +
                ", position=" + position +
                ", orientation=" + orientation +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}

package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to confirm a component has been reserved by a player.
 * This might be sent only to the reserving player or broadcast if others need to know counts.
 */
public class ComponentReservedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final ComponentDTO component; // The component that was reserved
    private final int reservedSlot; // Optional: if there are multiple reserve slots (e.g., 0 or 1)

    public ComponentReservedEvent(String playerId, ComponentDTO component, int reservedSlot) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.component = Objects.requireNonNull(component, "component cannot be null");
        this.reservedSlot = reservedSlot; // Assuming 0 or 1 for two slots
    }

    public String getPlayerId() {
        return playerId;
    }

    public ComponentDTO getComponent() {
        return component;
    }

    public int getReservedSlot() {
        return reservedSlot;
    }

    @Override
    public String toString() {
        return "ComponentReservedEvent{" +
                "playerId='" + playerId + '\'' +
                ", component=" + component.componentType() +
                ", reservedSlot=" + reservedSlot +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}

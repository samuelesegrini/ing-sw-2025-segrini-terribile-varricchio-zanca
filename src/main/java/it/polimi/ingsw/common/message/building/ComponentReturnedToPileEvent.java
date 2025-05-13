package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to all players when a component is returned
 * to the common face-up pile by a player.
 */
public class ComponentReturnedToPileEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final ComponentDTO component; // The component that was returned

    public ComponentReturnedToPileEvent(ComponentDTO component) {
        super();
        this.component = Objects.requireNonNull(component, "component cannot be null");
    }

    public ComponentDTO getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return "ComponentReturnedToPileEvent{" +
                "component=" + component.componentType() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
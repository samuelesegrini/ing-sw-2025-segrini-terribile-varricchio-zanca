package it.polimi.ingsw.common.message.building;


import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to a specific player, offering them a component tile
 * that was drawn from the face-down pile in response to their RequestTakeComponentCommand.
 * The player can then decide to place, return, or reserve it.
 */
public class ComponentOfferedToPlayerEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final ComponentDTO component;

    public ComponentOfferedToPlayerEvent(ComponentDTO component) {
        super();
        this.component = Objects.requireNonNull(component, "component cannot be null");
    }

    public ComponentDTO getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return "ComponentOfferedToPlayerEvent{" +
                "component=" + component.componentType() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
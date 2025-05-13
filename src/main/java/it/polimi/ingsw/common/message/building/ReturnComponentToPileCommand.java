package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Command sent by a client to return a component (that they took but decided not to use)
 * to the common face-up pile.
 */
public class ReturnComponentToPileCommand extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String componentInstanceId; // ID of the component instance the player is returning

    public ReturnComponentToPileCommand(String componentInstanceId) {
        super();
        this.componentInstanceId = Objects.requireNonNull(componentInstanceId, "componentInstanceId cannot be null");
    }

    public String getComponentInstanceId() {
        return componentInstanceId;
    }

    @Override
    public String toString() {
        return "ReturnComponentToPileCommand{" +
                "componentInstanceId='" + componentInstanceId + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

import java.util.Objects;

/**
 * Command sent by a client to reserve a component tile they are currently holding.
 */
public class ReserveComponentCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    private final String componentInstanceId; // ID of the component instance to reserve

    public ReserveComponentCommand(String componentInstanceId) {
        super();
        this.componentInstanceId = Objects.requireNonNull(componentInstanceId, "componentInstanceId cannot be null");
    }

    public String getComponentInstanceId() {
        return componentInstanceId;
    }

    @Override
    public String toString() {
        return "ReserveComponentCommand{" +
                "componentInstanceId='" + componentInstanceId + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
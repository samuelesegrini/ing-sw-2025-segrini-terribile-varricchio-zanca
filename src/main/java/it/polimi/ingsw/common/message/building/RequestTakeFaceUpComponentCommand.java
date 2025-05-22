package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;
import java.util.Objects;

/**
 * Command sent by a client to request taking a specific component tile
 * from the common face-up pile during the building phase.
 */
public class RequestTakeFaceUpComponentCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    private final String componentInstanceIdToTake; // The unique ID of the component in the face-up pile

    public RequestTakeFaceUpComponentCommand(String componentInstanceIdToTake) {
        super();
        this.componentInstanceIdToTake = Objects.requireNonNull(componentInstanceIdToTake, "componentInstanceIdToTake cannot be null");
    }

    public String getComponentInstanceIdToTake() {
        return componentInstanceIdToTake;
    }

    @Override
    public String toString() {
        return "RequestTakeFaceUpComponentCommand{" +
                "componentInstanceIdToTake='" + componentInstanceIdToTake + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
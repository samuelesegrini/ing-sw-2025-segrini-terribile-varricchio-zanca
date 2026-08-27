package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Doorman;
import it.polimi.ingsw.common.transport.Doorway;
import it.polimi.ingsw.common.transport.DoorwayContract;
import it.polimi.ingsw.common.transport.Liveness;
import org.junit.jupiter.api.DisplayName;

/**
 * The RMI door, held to the same contract as the socket one.
 *
 * <p>The point of the pair. Whatever this suite proves about one door it proves about the
 * other by the same assertions, which is what makes a mixed-transport game (requirement S5)
 * something the design guarantees rather than something the tests happen not to have caught.
 */
@DisplayName("a door made of an RMI registry")
class RmiDoorwayTest extends DoorwayContract {

    @Override
    protected Doorway open(Doorman doorman, Liveness liveness) {
        return RmiServer.listening(0, doorman, liveness);
    }

    @Override
    protected Channel<Command, Event> connectTo(Doorway door, ChannelListener<Event> listener) {
        return RmiConnector.connect("localhost", door.port(), listener, Liveness.DEFAULT);
    }
}

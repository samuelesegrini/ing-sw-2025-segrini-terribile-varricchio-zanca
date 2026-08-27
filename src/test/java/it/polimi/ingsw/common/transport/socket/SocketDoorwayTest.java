package it.polimi.ingsw.common.transport.socket;

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
 * The socket door, held to the contract both doors share.
 */
@DisplayName("a door made of a server socket")
class SocketDoorwayTest extends DoorwayContract {

    @Override
    protected Doorway open(Doorman doorman, Liveness liveness) {
        return SocketServer.listening(0, doorman, liveness);
    }

    @Override
    protected Channel<Command, Event> connectTo(Doorway door, ChannelListener<Event> listener) {
        return SocketConnector.connect("localhost", door.port(), listener, Liveness.DEFAULT);
    }
}

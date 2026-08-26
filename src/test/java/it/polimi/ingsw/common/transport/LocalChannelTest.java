package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import org.junit.jupiter.api.DisplayName;

/**
 * The conformance suite, run against the in-process channel.
 *
 * <p>First of what will be three. The socket and RMI implementations extend the same suite,
 * which is what makes requirement S5 — one game, both transports — a claim with something
 * behind it rather than an intention.
 */
@DisplayName("an in-process channel")
class LocalChannelTest extends ChannelContract {

    @Override
    protected Connected connect(Recorder<Event> atClient, Recorder<Command> atServer) {
        LocalChannel.Pair<Command, Event> pair =
                LocalChannel.connect(Command.class, Event.class, atClient, atServer);
        return new Connected(pair.near(), pair.far(), atClient, atServer);
    }
}

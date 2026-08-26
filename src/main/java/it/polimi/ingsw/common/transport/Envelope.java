package it.polimi.ingsw.common.transport;

import java.io.Serializable;

/**
 * What actually travels, as opposed to what the two sides think they are exchanging.
 *
 * <p>The protocol knows about commands and events. The transport needs one more thing — a
 * way to ask "are you still there?" — and putting that into the protocol would mean every
 * client had to handle a message that has nothing to do with the game, and every switch over
 * {@code Event} would grow a case for it.
 *
 * <p>So the transport wraps. A {@link Message} carries a command or an event and is passed
 * up; a {@link KeepAlive} and a {@link Goodbye} are dealt with by the transport and never
 * seen above it.
 */
public sealed interface Envelope extends Serializable {

    /**
     * A protocol message on its way somewhere.
     *
     * @param payload the command or event being carried
     */
    record Message(Serializable payload) implements Envelope {

        /**
         * Validates the envelope.
         *
         * @throws NullPointerException if there is nothing in it
         */
        public Message {
            if (payload == null) {
                throw new NullPointerException("an envelope carries something");
            }
        }
    }

    /**
     * Nothing at all, sent to prove the connection still works.
     *
     * <p>A socket that has gone away does not say so: writes succeed into a buffer and reads
     * block forever. Something has to be sent for the silence to become detectable, which is
     * what this is.
     */
    record KeepAlive() implements Envelope {
    }

    /**
     * The last thing a channel sends: this end is closing on purpose.
     *
     * <p>Without it, a clean close is only noticed the slow way. A socket happens to give it
     * away — the reader sees the end of the stream at once — but RMI does not: unexporting an
     * endpoint is invisible until somebody tries to call it, and a server with nothing to say
     * to a player who has just quit would go on believing in them until the heartbeat ran out.
     * Six seconds of a departed player still listed as present is the kind of difference
     * between two transports that requirement S5 exists to rule out.
     */
    record Goodbye() implements Envelope {
    }
}

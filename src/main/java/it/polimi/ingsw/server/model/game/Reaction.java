package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.protocol.Event;

import java.util.List;

/**
 * What a game did with a command.
 *
 * <p>Two outcomes, and the difference between them is who hears about it. An accepted command
 * changed the game, so everybody is told; a refused one changed nothing, so only the player
 * who sent it is.
 *
 * <p>Returned rather than dispatched, so that the whole aggregate can be tested by handing it
 * commands and looking at what comes back. Who the events actually go to is the controller's
 * business, and it is the only part of this that needs a network.
 */
public sealed interface Reaction {

    /**
     * The command was applied.
     *
     * <p>The narration says what happened. It does not include the {@code StateChanged} that
     * closes every batch, because that is built per recipient and the aggregate does not know
     * who is listening.
     *
     * @param narration what happened, in order
     */
    record Accepted(List<Event> narration) implements Reaction {

        /**
         * Takes a defensive copy.
         */
        public Accepted {
            narration = List.copyOf(narration);
        }

        /**
         * Returns an acceptance with nothing to narrate.
         *
         * <p>The common case. Most commands are worth doing and not worth describing: the
         * state that follows says everything a view needs.
         *
         * @return an empty acceptance
         */
        public static Accepted quietly() {
            return new Accepted(List.of());
        }
    }

    /**
     * The command was refused and nothing happened.
     *
     * <p>The reason is written for a person to read. The server already knows what went
     * wrong, and a numeric code would only have to be turned back into this sentence by every
     * client that ever connects.
     *
     * @param reason why, in a sentence
     */
    record Refused(String reason) implements Reaction {

        /**
         * Validates the refusal.
         *
         * @throws IllegalArgumentException if it does not say why
         */
        public Refused {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("a refusal has to say why");
            }
        }
    }
}

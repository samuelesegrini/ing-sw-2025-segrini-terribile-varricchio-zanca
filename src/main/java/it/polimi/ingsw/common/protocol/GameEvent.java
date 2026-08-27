package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.GameView;

/**
 * What happens to a game as a whole.
 */
public sealed interface GameEvent extends Event {

    /**
     * The whole picture, as this client is allowed to see it.
     *
     * <p>Sent at the end of every batch of events, and on its own when a client joins or
     * comes back. This is the event that makes the protocol safe: nothing is ever expressed
     * only as a delta, so a client cannot drift out of step by missing a message.
     *
     * @param state everything this player may know
     */
    record StateChanged(GameView state) implements GameEvent {

        /**
         * Validates the state.
         *
         * @throws NullPointerException if the state is {@code null}
         */
        public StateChanged {
            if (state == null) {
                throw new NullPointerException("a state event needs a state");
            }
        }
    }

    /**
     * The game moved to a new phase.
     *
     * <p>Redundant with the state that follows, and worth sending anyway: a phase change is
     * the one moment a view has to do something structural — clear the shipyard, draw the
     * route — rather than redraw what it already has.
     *
     * @param phase where the game has got to
     */
    record PhaseBegan(GamePhase phase) implements GameEvent {

        /**
         * Validates the phase.
         *
         * @throws NullPointerException if the phase is {@code null}
         */
        public PhaseBegan {
            if (phase == null) {
                throw new NullPointerException("a phase event needs a phase");
            }
        }
    }

    /**
     * A command was refused.
     *
     * <p>Sent only to whoever sent the command, and never followed by a state change,
     * because a refused command changed nothing. The reason is written for a person to
     * read: the server already knows what went wrong, and a numeric code would only have to
     * be turned back into this sentence by every client.
     *
     * @param command what was refused, by its type name
     * @param reason  why, in a sentence
     */
    record Rejected(String command, String reason) implements GameEvent {

        /**
         * Validates the refusal.
         *
         * @throws IllegalArgumentException if either part is blank
         */
        public Rejected {
            if (command == null || command.isBlank() || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("a refusal has to say what was refused and why");
            }
        }
    }

    /**
     * Somebody dropped, or came back.
     *
     * <p>A game carries on without a player who has dropped: their turns are skipped and
     * their ship keeps flying (requirement AF4). Everyone is told, because otherwise the
     * game appears to hang for no reason.
     *
     * @param player    whose connection changed
     * @param connected whether they are now attached
     */
    record ConnectionChanged(PlayerColor player, boolean connected) implements GameEvent {

        /**
         * Validates the change.
         *
         * @throws NullPointerException if the player is {@code null}
         */
        public ConnectionChanged {
            if (player == null) {
                throw new NullPointerException("a connection event needs a player");
            }
        }
    }

    /**
     * A question was answered on behalf of somebody who was not there.
     *
     * <p>Announced rather than done quietly, because the other players can see a turn go by
     * without anybody appearing to take it and would otherwise be left wondering whether the
     * game had stalled. It also tells a returning player what was decided for them.
     *
     * @param player who was away
     * @param what   the answer given for them, in words a person can read
     */
    record TurnSkipped(PlayerColor player, String what) implements GameEvent {

        /**
         * Validates the announcement.
         *
         * @throws NullPointerException if the player or the description is {@code null}
         */
        public TurnSkipped {
            if (player == null || what == null) {
                throw new NullPointerException("a skipped turn needs a player and a reason");
            }
        }
    }

    /**
     * The game has stopped because there is nobody left to play against.
     *
     * <p>One player alone cannot finish a flight — the cards ask questions of an order of
     * players, and an order of one is not a game. So it waits, and says how long it will wait,
     * rather than either carrying on absurdly or ending on the spot.
     *
     * @param secondsRemaining how long until the last player standing is given the win
     */
    record GameSuspended(long secondsRemaining) implements GameEvent {

        /**
         * Validates the announcement.
         *
         * @throws IllegalArgumentException if the wait is negative
         */
        public GameSuspended {
            if (secondsRemaining < 0) {
                throw new IllegalArgumentException(
                        "a game cannot wait for less than no time, got " + secondsRemaining);
            }
        }
    }

    /** Somebody came back, and the game is going again. */
    record GameResumed() implements GameEvent {
    }

    /**
     * The game is over and will accept nothing further.
     *
     * <p>The ledger itself travels in the final {@link StateChanged}, so that a client which
     * reads only state still sees the scores.
     */
    record GameEnded() implements GameEvent {
    }
}

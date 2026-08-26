package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.DamageReport;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;

/**
 * What happens during a flight.
 *
 * <p>All of these are narration. The state that follows them is the truth, and a client
 * that reads only that is still correct — but a flight told purely in board states is
 * unreadable, and half of what makes this game worth playing is watching a seven come up
 * on somebody else's meteor.
 */
public sealed interface FlightEvent extends Event {

    /**
     * A card was turned over.
     *
     * @param card which card
     */
    record CardRevealed(AdventureCardIdentity card) implements FlightEvent {

        /**
         * Validates the card.
         *
         * @throws NullPointerException if the card is {@code null}
         */
        public CardRevealed {
            if (card == null) {
                throw new NullPointerException("a reveal needs a card");
            }
        }
    }

    /**
     * The game is waiting for somebody to decide something.
     *
     * <p>Sent to everyone, not just to the player being asked. A view that knows only that
     * <em>somebody</em> is thinking cannot say who, and a game that appears to hang is
     * indistinguishable from one that has.
     *
     * @param prompt the decision, and who owes it
     */
    record Awaiting(PlayerPrompt prompt) implements FlightEvent {

        /**
         * Validates the prompt.
         *
         * @throws NullPointerException if the prompt is {@code null}
         */
        public Awaiting {
            if (prompt == null) {
                throw new NullPointerException("a waiting event needs a prompt");
            }
        }
    }

    /**
     * Two dice were thrown.
     *
     * <p>The sum is what the rules use — it names a row or a column — and the sum is
     * therefore what the model produces and what travels here.
     *
     * @param total what came up, two to twelve
     */
    record DiceRolled(int total) implements FlightEvent {

        /**
         * Validates the roll.
         *
         * @throws IllegalArgumentException if two dice could not have produced it
         */
        public DiceRolled {
            if (total < 2 || total > 12) {
                throw new IllegalArgumentException("two dice cannot come up " + total);
            }
        }
    }

    /**
     * A threat arrived at a ship and was dealt with, one way or another.
     *
     * @param player whose ship was in the way
     * @param hit    what arrived
     * @param damage what it did
     */
    record ThreatResolved(PlayerColor player, Hit hit, DamageReport damage) implements FlightEvent {

        /**
         * Validates the report.
         *
         * @throws NullPointerException if any part is {@code null}
         */
        public ThreatResolved {
            if (player == null || hit == null || damage == null) {
                throw new NullPointerException("a threat report needs a player, a hit and a result");
            }
        }
    }

    /**
     * A ship moved along the route.
     *
     * <p>Positions are absolute, so a view can tell a ship that is behind from one that has
     * been lapped. The two look identical on a circular board and mean very different
     * things (p.14).
     *
     * @param player who moved
     * @param from   where they were
     * @param to     where they are
     */
    record ShipMoved(PlayerColor player, int from, int to) implements FlightEvent {

        /**
         * Validates the move.
         *
         * @throws NullPointerException if the player is {@code null}
         */
        public ShipMoved {
            if (player == null) {
                throw new NullPointerException("a move needs a player");
            }
        }
    }

    /**
     * A ship left the route.
     *
     * <p>Either the player gave up, or the rules pushed them out: a lap down, out of crew,
     * or unable to move at all (p.14, p.20). The reason is worth carrying because from the
     * outside all three look the same.
     *
     * @param player who left
     * @param reason why, in a sentence
     */
    record ShipRetired(PlayerColor player, String reason) implements FlightEvent {

        /**
         * Validates the retirement.
         *
         * @throws NullPointerException     if the player is {@code null}
         * @throws IllegalArgumentException if the reason is blank
         */
        public ShipRetired {
            if (player == null) {
                throw new NullPointerException("a retirement needs a player");
            }
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("a retirement has to say why");
            }
        }
    }

    /**
     * The card on the table is finished with.
     */
    record CardResolved() implements FlightEvent {
    }
}

package it.polimi.ingsw.server.model.adventure.resolution;

import it.polimi.ingsw.server.model.player.PlayerColor;

/**
 * A decision an adventure card is waiting on.
 *
 * <p>Sealed, so that a client rendering prompts cannot quietly forget one when a new card
 * introduces a kind of decision. Every variant names the player being asked, because a
 * prompt addressed to nobody in particular is not a prompt.
 *
 * <p>Variants arrive with the cards that need them rather than being guessed at up
 * front — a prompt nobody sends is a prompt nobody has thought through.
 */
public sealed interface PlayerPrompt {

    /**
     * Returns who is being asked.
     *
     * @return the player whose answer the card is waiting for
     */
    PlayerColor player();

    /**
     * An offer the player may take or leave.
     *
     * <p>Shared by every card that puts something on the table for one player at a time:
     * an abandoned ship, an abandoned station, the reward for beating an enemy. All of
     * them cost flight days, and all of them may simply be declined (manual p.12, p.19).
     *
     * @param player      who is being offered it
     * @param description what is on offer, in the language of the card
     * @param flightDays  what taking it costs
     */
    record TakeOrLeave(PlayerColor player, String description, int flightDays) implements PlayerPrompt {

        /**
         * Validates the offer.
         *
         * @throws IllegalArgumentException if the cost is negative
         * @throws NullPointerException     if the player or description is {@code null}
         */
        public TakeOrLeave {
            if (player == null || description == null) {
                throw new NullPointerException("an offer needs a player and a description");
            }
            if (flightDays < 0) {
                throw new IllegalArgumentException("an offer cannot pay flight days, got " + flightDays);
            }
        }
    }
}

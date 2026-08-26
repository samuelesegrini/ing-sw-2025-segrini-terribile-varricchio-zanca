package it.polimi.ingsw.server.model.adventure.resolution;

import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Position;

import java.util.Set;

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

    /**
     * A call to declare engine power or firepower.
     *
     * <p>The declaration is where a player spends batteries, and it is the same decision
     * whichever attribute is being asked for: which doubles to run, knowing the charges
     * are gone either way (manual p.11). Single engines, single cannons and aliens are not
     * in the offer because they are never optional — a player may not declare less than
     * they have (p.19).
     *
     * @param player           who is being asked
     * @param attribute        which attribute the card wants
     * @param activatable      the doubles that could be run, each costing one charge
     * @param chargesAvailable how many charges the ship still holds
     */
    record DeclarePower(PlayerColor player, ShipAttribute attribute,
                        Set<Position> activatable, int chargesAvailable) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws IllegalArgumentException if the charge count is negative
         * @throws NullPointerException     if the player or attribute is {@code null}
         */
        public DeclarePower {
            if (player == null || attribute == null) {
                throw new NullPointerException("a declaration needs a player and an attribute");
            }
            if (chargesAvailable < 0) {
                throw new IllegalArgumentException("a ship cannot hold " + chargesAvailable + " charges");
            }
            activatable = Set.copyOf(activatable);
        }
    }
}
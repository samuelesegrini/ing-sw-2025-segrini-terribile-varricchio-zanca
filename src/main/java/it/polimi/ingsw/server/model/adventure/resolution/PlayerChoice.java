package it.polimi.ingsw.server.model.adventure.resolution;

import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.BatteryPlan;

/**
 * A player's answer to a {@link PlayerPrompt}.
 *
 * <p>Sealed alongside the prompts. A resolution checks that the answer came from the
 * player it asked and that it answers the question it asked — an answer to a different
 * question is a bug in the client, not a move, and is refused rather than guessed at.
 */
public sealed interface PlayerChoice {

    /**
     * Returns who is answering.
     *
     * @return the player who made this choice
     */
    PlayerColor player();

    /**
     * Taking what is on offer, and paying for it.
     *
     * @param player who is accepting
     */
    record Take(PlayerColor player) implements PlayerChoice {
    }

    /**
     * Leaving it, at no cost.
     *
     * <p>Always available. Every card that offers something also lets a player keep their
     * flight days instead (manual p.19), and a resolution that made accepting compulsory
     * would take a real decision away.
     *
     * @param player who is declining
     */
    record Leave(PlayerColor player) implements PlayerChoice {
    }

    /**
     * A declared attribute, and the charges being spent to reach it.
     *
     * @param player who is declaring
     * @param plan   which doubles they are paying to run
     */
    record Declaration(PlayerColor player, BatteryPlan plan) implements PlayerChoice {

        /**
         * Validates the declaration.
         *
         * @throws NullPointerException if the player or plan is {@code null}
         */
        public Declaration {
            if (player == null || plan == null) {
                throw new NullPointerException("a declaration needs a player and a battery plan");
            }
        }
    }
}
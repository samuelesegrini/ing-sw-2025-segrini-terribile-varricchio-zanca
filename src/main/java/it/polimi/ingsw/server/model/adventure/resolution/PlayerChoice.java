package it.polimi.ingsw.server.model.adventure.resolution;

import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.BatteryPlan;
import it.polimi.ingsw.server.model.ship.Position;

import java.util.List;

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

    /**
     * Finished with whatever the card was letting them do.
     *
     * <p>Ends a cargo window. What is still on the table stays there, and the card moves
     * on.
     *
     * @param player who is done
     */
    record Done(PlayerColor player) implements PlayerChoice {
    }

    /**
     * The cabins a player is taking their losses out of.
     *
     * <p>One entry per crew member surrendered, so a cabin holding two humans can be named
     * twice. The resolution checks the list is the right length and that every cabin named
     * still has somebody in it.
     *
     * @param player who is giving up crew
     * @param cabins one entry per crew member, in the order they leave
     */
    record CrewGiven(PlayerColor player, List<Position> cabins) implements PlayerChoice {

        /**
         * Takes a defensive copy of the cabins.
         *
         * @throws NullPointerException if the player or the list is {@code null}
         */
        public CrewGiven {
            if (player == null) {
                throw new NullPointerException("a crew answer needs a player");
            }
            cabins = List.copyOf(cabins);
        }
    }
}
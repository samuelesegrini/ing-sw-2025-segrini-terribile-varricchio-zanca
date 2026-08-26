package it.polimi.ingsw.common.game;


import java.util.List;
import java.util.Set;

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

    /**
     * What a player is putting in front of an incoming shot, if anything.
     *
     * @param player    whose ship is in the way
     * @param component the shield or cannon to use, or empty to take the hit
     */
    record DefenceChosen(PlayerColor player, java.util.Optional<Position> component) implements PlayerChoice {

        /**
         * Validates the answer.
         *
         * @throws NullPointerException if the player or the optional is {@code null}
         */
        public DefenceChosen {
            if (player == null || component == null) {
                throw new NullPointerException("a defence answer needs a player and an optional component");
            }
        }

        /**
         * Returns the answer to take the hit.
         *
         * @param player who is taking it
         * @return a defence that does nothing
         */
        public static DefenceChosen none(PlayerColor player) {
            return new DefenceChosen(player, java.util.Optional.empty());
        }

        /**
         * Returns the answer to use one component.
         *
         * @param player    who is defending
         * @param component the shield or cannon
         * @return the answer
         */
        public static DefenceChosen using(PlayerColor player, Position component) {
            return new DefenceChosen(player, java.util.Optional.of(component));
        }
    }

    /**
     * The piece of a broken ship the player is carrying on with.
     *
     * @param player whose ship came apart
     * @param piece  the cells to keep
     */
    record FragmentKept(PlayerColor player, Set<Position> piece) implements PlayerChoice {

        /**
         * Takes a defensive copy of the piece.
         *
         * @throws NullPointerException if the player or the piece is {@code null}
         */
        public FragmentKept {
            if (player == null) {
                throw new NullPointerException("a fragment answer needs a player");
            }
            piece = Set.copyOf(piece);
        }
    }

    /**
     * The planet a player is landing on.
     *
     * @param player who is landing
     * @param planet the planet's printed number
     */
    record PlanetChosen(PlayerColor player, int planet) implements PlayerChoice {

        /**
         * Validates the answer.
         *
         * @throws NullPointerException     if the player is {@code null}
         * @throws IllegalArgumentException if the planet number is not positive
         */
        public PlanetChosen {
            if (player == null) {
                throw new NullPointerException("a landing needs a player");
            }
            if (planet < 1) {
                throw new IllegalArgumentException("planets are numbered from one, got " + planet);
            }
        }
    }
}
package it.polimi.ingsw.common.game;


import java.io.Serializable;
import java.util.Optional;
import java.util.List;
import java.util.Set;

/**
 * A player's answer to a {@link PlayerPrompt}.
 *
 * <p>Sealed alongside the prompts. A resolution checks that the answer came from the
 * player it asked and that it answers the question it asked — an answer to a different
 * question is a bug in the client, not a move, and is refused rather than guessed at.
 */
public sealed interface PlayerChoice extends Serializable {

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
     * @param component the shield or cannon to use, {@code null} to take the hit
     */
    record DefenceChosen(PlayerColor player, Position component) implements PlayerChoice {

        /**
         * Validates the answer.
         *
         * @throws NullPointerException if the player is {@code null}
         */
        public DefenceChosen {
            if (player == null) {
                throw new NullPointerException("a defence answer needs a player");
            }
        }

        /**
         * Returns the component being put in front of the shot.
         *
         * @return the shield or cannon, or empty when the player is taking the hit
         */
        public java.util.Optional<Position> componentIfAny() {
            return java.util.Optional.ofNullable(component);
        }

        /**
         * Returns the answer to take the hit.
         *
         * @param player who is taking it
         * @return a defence that does nothing
         */
        public static DefenceChosen none(PlayerColor player) {
            return new DefenceChosen(player, null);
        }

        /**
         * Returns the answer to use one component.
         *
         * @param player    who is defending
         * @param component the shield or cannon
         * @return the answer
         */
        public static DefenceChosen using(PlayerColor player, Position component) {
            if (component == null) {
                throw new NullPointerException("use none() to take the hit, not using(null)");
            }
            return new DefenceChosen(player, component);
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
     * What a player is doing with the goods a card is offering.
     *
     * <p>Arranging cargo is several decisions, not one. A player landing on a planet takes a
     * cube, decides the red one will not fit anywhere useful, moves a blue one to make room,
     * and only then says they have finished. Until this existed the only answer to an
     * {@link PlayerPrompt.ArrangeCargo} was {@link Done}, so the goods were offered and could
     * never be taken.
     *
     * @param player who is stowing
     * @param what   the kind of move
     * @param hold   the hold being loaded, emptied, or moved out of
     * @param to     the hold being moved into, {@code null} unless this is a move
     * @param colour which cube
     */
    record CargoStowed(PlayerColor player, Stowing what, Position hold, Position to,
                       GoodColor colour) implements PlayerChoice {

        /**
         * Validates the move.
         *
         * @throws NullPointerException if a part this kind of move needs is {@code null}
         */
        public CargoStowed {
            if (player == null || what == null || hold == null || colour == null) {
                throw new NullPointerException("stowing needs a player, a move, a hold and a cube");
            }
            if (what == Stowing.MOVE && to == null) {
                throw new NullPointerException("moving a cube needs somewhere to move it to");
            }
        }

        /**
         * Takes a cube a card is offering.
         *
         * @param player who is taking it
         * @param hold   where to put it
         * @param colour which cube
         * @return the answer
         */
        public static CargoStowed load(PlayerColor player, Position hold, GoodColor colour) {
            return new CargoStowed(player, Stowing.LOAD, hold, null, colour);
        }

        /**
         * Moves a cube already aboard from one hold to another.
         *
         * <p>Worth doing because a red cube needs a special hold, and the room it wants may be
         * taken by something that would sit anywhere.
         *
         * @param player who is rearranging
         * @param from   where it is
         * @param to     where it should go
         * @param colour which cube
         * @return the answer
         */
        public static CargoStowed move(PlayerColor player, Position from, Position to,
                                       GoodColor colour) {
            return new CargoStowed(player, Stowing.MOVE, from, to, colour);
        }

        /**
         * Throws a cube overboard, returning it to the bank.
         *
         * @param player who is jettisoning it
         * @param hold   where it is
         * @param colour which cube
         * @return the answer
         */
        public static CargoStowed jettison(PlayerColor player, Position hold, GoodColor colour) {
            return new CargoStowed(player, Stowing.JETTISON, hold, null, colour);
        }

        /**
         * Returns where a moved cube is going.
         *
         * @return the destination hold, or empty for anything but a move
         */
        public Optional<Position> destination() {
            return Optional.ofNullable(to);
        }
    }

    /** The three things a player can do with a cube while cargo operations are open. */
    enum Stowing {

        /** Take one the card is offering. */
        LOAD,

        /** Shift one already aboard, usually to free a special hold for a red cube. */
        MOVE,

        /** Throw one overboard, back to the bank. */
        JETTISON
    }

    /**
     * Which planet a player is landing on.
     *
     * <p>A planet taken is gone: the leader chooses first, and whoever is behind them chooses
     * from what is left (manual p.12).
     *
     * @param player who is landing
     * @param planet which planet, counted from zero as the card prints them
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
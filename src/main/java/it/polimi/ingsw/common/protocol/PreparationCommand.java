package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.Position;

import java.util.Optional;
import java.util.Set;

/**
 * Between the shipyard and the launch: making the ship legal, then filling it with people.
 *
 * <p>Two phases, four commands, and everybody works at once again — nobody waits their
 * turn to fix their own ship.
 */
public sealed interface PreparationCommand extends Command {

    /**
     * Throws away a component that should not be there.
     *
     * <p>The only way to fix an illegal ship. A component pulled off is lost and counts
     * against the player at scoring time, which is the price of having built badly
     * (p.9).
     *
     * <p>Removing one can break the ship into pieces, in which case the next thing owed is
     * a {@link KeepPiece}.
     *
     * @param cell which component to throw away
     */
    record RemoveComponent(Position cell) implements PreparationCommand {

        /**
         * Validates the choice.
         *
         * @throws NullPointerException if the cell is {@code null}
         */
        public RemoveComponent {
            if (cell == null) {
                throw new NullPointerException("removing a component needs a cell");
            }
        }
    }

    /**
     * Chooses which piece of a broken ship to keep.
     *
     * <p>Everything outside the chosen piece is lost. The manual offers the choice however
     * lopsided the pieces are, so a player may keep the smaller half if they prefer what is
     * in it (p.10).
     *
     * @param piece the cells to keep flying
     */
    record KeepPiece(Set<Position> piece) implements PreparationCommand {

        /**
         * Takes a defensive copy of the piece.
         *
         * @throws IllegalArgumentException if the piece is empty
         */
        public KeepPiece {
            piece = Set.copyOf(piece);
            if (piece.isEmpty()) {
                throw new IllegalArgumentException("keeping nothing is not one of the choices");
            }
        }
    }

    /**
     * Puts crew in a cabin.
     *
     * <p>A cabin takes two people or one alien, never a mixture, and an alien only where
     * a life support module of its colour is welded to the cabin — <em>welded</em>, not
     * merely next to it (p.9).
     *
     * @param cabin which cabin
     * @param alien which alien to board, {@code null} for two people
     */
    record BoardCrew(Position cabin, AlienColor alien) implements PreparationCommand {

        /**
         * Validates the choice.
         *
         * @throws NullPointerException if the cabin is {@code null}
         */
        public BoardCrew {
            if (cabin == null) {
                throw new NullPointerException("boarding crew needs a cabin");
            }
        }

        /**
         * Returns the alien being boarded.
         *
         * @return the alien, or empty when the cabin is taking people
         */
        public Optional<AlienColor> alienIfAny() {
            return Optional.ofNullable(alien);
        }
    }

    /**
     * Declares this ship crewed and ready to launch.
     *
     * <p>Every cabin still empty is filled with people, which is the manual's default and
     * saves a player four identical commands (p.9).
     */
    record FinishPreparation() implements PreparationCommand {
    }
}

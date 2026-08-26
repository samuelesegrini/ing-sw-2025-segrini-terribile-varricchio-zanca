package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;

import java.util.Optional;

/**
 * The shipyard: everything a player does while the ships are being built.
 *
 * <p>Building is the one phase where everybody acts at once and nobody waits their turn
 * (manual p.5). So these commands arrive interleaved from every player, and each is
 * answered on its own — there is no notion of whose go it is until the ships launch.
 *
 * <p>The order that matters is the small one inside a single placement. A tile is taken
 * into the hand, put down somewhere, turned until it looks right, and only then welded.
 * Until it is welded it can be picked up again for nothing; afterwards it is part of the
 * ship and comes off only by being thrown away (p.6).
 */
public sealed interface BuildingCommand extends Command {

    /**
     * Takes the top tile off the face-down heap.
     *
     * <p>What it is is not known until it is in the hand, which is the whole game.
     */
    record DrawFromPool() implements BuildingCommand {
    }

    /**
     * Takes a named tile from the face-up pile.
     *
     * <p>The pile is a one-way door: a tile somebody rejected is public, and anybody may
     * take it, but nothing ever goes back to being face down (p.6).
     *
     * @param tileId which tile
     */
    record TakeFaceUp(String tileId) implements BuildingCommand {

        /**
         * Validates the choice.
         *
         * @throws IllegalArgumentException if the id is blank
         */
        public TakeFaceUp {
            if (tileId == null || tileId.isBlank()) {
                throw new IllegalArgumentException("taking a tile needs a tile to take");
            }
        }
    }

    /**
     * Takes back a tile this player set aside earlier.
     *
     * @param tileId which of their reserved tiles
     */
    record TakeReserved(String tileId) implements BuildingCommand {

        /**
         * Validates the choice.
         *
         * @throws IllegalArgumentException if the id is blank
         */
        public TakeReserved {
            if (tileId == null || tileId.isBlank()) {
                throw new IllegalArgumentException("taking a reserved tile needs a tile to take");
            }
        }
    }

    /**
     * Puts the tile in hand onto the face-up pile.
     *
     * <p>Free, and irreversible in the sense that everyone has now seen it.
     */
    record ReturnToPool() implements BuildingCommand {
    }

    /**
     * Sets the tile in hand aside for later.
     *
     * <p>Two slots, level II only, and a reserved tile never welded counts as lost at
     * scoring time (p.7). Reserving is therefore a bet, not free storage.
     */
    record Reserve() implements BuildingCommand {
    }

    /**
     * Puts the tile in hand down on a cell, without welding it.
     *
     * <p>The cell has to be on the ship's outline and touching what is already built.
     * Whether the connectors match is <em>not</em> checked here: the manual lets a player
     * build an illegal ship and find out during validation, and taking that away would be
     * changing the game (p.9).
     *
     * @param cell     where to put it
     * @param rotation how far to turn it
     */
    record PlaceInHand(Position cell, Rotation rotation) implements BuildingCommand {

        /**
         * Validates the placement.
         *
         * @throws NullPointerException if the cell or the rotation is {@code null}
         */
        public PlaceInHand {
            if (cell == null || rotation == null) {
                throw new NullPointerException("a placement needs a cell and a rotation");
            }
        }
    }

    /**
     * Moves or turns the tile that is down but not yet welded.
     *
     * <p>Costs nothing and may be repeated. This is the fiddling a player does before
     * committing, and separating it from {@link Weld} is what lets a view show a tile in
     * place before it is final.
     *
     * @param cell     where to move it to, possibly where it already is
     * @param rotation how far to turn it
     */
    record AdjustPlacement(Position cell, Rotation rotation) implements BuildingCommand {

        /**
         * Validates the adjustment.
         *
         * @throws NullPointerException if the cell or the rotation is {@code null}
         */
        public AdjustPlacement {
            if (cell == null || rotation == null) {
                throw new NullPointerException("an adjustment needs a cell and a rotation");
            }
        }
    }

    /**
     * Welds the tile that is down, making it part of the ship.
     *
     * <p>After this it can only be thrown away, and throwing it away counts against the
     * player at scoring time.
     */
    record Weld() implements BuildingCommand {
    }

    /**
     * Picks up one of the face-down card piles to look at it.
     *
     * <p>Level II only, and it costs time rather than money: a player looking at cards is
     * not building, and the glass is still running (p.17).
     *
     * @param pile which pile, counted from zero
     */
    record ScoutPile(int pile) implements BuildingCommand {

        /**
         * Validates the choice.
         *
         * @throws IllegalArgumentException if the pile is negative
         */
        public ScoutPile {
            if (pile < 0) {
                throw new IllegalArgumentException("there is no pile " + pile);
            }
        }
    }

    /**
     * Puts back the pile this player was looking at.
     */
    record PutPileBack() implements BuildingCommand {
    }

    /**
     * Turns the hourglass over.
     *
     * <p>Anyone may turn it, but the last turn of the glass may only be made by a player
     * who has already finished their ship (p.17). That is what stops somebody still
     * building from starting the final countdown at a moment that suits them.
     */
    record FlipTimer() implements BuildingCommand {
    }

    /**
     * Declares this ship finished and takes a place on the starting line.
     *
     * <p>On level II a player picks their space, and picking early is worth a head start
     * (p.17). On the test flight there is no choice: spaces go in the order players
     * finished (p.8), and the field is then ignored.
     *
     * @param startSpace which space to take, {@code null} to let finishing order decide
     */
    record FinishBuilding(Integer startSpace) implements BuildingCommand {

        /**
         * Returns the chosen space.
         *
         * @return the space, or empty when finishing order decides
         */
        public Optional<Integer> startSpaceIfAny() {
            return Optional.ofNullable(startSpace);
        }
    }
}

package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.GoodColor;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * One occupied cell of a ship: the tile welded there and whatever is aboard it.
 *
 * <p>Everything a cell can hold is carried here rather than in parallel maps, because a
 * view draws a cell at a time and a parallel map is a chance to draw cargo in an empty
 * square.
 *
 * <p>A cabin holds either people or one alien, never both. That is the manual's rule
 * (p.9), and it is why the alien is a single value rather than a count.
 *
 * @param tile      the component welded here
 * @param batteries charges still in it, zero for anything that is not a battery
 * @param cargo     the cubes stowed in it, empty for anything that is not a hold
 * @param humans    the people aboard, zero to two
 * @param alien     the alien aboard, {@code null} when there is none
 */
public record CellView(TileView tile, int batteries, List<GoodColor> cargo,
                       int humans, AlienColor alien) implements Serializable {

    /**
     * Validates the occupancy and takes a defensive copy of the cargo.
     *
     * @throws NullPointerException     if the tile is {@code null}
     * @throws IllegalArgumentException if a count is negative, or people and an alien share a cabin
     */
    public CellView {
        if (tile == null) {
            throw new NullPointerException("a cell view needs a tile");
        }
        if (batteries < 0 || humans < 0) {
            throw new IllegalArgumentException("a cell cannot hold a negative amount of anything");
        }
        if (humans > 0 && alien != null) {
            throw new IllegalArgumentException("a cabin holds people or an alien, never both");
        }
        cargo = List.copyOf(cargo);
    }

    /**
     * Returns the alien aboard.
     *
     * @return the alien, or empty when this cell has none
     */
    public Optional<AlienColor> alienIfAny() {
        return Optional.ofNullable(alien);
    }
}

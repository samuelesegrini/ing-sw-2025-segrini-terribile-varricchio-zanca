package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Rotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A cargo hold welded to a ship.
 *
 * <p>Each slot takes one cube. Red cubes are hazardous and only a special hold will
 * carry them; a special hold will carry anything (manual p.7).
 *
 * <p>Cubes may only be moved while a card is letting the player load — that is the one
 * moment they can be redistributed or thrown overboard (quick reference). This type
 * enforces what fits where; when the moving is allowed is the ship's business.
 */
public final class CargoHoldComponent implements ShipComponent {

    private final Tile tile;
    private final Rotation rotation;
    private final List<GoodColor> cubes = new ArrayList<>();

    /**
     * Wraps a cargo hold tile, empty as every ship starts.
     *
     * @param tile     the printed piece
     * @param rotation how far it is turned
     * @throws IllegalArgumentException if the tile is not a cargo hold
     */
    public CargoHoldComponent(Tile tile, Rotation rotation) {
        Components.require(tile, ComponentKind.CARGO_HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        this.tile = tile;
        this.rotation = rotation;
    }

    @Override
    public Tile tile() {
        return tile;
    }

    @Override
    public Rotation rotation() {
        return rotation;
    }

    /**
     * Returns how many cubes this hold takes when full.
     *
     * @return one to three, as printed on the tile
     */
    public int capacity() {
        return tile.capacity();
    }

    /**
     * Tells whether this hold is reinforced against hazardous material.
     *
     * @return {@code true} for a special hold
     */
    public boolean isSpecial() {
        return kind() == ComponentKind.SPECIAL_CARGO_HOLD;
    }

    /**
     * Returns what is currently stored, most valuable first.
     *
     * @return an unmodifiable view of the cubes
     */
    public List<GoodColor> contents() {
        List<GoodColor> sorted = new ArrayList<>(cubes);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns how many cubes are stored.
     *
     * @return the number of occupied slots
     */
    public int load() {
        return cubes.size();
    }

    /**
     * Tells whether every slot is taken.
     *
     * @return {@code true} when the hold is full
     */
    public boolean isFull() {
        return cubes.size() >= capacity();
    }

    /**
     * Tells whether a cube of the given colour could go in right now.
     *
     * @param color the colour of the cube
     * @return {@code true} when there is room and the hold is rated for that colour
     */
    public boolean accepts(GoodColor color) {
        return !isFull() && (isSpecial() || !color.requiresSpecialHold());
    }

    /**
     * Puts a cube in.
     *
     * @param color the colour of the cube
     * @throws IllegalStateException if the hold is full or not rated for that colour
     */
    public void store(GoodColor color) {
        if (isFull()) {
            throw new IllegalStateException(id() + " is full");
        }
        if (!accepts(color)) {
            throw new IllegalStateException(id() + " is not rated to carry " + color + " cubes");
        }
        cubes.add(color);
    }

    /**
     * Takes one cube of the given colour out.
     *
     * @param color the colour to remove
     * @throws IllegalStateException if no cube of that colour is stored
     */
    public void remove(GoodColor color) {
        if (!cubes.remove(color)) {
            throw new IllegalStateException(id() + " is not carrying a " + color + " cube");
        }
    }

    /**
     * Returns the most valuable cube stored, without removing it.
     *
     * <p>Cards that take goods always take the most valuable first (manual p.11), so
     * this is what the ship compares across its holds.
     *
     * @return the most valuable cube, or empty when the hold is empty
     */
    public Optional<GoodColor> mostValuable() {
        return cubes.stream().min(GoodColor::compareTo);
    }

    /**
     * Throws everything overboard.
     *
     * <p>Used when the component is lost: tokens on a destroyed piece go back to the
     * bank at once (manual p.10).
     */
    public void jettisonAll() {
        cubes.clear();
    }
}

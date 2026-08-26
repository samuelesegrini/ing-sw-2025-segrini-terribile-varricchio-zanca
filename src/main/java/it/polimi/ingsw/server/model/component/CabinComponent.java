package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Rotation;
import java.util.Optional;

/**
 * A cabin welded to a ship, including the starting cabin.
 *
 * <p>A cabin holds either two humans or one alien; the alien takes the space of both
 * (manual p.18). The starting cabin is the exception that never takes an alien at all.
 *
 * <p>What this type enforces is the capacity of one cabin. Whether an alien is allowed
 * — the life support attached to it, one alien per colour across the ship — depends on
 * the rest of the ship and belongs there.
 */
public final class CabinComponent implements ShipComponent {

    /** How many humans fit in one cabin. */
    public static final int HUMAN_CAPACITY = 2;

    private final Tile tile;
    private final Rotation rotation;
    private int humans;
    private AlienColor alien;

    /**
     * Wraps a cabin tile, empty until the ship is prepared for launch.
     *
     * @param tile     the printed piece
     * @param rotation how far it is turned
     * @throws IllegalArgumentException if the tile is not a cabin
     */
    public CabinComponent(Tile tile, Rotation rotation) {
        Components.require(tile, ComponentKind.CABIN, ComponentKind.STARTING_CABIN);
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
     * Tells whether this is the cabin the ship was built around.
     *
     * @return {@code true} for the starting cabin
     */
    public boolean isStartingCabin() {
        return kind() == ComponentKind.STARTING_CABIN;
    }

    /**
     * Tells whether this cabin could ever take an alien.
     *
     * <p>The starting cabin never can. The manual's explanation is that the paint in
     * there smells odd (p.18).
     *
     * @return {@code true} for any cabin but the starting one
     */
    public boolean canHostAlien() {
        return !isStartingCabin();
    }

    /**
     * Returns how many humans are aboard this cabin.
     *
     * @return zero, one or two
     */
    public int humans() {
        return humans;
    }

    /**
     * Returns the alien aboard this cabin, if any.
     *
     * @return the alien's colour, or empty when there is none
     */
    public Optional<AlienColor> alien() {
        return Optional.ofNullable(alien);
    }

    /**
     * Returns how many crew members this cabin contributes.
     *
     * <p>Aliens count as crew everywhere it matters — combat zones, abandoned stations,
     * slavers (manual p.18) — so an alien counts as one, not as the two humans it
     * displaced.
     *
     * @return the number of crew aboard
     */
    public int crewCount() {
        return alien != null ? 1 : humans;
    }

    /**
     * Tells whether anyone is aboard.
     *
     * @return {@code true} when the cabin is empty
     */
    public boolean isEmpty() {
        return crewCount() == 0;
    }

    /**
     * Fills the cabin with its two humans.
     *
     * @throws IllegalStateException if the cabin is not empty
     */
    public void boardHumans() {
        requireEmpty();
        humans = HUMAN_CAPACITY;
    }

    /**
     * Puts an alien aboard, in place of the two humans.
     *
     * @param color the alien's species
     * @throws IllegalStateException if the cabin is not empty or cannot host an alien
     */
    public void boardAlien(AlienColor color) {
        requireEmpty();
        if (!canHostAlien()) {
            throw new IllegalStateException(id() + " is the starting cabin and never takes an alien");
        }
        alien = color;
    }

    /**
     * Removes one crew member of the given kind.
     *
     * @param removeAlien {@code true} to give up the alien, {@code false} to give up a human
     * @throws IllegalStateException if there is nobody of that kind to remove
     */
    public void removeOne(boolean removeAlien) {
        if (removeAlien) {
            if (alien == null) {
                throw new IllegalStateException(id() + " has no alien aboard");
            }
            alien = null;
            return;
        }
        if (humans == 0) {
            throw new IllegalStateException(id() + " has no humans aboard");
        }
        humans--;
    }

    /**
     * Empties the cabin.
     *
     * <p>Used when the component is lost, and when a life support module is destroyed
     * and its alien has to leave (manual p.18).
     */
    public void evacuate() {
        humans = 0;
        alien = null;
    }

    private void requireEmpty() {
        if (!isEmpty()) {
            throw new IllegalStateException(id() + " already has crew aboard");
        }
    }
}

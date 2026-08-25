package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Rotation;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One player building one ship.
 *
 * <p>Models the two things a player is holding at any moment: the tile in their hand,
 * and the tile they just put down but have not committed to. The manual gives each its
 * own rule. Only one tile may be held at a time, and it has to be dealt with — welded or
 * returned — before another can be taken (p.4). The tile just placed may still be slid
 * around and turned, right up until the player reaches for the next one, at which point
 * it is welded and final (p.5).
 *
 * <p>That second rule is what makes drawing a tile a commitment to the previous one, and
 * it is why welding is a side effect of taking rather than an action of its own. Nobody
 * ever says "I weld this"; they say "I'll take another", and the weld follows.
 */
public final class ShipBuilder {

    private final Ship ship;
    private final ComponentPool pool;

    private final List<ComponentTile> reserved = new ArrayList<>();

    private ComponentTile inHand;
    private boolean heldTileWasReserved;
    private Position unwelded;

    /**
     * Starts building a ship from a shared pool.
     *
     * @param ship the ship being built, already holding its starting cabin
     * @param pool the heap every player draws from
     */
    public ShipBuilder(Ship ship, ComponentPool pool) {
        this.ship = ship;
        this.pool = pool;
    }

    /**
     * Returns the ship being built.
     *
     * @return the ship
     */
    public Ship ship() {
        return ship;
    }

    /**
     * Returns the tile the player is holding.
     *
     * @return the tile in hand, or empty when their hands are free
     */
    public Optional<ComponentTile> inHand() {
        return Optional.ofNullable(inHand);
    }

    /**
     * Returns the cell holding the tile that is placed but not yet welded.
     *
     * @return the cell, or empty when nothing is still movable
     */
    public Optional<Position> unweldedCell() {
        return Optional.ofNullable(unwelded);
    }

    /**
     * Returns the tiles set aside in the corner of the board.
     *
     * @return an unmodifiable view of the reserved tiles
     */
    public List<ComponentTile> reserved() {
        return List.copyOf(reserved);
    }

    /**
     * Tells whether this level lets a player set tiles aside at all.
     *
     * <p>Reserving arrives with the complete game (manual p.17); a test flight has no
     * reservation area in play.
     *
     * @return {@code true} when the board allows reservations
     */
    public boolean reservationAllowed() {
        return ship.board().allowsReservation();
    }

    /**
     * Tells whether there is room to set another tile aside.
     *
     * @return {@code true} when a reservation slot is free
     */
    public boolean canReserve() {
        return reservationAllowed() && reserved.size() < ship.board().reservationSlots();
    }

    // ---------------------------------------------------------------- taking

    /**
     * Takes a tile from the face-down heap without seeing it first.
     *
     * <p>Welds whatever was still loose, since reaching for a new tile is what commits
     * the last one (manual p.5).
     *
     * @return the tile drawn, seen by this player alone until they decide what to do with it
     * @throws IllegalStateException if the player is already holding a tile, or the heap is empty
     */
    public ComponentTile drawFaceDown() {
        requireEmptyHand();
        weld();
        inHand = pool.drawFaceDown();
        heldTileWasReserved = false;
        return inHand;
    }

    /**
     * Takes a named tile from the face-up pile.
     *
     * <p>Welds whatever was still loose, for the same reason as a face-down draw.
     *
     * @param tileId the tile to take
     * @return the tile taken
     * @throws IllegalStateException if the player is already holding a tile, or no such tile is on show
     */
    public ComponentTile takeFaceUp(String tileId) {
        requireEmptyHand();
        weld();
        inHand = pool.takeFaceUp(tileId);
        heldTileWasReserved = false;
        return inHand;
    }

    /**
     * Puts the held tile back on the table, face up.
     *
     * <p>It stays face up from then on, which is what makes this a real choice rather
     * than an undo: the tile becomes something any other player can pick deliberately.
     *
     * @throws IllegalStateException if the player is holding nothing
     */
    public void returnToPool() {
        ComponentTile returned = requireHeldTile();
        if (heldTileWasReserved) {
            throw new IllegalStateException(
                    returned.id() + " was reserved: a reserved tile never goes back on the table");
        }
        inHand = null;
        pool.returnFaceUp(returned);
    }

    // ---------------------------------------------------------------- reserving

    /**
     * Sets the held tile aside in the corner of the board.
     *
     * <p>A reserved tile is out of everyone's reach, this player's included, until they
     * attach it. What it is not is an escape route: once reserved, a tile can never go
     * back on the table, and one still sitting in the corner when building ends is a
     * component lost along the route, worth a credit off the final score (manual p.17).
     *
     * @throws IllegalStateException if the player is holding nothing, the level does not
     *                               allow reserving, or both slots are taken
     */
    public void reserve() {
        ComponentTile tile = requireHeldTile();
        if (!reservationAllowed()) {
            throw new IllegalStateException("this level has no reservation area");
        }
        if (!canReserve()) {
            throw new IllegalStateException(
                    "both reservation slots are taken: " + reserved.stream().map(ComponentTile::id).toList());
        }
        inHand = null;
        heldTileWasReserved = false;
        reserved.add(tile);
    }

    /**
     * Picks a reserved tile back up.
     *
     * <p>Counts as taking a tile, so it welds whatever was still loose. The tile comes
     * back marked, because it may be attached but never returned to the table — the only
     * two things that can happen to it are being welded on or being written off.
     *
     * @param tileId the reserved tile to pick up
     * @throws IllegalStateException if the player is already holding a tile, or no such
     *                               tile is reserved
     */
    public void takeReserved(String tileId) {
        requireEmptyHand();
        ComponentTile tile = reserved.stream()
                .filter(candidate -> candidate.id().equals(tileId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no tile named " + tileId + " is reserved"));
        weld();
        reserved.remove(tile);
        inHand = tile;
        heldTileWasReserved = true;
    }

    // ---------------------------------------------------------------- placing

    /**
     * Attaches the held tile to the ship.
     *
     * <p>The tile is on the ship immediately but stays movable: it can be slid and turned
     * with {@link #adjust} until the player takes another tile.
     *
     * @param cell     where it goes
     * @param rotation how far to turn it
     * @throws IllegalStateException    if the player is holding nothing
     * @throws IllegalArgumentException if the cell is taken, off the ship, or touching nothing
     */
    public void attach(Position cell, Rotation rotation) {
        ComponentTile tile = requireHeldTile();
        ship.place(cell, tile, rotation);
        inHand = null;
        heldTileWasReserved = false;
        unwelded = cell;
    }

    /**
     * Moves or turns the tile that is placed but not yet welded.
     *
     * <p>Manual p.5 allows this freely while looking for the best spot, and only while
     * looking for it: once the player reaches for another tile the piece is welded and
     * stays where it is.
     *
     * @param cell     where to move it
     * @param rotation how far to turn it
     * @throws IllegalStateException    if nothing is loose, or the player is holding a tile
     * @throws IllegalArgumentException if the new cell will not take it
     */
    public void adjust(Position cell, Rotation rotation) {
        if (unwelded == null) {
            throw new IllegalStateException("nothing is loose to move: the last tile is already welded");
        }
        requireEmptyHand();

        ShipComponent loose = ship.remove(unwelded).orElseThrow();
        try {
            ship.place(cell, loose.tile(), rotation);
        } catch (RuntimeException rejected) {
            ship.place(unwelded, loose.tile(), loose.rotation());
            throw rejected;
        }
        unwelded = cell;
    }

    /**
     * Fixes the loose tile in place.
     *
     * <p>Called for the player rather than by them: taking another tile welds the last
     * one, and so does looking at a pile of adventure cards or finishing the ship. There
     * is no separate "weld" action in the game.
     */
    public void weld() {
        unwelded = null;
    }

    private void requireEmptyHand() {
        if (inHand != null) {
            throw new IllegalStateException("one tile at a time: " + inHand.id() + " is still in hand");
        }
    }

    private ComponentTile requireHeldTile() {
        if (inHand == null) {
            throw new IllegalStateException("the player is not holding a tile");
        }
        return inHand;
    }
}

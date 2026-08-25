package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.server.model.component.ComponentTile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * The heap of tiles in the middle of the table, and the tiles lying face up beside it.
 *
 * <p>Two piles, with different rules. The face-down heap is a lucky dip: a player takes
 * one without knowing what it is, and only they see it until they decide what to do with
 * it. The face-up pile is public — those tiles were tried and returned by somebody, and
 * anyone can see exactly what is on offer before reaching for it (manual p.4).
 *
 * <p>A tile that has been turned face up never goes back down. That is what makes the
 * face-up pile grow into a known, pickable resource over the course of building, and
 * what makes returning a tile a real decision rather than a free undo.
 */
public final class ComponentPool {

    private final List<ComponentTile> faceDown;
    private final Map<String, ComponentTile> faceUp = new LinkedHashMap<>();
    private final RandomGenerator random;

    /**
     * Creates a pool holding every tile face down.
     *
     * @param tiles  the tiles to fill the heap with
     * @param random the source of randomness for face-down draws
     * @throws NullPointerException if either argument is {@code null}
     */
    public ComponentPool(List<ComponentTile> tiles, RandomGenerator random) {
        this.faceDown = new ArrayList<>(tiles);
        this.random = random;
    }

    /**
     * Returns how many tiles are still face down.
     *
     * <p>Public knowledge: everyone at the table can see how big the heap is, just not
     * what is in it.
     *
     * @return the number of unseen tiles
     */
    public int faceDownCount() {
        return faceDown.size();
    }

    /**
     * Returns the tiles lying face up, in the order they were returned.
     *
     * @return an unmodifiable view of what everyone can see
     */
    public List<ComponentTile> faceUp() {
        return List.copyOf(faceUp.values());
    }

    /**
     * Tells whether anything is left to draw.
     *
     * @return {@code true} when both piles are empty
     */
    public boolean isExhausted() {
        return faceDown.isEmpty() && faceUp.isEmpty();
    }

    /**
     * Takes a tile from the face-down heap without looking.
     *
     * <p>Which tile comes up is decided here rather than by the player, which is the
     * digital equivalent of not being able to feel around for a good one.
     *
     * @return the tile drawn
     * @throws IllegalStateException if the heap is empty
     */
    public ComponentTile drawFaceDown() {
        if (faceDown.isEmpty()) {
            throw new IllegalStateException("the face-down heap is empty");
        }
        return faceDown.remove(random.nextInt(faceDown.size()));
    }

    /**
     * Takes a named tile from the face-up pile.
     *
     * <p>Two players reaching for the same tile is expected during building. Commands are
     * applied one at a time, so the first to arrive gets it and the second is told it is
     * gone — which is the same answer the physical game gives, just without the elbows.
     *
     * @param tileId the tile to take
     * @return the tile
     * @throws IllegalStateException if no such tile is face up
     */
    public ComponentTile takeFaceUp(String tileId) {
        ComponentTile taken = faceUp.remove(tileId);
        if (taken == null) {
            throw new IllegalStateException("no tile named " + tileId + " is face up");
        }
        return taken;
    }

    /**
     * Looks at a face-up tile without taking it.
     *
     * @param tileId the tile to look at
     * @return the tile, or empty when it is not on offer
     */
    public Optional<ComponentTile> peekFaceUp(String tileId) {
        return Optional.ofNullable(faceUp.get(tileId));
    }

    /**
     * Puts a tile back, face up.
     *
     * <p>Face up and staying that way: a returned tile is public from then on, and cannot
     * be turned back over (manual p.4).
     *
     * @param tile the tile being given up
     * @throws IllegalStateException if that tile is already in the pool
     */
    public void returnFaceUp(ComponentTile tile) {
        if (faceUp.containsKey(tile.id()) || faceDown.contains(tile)) {
            throw new IllegalStateException(tile.id() + " is already in the pool");
        }
        faceUp.put(tile.id(), tile);
    }
}

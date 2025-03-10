package it.polimi.ingsw.model.domain.ship;

import java.util.Map;

public class Grid<T> {
    private Map<Position, T> grid;

    public Grid() {}

    /**
     * Inserts an element into the grid at the specified position.
     * If an element already exists at this position, it will be replaced.
     *
     * @param position the position where the element should be placed
     * @param component the component to be added to the grid
     *
     */
    public void put (Position position, T component){};

    /**
     * Removes and returns the element at the specified position, if present.
     * If no element exists at the given position, this method returns {@code null}.
     *
     * @param position the position of the element to be removed
     * @return the removed element, or {@code null} if no element was found at the given position
     *
     */
    public T remove(Position position) {return null;}

    /**
     * Checks whether there is an element at the specified position in the grid.
     *
     * @param position the position to check
     * @return {@code true} if an element exists at the given position, {@code false} otherwise
     */
    public boolean containsKey(Position position) {return false;}

    /**
     * Checks whether the grid is empty.
     *
     * @return {@code true} if the grid contains no elements, {@code false} otherwise
     */
    public boolean isEmpty() {return false;}

    /**
     * Returns the number of elements currently stored in the grid.
     *
     * @return the number of elements in the grid
     */
    public int size() {return 0;}
}

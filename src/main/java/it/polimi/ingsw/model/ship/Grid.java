package it.polimi.ingsw.model.ship;

public class Grid<T> {
    private Map<Position, T> grid;

    public Grid() {}

    public void put() {}
    public T remove(Position position) {return null;}
    public boolean containsKey(Position position) {return false;}
    public boolean isEmpty() {return false;}
    public int size() {return 0;}
}

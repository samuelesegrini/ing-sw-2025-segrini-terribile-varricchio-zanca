package it.polimi.ingsw.network.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data Transfer Object representing a position on a 2D grid.
 * Using a class here for potential compatibility or future fields,
 * though a record could also work.
 */
public class PositionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    public PositionDTO() {
        // Default constructor for serialization
    }

    public PositionDTO(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionDTO that = (PositionDTO) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "PositionDTO{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
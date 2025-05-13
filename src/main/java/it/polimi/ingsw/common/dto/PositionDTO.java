package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data Transfer Object for a position on a 2D grid.
 * Typically used for ship component placement.
 */
public record PositionDTO(int row, int col) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PositionDTO { }

    @Override
    public String toString() {
        return "PositionDTO{" +
                "row=" + row +
                ", col=" + col +
                '}';
    }
}
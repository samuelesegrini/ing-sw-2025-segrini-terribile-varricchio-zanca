package it.polimi.ingsw.common.dto;

import it.polimi.ingsw.server.model.enums.ship.Direction; // Assuming common access

import java.io.Serializable;
import java.util.Objects;

/**
 * Data Transfer Object representing a component placed on a ship board.
 */
public record PlacedComponentDTO(
        ComponentDTO component,
        PositionDTO position,
        Direction orientation // The orientation of the component as placed on the board
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlacedComponentDTO {
        Objects.requireNonNull(component, "component cannot be null");
        Objects.requireNonNull(position, "position cannot be null");
        Objects.requireNonNull(orientation, "orientation cannot be null");
    }

    @Override
    public String toString() {
        return "PlacedComponentDTO{" +
                "component=" + component.componentType() + "@" + component.componentInstanceId() +
                ", position=" + position +
                ", orientation=" + orientation +
                '}';
    }
}
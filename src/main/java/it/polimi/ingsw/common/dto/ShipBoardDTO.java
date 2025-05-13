package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Data Transfer Object representing the state of a player's ship board.
 */
public record ShipBoardDTO(
        List<PlacedComponentDTO> placedComponents,
        List<ComponentDTO> reservedComponents
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ShipBoardDTO {
        Objects.requireNonNull(placedComponents, "placedComponents cannot be null");
        Objects.requireNonNull(reservedComponents, "reservedComponents cannot be null");
        // Defensive copies
        placedComponents = new ArrayList<>(placedComponents);
        reservedComponents = new ArrayList<>(reservedComponents);
    }

    // Constructor for convenience if you want to pass modifiable lists during creation
    public ShipBoardDTO(List<PlacedComponentDTO> placedComponents, List<ComponentDTO> reservedComponents, boolean fromInternal) {
        this(new ArrayList<>(placedComponents), new ArrayList<>(reservedComponents));
    }


    @Override
    public List<PlacedComponentDTO> placedComponents() {
        return new ArrayList<>(placedComponents); // Return defensive copy
    }

    @Override
    public List<ComponentDTO> reservedComponents() {
        return new ArrayList<>(reservedComponents); // Return defensive copy
    }

    @Override
    public String toString() {
        return "ShipBoardDTO{" +
                "placedCount=" + placedComponents.size() +
                ", reservedCount=" + reservedComponents.size() +
                '}';
    }
}
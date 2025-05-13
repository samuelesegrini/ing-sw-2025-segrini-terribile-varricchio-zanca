package it.polimi.ingsw.common.dto;

import it.polimi.ingsw.server.model.enums.ship.ComponentType; // Assuming common access or will be String
import it.polimi.ingsw.server.model.enums.ship.ConnectorType; // Assuming common access or will be String
import it.polimi.ingsw.server.model.enums.ship.Direction;     // Assuming common access or will be String

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object for a ship component.
 */
public record ComponentDTO(
        String componentInstanceId, // A unique ID for this specific instance of the component tile
        ComponentType componentType,
        Map<Direction, ConnectorType> connectors, // Connectors in their current orientation
        Direction currentOrientation,
        Map<String, Object> properties // E.g., "capacity:3", "power:1"
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public ComponentDTO {
        Objects.requireNonNull(componentInstanceId, "componentInstanceId cannot be null");
        Objects.requireNonNull(componentType, "componentType cannot be null");
        Objects.requireNonNull(connectors, "connectors cannot be null");
        Objects.requireNonNull(currentOrientation, "currentOrientation cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
    }

    // toString can be verbose, simplified for brevity or use default record toString
    @Override
    public String toString() {
        return "ComponentDTO{" +
                "id='" + componentInstanceId + '\'' +
                ", type=" + componentType +
                ", orientation=" + currentOrientation +
                '}';
    }
}
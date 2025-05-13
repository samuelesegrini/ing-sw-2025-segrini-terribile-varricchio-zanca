package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data Transfer Object for a penalty applied during ship correction or game events.
 */
public record PenaltyDTO(
        String penaltyType, // e.g., "COMPONENT_REMOVED", "CREDIT_DEDUCTED"
        String description,
        int value,          // Optional: e.g., number of credits
        ComponentDTO affectedComponent, // Optional
        PositionDTO componentPosition   // Optional
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PenaltyDTO {
        Objects.requireNonNull(penaltyType, "penaltyType cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }

    // Constructor for penalties without component/value details
    public PenaltyDTO(String penaltyType, String description) {
        this(penaltyType, description, 0, null, null);
    }

    @Override
    public String toString() {
        return "PenaltyDTO{" +
                "penaltyType='" + penaltyType + '\'' +
                ", description='" + description + '\'' +
                (value != 0 ? ", value=" + value : "") +
                (affectedComponent != null ? ", affectedComponent=" + affectedComponent.componentType() : "") +
                (componentPosition != null ? ", componentPosition=" + componentPosition : "") +
                '}';
    }
}
package it.polimi.ingsw.server.model.domain.general.config;

import java.io.Serializable;

import java.util.List;

public record ShipGridConfig(
        String image,
        int rows,
        int cols,
        List<PositionConfig> reservedComponentsPositions,
        List<PositionConfig> forbiddenPositions
) implements Serializable {
    private static final long serialVersionUID = 1L;
}


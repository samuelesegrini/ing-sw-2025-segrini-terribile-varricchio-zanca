package it.polimi.ingsw.server.model.domain.general.config;

import java.util.List;

public record ShipGridConfig(
        String image,
        int rows,
        int cols,
        List<PositionConfig> reservedComponentsPositions,
        List<PositionConfig> forbiddenPositions
) {}
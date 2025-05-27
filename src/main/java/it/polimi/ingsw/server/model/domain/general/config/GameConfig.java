package it.polimi.ingsw.server.model.domain.general.config;

import it.polimi.ingsw.server.model.enums.GameLevel;

public record GameConfig(
        GameLevel levelEnum,
        FlightBoardConfig flightBoardConfig,
        ShipGridConfig shipGridConfig
) {}
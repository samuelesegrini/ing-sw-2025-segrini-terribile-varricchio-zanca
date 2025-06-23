package it.polimi.ingsw.server.model.domain.general.config;

import it.polimi.ingsw.server.model.enums.GameLevel;

import java.io.Serializable;

public record GameConfig(
        GameLevel levelEnum,
        FlightBoardConfig flightBoardConfig,
        ShipGridConfig shipGridConfig
) implements Serializable {}


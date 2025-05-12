package it.polimi.ingsw.server.model.domain.general.config;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;

/**
 * Record representing the game configuration parameters
 */
public record GameConfig(
    int boardSize,
    int buildingTime,
    int flightDays,
    Map<String, Integer> rewards,
    Map<GoodType, Integer> goodsPrices,
    Map<String, Object> adventureDeckConfig
) {}

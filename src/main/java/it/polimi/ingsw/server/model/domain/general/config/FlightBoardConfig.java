package it.polimi.ingsw.server.model.domain.general.config;

import java.util.List;

public record FlightBoardConfig(
        String image,
        String length, // JSON has this as string, e.g., "18"
        List<Integer> startingPositions,
        RewardSystemConfig rewardSystem
) {}
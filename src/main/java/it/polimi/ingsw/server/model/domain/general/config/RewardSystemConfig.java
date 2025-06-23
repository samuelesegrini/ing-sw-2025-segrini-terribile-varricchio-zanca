package it.polimi.ingsw.server.model.domain.general.config;

import java.util.Map;

public record RewardSystemConfig(
        Map<String, Integer> positionBonus, // Keys like "FIRST", "SECOND"
        Map<String, Integer> resourceBonus, // Keys like "RED", "BLUE"
        int bestLookingShipBonus,
        int exposedConnectorsPenalty
) {}
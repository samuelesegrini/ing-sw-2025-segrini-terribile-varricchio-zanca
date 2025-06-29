package it.polimi.ingsw.server.model.domain.general.config;

import java.io.Serializable;
import java.util.Map;

public record RewardSystemConfig(
        Map<String, Integer> positionBonus, // Keys like "FIRST", "SECOND"
        Map<String, Integer> resourceBonus, // Keys like "RED", "BLUE"
        int bestLookingShipBonus,
        int exposedConnectorsPenalty
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
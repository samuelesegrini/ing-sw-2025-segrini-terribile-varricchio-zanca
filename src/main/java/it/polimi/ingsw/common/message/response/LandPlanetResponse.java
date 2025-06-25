package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.UUID;

/**
 * Response to a successful land on a planet request.
 */
public class LandPlanetResponse extends AbstractResponse {
    private final int lostFlightDays;
    private final Map<GoodType, Integer> collectedGoods;

    public LandPlanetResponse(UUID correlationId, int lostFlightDays, Map<GoodType, Integer> collectedGoods) {
        super(correlationId);
        this.lostFlightDays = lostFlightDays;
        this.collectedGoods = Map.copyOf(collectedGoods);
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}
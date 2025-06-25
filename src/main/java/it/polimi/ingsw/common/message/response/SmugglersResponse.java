package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following a SmugglersCard).
 */
public class SmugglersResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final boolean hasWon;
    private final Map<GoodType, Integer> lostGoods;
    private final int lostFlightDays;
    private final Map<GoodType, Integer> collectedGoods;

    public SmugglersResponse(UUID correlationId, int usedBatteries, boolean hasWon, Map<GoodType, Integer> lostGoods, int lostFlightDays, Map<GoodType, Integer> collectedGoods) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.hasWon = hasWon;
        this.lostGoods = Map.copyOf(lostGoods);
        this.lostFlightDays = lostFlightDays;
        this.collectedGoods = Map.copyOf(collectedGoods);
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}
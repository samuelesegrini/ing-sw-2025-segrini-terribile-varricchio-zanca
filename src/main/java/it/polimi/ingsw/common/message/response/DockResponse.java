package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.UUID;

/**
 * Response to a successful dock on an abandoned ship/station request.
 */
public class DockResponse extends AbstractResponse {
    private final int lostFlightDays;
    private final int lostCrew;
    private final Map<GoodType, Integer> collectedGoods;
    private final int collectedCredits;

    public DockResponse(UUID correlationId, int lostFlightDays, int lostCrew, Map<GoodType, Integer> collectedGoods, int collectedCredits) {
        super(correlationId);
        this.lostFlightDays = lostFlightDays;
        this.lostCrew = lostCrew;
        this.collectedGoods = Map.copyOf(collectedGoods);
        this.collectedCredits = collectedCredits;
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}
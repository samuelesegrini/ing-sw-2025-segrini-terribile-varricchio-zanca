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

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param lostFlightDays The number of flight days to lose
     * @param lostCrew The number of crew members to lose
     * @param collectedGoods The number of goods collected
     * @param collectedCredits The number of credits collected
     */

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

        context.getController().getUI().onDockResponse(this);
    }
}
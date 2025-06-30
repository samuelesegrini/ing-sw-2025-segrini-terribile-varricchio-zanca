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

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param usedBatteries The number of  used batteries
     * @param hasWon whether the player has won
     * @param lostGoods The number of every type of lost goods
     * @param lostFlightDays The number of lost flight days
     * @param collectedGoods The number of every type of collected goods
     */

    public SmugglersResponse(UUID correlationId, int usedBatteries, boolean hasWon, Map<GoodType, Integer> lostGoods, int lostFlightDays, Map<GoodType, Integer> collectedGoods) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.hasWon = hasWon;
        this.lostGoods = Map.copyOf(lostGoods);
        this.lostFlightDays = lostFlightDays;
        this.collectedGoods = Map.copyOf(collectedGoods);
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}
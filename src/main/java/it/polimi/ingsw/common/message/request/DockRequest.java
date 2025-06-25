package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;

/**
 * Request from a player to dock after either AbandonedShipChard or AbandonedStationCard.
 */
public class DockRequest extends AbstractRequest {
    private final boolean isDocking;

    public DockRequest(boolean isDocking) {
        this.isDocking = isDocking;
    }

    @Override
    public Response execute(RequestContext context) {
        return null; // TODO
    }
}
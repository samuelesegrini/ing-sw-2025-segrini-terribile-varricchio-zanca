package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.Response;

/**
 * Request from a player to land on a planet (after PlanetsCard).
 * planetIndex = 0 if the player decides not to land on a planet.
 */
public class LandPlanetRequest extends AbstractRequest {
    private final int planetIndex;

    /**
     * constructor
     *
     * @param planetIndex the index of the planet to land on
     */

    /**
     *
     * @param planetIndex  the index of the planet to land on
     */

    public LandPlanetRequest(int planetIndex) {
        this.planetIndex = planetIndex;
    }

    /**
     *
     * @param context The execution context providing access to server resources
     * @return null
     */
    @Override
    public Response execute(RequestContext context) {
        return null; // TODO
    }
}
package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;

/**
 * Request from a player to land on a planet (after PlanetsCard).
 * planetIndex = 0 if the player decides not to land on a planet.
 */
public class LandPlanetRequest extends AbstractRequest {
    private final int planetIndex;

    public LandPlanetRequest(int planetIndex) {
        this.planetIndex = planetIndex;
    }

    @Override
    public Response execute(RequestContext context) {
        return null; // TODO
    }
}
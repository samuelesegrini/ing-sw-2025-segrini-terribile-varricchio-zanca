package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.GameInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response with list of available games.
 */
public class ListGamesResponse extends AbstractResponse {
    private final List<GameInfo> games;

    public ListGamesResponse(UUID correlationId, List<GameInfo> games) {
        super(correlationId);
        this.games = new ArrayList<>(games);
    }

    public List<GameInfo> getGames() {
        return new ArrayList<>(games);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.getModel().setAvailableGames(games);
        } else {
            context.showError("Could not fetch games", getErrorMessage());
        }
    }

}
package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response with list of available games.
 */
public class ListGamesResponse extends AbstractResponse {
    private final List<GameInfo> games;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param gameModels The list of game models
     */

    public ListGamesResponse(UUID correlationId, List<GameModel> gameModels) {
        super(correlationId);
        this.games = gameModels.stream()
                .map(GameInfo::fromGameModel)
                .collect(Collectors.toList());
    }

    /**
     *
     * @return a list of the available games
     */

    public List<GameInfo> getGames() {
        return new ArrayList<>(games);
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.getClientState().setAvailableGames(games);
        } else {
            context.showError("Could not fetch games", getErrorMessage());
        }

        context.getController().getUI().onListGamesResponse(this);
    }

}
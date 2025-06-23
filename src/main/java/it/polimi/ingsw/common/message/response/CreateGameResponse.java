package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to game creation request.
 */
public class CreateGameResponse extends AbstractResponse {
    private final String gameId;
    private final String gameName;
    private final int maxPlayers;
    private final it.polimi.ingsw.server.model.enums.GameLevel gameLevel;

    public CreateGameResponse(UUID correlationId, String gameId, String gameName, 
                            int maxPlayers, it.polimi.ingsw.server.model.enums.GameLevel gameLevel) {
        super(correlationId);
        this.gameId = gameId;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
    }
    
    // Backwards compatibility constructor
    public CreateGameResponse(UUID correlationId, String gameId, String gameName) {
        this(correlationId, gameId, gameName, 4, it.polimi.ingsw.server.model.enums.GameLevel.TEST_FLIGHT);
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Game created successfully - let GameCreatedEvent handle the lobby transition
        // Show success notification
        context.showNotification("Game Created", 
            "Successfully created game: " + gameName, 
            NotificationType.SUCCESS);
    }

}
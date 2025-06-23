package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to game creation request.
 */
public class CreateGameResponse extends AbstractResponse {
    private final String gameId;
    private final String gameName;

    public CreateGameResponse(UUID correlationId, String gameId, String gameName) {
        super(correlationId);
        this.gameId = gameId;
        this.gameName = gameName;
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // No longer need a notification here, as the GameCreatedEvent provides feedback
        // and triggers the UI transition. This avoids duplicate notifications.
    }

}
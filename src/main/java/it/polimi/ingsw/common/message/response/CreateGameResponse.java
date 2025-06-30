package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Lightweight response to game creation request.
 * Acknowledges that the request was processed successfully and provides the gameId.
 * The GameCreatedEvent is the single source of truth for state updates.
 */
public class CreateGameResponse extends AbstractResponse {
    private final String gameId;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param gameId The game ID
     */

    public CreateGameResponse(UUID correlationId, String gameId) {
        super(correlationId);
        this.gameId = gameId;
    }

    /**
     *
     * @return the game ID
     */

    public String getGameId() {
        return gameId;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        Logger logger = java.util.logging.Logger.getLogger(CreateGameResponse.class.getName());
        logger.info("🎯 CREATE GAME RESPONSE - Received confirmation that the create game request was successful. GameId: " + gameId);

        // Show a simple acknowledgment notification
        context.showNotification("Request Acknowledged", 
            "Game creation request processed successfully.", 
            NotificationType.SUCCESS);

        context.getController().getUI().onCreateGameResponse(this);
    }

}
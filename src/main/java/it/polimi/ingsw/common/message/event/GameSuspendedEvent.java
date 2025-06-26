package it.polimi.ingsw.common.message.event;

import java.util.logging.Logger;

/**
 * Broadcast if a game is paused (e.g., only one player remains active).
 */
public class GameSuspendedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameSuspendedEvent.class.getName());
    private final String reason;

    public GameSuspendedEvent(String gameId, String reason) {
        super(EventType.GAME_SUSPENDED, gameId, null);
        this.reason = reason;
        LOGGER.fine("GameSuspendedEvent instantiated for game: " + gameId + ", reason: " + reason);
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.fine("Handling GameSuspendedEvent for game: " + gameId + ", reason: " + reason);
        // Client UI should show a "Game Paused" overlay with the reason.
        // context.getGameUI().showGameSuspendedOverlay(reason);
    }
}
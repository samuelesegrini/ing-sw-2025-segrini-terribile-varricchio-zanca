package it.polimi.ingsw.common.message.event;

/**
 * Broadcast if a game is paused (e.g., only one player remains active).
 */
public class GameSuspendedEvent extends AbstractEvent {
    private final String reason;

    public GameSuspendedEvent(String gameId, String reason) {
        super(EventType.GAME_SUSPENDED, gameId, null);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Client UI should show a "Game Paused" overlay with the reason.
        // context.getGameUI().showGameSuspendedOverlay(reason);
    }
}
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ClientModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Event broadcast when a game ends.
 * Updates client state and displays final results or end reason to players.
 */
public class GameEndedEvent extends AbstractEvent {
    private final String reason;
    private final Map<String, Integer> finalScores;

    public GameEndedEvent(String gameId, String reason, Map<String, Integer> finalScores) {
        super(EventType.GAME_ENDED, gameId, null);
        this.reason = reason;
        this.finalScores = finalScores != null ? new HashMap<>(finalScores) : null;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Integer> getFinalScores() {
        return finalScores != null ? new HashMap<>(finalScores) : null;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update model state - game has ended
            if (context.getController() != null && context.getController().getModel() != null) {
                ClientModel model = context.getController().getModel();
                if (gameId.equals(model.getCurrentGameId())) {
                    // Note: setGameStarted method would need to be added to ClientModel if it doesn't exist
                    // model.setGameStarted(false);
                    model.setCurrentGame(null);
                    model.setCurrentView(ClientModel.ViewState.LOBBY);
                }
            }

            // Show game results or end notification
            if (context.getNotificationService() != null) {
                if (finalScores != null && !finalScores.isEmpty()) {
                    // Show game results with scores
                    StringBuilder scoreText = new StringBuilder("Final Scores:\n");
                    finalScores.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .forEach(entry -> scoreText.append(entry.getKey())
                                    .append(": ").append(entry.getValue()).append("\n"));
                    
                    context.getNotificationService().showInfo(
                            "Game Ended",
                            scoreText.toString()
                    );
                } else {
                    // Show simple end notification
                    context.getNotificationService().showWarning(
                            "Game Ended",
                            reason != null ? reason : "The game has ended"
                    );
                }
            }

//            // Show results in UI if available
//            if (context.getGameUI() != null) {
//                // Note: This would need to be implemented based on the actual UI interface
//                // context.getGameUI().showGameResults(finalScores, reason);
//            }
        });
    }
}
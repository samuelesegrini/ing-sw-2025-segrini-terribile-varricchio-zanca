package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.Map;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event sent to players when their input is needed during adventure card resolution.
 * This replaces individual request types with a unified choice collection system.
 */
public class PlayerChoiceRequestEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerChoiceRequestEvent.class.getName());
    
    private final PlayerId targetPlayer;
    private final String choiceType;
    private final String prompt;
    private final Map<String, Object> choiceParameters;
    private final List<String> validChoices;
    private final long timeoutMs;
    private final String defaultChoice;
    
    /**
     * Creates a player choice request event.
     * 
     * @param gameId The game ID
     * @param targetPlayer The player who needs to make a choice
     * @param choiceType The type of choice ("battery_usage", "planet_selection", "dock_decision", etc.)
     * @param prompt The human-readable prompt to show the player
     * @param choiceParameters Additional parameters needed for the choice
     * @param validChoices List of valid choice values
     * @param timeoutMs Timeout in milliseconds (0 = no timeout)
     * @param defaultChoice Default choice if timeout occurs
     */
    public PlayerChoiceRequestEvent(String gameId, PlayerId targetPlayer, String choiceType, 
                                  String prompt, Map<String, Object> choiceParameters,
                                  List<String> validChoices, long timeoutMs, String defaultChoice) {
        super(EventType.PLAYER_CHOICE_REQUEST, gameId, targetPlayer);
        this.targetPlayer = targetPlayer;
        this.choiceType = choiceType;
        this.prompt = prompt;
        this.choiceParameters = choiceParameters;
        this.validChoices = validChoices;
        this.timeoutMs = timeoutMs;
        this.defaultChoice = defaultChoice;
        
        LOGGER.info("PlayerChoiceRequestEvent created: " + choiceType + " for player " + targetPlayer + " in game " + gameId);
    }
    
    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Only process if this event is for the current player
            if (context.getClientState() != null && 
                targetPlayer.equals(context.getClientState().getPlayerId())) {
                
                LOGGER.info("Processing choice request: " + choiceType + " - " + prompt);
                
                // Show choice UI based on choice type
                switch (choiceType) {
                    case "battery_usage" -> showBatteryChoiceUI(context);
                    case "planet_selection" -> showPlanetChoiceUI(context);
                    case "dock_decision" -> showDockChoiceUI(context);
                    case "engine_strength" -> showEngineChoiceUI(context);
                    default -> showGenericChoiceUI(context);
                }
            }
        });
    }
    
    private void showBatteryChoiceUI(ClientEventContext context) {
        // Implementation for battery usage choice UI
        LOGGER.info("Showing battery usage UI: " + prompt);
        // TODO: Integrate with actual UI system
    }
    
    private void showPlanetChoiceUI(ClientEventContext context) {
        // Implementation for planet selection UI
        LOGGER.info("Showing planet selection UI: " + prompt);
        // TODO: Integrate with actual UI system
    }
    
    private void showDockChoiceUI(ClientEventContext context) {
        // Implementation for dock decision UI
        LOGGER.info("Showing dock decision UI: " + prompt);
        // TODO: Integrate with actual UI system
    }
    
    private void showEngineChoiceUI(ClientEventContext context) {
        // Implementation for engine strength UI
        LOGGER.info("Showing engine strength UI: " + prompt);
        // TODO: Integrate with actual UI system
    }
    
    private void showGenericChoiceUI(ClientEventContext context) {
        // Implementation for generic choice UI
        LOGGER.info("Showing generic choice UI: " + prompt);
        // TODO: Integrate with actual UI system
    }
    
    // Getters
    public PlayerId getTargetPlayer() { return targetPlayer; }
    public String getChoiceType() { return choiceType; }
    public String getPrompt() { return prompt; }
    public Map<String, Object> getChoiceParameters() { return choiceParameters; }
    public List<String> getValidChoices() { return validChoices; }
    public long getTimeoutMs() { return timeoutMs; }
    public String getDefaultChoice() { return defaultChoice; }
}
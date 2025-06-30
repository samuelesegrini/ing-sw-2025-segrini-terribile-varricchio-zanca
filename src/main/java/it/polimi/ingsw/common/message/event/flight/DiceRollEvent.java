package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

import java.util.List;
import java.util.logging.Logger;

/**
 * Broadcast by the server whenever dice are rolled to ensure all clients see the same random result.
 */
public class DiceRollEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(DiceRollEvent.class.getName());
    private final String purpose;
    private final List<Integer> diceValues;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param purpose the purpose of the dice roll
     * @param diceValues the rolled dices values
     */

    public DiceRollEvent(String gameId, String purpose, List<Integer> diceValues) {
        super(EventType.DICE_ROLLED, gameId, null);
        this.purpose = purpose;
        this.diceValues = List.copyOf(diceValues);
        LOGGER.fine("DiceRollEvent instantiated for game: " + gameId + ", purpose: " + purpose + ", values: " + diceValues);
    }

    /**
     *
     * @return  the purpose of the dice roll
     */

    public String getPurpose() {
        return purpose;
    }

    /**
     *
     * @return the rolled dices values
     */

    public List<Integer> getDiceValues() {
        return diceValues;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.fine("Handling DiceRollEvent for purpose: " + purpose + ", values: " + diceValues);
        
        // Show dice roll in the UI
        context.runOnUIThread(() -> {
            try {
                // Use notification service to display dice roll
                String rollMessage = String.format("🎲 Dice Roll (%s): %s", purpose, 
                    diceValues.toString().replaceAll("[\\[\\]]", ""));
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification("Dice Roll", rollMessage, 
                    it.polimi.ingsw.client.ui.NotificationType.INFO));
                
                // If TUI is active and supports dice display, show detailed dice roll
                var controller = context.getController();
                if (controller != null && controller.getCurrentView() != null) {
                    var currentView = controller.getCurrentView();
                    
                    // Check if current view is TuiFlightView and can show dice
                    if (currentView.getClass().getSimpleName().equals("TuiFlightView")) {
                        try {
                            // Use reflection to call showDiceRoll method if available
                            var showDiceMethod = currentView.getClass().getMethod("showDiceRoll", String.class, List.class);
                            showDiceMethod.invoke(currentView, purpose, diceValues);
                        } catch (Exception e) {
                            // Fallback to simple logging if reflection fails
                            LOGGER.info("Dice roll: " + purpose + " = " + diceValues);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to display dice roll in UI: " + e.getMessage());
            }
        });
    }
}
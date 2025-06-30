package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

import java.util.logging.Logger;

/**
 * Event broadcast when a combat encounter begins.
 * Initiates combat UI and prompts players for their combat decisions.
 */
public class CombatStartedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(CombatStartedEvent.class.getName());
    private final String enemyName;
    private final int enemyStrength;
    private final String enemyDescription;
    private final CombatType combatType;
    private final long decisionTimeLimit;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param enemyName the enemy name
     * @param enemyStrength the enemy strength
     * @param enemyDescription the enemy description
     * @param combatType the type of the combat
     * @param decisionTimeLimit the decision time limit
     */

    public CombatStartedEvent(String gameId, String enemyName, int enemyStrength, 
                             String enemyDescription, CombatType combatType, long decisionTimeLimit) {
        super(EventType.COMBAT_STARTED, gameId, null);
        this.enemyName = enemyName;
        this.enemyStrength = enemyStrength;
        this.enemyDescription = enemyDescription;
        this.combatType = combatType;
        this.decisionTimeLimit = decisionTimeLimit;
        LOGGER.fine("CombatStartedEvent instantiated for game: " + gameId + ", enemy: " + enemyName + ", type: " + combatType);
    }

    /**
     *
     * @return  the enemy name
     */

    public String getEnemyName() {
        return enemyName;
    }

    /**
     *
     * @return  the enemy strength
     */

    public int getEnemyStrength() {
        return enemyStrength;
    }

    /**
     *
     * @return  the enemy description
     */

    public String getEnemyDescription() {
        return enemyDescription;
    }

    /**
     *
     * @return the type of the combat
     */


    public CombatType getCombatType() {
        return combatType;
    }

    /**
     *
     * @return  the decision time limit
     */

    public long getDecisionTimeLimit() {
        return decisionTimeLimit;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with combat information
            if (context.getClientState() != null) {
                // context.getClientState().startCombat(enemyName, enemyStrength, combatType);
                // context.getClientState().setCombatTimeLimit(decisionTimeLimit);
            }

            // Show combat notification
            if (context.getNotificationService() != null) {
                String message = String.format("Combat! %s (Strength: %d)", enemyName, enemyStrength);
                if (decisionTimeLimit > 0) {
                    message += " - " + decisionTimeLimit + "s to decide";
                }
                LOGGER.fine("Displaying combat started notification: " + message);
                context.getNotificationService().showNotification(new Notification(
                        "Combat Encounter",
                        message,
                        NotificationType.ERROR
                ));
            }

            // Show combat UI
            // if (context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showCombatInterface(enemyName, enemyStrength, enemyDescription, combatType);
            //     // context.getGameUI().startCombatTimer(decisionTimeLimit);
            // }
        });
    }

    /**
     * Types of combat encounters in Galaxy Trucker.
     */
    public enum CombatType {
        PIRATES,        // Standard pirate combat
        SLAVERS,        // Slavers - special rules for crew
        SMUGGLERS,      // Smugglers - different rewards/penalties
        ALIEN_WARSHIP,  // Powerful alien combat encounter
        ABANDONED_FIGHTER; // Weaker but may have salvage
    }
}
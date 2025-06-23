package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when a combat encounter begins.
 * Initiates combat UI and prompts players for their combat decisions.
 */
public class CombatStartedEvent extends AbstractEvent {
    private final String enemyName;
    private final int enemyStrength;
    private final String enemyDescription;
    private final CombatType combatType;
    private final long decisionTimeLimit;

    public CombatStartedEvent(String gameId, String enemyName, int enemyStrength, 
                             String enemyDescription, CombatType combatType, long decisionTimeLimit) {
        super(EventType.COMBAT_STARTED, gameId, null);
        this.enemyName = enemyName;
        this.enemyStrength = enemyStrength;
        this.enemyDescription = enemyDescription;
        this.combatType = combatType;
        this.decisionTimeLimit = decisionTimeLimit;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public int getEnemyStrength() {
        return enemyStrength;
    }

    public String getEnemyDescription() {
        return enemyDescription;
    }

    public CombatType getCombatType() {
        return combatType;
    }

    public long getDecisionTimeLimit() {
        return decisionTimeLimit;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with combat information
            if (context.getGameState() != null) {
                // context.getGameState().startCombat(enemyName, enemyStrength, combatType);
                // context.getGameState().setCombatTimeLimit(decisionTimeLimit);
            }

            // Show combat notification
            if (context.getNotificationService() != null) {
                String message = String.format("Combat! %s (Strength: %d)", enemyName, enemyStrength);
                if (decisionTimeLimit > 0) {
                    message += " - " + decisionTimeLimit + "s to decide";
                }
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
        ABANDONED_FIGHTER // Weaker but may have salvage
    }
}
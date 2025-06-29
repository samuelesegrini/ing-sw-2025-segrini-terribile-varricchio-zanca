package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a combat encounter is resolved.
 * Shows the combat results and applies consequences to players.
 */
public class CombatResolvedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(CombatResolvedEvent.class.getName());
    private final String enemyName;
    private final String playerId;
    private final String playerNickname;
    private final int playerStrength;
    private final int enemyStrength;
    private final CombatOutcome outcome;
    private final CombatReward reward;
    private final CombatPenalty penalty;

    public CombatResolvedEvent(String gameId, String enemyName, String playerId, String playerNickname,
                              int playerStrength, int enemyStrength, CombatOutcome outcome,
                              CombatReward reward, CombatPenalty penalty) {
        super(EventType.COMBAT_RESOLVED, gameId, PlayerId.fromString(playerId));
        this.enemyName = enemyName;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.playerStrength = playerStrength;
        this.enemyStrength = enemyStrength;
        this.outcome = outcome;
        this.reward = reward;
        this.penalty = penalty;
        LOGGER.fine("CombatResolvedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", enemy: " + enemyName + ", outcome: " + outcome);
    }

    public String getEnemyName() {
        return enemyName;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getPlayerStrength() {
        return playerStrength;
    }

    public int getEnemyStrength() {
        return enemyStrength;
    }

    public CombatOutcome getOutcome() {
        return outcome;
    }

    public CombatReward getReward() {
        return reward;
    }

    public CombatPenalty getPenalty() {
        return penalty;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with combat results
            if (context.getClientState() != null) {
                // context.getClientState().resolveCombat(playerId, outcome, reward, penalty);
            }

            // Show combat result notification
            if (context.getNotificationService() != null) {
                String message;
                NotificationType notificationType;
                
                if (context.isLocalPlayer(playerId)) {
                    // Notification for the local player
                    message = String.format("Combat vs %s: %s (%d vs %d)", 
                            enemyName, outcome.getDescription(), playerStrength, enemyStrength);
                    if (reward != null) {
                        message += " - " + reward.getDescription();
                    }
                    if (penalty != null) {
                        message += " - " + penalty.getDescription();
                    }
                    notificationType = outcome == CombatOutcome.VICTORY ? NotificationType.INFO : NotificationType.WARNING;
                    LOGGER.fine("Displaying combat result notification for local player: " + message);
                } else {
                    // Notification for other players
                    message = String.format("%s fought %s: %s", 
                            playerNickname, enemyName, outcome.getDescription());
                    notificationType = NotificationType.INFO;
                    LOGGER.fine("Displaying combat result notification for other player: " + message);
                }
                context.getNotificationService().showNotification(new Notification(
                        "Combat Result",
                        message,
                        notificationType
                ));
            }

            // Update combat UI
            // if (context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showCombatResult(playerId, outcome, playerStrength, enemyStrength, reward, penalty);
            //     // context.getGameUI().hideCombatInterface();
            // }
        });
    }

    /**
     * Possible outcomes of combat encounters.
     */
    public enum CombatOutcome {
        VICTORY("Victory!"),
        DEFEAT("Defeated"),
        DRAW("Draw"),
        FLED("Fled from combat");

        private final String description;

        CombatOutcome(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents rewards gained from successful combat.
     */
    public static class CombatReward {
        private static final Logger LOGGER = Logger.getLogger(CombatReward.class.getName());
        private final int credits;
        private final int goods;
        private final String description;

        public CombatReward(int credits, int goods, String description) {
            this.credits = credits;
            this.goods = goods;
            this.description = description;
            LOGGER.fine("CombatReward instantiated: credits=" + credits + ", goods=" + goods + ", description='" + description + "'");
        }

        public int getCredits() {
            return credits;
        }

        public int getGoods() {
            return goods;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents penalties from failed combat.
     */
    public static class CombatPenalty {
        private static final Logger LOGGER = Logger.getLogger(CombatPenalty.class.getName());
        private final int creditsLost;
        private final int goodsLost;
        private final int crewLost;
        private final int componentsDamaged;
        private final String description;

        public CombatPenalty(int creditsLost, int goodsLost, int crewLost, 
                           int componentsDamaged, String description) {
            this.creditsLost = creditsLost;
            this.goodsLost = goodsLost;
            this.crewLost = crewLost;
            this.componentsDamaged = componentsDamaged;
            this.description = description;
            LOGGER.fine("CombatPenalty instantiated: creditsLost=" + creditsLost + ", goodsLost=" + goodsLost + ", crewLost=" + crewLost + ", componentsDamaged=" + componentsDamaged + ", description='" + description + "'");
        }

        public int getCreditsLost() {
            return creditsLost;
        }

        public int getGoodsLost() {
            return goodsLost;
        }

        public int getCrewLost() {
            return crewLost;
        }

        public int getComponentsDamaged() {
            return componentsDamaged;
        }

        public String getDescription() {
            return description;
        }
    }
}
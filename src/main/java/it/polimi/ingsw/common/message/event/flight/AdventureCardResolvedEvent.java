package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.Map;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event fired when an adventure card has been fully resolved.
 * Contains all the results and effects that were applied during resolution.
 */
public class AdventureCardResolvedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardResolvedEvent.class.getName());
    
    private final AdventureCard card;
    private final Map<PlayerId, Map<String, Object>> playerResults;
    private final List<String> globalEffects;
    private final boolean cardCompleted;
    
    /**
     * Creates an adventure card resolved event.
     * 
     * @param gameId The game ID
     * @param card The adventure card that was resolved
     * @param playerResults Map of player-specific results and effects
     * @param globalEffects List of global effects that affected all players
     * @param cardCompleted Whether the card was fully completed or skipped
     */
    public AdventureCardResolvedEvent(String gameId, AdventureCard card,
                                    Map<PlayerId, Map<String, Object>> playerResults,
                                    List<String> globalEffects, boolean cardCompleted) {
        super(EventType.ADVENTURE_CARD_RESOLVED, gameId, null);
        this.card = card;
        this.playerResults = playerResults;
        this.globalEffects = globalEffects;
        this.cardCompleted = cardCompleted;
        
        LOGGER.info("AdventureCardResolvedEvent created: " + card.getType() + 
                   " for game " + gameId + " (completed: " + cardCompleted + ")");
    }
    
    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.info("Processing adventure card resolution: " + card.getType());
            
            // Update client state with resolution results
            if (context.getClientState() != null) {
                PlayerId clientPlayerId = context.getClientState().getPlayerIdObject();
                
                // Get results specific to this client
                Map<String, Object> clientResults = playerResults.get(clientPlayerId);
                if (clientResults != null) {
                    applyPlayerResults(context, clientResults);
                }
                
                // Apply global effects
                applyGlobalEffects(context, globalEffects);
                
                // Show resolution summary
                showResolutionSummary(context);
            }
        });
    }
    
    private void applyPlayerResults(ClientEventContext context, Map<String, Object> results) {
        LOGGER.info("Applying player-specific results: " + results.size() + " effects");
        
        for (Map.Entry<String, Object> result : results.entrySet()) {
            String effectType = result.getKey();
            Object effectValue = result.getValue();
            
            switch (effectType) {
                case "credits_gained" -> {
                    int credits = (Integer) effectValue;
                    LOGGER.info("Player gained " + credits + " credits");
                    // TODO: Update client credit display
                }
                case "credits_lost" -> {
                    int credits = (Integer) effectValue;
                    LOGGER.info("Player lost " + credits + " credits");
                    // TODO: Update client credit display
                }
                case "position_changed" -> {
                    int newPosition = (Integer) effectValue;
                    LOGGER.info("Player moved to position " + newPosition);
                    // TODO: Update client flight board display
                }
                case "components_damaged" -> {
                    @SuppressWarnings("unchecked")
                    List<String> damagedComponents = (List<String>) effectValue;
                    LOGGER.info("Components damaged: " + damagedComponents);
                    // TODO: Update client ship display
                }
                case "goods_gained" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> goods = (Map<String, Integer>) effectValue;
                    LOGGER.info("Goods gained: " + goods);
                    // TODO: Update client cargo display
                }
                case "eliminated" -> {
                    boolean eliminated = (Boolean) effectValue;
                    if (eliminated) {
                        LOGGER.info("Player eliminated from flight");
                        // TODO: Show elimination message
                    }
                }
                default -> LOGGER.warning("Unknown effect type: " + effectType);
            }
        }
    }
    
    private void applyGlobalEffects(ClientEventContext context, List<String> effects) {
        LOGGER.info("Applying global effects: " + effects.size() + " effects");
        
        for (String effect : effects) {
            LOGGER.info("Global effect: " + effect);
            // TODO: Apply global effects to client state
        }
    }
    
    private void showResolutionSummary(ClientEventContext context) {
        // Show a summary of what happened during card resolution
        if (context.getNotificationService() != null) {
            String summary = buildResolutionSummary();
            // TODO: Show resolution summary notification
            LOGGER.info("Resolution summary: " + summary);
        }
    }
    
    private String buildResolutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(card.getType()).append(" resolved");
        
        if (!cardCompleted) {
            summary.append(" (skipped)");
        }
        
        return summary.toString();
    }
    
    // Getters
    public AdventureCard getCard() { return card; }
    public Map<PlayerId, Map<String, Object>> getPlayerResults() { return playerResults; }
    public List<String> getGlobalEffects() { return globalEffects; }
    public boolean isCardCompleted() { return cardCompleted; }
}
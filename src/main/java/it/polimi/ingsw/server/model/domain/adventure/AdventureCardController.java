package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls the flow and execution of adventure cards during the flight phase.
 * Manages player turns, timeouts, and card resolution.
 */
public class AdventureCardController {
    private final ScheduledExecutorService executor;
    private final Map<String, AdventureCardState> activeCards;
    private AdventureCard currentCard;
    private AdventureCardState currentState;
    
    public AdventureCardController() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.activeCards = new ConcurrentHashMap<>();
    }
    
    public AdventureCardController(Object gameModel, Object eventPublisher) {
        this(); // Use default constructor
    }
    
    /**
     * Starts a new adventure card for the given player.
     * 
     * @param card The adventure card to start
     * @param playerId The player who drew the card
     */
    public void startAdventureCard(AdventureCard card, PlayerId playerId) {
        this.currentCard = card;
        this.currentState = new AdventureCardState(card, playerId.toString());
        activeCards.put(playerId.toString(), currentState);
        
        // Start the timeout timer if applicable
        if (requiresTimeout(card)) {
            scheduleTimeout(playerId, 30000); // 30-second timeout
        }
    }
    
    /**
     * Records a player's choice for the current adventure card.
     * 
     * @param playerId The player making the choice
     * @param choiceType The type of choice being made
     * @param value The choice value
     */
    public void recordPlayerChoice(PlayerId playerId, String choiceType, Object value) {
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            AdventureCardState.PlayerChoice choice = state.getPlayerChoice(playerId);
            if (choice != null) {
                // Record the choice (simplified implementation)
                choice.setParameter(choiceType, value);
            }
        }
    }
    
    /**
     * Processes combat strength declaration for combat cards.
     * 
     * @param playerId The player declaring strength
     * @param combatStrength The declared combat strength
     * @param engineStrength The declared engine strength  
     * @param crewStrength The crew strength bonus
     * @param batteryUsed Number of batteries used
     */
    public void processCombatStrength(PlayerId playerId, int combatStrength, int engineStrength, int crewStrength, int batteryUsed) {
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null && state.getCard().getType() == AdventureType.PIRATES) {
            AdventureCardState.PlayerChoice choice = state.getPlayerChoice(playerId);
            if (choice != null) {
                choice.setParameter("combatStrength", combatStrength);
                choice.setParameter("engineStrength", engineStrength);
                choice.setParameter("crewStrength", crewStrength);
                choice.setParameter("batteryUsed", batteryUsed);
            }
        }
    }
    
    /**
     * Resolves the current adventure card based on player choices.
     * 
     * @param playerId The player whose card is being resolved
     */
    public void resolveCard(PlayerId playerId) {
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            // Simplified resolution - actual implementation would handle different card types
            state.setResolved(true);
            activeCards.remove(playerId.toString());
        }
    }
    
    /**
     * Cleans up resources and shuts down the controller.
     */
    public void cleanup() {
        activeCards.clear();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Gets the current adventure card state for a player.
     * 
     * @param playerId The player ID
     * @return The adventure card state, or null if none active
     */
    public AdventureCardState getCardState(PlayerId playerId) {
        return activeCards.get(playerId.toString());
    }
    
    /**
     * Checks if the controller is currently processing a card.
     * 
     * @return true if processing a card
     */
    public boolean isProcessingCard() {
        return !activeCards.isEmpty();
    }
    
    /**
     * Handles a player's choice for an adventure card.
     * 
     * @param playerId The player making the choice
     * @param choice The player's choice
     * @return true if the choice was successfully processed
     */
    public boolean handlePlayerChoice(PlayerId playerId, AdventureCardState.PlayerChoice choice) {
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            // Process the choice based on the card type
            AdventureCard card = state.getCard();
            
            // Apply the choice effects based on card type
            switch (card.getType()) {
                case PIRATES, SLAVERS -> processCombatChoice(playerId, choice);
                case PLANETS -> processPlanetChoice(playerId, choice);
                case ABANDONED_SHIP, ABANDONED_STATION -> processAbandonedLocationChoice(playerId, choice);
                default -> {
                    // Generic choice processing
                    state.setResolved(true);
                    activeCards.remove(playerId.toString());
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the given card type requires a timeout.
     * 
     * @param card The adventure card
     * @return true if timeout is required
     */
    private boolean requiresTimeout(AdventureCard card) {
        return card.getType() == AdventureType.PIRATES || 
               card.getType() == AdventureType.PLANETS;
    }
    
    /**
     * Schedules a timeout for the given player's card.
     * 
     * @param playerId The player ID
     * @param timeoutMs Timeout in milliseconds
     */
    private void scheduleTimeout(PlayerId playerId, long timeoutMs) {
        executor.schedule(() -> {
            AdventureCardState state = activeCards.get(playerId.toString());
            if (state != null && !state.isResolved()) {
                // Handle timeout - use default choices
                handleTimeout(playerId);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Handles timeout for a player's adventure card.
     * 
     * @param playerId The player who timed out
     */
    private void handleTimeout(PlayerId playerId) {
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            // Use default/minimal choices for timeout
            AdventureCardState.PlayerChoice choice = state.getPlayerChoice(playerId);
            if (choice != null && state.getCard().getType() == AdventureType.PIRATES) {
                choice.setParameter("combatStrength", 0);
                choice.setParameter("engineStrength", 0);
                choice.setParameter("crewStrength", 0);
                choice.setParameter("batteryUsed", 0);
            }
            resolveCard(playerId);
        }
    }
    
    /**
     * Processes a combat choice for combat-type cards.
     */
    private void processCombatChoice(PlayerId playerId, AdventureCardState.PlayerChoice choice) {
        // Simplified implementation - just resolve the card
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            state.setResolved(true);
            activeCards.remove(playerId.toString());
        }
    }
    
    /**
     * Processes a planet choice for planet cards.
     */
    private void processPlanetChoice(PlayerId playerId, AdventureCardState.PlayerChoice choice) {
        // Simplified implementation - just resolve the card
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            state.setResolved(true);
            activeCards.remove(playerId.toString());
        }
    }
    
    /**
     * Processes a choice for abandoned location cards.
     */
    private void processAbandonedLocationChoice(PlayerId playerId, AdventureCardState.PlayerChoice choice) {
        // Simplified implementation - just resolve the card
        AdventureCardState state = activeCards.get(playerId.toString());
        if (state != null) {
            state.setResolved(true);
            activeCards.remove(playerId.toString());
        }
    }
    
    /**
     * Starts card resolution for a drawn adventure card.
     * 
     * @param card The adventure card to start resolution for
     */
    public void startCardResolution(AdventureCard card) {
        // Implementation would start the card resolution process
        // For now, just a placeholder
    }
}
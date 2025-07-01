package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Event fired when it's a specific player's turn to make choices for an adventure card.
 * Some cards (like Pirates, Planets, Combat Zone) require players to act in flight order.
 */
public class AdventureCardPlayerTurnEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardPlayerTurnEvent.class.getName());
    
    private final AdventureCard card;
    private final PlayerId currentPlayer;
    private final List<PlayerId> turnOrder;
    private final int currentTurnIndex;
    private final String turnContext;
    private final Map<String, Object> turnData;
    private final long turnTimeoutMs;
    
    /**
     * Creates an adventure card player turn event.
     * 
     * @param gameId The game ID
     * @param card The adventure card being resolved
     * @param currentPlayer The player whose turn it is
     * @param turnOrder The complete turn order for this card
     * @param currentTurnIndex The current index in the turn order
     * @param turnContext Description of what the player needs to do
     * @param turnData Additional data specific to this turn
     * @param turnTimeoutMs Timeout for this turn in milliseconds (0 = no timeout)
     */
    public AdventureCardPlayerTurnEvent(String gameId, AdventureCard card, PlayerId currentPlayer,
                                      List<PlayerId> turnOrder, int currentTurnIndex, 
                                      String turnContext, Map<String, Object> turnData,
                                      long turnTimeoutMs) {
        super(EventType.ADVENTURE_CARD_PLAYER_TURN, gameId, currentPlayer);
        this.card = card;
        this.currentPlayer = currentPlayer;
        this.turnOrder = turnOrder;
        this.currentTurnIndex = currentTurnIndex;
        this.turnContext = turnContext;
        this.turnData = turnData;
        this.turnTimeoutMs = turnTimeoutMs;
        
        LOGGER.info("AdventureCardPlayerTurnEvent created: " + card.toString() +
                   " - Player " + currentPlayer + "'s turn (" + (currentTurnIndex + 1) + "/" + turnOrder.size() + ")");
    }
    
    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            if (context.getClientState() != null) {
                PlayerId clientPlayerId = PlayerId.fromString(context.getClientState().getPlayerId()); // TODO 00: NON SO SE FUNZIONI
                
                if (currentPlayer.equals(clientPlayerId)) {
                    // It's this client's turn
                    handleMyTurn(context);
                } else {
                    // It's another player's turn - show waiting UI
                    handleOtherPlayerTurn(context);
                }
                
                // Update turn indicator for all players
                updateTurnIndicator(context);
            }
        });
    }
    
    private void handleMyTurn(ClientEventContext context) {
        LOGGER.info("It's my turn for " + card.toString() + ": " + turnContext);
        
        // Show turn-specific UI based on card type and context
        switch (card.getType()) {
            case PIRATES, SLAVERS, COMBAT_ZONE -> showCombatTurnUI(context);
            case PLANETS -> showPlanetsTurnUI(context);
            case ABANDONED_SHIP, ABANDONED_STATION -> showAbandonedLocationTurnUI(context);
            case OPEN_SPACE -> showOpenSpaceTurnUI(context);
            default -> showGenericTurnUI(context);
        }
        
        // Start turn timer if applicable
        if (turnTimeoutMs > 0) {
            startTurnTimer(context);
        }
    }
    
    private void handleOtherPlayerTurn(ClientEventContext context) {
        LOGGER.info("Waiting for player " + currentPlayer + " to complete their turn");
        
        // Show waiting UI
        showWaitingForPlayerUI(context);
    }
    
    private void updateTurnIndicator(ClientEventContext context) {
        // Update UI to show current turn order and who's active
        LOGGER.info("Turn " + (currentTurnIndex + 1) + "/" + turnOrder.size() + 
                   " - Current player: " + currentPlayer);
        
        // TODO: Update actual UI turn indicator
    }
    
    private void showCombatTurnUI(ClientEventContext context) {
        // Show combat-specific turn UI (declare strength, use batteries, etc.)
        LOGGER.info("Showing combat turn UI: " + turnContext);
        
        // Extract combat-specific data
        Integer enemyStrength = (Integer) turnData.get("enemy_strength");
        Integer maxBatteries = (Integer) turnData.get("max_batteries");
        String combatType = (String) turnData.get("combat_type"); // "cannons" or "engines"
        
        LOGGER.info("Combat parameters - Enemy: " + enemyStrength + 
                   ", Max Batteries: " + maxBatteries + ", Type: " + combatType);
        
        // TODO: Show actual combat UI with strength calculator
    }
    
    private void showPlanetsTurnUI(ClientEventContext context) {
        // Show planet selection UI
        LOGGER.info("Showing planets turn UI: " + turnContext);
        
        // Extract planet-specific data
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> availablePlanets = (List<Map<String, Object>>) turnData.get("available_planets");
        Integer cargoSpace = (Integer) turnData.get("cargo_space");
        
        LOGGER.info("Planet selection - Available: " + availablePlanets.size() + 
                   ", Cargo Space: " + cargoSpace);
        
        // TODO: Show actual planet selection UI
    }
    
    private void showAbandonedLocationTurnUI(ClientEventContext context) {
        // Show dock/skip decision UI
        LOGGER.info("Showing abandoned location turn UI: " + turnContext);
        
        // Extract location-specific data
        Integer requiredCrew = (Integer) turnData.get("required_crew");
        Integer playerCrew = (Integer) turnData.get("player_crew");
        String reward = (String) turnData.get("reward");
        
        LOGGER.info("Abandoned location - Required crew: " + requiredCrew + 
                   ", Player crew: " + playerCrew + ", Reward: " + reward);
        
        // TODO: Show actual dock/skip UI
    }
    
    private void showOpenSpaceTurnUI(ClientEventContext context) {
        // Show engine strength declaration UI
        LOGGER.info("Showing open space turn UI: " + turnContext);
        
        // Extract engine-specific data
        Integer maxBatteries = (Integer) turnData.get("max_batteries");
        Integer baseEngineStrength = (Integer) turnData.get("base_engine_strength");
        
        LOGGER.info("Open space - Max Batteries: " + maxBatteries + 
                   ", Base Engine Strength: " + baseEngineStrength);
        
        // TODO: Show actual engine strength UI
    }
    
    private void showGenericTurnUI(ClientEventContext context) {
        // Show generic turn UI for unknown card types
        LOGGER.info("Showing generic turn UI: " + turnContext);
        // TODO: Show actual generic UI
    }
    
    private void showWaitingForPlayerUI(ClientEventContext context) {
        // Show waiting indicator
        if (context.getNotificationService() != null) {
            // TODO: Show waiting notification
        }
        
        // TODO: Update UI to show waiting state
    }
    
    private void startTurnTimer(ClientEventContext context) {
        // Start countdown timer for this turn
        LOGGER.info("Starting turn timer: " + turnTimeoutMs + "ms");
        // TODO: Implement actual turn timer UI
    }
    
    // Getters
    public AdventureCard getCard() { return card; }
    public PlayerId getCurrentPlayer() { return currentPlayer; }
    public List<PlayerId> getTurnOrder() { return turnOrder; }
    public int getCurrentTurnIndex() { return currentTurnIndex; }
    public String getTurnContext() { return turnContext; }
    public Map<String, Object> getTurnData() { return turnData; }
    public long getTurnTimeoutMs() { return turnTimeoutMs; }
}
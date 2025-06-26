package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the state of an adventure card during player choice resolution.
 * Implements the state machine for turn-based adventure card processing.
 */
public class AdventureCardState {
    
    public enum CardProcessingState {
        REVEALED,           // Card drawn, awaiting choices
        COLLECTING_CHOICES, // Players making decisions
        RESOLVING,          // Applying choices
        COMPLETED           // Card fully processed
    }
    
    public enum PlayerChoiceState {
        WAITING_FOR_TURN,   // Player hasn't had their turn yet
        CHOOSING,           // Player's turn to make choice
        CHOSEN,             // Player has made their choice
        SKIPPED,            // Player skipped their turn (timeout/abandon)
        NOT_ELIGIBLE        // Player not eligible for this card
    }
    
    private final AdventureCard card;
    private final String gameId;
    private CardProcessingState processingState;
    private final Map<PlayerId, PlayerChoiceState> playerStates;
    private final Map<PlayerId, PlayerChoice> playerChoices;
    private PlayerId currentPlayerTurn;
    private long turnStartTime;
    private static final long TURN_TIMEOUT_MS = 30000; // 30 seconds per turn
    
    public AdventureCardState(AdventureCard card, String gameId) {
        this.card = card;
        this.gameId = gameId;
        this.processingState = CardProcessingState.REVEALED;
        this.playerStates = new ConcurrentHashMap<>();
        this.playerChoices = new ConcurrentHashMap<>();
        this.turnStartTime = System.currentTimeMillis();
    }
    
    public AdventureCard getCard() {
        return card;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public CardProcessingState getProcessingState() {
        return processingState;
    }
    
    public void setProcessingState(CardProcessingState state) {
        this.processingState = state;
    }
    
    public PlayerChoiceState getPlayerState(PlayerId playerId) {
        return playerStates.getOrDefault(playerId, PlayerChoiceState.NOT_ELIGIBLE);
    }
    
    public void setPlayerState(PlayerId playerId, PlayerChoiceState state) {
        playerStates.put(playerId, state);
    }
    
    public PlayerId getCurrentPlayerTurn() {
        return currentPlayerTurn;
    }
    
    public void setCurrentPlayerTurn(PlayerId playerId) {
        this.currentPlayerTurn = playerId;
        this.turnStartTime = System.currentTimeMillis();
        if (playerId != null) {
            setPlayerState(playerId, PlayerChoiceState.CHOOSING);
        }
    }
    
    public void setPlayerChoice(PlayerId playerId, PlayerChoice choice) {
        playerChoices.put(playerId, choice);
        setPlayerState(playerId, PlayerChoiceState.CHOSEN);
    }
    
    public PlayerChoice getPlayerChoice(PlayerId playerId) {
        return playerChoices.get(playerId);
    }
    
    public Map<PlayerId, PlayerChoice> getAllPlayerChoices() {
        return new HashMap<>(playerChoices);
    }
    
    public boolean hasPlayerMadeChoice(PlayerId playerId) {
        return playerChoices.containsKey(playerId);
    }
    
    public boolean isPlayersTurn(PlayerId playerId) {
        return playerId != null && playerId.equals(currentPlayerTurn);
    }
    
    public boolean hasTurnTimedOut() {
        return System.currentTimeMillis() - turnStartTime > TURN_TIMEOUT_MS;
    }
    
    public long getRemainingTurnTime() {
        long elapsed = System.currentTimeMillis() - turnStartTime;
        return Math.max(0, TURN_TIMEOUT_MS - elapsed);
    }
    
    public boolean areAllEligiblePlayersFinished() {
        return playerStates.entrySet().stream()
            .filter(entry -> entry.getValue() != PlayerChoiceState.NOT_ELIGIBLE)
            .allMatch(entry -> 
                entry.getValue() == PlayerChoiceState.CHOSEN || 
                entry.getValue() == PlayerChoiceState.SKIPPED
            );
    }
    
    public void skipCurrentPlayer() {
        if (currentPlayerTurn != null) {
            setPlayerState(currentPlayerTurn, PlayerChoiceState.SKIPPED);
            currentPlayerTurn = null;
        }
    }
    
    public void initializeEligiblePlayers(java.util.List<PlayerId> playerOrder) {
        for (PlayerId playerId : playerOrder) {
            setPlayerState(playerId, PlayerChoiceState.WAITING_FOR_TURN);
        }
    }
    
    /**
     * Represents a player's choice for an adventure card.
     */
    public static class PlayerChoice {
        private final PlayerId playerId;
        private final AdventureChoiceType choiceType;
        private final Map<String, Object> parameters;
        
        public PlayerChoice(PlayerId playerId, AdventureChoiceType choiceType) {
            this.playerId = playerId;
            this.choiceType = choiceType;
            this.parameters = new HashMap<>();
        }
        
        public PlayerId getPlayerId() {
            return playerId;
        }
        
        public AdventureChoiceType getChoiceType() {
            return choiceType;
        }
        
        public void setParameter(String key, Object value) {
            parameters.put(key, value);
        }
        
        public Object getParameter(String key) {
            return parameters.get(key);
        }
        
        public int getIntParameter(String key, int defaultValue) {
            Object value = parameters.get(key);
            return value instanceof Integer ? (Integer) value : defaultValue;
        }
        
        public Map<String, Object> getAllParameters() {
            return new HashMap<>(parameters);
        }
    }
    
    /**
     * Types of choices players can make for adventure cards.
     */
    public enum AdventureChoiceType {
        PLANET_CHOICE,          // Choose which planet to land on (or skip)
        COMBAT_STRENGTH,        // Declare cannon strength for combat
        ENGINE_STRENGTH,        // Declare engine strength for open space
        ABANDONED_SHIP_CHOICE,  // Choose to dock at abandoned ship
        ABANDONED_STATION_CHOICE, // Choose to dock at abandoned station
        SKIP_TURN              // Skip this opportunity
    }
    
    private boolean resolved = false;
    
    /**
     * Sets whether this card state is resolved.
     */
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
    
    /**
     * Checks if this card state is resolved.
     */
    public boolean isResolved() {
        return resolved;
    }
}
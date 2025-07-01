package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.common.message.event.flight.AdventureCardPlayerTurnEvent;
import it.polimi.ingsw.common.message.event.flight.PlayerChoiceRequestEvent;
import it.polimi.ingsw.common.message.event.flight.AdventureCardResolvedEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Enhanced Adventure Card Controller with complete turn and choice management.
 * Manages the complete flow: turns, choices, resolution, and events.
 */
public class AdventureCardController {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardController.class.getName());
    
    private final GameModel gameModel;
    private final ScheduledExecutorService executor;
    
    // Current card state
    private AdventureCard currentCard;
    private Map<PlayerId, Map<String, Object>> playerChoices;
    private List<PlayerId> turnOrder;
    private int currentTurnIndex;
    private boolean cardResolutionInProgress;
    
    // Turn-based vs simultaneous cards
    private boolean isTurnBasedCard;
    private Set<PlayerId> pendingChoices;
    
    public AdventureCardController(GameModel gameModel) {
        if (gameModel == null) {
            throw new IllegalArgumentException("GameModel cannot be null");
        }
        
        this.gameModel = gameModel;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.playerChoices = new ConcurrentHashMap<>();
        this.pendingChoices = new HashSet<>();
        this.cardResolutionInProgress = false;
        
        LOGGER.info("AdventureCardController initialized with GameModel");
    }
    
    /**
     * Starts card resolution for a drawn adventure card.
     * This is the main entry point for card processing.
     */
    public void startCardResolution(AdventureCard card) {
        if (card == null) {
            LOGGER.warning("Cannot start resolution for null card");
            return;
        }
        
        if (cardResolutionInProgress) {
            LOGGER.warning("Card resolution already in progress, skipping: " + card.getName());
            return;
        }
        
        LOGGER.info("Starting resolution for adventure card: " + card.getName());
        
        this.currentCard = card;
        this.cardResolutionInProgress = true;
        this.playerChoices.clear();
        this.pendingChoices.clear();
        this.currentTurnIndex = 0;
        
        // Determine card flow type and start appropriate resolution
        this.isTurnBasedCard = requiresTurns(card);
        this.turnOrder = getFlightOrder();
        
        if (isTurnBasedCard) {
            startTurnBasedResolution();
        } else if (requiresPlayerChoices(card)) {
            startSimultaneousChoiceResolution();
        } else {
            // Cards that need no player input (Meteor Swarm, Stardust, etc.)
            startImmediateResolution();
        }
    }
    
    /**
     * Records a player's choice for the current adventure card.
     */
    public boolean recordPlayerChoice(PlayerId playerId, String choiceType, String choiceValue) {
        if (playerId == null || choiceType == null || choiceValue == null) {
            LOGGER.warning("Invalid choice parameters");
            return false;
        }
        
        if (!cardResolutionInProgress || currentCard == null) {
            LOGGER.warning("No card resolution in progress for choice from: " + playerId);
            return false;
        }
        
        LOGGER.info("Recording choice: " + choiceType + "=" + choiceValue + " from player " + playerId);
        
        // Store the choice
        playerChoices.computeIfAbsent(playerId, k -> new HashMap<>()).put(choiceType, choiceValue);
        pendingChoices.remove(playerId);
        
        // Check if we can proceed
        if (isTurnBasedCard) {
            // For turn-based cards, advance to next turn
            advanceToNextTurn();
        } else {
            // For simultaneous cards, check if all players have chosen
            if (pendingChoices.isEmpty()) {
                completeCardResolution();
            }
        }
        
        return true;
    }
    
    // === CARD RESOLUTION FLOW METHODS ===
    
    private void startTurnBasedResolution() {
        LOGGER.info("Starting turn-based resolution for: " + currentCard.getName());
        
        if (turnOrder.isEmpty()) {
            completeCardResolution();
            return;
        }
        
        currentTurnIndex = 0;
        processCurrentPlayerTurn();
    }
    
    private void startSimultaneousChoiceResolution() {
        LOGGER.info("Starting simultaneous choice resolution for: " + currentCard.getName());
        
        // Request choices from all players simultaneously
        for (PlayerId playerId : turnOrder) {
            requestPlayerChoice(playerId);
        }
    }
    
    private void startImmediateResolution() {
        LOGGER.info("Starting immediate resolution for: " + currentCard.getName());
        
        // Apply card effects directly using visitor pattern
        applyCardEffects();
        completeCardResolution();
    }
    
    private void processCurrentPlayerTurn() {
        if (currentTurnIndex >= turnOrder.size()) {
            // All turns completed
            completeCardResolution();
            return;
        }
        
        PlayerId currentPlayer = turnOrder.get(currentTurnIndex);
        LOGGER.info("Processing turn for player: " + currentPlayer + " (" + (currentTurnIndex + 1) + "/" + turnOrder.size() + ")");
        
        // Send turn event to all players
        Map<String, Object> turnData = buildTurnData(currentPlayer);
        String turnContext = buildTurnContext(currentPlayer);
        
        AdventureCardPlayerTurnEvent turnEvent = new AdventureCardPlayerTurnEvent(
            gameModel.getGameId(),
            currentCard,
            currentPlayer,
            turnOrder,
            currentTurnIndex,
            turnContext,
            turnData,
            30000 // 30 second timeout
        );
        
        // Fire event via GameModel
        if (gameModel.getPropertyChangeSupport() != null) {
            gameModel.getPropertyChangeSupport().firePropertyChange("eventPublished", null, turnEvent);
        }
        
        // Add player to pending choices
        pendingChoices.add(currentPlayer);
        
        // Schedule timeout for this turn
        schedulePlayerTimeout(currentPlayer, 30000);
    }
    
    private void requestPlayerChoice(PlayerId playerId) {
        LOGGER.info("Requesting choice from player: " + playerId);
        
        Map<String, Object> choiceParams = buildChoiceParameters(playerId);
        List<String> validChoices = buildValidChoices(playerId);
        String prompt = buildChoicePrompt(playerId);
        String choiceType = getChoiceType();
        
        PlayerChoiceRequestEvent choiceEvent = new PlayerChoiceRequestEvent(
            gameModel.getGameId(),
            playerId,
            choiceType,
            prompt,
            choiceParams,
            validChoices,
            30000, // 30 second timeout
            getDefaultChoice(playerId)
        );
        
        // Fire event via GameModel
        if (gameModel.getPropertyChangeSupport() != null) {
            gameModel.getPropertyChangeSupport().firePropertyChange("eventPublished", null, choiceEvent);
        }
        
        pendingChoices.add(playerId);
        schedulePlayerTimeout(playerId, 30000);
    }
    
    private void advanceToNextTurn() {
        currentTurnIndex++;
        processCurrentPlayerTurn();
    }
    
    private void completeCardResolution() {
        LOGGER.info("Completing card resolution for: " + currentCard.getName());
        
        // Apply card effects with player choices
        Map<PlayerId, Map<String, Object>> results = applyCardEffectsWithChoices();
        
        // Create resolution event
        AdventureCardResolvedEvent resolvedEvent = new AdventureCardResolvedEvent(
            gameModel.getGameId(),
            currentCard,
            results,
            buildGlobalEffects(),
            true
        );
        
        // Fire event via GameModel
        if (gameModel.getPropertyChangeSupport() != null) {
            gameModel.getPropertyChangeSupport().firePropertyChange("eventPublished", null, resolvedEvent);
        }
        
        // Reset state
        resetCardState();
    }
    
    private void resetCardState() {
        this.currentCard = null;
        this.cardResolutionInProgress = false;
        this.playerChoices.clear();
        this.pendingChoices.clear();
        this.currentTurnIndex = 0;
        this.isTurnBasedCard = false;
    }
    
    // === CARD TYPE CLASSIFICATION ===
    
    private boolean requiresTurns(AdventureCard card) {
        return switch (card.getType()) {
            case PIRATES, SLAVERS, COMBAT_ZONE -> true;
            case PLANETS -> true;
            case ABANDONED_SHIP, ABANDONED_STATION -> true;
            case OPEN_SPACE -> true; // Fixed: Open Space is turn-based sequential
            default -> false;
        };
    }
    
    private boolean requiresPlayerChoices(AdventureCard card) {
        return switch (card.getType()) {
            case METEOR_SWARM -> false; // Immediate dice rolls
            case STARDUST -> false; // Immediate exposed connector count
            case EPIDEMIC -> false; // Immediate connected cabin analysis
            case SABOTAGE -> false; // Immediate random component destruction
            default -> requiresTurns(card); // Turn-based cards also need choices
        };
    }
    
    private String getChoiceType() {
        return switch (currentCard.getType()) {
            case PIRATES, SLAVERS, COMBAT_ZONE -> "battery_usage";
            case PLANETS -> "planet_selection";
            case ABANDONED_SHIP, ABANDONED_STATION -> "dock_decision";
            case OPEN_SPACE -> "engine_strength";
            default -> "generic_choice";
        };
    }
    
    // === HELPER METHODS ===
    
    private List<PlayerId> getFlightOrder() {
        // Get current flight order from FlightBoard
        if (gameModel.getFlightBoard() != null) {
            return gameModel.getFlightBoard().getCurrentOrder().stream()
                    .map(Player::getId)
                    .toList();
        }
        
        // Fallback to player list order
        return gameModel.getPlayers().stream()
                .map(Player::getId)
                .toList();
    }
    
    private Map<String, Object> buildTurnData(PlayerId playerId) {
        Map<String, Object> data = new HashMap<>();
        Player player = gameModel.getPlayerById(playerId);
        
        if (player != null && player.getShip() != null) {
            data.put("max_batteries", player.getShip().getBatteries());
            data.put("player_crew", player.getShip().getCrewCount());
            data.put("cargo_space", player.getShip().getCargoCapacity());
            
            // Card-specific data
            switch (currentCard.getType()) {
                case PIRATES, SLAVERS -> {
                    data.put("enemy_strength", 4); // TODO: Get from card
                    data.put("combat_type", "cannons");
                }
                case PLANETS -> {
                    data.put("available_planets", new ArrayList<>()); // TODO: Get from card
                }
                case ABANDONED_SHIP, ABANDONED_STATION -> {
                    data.put("required_crew", 2); // TODO: Get from card
                    data.put("reward", "2 credits"); // TODO: Get from card
                }
                case OPEN_SPACE -> {
                    data.put("max_engine_strength", player.getShip().getEngines());
                    data.put("base_engines", player.getShip().getEngines()); // TODO: distinguish base vs double engines
                }
            }
        }
        
        return data;
    }
    
    private String buildTurnContext(PlayerId playerId) {
        return switch (currentCard.getType()) {
            case PIRATES -> "Declare combat strength against Pirates";
            case SLAVERS -> "Declare combat strength against Slavers";
            case COMBAT_ZONE -> "Declare strength for Combat Zone";
            case PLANETS -> "Choose planet to land on or skip";
            case ABANDONED_SHIP -> "Choose to dock or skip";
            case ABANDONED_STATION -> "Choose to dock or skip";
            case OPEN_SPACE -> "Declare engine strength to move forward";
            default -> "Make your choice";
        };
    }
    
    private Map<String, Object> buildChoiceParameters(PlayerId playerId) {
        return buildTurnData(playerId);
    }
    
    private List<String> buildValidChoices(PlayerId playerId) {
        Player player = gameModel.getPlayerById(playerId);
        List<String> choices = new ArrayList<>();
        
        switch (currentCard.getType()) {
            case PIRATES, SLAVERS, COMBAT_ZONE, OPEN_SPACE -> {
                // Battery usage choices
                int maxBatteries = player != null && player.getShip() != null ? 
                    player.getShip().getBatteries() : 0;
                for (int i = 0; i <= maxBatteries; i++) {
                    choices.add(String.valueOf(i));
                }
            }
            case PLANETS -> {
                choices.add("skip");
                choices.add("planet_1");
                choices.add("planet_2");
                choices.add("planet_3");
                // TODO: Get actual planets from card
            }
            case ABANDONED_SHIP, ABANDONED_STATION -> {
                choices.add("dock");
                choices.add("skip");
            }
        }
        
        return choices;
    }
    
    private String buildChoicePrompt(PlayerId playerId) {
        return switch (currentCard.getType()) {
            case PIRATES -> "How many batteries do you want to use for combat?";
            case OPEN_SPACE -> "How many batteries do you want to use for double engines?";
            case PLANETS -> "Which planet do you want to land on?";
            case ABANDONED_SHIP -> "Do you want to dock at the abandoned ship?";
            default -> "Make your choice";
        };
    }
    
    private String getDefaultChoice(PlayerId playerId) {
        return switch (currentCard.getType()) {
            case PIRATES, SLAVERS, COMBAT_ZONE, OPEN_SPACE -> "0";
            case PLANETS -> "skip";
            case ABANDONED_SHIP, ABANDONED_STATION -> "skip";
            default -> "skip";
        };
    }
    
    private void schedulePlayerTimeout(PlayerId playerId, long timeoutMs) {
        executor.schedule(() -> {
            if (pendingChoices.contains(playerId)) {
                LOGGER.warning("Player " + playerId + " timed out, using default choice");
                String defaultChoice = getDefaultChoice(playerId);
                recordPlayerChoice(playerId, getChoiceType(), defaultChoice);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    private void applyCardEffects() {
        // Use existing visitor pattern to apply immediate effects
        if (gameModel != null) {
            gameModel.resolveAdventureCard(currentCard);
        }
    }
    
    private Map<PlayerId, Map<String, Object>> applyCardEffectsWithChoices() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        
        // Apply card-specific effects based on player choices
        switch (currentCard.getType()) {
            case OPEN_SPACE -> results = applyOpenSpaceEffects();
            case PLANETS -> results = applyPlanetsEffects();
            case PIRATES, SLAVERS -> results = applyCombatEffects();
            case ABANDONED_SHIP, ABANDONED_STATION -> results = applyDockingEffects();
            case COMBAT_ZONE -> results = applyCombatZoneEffects();
            default -> {
                // Fallback for cards without choices
                for (PlayerId playerId : turnOrder) {
                    Map<String, Object> playerResult = new HashMap<>();
                    playerResult.put("choice_made", playerChoices.getOrDefault(playerId, new HashMap<>()));
                    results.put(playerId, playerResult);
                }
            }
        }
        
        return results;
    }
    
    private Map<PlayerId, Map<String, Object>> applyOpenSpaceEffects() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        
        for (PlayerId playerId : turnOrder) {
            Player player = gameModel.getPlayerById(playerId);
            if (player == null) continue;
            
            Map<String, Object> playerChoicesMap = playerChoices.getOrDefault(playerId, new HashMap<>());
            String batteriesUsedStr = (String) playerChoicesMap.getOrDefault("engine_strength", "0");
            int batteriesUsed = Integer.parseInt(batteriesUsedStr);
            
            // Calculate engine strength (base engines + double engine bonuses)
            int baseEngines = player.getShip().getEngines();
            int doubleEngineBonus = Math.min(batteriesUsed, player.getShip().getBatteries());
            int totalEngineStrength = baseEngines + (doubleEngineBonus * 2); // Each battery gives +2 for double engines
            
            // Spend batteries
            player.getShip().setBatteries(player.getShip().getBatteries() - doubleEngineBonus);
            
            // Move player forward
            if (gameModel.getFlightBoard() != null) {
                int initialPosition = player.getFlightData().getPosition();
                gameModel.getFlightBoard().movePlayer(player, totalEngineStrength, true); // true = forward movement
                int finalPosition = player.getFlightData().getPosition();
                
                LOGGER.info("Open Space: Player " + playerId + " used " + doubleEngineBonus + 
                           " batteries, engine strength " + totalEngineStrength + 
                           ", moved from " + initialPosition + " to " + finalPosition);
            }
            
            // Record results
            Map<String, Object> playerResult = new HashMap<>();
            playerResult.put("batteries_used", doubleEngineBonus);
            playerResult.put("engine_strength_declared", totalEngineStrength);
            playerResult.put("base_engines", baseEngines);
            playerResult.put("double_engine_bonus", doubleEngineBonus * 2);
            playerResult.put("choice_made", playerChoicesMap);
            results.put(playerId, playerResult);
        }
        
        // Update flight order after movement
        if (gameModel.getFlightBoard() != null) {
            gameModel.getFlightBoard().updateCurrentOrder();
        }
        
        return results;
    }
    
    // Placeholder methods for other card types - TODO: implement properly
    private Map<PlayerId, Map<String, Object>> applyPlanetsEffects() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        // TODO: Implement planets choice application
        return results;
    }
    
    private Map<PlayerId, Map<String, Object>> applyCombatEffects() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        // TODO: Implement combat choice application
        return results;
    }
    
    private Map<PlayerId, Map<String, Object>> applyDockingEffects() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        // TODO: Implement docking choice application
        return results;
    }
    
    private Map<PlayerId, Map<String, Object>> applyCombatZoneEffects() {
        Map<PlayerId, Map<String, Object>> results = new HashMap<>();
        // TODO: Implement combat zone choice application
        return results;
    }
    
    private List<String> buildGlobalEffects() {
        // TODO: Build list of global effects that affected all players
        return Arrays.asList("Adventure card " + currentCard.getName() + " resolved");
    }
    
    // === COMPATIBILITY METHODS ===
    
    @Deprecated
    public void startAdventureCard(AdventureCard card, PlayerId playerId) {
        startCardResolution(card);
    }
    
    @Deprecated
    public void recordPlayerChoice(PlayerId playerId, String choiceType, Object value) {
        recordPlayerChoice(playerId, choiceType, String.valueOf(value));
    }
    
    // === COMPATIBILITY METHODS FOR EXISTING REQUEST CLASSES ===
    
    /**
     * Checks if the controller is currently processing a card
     */
    public boolean isProcessingCard() {
        return cardResolutionInProgress && currentCard != null;
    }
    
    /**
     * Handles a player choice via the new AdventureCardState system
     */
    public boolean handlePlayerChoice(PlayerId playerId, it.polimi.ingsw.server.model.domain.adventure.AdventureCardState.PlayerChoice choice) {
        if (!cardResolutionInProgress || currentCard == null) {
            return false;
        }
        
        // Convert AdventureCardState.PlayerChoice to our internal format
        String choiceType = choice.getChoiceType().toString().toLowerCase();
        String choiceValue = extractChoiceValue(choice);
        
        return recordPlayerChoice(playerId, choiceType, choiceValue);
    }
    
    /**
     * Extracts the choice value from an AdventureCardState.PlayerChoice
     */
    private String extractChoiceValue(it.polimi.ingsw.server.model.domain.adventure.AdventureCardState.PlayerChoice choice) {
        switch (choice.getChoiceType()) {
            case ENGINE_STRENGTH -> {
                Integer engineStrength = (Integer) choice.getParameter("engineStrength");
                Integer batteriesToUse = (Integer) choice.getParameter("batteriesToUse");
                // For Open Space, we care about batteries used for double engines
                return String.valueOf(batteriesToUse != null ? batteriesToUse : 0);
            }
            case PLANET_CHOICE -> {
                String planetChoice = (String) choice.getParameter("planetChoice");
                return planetChoice != null ? planetChoice : "skip";
            }
            case ABANDONED_SHIP_CHOICE, ABANDONED_STATION_CHOICE -> {
                Boolean dock = (Boolean) choice.getParameter("dock");
                return dock != null && dock ? "dock" : "skip";
            }
            case COMBAT_STRENGTH -> {
                Integer batteries = (Integer) choice.getParameter("batteries");
                return String.valueOf(batteries != null ? batteries : 0);
            }
            default -> {
                return "0";
            }
        }
    }

    public void cleanup() {
        resetCardState();
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
}
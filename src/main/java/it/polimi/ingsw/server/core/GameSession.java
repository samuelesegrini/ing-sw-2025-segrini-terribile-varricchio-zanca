package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.BuildingTimerFlippedEvent;
import it.polimi.ingsw.common.message.event.PlayerJoinedGameEvent;
import it.polimi.ingsw.common.message.event.PlayerLeftGameEvent;
import it.polimi.ingsw.common.message.event.PlayerReadyChangedEvent;
import it.polimi.ingsw.common.message.event.GameStartedEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GameSession {
    private static final Logger LOGGER = Logger.getLogger(GameSession.class.getName());

    private final String gameId;
    private final String gameName;
    private final PlayerId creatorId;
    private final GameModel gameModel;
    private final int maxPlayers;
    private final Map<PlayerId, PlayerState> playerStates;
    private final GameConfigurationManager configManager;
    private final PlayerSessionRegistry playerRegistry;
    private final Object lock = new Object();

    // Building phase management
    private final Map<String, Component> availableComponents;
    private final Map<String, PlayerId> componentOwnership; // componentId -> playerId
    private final Set<String> usedComponents;
    private final Map<String, Component> faceUpComponents; // Face-up components pile
    private final Map<PlayerId, List<String>> playerHeldComponents; // playerId -> list of componentIds

    // Turn management
    private int currentPlayerIndex = 0;
    private long phaseStartTime;
    private ScheduledFuture<?> phaseTimer;

    // Game state
    private volatile GamePhase currentPhase;
    private volatile boolean started;
    private volatile boolean ended;
    
    // Adventure card management
    private AdventureCardController adventureCardController;
    
    // Building timer management
    private BuildingTimer buildingTimer;
    
    // Property change support
    private final PropertyChangeSupport propertyChangeSupport;

    public GameSession(String gameId, String gameName, PlayerId creatorId,
                       int maxPlayers, GameLevel gameLevel,
                       GameConfigurationManager configManager,
                       PlayerSessionRegistry playerRegistry) {

        this.gameId = gameId;
        this.gameName = gameName;
        this.creatorId = creatorId;
        this.maxPlayers = maxPlayers;
        this.configManager = configManager;
        this.playerRegistry = playerRegistry;
        // Create decks from configuration manager
        ComponentDeck componentDeck = configManager.createComponentDeck(gameLevel);
        AdventureDeck adventureDeck = configManager.createAdventureDeck(gameLevel);
        
        this.gameModel = new GameModel(gameId, gameName, gameLevel, configManager.getConfigForLevel(gameLevel), 
                                     componentDeck, adventureDeck, maxPlayers);
        this.playerStates = new ConcurrentHashMap<>();
        this.availableComponents = new ConcurrentHashMap<>();
        this.componentOwnership = new ConcurrentHashMap<>();
        this.usedComponents = new HashSet<>();
        this.faceUpComponents = new ConcurrentHashMap<>();
        this.playerHeldComponents = new ConcurrentHashMap<>();
        this.currentPhase = GamePhase.SETUP;
        this.started = false;
        this.ended = false;
        this.propertyChangeSupport = new PropertyChangeSupport(this);

        // Add creator as first player and mark them as ready
        LOGGER.info("🎯 CREATOR SETUP - Adding creator " + creatorId + " to game " + gameId);
        boolean addedSuccessfully = addPlayer(creatorId);
        LOGGER.info("🎯 CREATOR ADDED - Result: " + addedSuccessfully + " for creator " + creatorId);
        
        // Ensure creator is always ready by default (double-check)
        PlayerState creatorState = playerStates.get(creatorId);
        if (creatorState != null) {
            boolean wasAlreadyReady = creatorState.isReady();
            creatorState.setReady(true);
            syncPlayerReadyStatus(creatorId, true); // Sync to GameModel
            LOGGER.info("🎯 CREATOR READY STATUS - Creator " + creatorId + " was ready: " + wasAlreadyReady + ", now ready: " + creatorState.isReady());
            
            // Debug: Print all player states and GameModel player ready status
            LOGGER.info("🎯 ALL PLAYERS STATUS in game " + gameId + ":");
            for (Map.Entry<PlayerId, PlayerState> entry : playerStates.entrySet()) {
                Player gameModelPlayer = gameModel.getPlayerById(entry.getKey());
                boolean gameModelReady = gameModelPlayer != null ? gameModelPlayer.isReady() : false;
                LOGGER.info("  - Player " + entry.getKey() + ": PlayerState ready=" + entry.getValue().isReady() + ", GameModel ready=" + gameModelReady);
            }
        } else {
            LOGGER.severe("🎯 CREATOR ERROR - Failed to set creator " + creatorId + " as ready - PlayerState not found");
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Adds a player to the game session.
     */
    public boolean addPlayer(PlayerId playerId) {
        synchronized (lock) {
            if (started || playerStates.size() >= maxPlayers) {
                return false;
            }

            PlayerState state = new PlayerState(playerId);
            LOGGER.info("🎯 ADD PLAYER - Created PlayerState for " + playerId + ", initial ready: " + state.isReady());
            
            // If this is the creator (first player), mark them as ready
            boolean isCreator = playerId.equals(creatorId);
            LOGGER.info("🎯 CREATOR CHECK - Is " + playerId + " the creator " + creatorId + "? " + isCreator);
            
            if (isCreator) {
                state.setReady(true);
                LOGGER.info("🎯 CREATOR READY - Set creator " + playerId + " as ready in PlayerState, now ready: " + state.isReady());
            }
            
            playerStates.put(playerId, state);
            LOGGER.info("🎯 PLAYER STORED - Player " + playerId + " stored in playerStates with ready: " + state.isReady());

            // Add to game model
            gameModel.addPlayer(playerId, playerId.getNickname());
            LOGGER.info("🎯 GAME MODEL - Added player " + playerId + " to GameModel");
            
            // Synchronize ready status between PlayerState and Player object
            if (isCreator) {
                syncPlayerReadyStatus(playerId, true);
                LOGGER.info("🎯 SYNC READY - Synchronized creator ready status to GameModel Player object");
            }

            LOGGER.info("🎯 JOIN SUCCESS - " + playerId + " -> " + gameId + (isCreator ? " (creator - ready: " + state.isReady() + ")" : ""));
            
            String playerNickname = playerRegistry.getPlayerNickname(playerId);
            PlayerJoinedGameEvent event = new PlayerJoinedGameEvent(
                    gameId, playerId, playerNickname, 
                    playerStates.size(), this.getGameModel()
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
            
            return true;
        }
    }
    
    /**
     * Adds a player to the game session (legacy String overload).
     */
    public boolean addPlayer(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return addPlayer(playerId);
    }

    /**
     * Removes a player from the game session.
     */
    public boolean removePlayer(PlayerId playerId) {
        synchronized (lock) {
            PlayerState removed = playerStates.remove(playerId);
            if (removed == null) {
                return false;
            }

            // Remove from game model
            gameModel.removePlayer(playerId);

            // Return player's components to pool
            returnPlayerComponents(playerId);

            LOGGER.info("Left: " + playerId + " <- " + gameId);

            String playerNickname = playerRegistry.getPlayerNickname(playerId);
            PlayerLeftGameEvent event = new PlayerLeftGameEvent(gameId, playerId, playerNickname);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);

            // Check if game should end
            if (playerStates.isEmpty()) {
                endGame("All players left");
            } else if (started && playerStates.size() < 2) {
                endGame("Not enough players to continue");
            }

            return true;
        }
    }
    
    /**
     * Removes a player from the game session (legacy String overload).
     */
    public boolean removePlayer(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return removePlayer(playerId);
    }

    /**
     * Sets a player's ready status.
     * Creator can change their ready status just like any other player.
     */
    public void setPlayerReady(PlayerId playerId, boolean ready) {
        synchronized (lock) {
            PlayerState state = playerStates.get(playerId);
            if (state != null) {
                boolean isCreator = playerId.equals(creatorId);
                state.setReady(ready);
                
                // Synchronize with GameModel Player object
                syncPlayerReadyStatus(playerId, ready);
                
                LOGGER.info("Ready: " + playerId + "=" + ready + (isCreator ? " (creator)" : "") + " - synced to GameModel");
                
                String playerNickname = playerRegistry.getPlayerNickname(playerId);
                PlayerReadyChangedEvent event = new PlayerReadyChangedEvent(gameId, playerId, playerNickname, ready);
                propertyChangeSupport.firePropertyChange("eventPublished", null, event);
            }
        }
    }
    
    /**
     * Synchronizes the ready status between PlayerState and GameModel Player object.
     */
    private void syncPlayerReadyStatus(PlayerId playerId, boolean ready) {
        Player player = gameModel.getPlayerById(playerId);
        if (player != null) {
            player.setReady(ready);
            LOGGER.info("🔄 SYNC - Player " + playerId + " ready status set to " + ready + " in GameModel");
        } else {
            LOGGER.warning("🔄 SYNC FAILED - Player " + playerId + " not found in GameModel for ready sync");
        }
    }
    
    /**
     * Sets a player's ready status (legacy String overload).
     */
    public void setPlayerReady(String playerIdString, boolean ready) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        setPlayerReady(playerId, ready);
    }

    /**
     * Checks if the game can start.
     */
    public boolean canStart() {
        synchronized (lock) {
            if (started || playerStates.size() < 2) {
                return false;
            }

            // Check if all players are ready
            return playerStates.values().stream().allMatch(PlayerState::isReady);
        }
    }

    /**
     * Starts the game.
     */
    public boolean startGame() {
        synchronized (lock) {
            if (!canStart()) {
                return false;
            }

            started = true;
            gameModel.initializeGame();
            gameModel.startGame();

            // Initialize components
            initializeComponents();

            // Start building phase
            transitionToPhase(GamePhase.BUILDING);

            LOGGER.info("Started: " + gameId + " (" + playerStates.size() + " players)");
            
            GameStartedEvent event = new GameStartedEvent(gameId, this.getGameModel(), creatorId);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
            
            return true;
        }
    }

    /**
     * Transitions to a new game phase.
     */
    private void transitionToPhase(GamePhase newPhase) {
        synchronized (lock) {
            this.currentPhase = newPhase;
            this.phaseStartTime = System.currentTimeMillis();
            gameModel.changePhase(newPhase);

            // Cancel any existing timer
            if (phaseTimer != null) {
                phaseTimer.cancel(false);
            }

            switch (newPhase) {
                case BUILDING:
                    startBuildingPhase();
                    break;
                case FLIGHT:
                    startFlightPhase();
                    break;
                case END:
                    endGame("Game completed");
                    break;
            }
        }
    }

    /**
     * Initializes components for the building phase.
     */
    private void initializeComponents() {
        // Load components from deck
        List<Component> components = configManager.getAllComponents();

        // Shuffle and distribute
        Collections.shuffle(components);

        for (Component component : components) {
            // Use the component's original ID (which contains the image path)
            String componentId = component.getId();
            availableComponents.put(componentId, component);
        }

        LOGGER.info("Components: " + availableComponents.size() + " initialized");
    }

    /**
     * Starts the building phase.
     */
    private void startBuildingPhase() {
        LOGGER.info("Phase: BUILDING -> " + gameId);
        phaseStartTime = System.currentTimeMillis();

        // Initialize BuildingTimer for supported game levels
        GameLevel level = gameModel.getGameLevel();
        if (supportsTimerSystem(level)) {
            buildingTimer = new BuildingTimer(level);
            buildingTimer.setEventListener(this::handleTimerEvent);
            buildingTimer.startBuildingPhase();
            
            LOGGER.info("Building timer initialized for level: " + level);
        } else {
            LOGGER.info("Timer system disabled for level: " + level);
        }

        // Legacy timer system (disabled to prevent auto-flight transition)
        int buildingTimeMinutes = 0;

        if (buildingTimeMinutes > 0) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            phaseTimer = scheduler.schedule(() -> {
                synchronized (lock) {
                    if (currentPhase == GamePhase.BUILDING) {
                        // Force validation for all players
                        validateAllShips();
                        transitionToPhase(GamePhase.FLIGHT);
                    }
                }
            }, buildingTimeMinutes, TimeUnit.MINUTES);
        }
    }
    
    /**
     * Checks if the timer system is supported for the given game level.
     */
    private boolean supportsTimerSystem(GameLevel level) {
        return level != GameLevel.TEST_FLIGHT;
    }
    
    /**
     * Handles timer events from the BuildingTimer.
     */
    private void handleTimerEvent(BuildingTimer.TimerEvent event, String playerId, long timeRemaining) {
        switch (event) {
            case BUILDING_ENDED -> {
                // End building phase and transition to flight
                LOGGER.info("Building phase ended by timer - transitioning to flight");
                endBuildingPhase();
            }
            case FIRST_TIMER_EXPIRED, SECOND_TIMER_EXPIRED -> {
                // Broadcast timer expiration to all players
                broadcastTimerExpired(event, timeRemaining);
            }
            // Other events are handled by the BuildingTimerFlippedEvent when players flip
        }
    }
    
    /**
     * Broadcasts timer expiration events to all players.
     */
    private void broadcastTimerExpired(BuildingTimer.TimerEvent event, long timeRemaining) {
        // This could trigger automatic UI updates or notifications
        LOGGER.info("Timer event: " + event + ", time remaining: " + timeRemaining);
        // Additional broadcast logic could be added here if needed
    }
    
    /**
     * Ends the building phase and transitions to flight.
     */
    private void endBuildingPhase() {
        synchronized (lock) {
            if (currentPhase == GamePhase.BUILDING) {
                // Force validation for all players
                validateAllShips();
                transitionToPhase(GamePhase.FLIGHT);
            }
        }
    }


    /**
     * Starts the flight phase.
     */
    private void startFlightPhase() {
        LOGGER.info("Phase: FLIGHT -> " + gameId);

        // Initialize flight board
        gameModel.getAdventureDeck().startFlightPhase();

//        // Initialize adventure card controller
//        if (adventureCardController == null) {
//            if (eventPublisher != null) {
//                adventureCardController = new AdventureCardController(gameModel, eventPublisher);
//            } else {
//                LOGGER.warning("EventPublisher not available - adventure card controller will be created later");
//            }
//        }

        // Update player order based on ship stats
        updatePlayerOrder();
    }

    /**
     * Gets an available component.
     */
    public Component getAvailableComponent(String componentId) {
        synchronized (lock) {
            if (usedComponents.contains(componentId)) {
                return null;
            }
            return availableComponents.get(componentId);
        }
    }

    /**
     * Gets all available components for building phase sync.
     */
    public Map<String, Component> getAvailableComponents() {
        synchronized (lock) {
            Map<String, Component> available = new HashMap<>();
            for (Map.Entry<String, Component> entry : availableComponents.entrySet()) {
                if (!usedComponents.contains(entry.getKey())) {
                    available.put(entry.getKey(), entry.getValue());
                }
            }
            return available;
        }
    }

    /**
     * Marks a component as used by a player.
     */
    public void useComponent(String componentId, PlayerId playerId) {
        synchronized (lock) {
            usedComponents.add(componentId);
            componentOwnership.put(componentId, playerId);
        }
    }
    
    /**
     * Marks a component as used by a player (legacy String overload).
     */
    public void useComponent(String componentId, String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        useComponent(componentId, playerId);
    }

    /**
     * Returns a component to the available pool.
     */
    public void returnComponent(Component component) {
        synchronized (lock) {
            // Find the component ID
            String componentId = null;
            for (Map.Entry<String, Component> entry : availableComponents.entrySet()) {
                if (entry.getValue() == component) {
                    componentId = entry.getKey();
                    break;
                }
            }

            if (componentId != null) {
                usedComponents.remove(componentId);
                componentOwnership.remove(componentId);
            }
        }
    }

    /**
     * Returns all components owned by a player.
     */
    private void returnPlayerComponents(PlayerId playerId) {
        synchronized (lock) {
            List<String> toReturn = new ArrayList<>();
            for (Map.Entry<String, PlayerId> entry : componentOwnership.entrySet()) {
                if (entry.getValue().equals(playerId)) {
                    toReturn.add(entry.getKey());
                }
            }

            for (String componentId : toReturn) {
                usedComponents.remove(componentId);
                componentOwnership.remove(componentId);
            }
            
            // Also clear held components for this player
            playerHeldComponents.remove(playerId);
        }
    }

    /**
     * Gets a face-up component by ID.
     */
    public Component getFaceUpComponent(String componentId) {
        synchronized (lock) {
            return faceUpComponents.get(componentId);
        }
    }

    /**
     * Reserves a face-up component for a player.
     */
    public void reserveFaceUpComponent(String componentId, PlayerId playerId) {
        synchronized (lock) {
            Component component = faceUpComponents.remove(componentId);
            if (component != null) {
                // Add to player's held components
                playerHeldComponents.computeIfAbsent(playerId, k -> new ArrayList<>()).add(componentId);
                componentOwnership.put(componentId, playerId);
                // Keep the component in available pool but mark as owned
                availableComponents.put(componentId, component);
            }
        }
    }
    
    /**
     * Reserves a face-up component for a player (legacy String overload).
     */
    public void reserveFaceUpComponent(String componentId, String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        reserveFaceUpComponent(componentId, playerId);
    }

    /**
     * Gets the list of components held by a player.
     */
    public List<String> getPlayerHeldComponents(PlayerId playerId) {
        synchronized (lock) {
            return new ArrayList<>(playerHeldComponents.getOrDefault(playerId, new ArrayList<>()));
        }
    }
    
    /**
     * Gets the list of components held by a player (legacy String overload).
     */
    public List<String> getPlayerHeldComponents(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getPlayerHeldComponents(playerId);
    }

    /**
     * Adds a component to the face-up pile (returned by player).
     */
    public void addToFaceUpPile(String componentId, Component component) {
        synchronized (lock) {
            faceUpComponents.put(componentId, component);
            // Remove from any player's held components
            for (List<String> heldList : playerHeldComponents.values()) {
                heldList.remove(componentId);
            }
            componentOwnership.remove(componentId);
        }
    }

    /**
     * Gets the building timer instance for this game session.
     */
    public BuildingTimer getBuildingTimer() {
        return buildingTimer;
    }
    
    /**
     * Checks if a player has finished building their ship.
     * A ship is considered finished if it meets minimum requirements.
     */
    public boolean isPlayerShipFinished(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        // Check if player has marked themselves as ready
        if (player.isReady()) {
            return true;
        }
        
        // Alternative: Check if ship meets minimum requirements
        Ship ship = player.getShip();
        if (ship == null) {
            return false;
        }
        
        ship.updateStats();
        
        // Minimum requirements to be considered "finished"
        return ship.getEngines() >= 1 &&
               ship.getCrew() >= 2 &&
               ship.isStructurallyValid();
    }
    
    /**
     * Flips the building timer following Galaxy Trucker three-stage rules.
     * @param playerId The player attempting to flip the timer
     * @param playerHasFinishedShip Whether the player has finished their ship
     * @return FlipResult indicating success or reason for failure
     */
    public BuildingTimer.FlipResult flipBuildingTimer(String playerId, boolean playerHasFinishedShip) {
        synchronized (lock) {
            if (currentPhase != GamePhase.BUILDING) {
                throw new IllegalStateException("Can only flip timer during building phase");
            }
            
            if (buildingTimer == null) {
                throw new IllegalStateException("Timer system not active for this game level");
            }
            
            return buildingTimer.flipTimer(playerId, playerHasFinishedShip);
        }
    }
    
    /**
     * Legacy method for backward compatibility - adds fixed additional time.
     * @deprecated Use the new three-stage timer system instead
     */
    @Deprecated
    public long flipBuildingTimer(long additionalTime) {
        synchronized (lock) {
            if (currentPhase != GamePhase.BUILDING) {
                throw new IllegalStateException("Can only flip timer during building phase");
            }
            
            // Cancel existing timer and create new one with extended time
            if (phaseTimer != null) {
                phaseTimer.cancel(false);
            }
            
            // Calculate new remaining time
            long elapsed = System.currentTimeMillis() - phaseStartTime;
            long baseTime = 60000; // 1 minute base time
            long newTotalTime = baseTime + additionalTime;
            long newTimeRemaining = Math.max(0, newTotalTime - elapsed);
            
            // Start new timer with extended time
            if (newTimeRemaining > 0) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                phaseTimer = scheduler.schedule(() -> {
                    synchronized (lock) {
                        if (currentPhase == GamePhase.BUILDING) {
                            // Force validation for all players
                            validateAllShips();
                            transitionToPhase(GamePhase.FLIGHT);
                        }
                    }
                }, newTimeRemaining, TimeUnit.MILLISECONDS);
            }
            
            return newTimeRemaining;
        }
    }

    /**
     * Validates all player ships.
     */
    private void validateAllShips() {
        for (PlayerId playerId : playerStates.keySet()) {
            Player player = getPlayer(playerId);
            if (player != null) {
                player.getShip().updateStats();
                // Mark validation complete
                playerStates.get(playerId).setShipValidated(true);
            }
        }
    }

    /**
     * Updates player order for flight phase.
     */
    private void updatePlayerOrder() {
        gameModel.getFlightBoard().updateCurrentOrder();
    }

    /**
     * Ends the game.
     */
    private void endGame(String reason) {
        synchronized (lock) {
            if (ended) {
                return;
            }

            ended = true;
            currentPhase = GamePhase.END;

            if (phaseTimer != null) {
                phaseTimer.cancel(false);
            }
            
            // Shutdown building timer if active
            if (buildingTimer != null) {
                buildingTimer.shutdown();
                buildingTimer = null;
            }
            
            // Shutdown adventure card controller if active
            if (adventureCardController != null) {
                adventureCardController.cleanup();
                adventureCardController = null;
            }

            LOGGER.info("Ended: " + gameId + " (" + reason + ")");
        }
    }

    /**
     * Gets the current adventure card.
     */
    public AdventureCard getCurrentAdventureCard() {
        return gameModel.getAdventureDeck().getCurrentCard().orElse(null);
    }

    /**
     * Draws the next adventure card.
     */
    public AdventureCard drawNextAdventureCard() {
        return gameModel.getAdventureDeck().drawNextCard().orElse(null);
    }

    // Getters

    public String getGameId() {
        return gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public PlayerId getCreatorId() {
        return creatorId;
    }
    

    public GameModel getGameModel() {
        return gameModel;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayerCount() {
        synchronized (lock) {
            return playerStates.size();
        }
    }

    public Set<PlayerId> getPlayerIds() {
        synchronized (lock) {
            return new HashSet<>(playerStates.keySet());
        }
    }
    

    public Player getPlayer(PlayerId playerId) {
        System.out.println("[DEBUG] GameSession.getPlayer - Input playerId: " + playerId);
        Player result = gameModel.getPlayerById(playerId);
        System.out.println("[DEBUG] GameSession.getPlayer - Result: " + (result != null ? result.getId() : "null"));
        if (result == null) {
            System.out.println("[DEBUG] GameSession.getPlayer - Available players in GameModel:");
            gameModel.getPlayers().forEach(p -> 
                System.out.println("[DEBUG]   - Player: " + p.getId() + ", Nickname: " + p.getId().getNickname()));
        }
        return result;
    }
    
    /**
     * Gets a player by string ID (legacy compatibility).
     */
    public Player getPlayer(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getPlayer(playerId);
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean canJoin() {
        synchronized (lock) {
            return !started && !ended && playerStates.size() < maxPlayers;
        }
    }

    public GameLevel getGameLevel() {
        return gameModel.getLevel();
    }

    public Map<String, Object> getGameState() {
        synchronized (lock) {
            Map<String, Object> state = new HashMap<>();
            state.put("gameId", gameId);
            state.put("phase", currentPhase);
            state.put("players", new ArrayList<>(playerStates.keySet()));
            state.put("started", started);
            state.put("ended", ended);
            return state;
        }
    }

    public List<Player> getPlayers() {
        synchronized (lock) {
            return gameModel.getPlayers();
        }
    }

    public Component getComponentById(String componentId) {
        return availableComponents.get(componentId);
    }
    
    public AdventureCardController getAdventureCardController() {
        return adventureCardController;
    }
    
    /**
     * Updates the event publisher and initializes adventure card controller if needed.
     * Used for deferred initialization.
     */
    public void updateEventPublisher(EventPublisher newEventPublisher) {
        if (newEventPublisher != null && adventureCardController == null && currentPhase == GamePhase.FLIGHT) {
            adventureCardController = new AdventureCardController(gameModel, newEventPublisher);
            LOGGER.info("Adventure card controller initialized with updated event publisher");
        }
    }
    
    public PlayerState getPlayerState(PlayerId playerId) {
        synchronized (lock) {
            return playerStates.get(playerId);
        }
    }
    
    /**
     * Gets player state by string ID (legacy compatibility).
     */
    public PlayerState getPlayerState(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getPlayerState(playerId);
    }
    
    public boolean areAllPlayersReady() {
        synchronized (lock) {
            if (playerStates.isEmpty()) {
                return false;
            }
            return playerStates.values().stream().allMatch(PlayerState::isReady);
        }
    }
    
    public boolean isCreator(PlayerId playerId) {
        synchronized (lock) {
            return creatorId != null && creatorId.equals(playerId);
        }
    }
    
    /**
     * Checks if a player is the creator (legacy String overload).
     */
    public boolean isCreator(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return isCreator(playerId);
    }
    
    public it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig getShipGridConfig() {
        return gameModel.getConfig().shipGridConfig();
    }
    
    public ShipBuildingSyncState getShipBuildingSyncState(PlayerId playerId) {
        synchronized (lock) {
            ShipBuildingSyncState syncState = new ShipBuildingSyncState();
            
            // Get player's ship grid
            Player player = getPlayer(playerId);
            if (player != null && player.getShip() != null) {
                var ship = player.getShip();
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 7; col++) {
                        var component = ship.getBoard()[row][col];
                        if (component != null) {
                            syncState.shipGrid.put(
                                new it.polimi.ingsw.server.model.domain.ship.Position(row, col),
                                component.getType()
                            );
                        }
                    }
                }
                
                // Add forbidden positions
                syncState.forbiddenPositions.addAll(ship.forbiddenPositions);
            }
            
            // Add available face-down tiles (all components from the deck)
            for (Component component : availableComponents.values()) {
                syncState.availableTiles.add(component.getType());
            }
            
            // Add face-up tiles (returned components)
            for (Component component : faceUpComponents.values()) {
                syncState.availableTiles.add(component.getType());
            }
            
            // Add player's held tiles
            List<String> heldComponentIds = playerHeldComponents.getOrDefault(playerId, new ArrayList<>());
            for (String componentId : heldComponentIds) {
                Component component = availableComponents.get(componentId);
                if (component != null) {
                    syncState.heldTiles.add(component.getType());
                }
            }
            
            // Calculate remaining building time
            if (currentPhase == GamePhase.BUILDING && phaseStartTime > 0) {
                long elapsed = System.currentTimeMillis() - phaseStartTime;
                long totalTime = 60000; // 1 minute in milliseconds  
                syncState.buildingTimeRemaining = Math.max(0, totalTime - elapsed);
            }
            
            return syncState;
        }
    }
    
    /**
     * Gets ship building sync state by string ID (legacy compatibility).
     */
    public ShipBuildingSyncState getShipBuildingSyncState(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getShipBuildingSyncState(playerId);
    }
    
    public static class ShipBuildingSyncState {
        public final Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType> shipGrid = new HashMap<>();
        public final List<it.polimi.ingsw.server.model.enums.ship.ComponentType> availableTiles = new ArrayList<>();
        public final List<it.polimi.ingsw.server.model.enums.ship.ComponentType> heldTiles = new ArrayList<>();
        public final Set<it.polimi.ingsw.server.model.domain.ship.Position> forbiddenPositions = new HashSet<>();
        public long buildingTimeRemaining = 0;
        public boolean timerFlipped = false;
    }


    /**
     * Inner class to track player state within the game.
     */
    public static class PlayerState {
        private final PlayerId playerId;
        private volatile boolean ready = false;
        private volatile boolean shipValidated = false;

        public PlayerState(PlayerId playerId) {
            this.playerId = playerId;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public boolean isShipValidated() {
            return shipValidated;
        }

        public void setShipValidated(boolean validated) {
            this.shipValidated = validated;
        }
        
        public PlayerId getPlayerId() {
            return playerId;
        }
    }
}
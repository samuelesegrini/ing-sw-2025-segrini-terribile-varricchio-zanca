package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
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
import it.polimi.ingsw.common.message.event.ComponentTakenEvent;
import it.polimi.ingsw.common.message.event.ComponentPlacedEvent;
import it.polimi.ingsw.common.message.event.ComponentReservedEvent;
import it.polimi.ingsw.common.message.event.ComponentOfferedEvent;
import it.polimi.ingsw.common.message.event.ShipValidationEvent;
import it.polimi.ingsw.common.message.event.PlayerReadyChangedEvent;
import it.polimi.ingsw.common.message.event.GameStartedEvent;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

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
    private final Set<PlayerId> joinedPlayers;
    private final GameConfigurationManager configManager;
    private final PlayerSessionRegistry playerRegistry;
    private final Object lock = new Object();

    // Building phase management - now delegated to GameModel.ComponentDeck

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

    // Building timer management delegated to GameModel

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
        this.joinedPlayers = ConcurrentHashMap.newKeySet();
        this.currentPhase = GamePhase.SETUP;
        this.started = false;
        this.ended = false;
        this.propertyChangeSupport = new PropertyChangeSupport(this);

        // Add creator as first player and mark them as ready
        boolean addedSuccessfully = addPlayer(creatorId);

        // Ensure creator is always ready by default
        Player creator = gameModel.getPlayerById(creatorId);
        if (creator != null) {
            creator.setReady(true);
        } else {
            LOGGER.severe("Failed to set creator " + creatorId + " as ready - Player not found in GameModel");
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Adds a player to the game session.
     */
    public boolean addPlayer(PlayerId playerId) {
        synchronized (lock) {
            if (started || joinedPlayers.size() >= maxPlayers) {
                return false;
            }

            // Add to game model
            gameModel.addPlayer(playerId, playerId.getNickname());
            joinedPlayers.add(playerId);

            // If this is the creator (first player), mark them as ready
            boolean isCreator = playerId.equals(creatorId);
            if (isCreator) {
                Player player = gameModel.getPlayerById(playerId);
                if (player != null) {
                    player.setReady(true);
                }
            }

            String playerNickname = playerRegistry.getPlayerNickname(playerId);
            PlayerJoinedGameEvent event = new PlayerJoinedGameEvent(
                    gameId, playerId, playerNickname,
                    joinedPlayers.size(), this.getGameModel()
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);

            return true;
        }
    }

    /**
     * Removes a player from the game session.
     */
    public boolean removePlayer(PlayerId playerId) {
        synchronized (lock) {
            if (!joinedPlayers.remove(playerId)) {
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
            if (joinedPlayers.isEmpty()) {
                endGame("All players left");
            } else if (started && joinedPlayers.size() < 2) {
                endGame("Not enough players to continue");
            }

            return true;
        }
    }

    /**
     * Sets a player's ready status.
     * Creator can change their ready status just like any other player.
     */
    public void setPlayerReady(PlayerId playerId, boolean ready) {
        // Set ready status directly in GameModel
        Player player = gameModel.getPlayerById(playerId);
        if (player != null) {
            player.setReady(ready);

            boolean isCreator = playerId.equals(creatorId);
            LOGGER.info("Ready: " + playerId + "=" + ready + (isCreator ? " (creator)" : ""));

            String playerNickname = playerRegistry.getPlayerNickname(playerId);
            PlayerReadyChangedEvent event = new PlayerReadyChangedEvent(gameId, playerId, playerNickname, ready);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }

    /**
     * Checks if the game can start.
     */
    public boolean canStart() {
        synchronized (lock) {
            if ( ended ) {
                LOGGER.warning("Cannot start an ended game.");
                return false;
            } else if (started) {
                LOGGER.warning("Cannot start an already started game.");
                return false;
            } else if (joinedPlayers.size() < 2 ) {
                LOGGER.warning("Not enough players to start the game.");
                return false;
            }

            // Check if all players are ready
            return gameModel.getPlayers().stream().allMatch(Player::isReady);
        }
    }

    /**
     * Starts the game.
     */
    public boolean startGame() {
        synchronized (lock) {
            if (ended) {
                LOGGER.warning("Cannot start game - game has already ended: " + gameId);
                return false;
            }
            if (started) {
                LOGGER.warning("Cannot start game - game has already started: " + gameId);
                return false;
            }

            if (!canStart()) {
                LOGGER.warning("Cannot start game - not all players are ready or not enough players: " + gameId);
                return false;
            }

            started = true;
            gameModel.initializeGame();
            gameModel.startGame();

            // Initialize components
            // Start building phase
            transitionToPhase(GamePhase.BUILDING);

            LOGGER.info("Started: " + gameId + " (" + joinedPlayers.size() + " players)");

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
     * Starts the building phase.
     */
    private void startBuildingPhase() {
        LOGGER.info("Phase: BUILDING -> " + gameId);
        phaseStartTime = System.currentTimeMillis();

        // Timer management is handled by GameModel
        GameLevel level = gameModel.getGameLevel();
        if (supportsTimerSystem(level)) {
            BuildingTimer timer = gameModel.getBuildingTimer();
            if (timer != null) {
                timer.setEventListener(this::handleTimerEvent);
                LOGGER.info("Building timer initialized for level: " + level);
            }
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
     * Returns all components owned by a player.
     */
    private void returnPlayerComponents(PlayerId playerId) {
        // Delegate to GameModel - no session state involved
        ComponentDeck deck = gameModel.getComponentDeck();
        Player player = gameModel.getPlayerById(playerId);

        if (player != null) {
            // Return held component
            Component heldComponent = player.getHeldComponent();
            if (heldComponent != null) {
                player.clearHeldComponent();
                deck.returnToFaceUp(heldComponent);
            }

            // Return any reserved components from the player's ship
            Ship ship = player.getShip();
            if (ship != null) {
                Set<Component> reservedComponents = ship.getReservedComponents();
                for (Component component : reservedComponents) {
                    deck.returnToFaceUp(component);
                }
                // TODO: Add clearReservedComponents method to Ship or handle individually
            }
        }
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
        // Phase check + delegation to GameModel - minimal sync needed
        if (currentPhase != GamePhase.BUILDING) {
            throw new IllegalStateException("Can only flip timer during building phase");
        }

        // Delegate to GameModel and convert boolean result to FlipResult
        boolean success = gameModel.flipBuildingTimer(PlayerId.fromString(playerId), playerHasFinishedShip);
        return success ? BuildingTimer.FlipResult.SUCCESS : BuildingTimer.FlipResult.INVALID_STAGE;
    }

    /**
     * Validates all player ships.
     */
    private void validateAllShips() {
        for (Player player : gameModel.getPlayers()) {
            if (player != null) {
                player.getShip().updateStats();
                // Validation is handled by player.setReady() in GameModel
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
                return; // Already ended
            }

            ended = true;
            currentPhase = GamePhase.END;

            if (phaseTimer != null) {
                phaseTimer.cancel(false);
            }

            // Shutdown building timer if active (handled by GameModel)
            BuildingTimer timer = gameModel.getBuildingTimer();
            if (timer != null) {
                timer.shutdown();
            }

            // Shutdown adventure card controller if active
            if (adventureCardController != null) {
                adventureCardController.cleanup();
                adventureCardController = null;
            }

            LOGGER.info("Ended: " + gameId + " (" + reason + ")");
        }
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
        return joinedPlayers.size();
    }

    public Set<PlayerId> getPlayerIds() {
        return new HashSet<>(joinedPlayers);
    }

    //TODO: questo metodo è usato solo da flight o da test, da eliminare una volta completata la flight
    public Player getPlayer(PlayerId playerId) {
        Player result = gameModel.getPlayerById(playerId);
        if (result == null) {
            gameModel.getPlayers().forEach(p ->
                System.out.println("[DEBUG]   - Player: " + p.getId() + ", Nickname: " + p.getId().getNickname()));
        }
        return result;
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
        // Volatile reads are atomic, ConcurrentHashMap.size() is thread-safe
        return !started && !ended && joinedPlayers.size() < maxPlayers;
    }

    public AdventureCardController getAdventureCardController() {
        return adventureCardController;
    }

    public boolean areAllPlayersReady() {
        // Check ready status directly from GameModel
        if (joinedPlayers.isEmpty()) {
            return false;
        }
        return gameModel.getPlayers().stream().allMatch(Player::isReady);
    }

    public boolean isCreator(PlayerId playerId) {
        return creatorId != null && creatorId.equals(playerId);
    }

}
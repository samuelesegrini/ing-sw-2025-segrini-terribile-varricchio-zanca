package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.flight.RewardSystem;
import it.polimi.ingsw.server.model.domain.flight.Route;
import it.polimi.ingsw.server.model.domain.general.config.FlightBoardConfig;
import it.polimi.ingsw.server.model.domain.general.config.RewardSystemConfig;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.general.config.GameConfig;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

// Event imports
import it.polimi.ingsw.common.message.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the current state of the game, including the current player, the flight board,
 * the adventure deck, the list of players, and the current game phase.
 */
public class GameModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String gameId;
    private final String gameName;
    private final GameLevel level;
    private final GameConfig config;
    private final List<Player> players;
    private final int maxPlayers;

    private GamePhase currentPhase;
    private ComponentDeck componentDeck;
    private AdventureDeck adventureDeck;
    private FlightBoard flightBoard;
    private int currentPlayerIndex;
    private Player leadPlayer;
    private boolean isInitialized;
    private BuildingTimer buildingTimer;
    private transient PropertyChangeSupport propertyChangeSupport;

    /**
     * Creates a new game model with the specified game ID, name, difficulty level, configuration, and number of players.
     * 
     * @param gameId The unique identifier for this game.
     * @param gameName The display name for this game.
     * @param level The difficulty level of the game.
     * @param config The game configuration for this level.
     * @param componentDeck The component deck for this game.
     * @param adventureDeck The adventure deck for this game.
     * @param playerCount The number of players who will participate in the game.
     * @throws IllegalArgumentException if playerCount is not within valid range
     */
    public GameModel(String gameId, String gameName, GameLevel level, GameConfig config, ComponentDeck componentDeck, 
                     it.polimi.ingsw.server.model.domain.adventure.AdventureDeck adventureDeck, int playerCount) {
        if (playerCount < 2 || playerCount > 4) {
            throw new IllegalArgumentException("Player count must be between 2 and 4");
        }

        this.gameId = gameId;
        this.gameName = gameName;
        this.level = level;
        this.config = config;
        this.componentDeck = componentDeck;
        this.adventureDeck = adventureDeck;
        this.players = new ArrayList<>();
        this.currentPhase = GamePhase.SETUP;
        this.currentPlayerIndex = 0;
        this.maxPlayers = playerCount;
        this.isInitialized = false;
        this.buildingTimer = new BuildingTimer(level);
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        
        // Register as timer event listener to fire events to clients
        this.buildingTimer.setEventListener(this::onTimerEvent);
    }
    
    /**
     * Handles timer events from BuildingTimer and fires appropriate events to clients
     */
    private void onTimerEvent(BuildingTimer.TimerEvent event, String playerId, long timeRemaining) {
        if (propertyChangeSupport != null) {
            BuildingTimerFlippedEvent timerEvent = new BuildingTimerFlippedEvent(
                gameId,
                PlayerId.fromString(playerId),
                event,
                buildingTimer.getCurrentStage(),
                timeRemaining
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, timerEvent);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Adds a new player to the game.
     * 
     * @param playerId The unique identifier for the player.
     * @param name The display name of the player.
     * @throws IllegalStateException if maximum players reached or game already started
     * @throws IllegalArgumentException if player ID already exists
     */
    public void addPlayer(PlayerId playerId, String name) {
        if (currentPhase != GamePhase.SETUP) {
            throw new IllegalStateException("Cannot add players after game has started");
        }
        if (players.size() >= maxPlayers) {
            throw new IllegalStateException("Maximum number of players reached");
        }
        if (getPlayerById(playerId) != null) {
            throw new IllegalArgumentException("Player with ID " + playerId + " already exists");
        }

        // Assign player colors based on join order
        PlayerColor[] colors = {PlayerColor.BLUE, PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW};
        PlayerColor assignedColor = colors[players.size() % colors.length];
        
        // Create player without ship
        Player player = new Player(playerId, assignedColor);
        
        // Create ship separately with proper configuration
        var shipGridConfig = config.shipGridConfig();
        var ship = new Ship(level, shipGridConfig);
        
        // Assign ship to player
        player.setShip(ship);
        
        // Place starting cabin at row 2, col 3
        Component startingCabin = getStartingCabinForPlayer(assignedColor);
        if (startingCabin != null) {
            Position startingPosition = new Position(2, 3);
            try {
                ship.addComponent(startingCabin, startingPosition);
                ship.updateStats();
            } catch (IllegalArgumentException e) {
                // Log error but continue - this shouldn't happen if position 2,3 is valid
                System.err.println("Failed to place starting cabin for player " + playerId + ": " + e.getMessage());
            }
        }
        
        players.add(player);
    }

    /**
     * Retrieves a player by their ID.
     * 
     * @param playerId The unique identifier of the player to retrieve.
     * @return The player with the specified ID, or null if no such player exists.
     */
    public Player getPlayerById(PlayerId playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public boolean isInitialized(){
        return isInitialized;
    }

    /**
     * Initializes the game by setting up the flight board, adventure deck, and component deck.
     * @throws IllegalStateException if not enough players or game already initialized
     */
    public void initializeGame() {
        if (players.size() < 2) {
            throw new IllegalStateException("Not enough players to start the game");
        }
        if (isInitialized) {
            throw new IllegalStateException("Game already initialized");
        }

        FlightBoardConfig fbCfg = config.flightBoardConfig();
        RewardSystemConfig rsCfg = fbCfg.rewardSystem();

        // Convert RewardSystemConfig to RewardSystem
        Map<PlayerOrder, Integer> positionBonusMap = rsCfg.positionBonus().entrySet().stream()
                .collect(Collectors.toMap(e -> PlayerOrder.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));
        Map<GoodType, Integer> resourceBonusMap = rsCfg.resourceBonus().entrySet().stream()
                .collect(Collectors.toMap(e -> GoodType.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue));

        RewardSystem rewardSystem = new RewardSystem(level, positionBonusMap, resourceBonusMap,
                rsCfg.bestLookingShipBonus(), rsCfg.exposedConnectorsPenalty());

        Route route = new Route(level, Integer.parseInt(fbCfg.length()), new ArrayList<>(fbCfg.startingPositions()), rewardSystem);
        this.flightBoard = new FlightBoard(level, route, players.size());
        
        // Initialize player positions on flight board (ships initialized during addPlayer)
        for (Player player : players) {
            flightBoard.registerPlayer(player);
        }

        isInitialized = true;
    }

    /**
     * Starts the game by transitioning to the building phase.
     * @throws IllegalStateException if game not initialized or already started
     */
    public void startGame() {
        if (!isInitialized) {
            throw new IllegalStateException("Game not initialized");
        }
        if (currentPhase != GamePhase.SETUP) {
            throw new IllegalStateException("Game already started");
        }
        
        changePhase(GamePhase.BUILDING);
        
        // Note: GameStartedEvent is fired by GameSession which has access to the requesterId
        // GameModel doesn't need to fire this event as it would be redundant
    }

    /**
     * Advances to the next phase of the game.
     */
    public void nextPhase() {
        GamePhase nextPhase = currentPhase.getNextPhase();
        changePhase(nextPhase);
    }

    /**
     * Changes the current phase of the game to the specified phase.
     * 
     * @param phase The new phase to transition to.
     */
    public void changePhase(GamePhase phase) {
        this.currentPhase = phase;
        initializePhase(phase);
    }

    /**
     * Initializes the specified phase, setting up any necessary state.
     * 
     * @param phase The phase to initialize.
     */
    public void initializePhase(GamePhase phase) {
        // Implementation depends on the specific phase requirements
        switch (phase) {
            case SETUP:
                // Initialize game setup
                break;
            case BUILDING:
                // Initialize building phase with timer system
                if (buildingTimer != null) {
                    buildingTimer.startBuildingPhase();
                }
                break;
            case FLIGHT:
                // Initialize flight phase - end building timer if active
                if (buildingTimer != null) {
                    buildingTimer.forceEndBuildingPhase();
                }
                
                // Flight board should already be initialized
                if (flightBoard == null) {
                    throw new IllegalStateException("Flight board not initialized");
                }
                
                // Adventure deck should be ready
                if (adventureDeck != null) {
                    adventureDeck.startFlightPhase();
                }
                
                System.out.println("Flight phase initialized");
                break;
            case END:
                calculateFinalScores();
                Player winner = determineWinner();
                // Cleanup timer resources
                if (buildingTimer != null) {
                    buildingTimer.shutdown();
                }
                break;
        }
    }

    /**
     * Draws a component from the deck for the current player.
     * @return Optional containing the drawn component, or empty if no components available
     * @throws IllegalStateException if not in building phase
     */
    public Optional<Component> drawComponent() {
        if (currentPhase != GamePhase.BUILDING) {
            throw new IllegalStateException("Can only draw components during building phase");
        }
        return componentDeck.draw();
    }

    /**
     * Draws an adventure card from the adventure deck.
     * @return Optional containing the drawn card, or empty if no cards available
     * @throws IllegalStateException if not in flight phase
     */
    public Optional<AdventureCard> drawAdventureCard() {
        if (currentPhase != GamePhase.FLIGHT) {
            throw new IllegalStateException("Can only draw adventure cards during flight phase");
        }
        return adventureDeck.drawNextCard();
    }

    /**
     * Resolves the effects of an adventure card.
     * @param card The adventure card to resolve.
     * @throws IllegalArgumentException if card is null
     */
    public void resolveAdventureCard(AdventureCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Adventure card cannot be null");
        }
        if (currentPhase != GamePhase.FLIGHT) {
            throw new IllegalStateException("Can only resolve adventure cards during flight phase");
        }
        // Adventure card resolution is handled by the card's visitor pattern
        // The card will apply its effects to the current players and ships
    }

    /**
     * Moves a player forward on the flight board by the specified number of spaces.
     * 
     * @param player The player to move.
     * @param spaces The number of spaces to move the player.
     */
    public void movePlayer(Player player, int spaces) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (currentPhase != GamePhase.FLIGHT) {
            throw new IllegalStateException("Can only move players during flight phase");
        }
        if (spaces <= 0) {
            throw new IllegalArgumentException("Spaces must be positive");
        }
        
        flightBoard.movePlayer(player, spaces, true);
        updateLeadPlayer();
    }

    /**
     * Updates the lead player based on current positions on the flight board.
     */
    private void updateLeadPlayer() {
        if (flightBoard != null) {
            Player currentLeader = flightBoard.getLeadingPlayer();
            if (currentLeader != this.leadPlayer) {
                this.leadPlayer = currentLeader;
            }
        }
    }

    /**
     * Calculates the final scores for all players at the end of the game.
     */
    private void calculateFinalScores() {
        RewardSystem rewardSystem =
            flightBoard == null ? null : flightBoard.getRoute().getRewardSystem();

        if (rewardSystem == null) {
            throw new IllegalStateException("Reward system is not initialized");
        } else{
            for (Player player : players) rewardSystem.calculateFinalScores(players, currentPhase);
        }
    }

    /**
     * Determines the winner of the game based on final scores.
     * 
     * @return The player who won the game.
     */
    private Player determineWinner() {
        if (players.isEmpty()) {
            return null;
        }
        
        calculateFinalScores();
        
        Player winner = players.get(0);
        int highestScore = winner.getFinalScore();
        
        for (Player player : players) {
            if (player.getFinalScore() > highestScore) {
                highestScore = player.getFinalScore();
                winner = player;
            }
        }
        
        return winner;
    }

    /**
     * Returns the current player in the game.
     * @return The player who is currently taking their turn.
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Moves to the next player in the game, transitioning the turn to the next player.
     * @return The player who will take their turn next.
     */
    public Player nextPlayer(){
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return players.get(currentPlayerIndex);
    }

    /**
     * Returns the flight board that tracks the players' progress in the game.
     * @return The flight board.
     */
    public FlightBoard getFlightBoard() {
        return flightBoard;
    }

    /**
     * Returns the adventure deck.
     * @return The adventure deck.
     */
    public AdventureDeck getAdventureDeck() {
        return adventureDeck;
    }

    /**
     * Returns a list of all the players participating in the game.
     * @return A list of players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Returns the current phase of the game.
     * @return The current game phase.
     */
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Sets the current phase of the game.
     * @param phase The new phase to set.
     */
    public void setCurrentPhase(GamePhase phase) {
        this.currentPhase = phase;
    }

    /**
     * Returns the game's unique identifier.
     * @return The game ID.
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Returns the game's difficulty level.
     * @return The game level.
     */
    public GameLevel getLevel() {
        return level;
    }

    /**
     * Returns the game's configuration.
     * @return The game configuration.
     */
    public GameConfig getConfig() {
        return config;
    }

    /**
     * Returns the component deck.
     * @return The component deck.
     */
    public ComponentDeck getComponentDeck() {
        return componentDeck;
    }

    /**
     * Sets the component deck for the game.
     * @param componentDeck The component deck to set.
     */
    public void setComponentDeck(ComponentDeck componentDeck) {
        this.componentDeck = componentDeck;
    }

    /**
     * Returns the current player index.
     * @return The index of the current player in the players list.
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Returns the lead player.
     * @return The lead player.
     */
    public Player getLeadPlayer() {
        return leadPlayer;
    }

    /**
     * Removes a player from the game.
     * 
     * @param playerId The ID of the player to remove.
     * @return true if the player was removed, false if the player was not found.
     */
    public boolean removePlayer(PlayerId playerId) {
        Player player = getPlayerById(playerId);
        if (player != null) {
            players.remove(player);
            return true;
        }
        return false;
    }
    
    // Building timer management
    
    /**
     * Attempts to flip the building timer to the next stage.
     * @param playerId The player attempting to flip the timer
     * @param playerHasCompletedShip Whether the player has completed their ship
     * @return true if timer was successfully flipped, false otherwise
     */
    public boolean flipBuildingTimer(PlayerId playerId, boolean playerHasCompletedShip) {
        if (buildingTimer != null && currentPhase == GamePhase.BUILDING) {
            BuildingTimer.FlipResult result = buildingTimer.flipTimer(playerId.toString(), playerHasCompletedShip);
            return result == BuildingTimer.FlipResult.SUCCESS;
        }
        return false;
    }
    
    /**
     * Gets the current building timer state
     */
    public BuildingTimer.TimerState getBuildingTimerState() {
        return buildingTimer != null ? buildingTimer.getCurrentState() : BuildingTimer.TimerState.IDLE;
    }
    
    /**
     * Gets the time remaining in the current building timer stage
     * @return Time remaining in milliseconds, or -1 if no timer is active
     */
    public long getBuildingTimeRemaining() {
        return buildingTimer != null ? buildingTimer.getTimeRemaining() : -1;
    }
    
    /**
     * Gets the building timer instance for event listener setup
     */
    public BuildingTimer getBuildingTimer() {
        return buildingTimer;
    }
    
    /**
     * Gets the game name
     * @return The game name
     */
    public String getGameName() {
        return gameName;
    }
    
    /**
     * Gets the game level (alias for getLevel for compatibility)
     * @return The game level
     */
    public GameLevel getGameLevel() {
        return level;
    }
    
    /**
     * Gets the current number of players
     * @return The current number of players
     */
    public int getCurrentPlayers() {
        return players.size();
    }
    
    /**
     * Gets the maximum number of players allowed
     * @return The maximum number of players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * Gets the creator/host ID of the game (first player who joined)
     * @return The creator player ID as string, or null if no players
     */
    public String getCreatorId() {
        return players.isEmpty() ? null : players.get(0).getId().toString();
    }
    
    // Component Management Operations (moved from GameSession)
    
    /**
     * Handles a player taking a component from the deck
     */
    public boolean takeComponent(PlayerId playerId) {
        if (currentPhase != GamePhase.BUILDING) {
            return false;
        }
        
        Player player = getPlayerById(playerId);
        if (player == null) return false;
        
        Optional<Component> drawnComponentOpt = componentDeck.draw();
        if (drawnComponentOpt.isEmpty()) return false;
        
        Component drawnComponent = drawnComponentOpt.get();
        
        // Player can only hold one component at a time
        if (player.getHeldComponent() != null) {
            // Return current held component to face-up pile
            componentDeck.returnToFaceUp(player.getHeldComponent());
        }
        
        player.setHeldComponent(drawnComponent);
        
        // Fire event
        if (propertyChangeSupport != null) {
            ComponentTakenEvent event = new ComponentTakenEvent(
                gameId, 
                drawnComponent, 
                player, 
                componentDeck
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
        
        return true;
    }
    
    /**
     * Handles a player placing a component on their ship
     */
    public boolean placeComponent(PlayerId playerId, Component component, it.polimi.ingsw.server.model.domain.ship.Position position) {
        if (currentPhase != GamePhase.BUILDING) {
            return false;
        }
        
        Player player = getPlayerById(playerId);
        if (player == null || player.getShip() == null) return false;
        
        // Verify player owns this component
        if (!component.equals(player.getHeldComponent())) {
            return false;
        }
        
        try {
            player.getShip().addComponent(component, position);
            player.getShip().updateStats();
            player.clearHeldComponent(); // Component is now on ship
            
            // Fire ComponentPlacedEvent
            if (propertyChangeSupport != null) {
                ComponentPlacedEvent event = new ComponentPlacedEvent(
                    gameId,
                    player,
                    component,
                    player.getShip(),
                    componentDeck
                );
                propertyChangeSupport.firePropertyChange("eventPublished", null, event);
            }
            
            // Fire ShipStatsUpdatedEvent (centralized from Ship.updateStats())
            fireShipStatsUpdatedEvent(playerId, player.getNickname(), player.getShip());
            
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Handles a player reserving a component for later placement
     */
    public boolean reserveComponent(PlayerId playerId, String componentId) {
        if (currentPhase != GamePhase.BUILDING) {
            return false;
        }
        
        Player player = getPlayerById(playerId);
        if (player == null || player.getShip() == null) return false;
        
        // Check if reservations are supported for this ship based on configuration
        if (!player.getShip().supportsComponentReservation()) {
            return false;
        }
        
        // Find component in player's held component or face-up pile
        Component component = null;
        if (player.getHeldComponent() != null && player.getHeldComponent().getId().equals(componentId)) {
            component = player.getHeldComponent();
        } else {
            component = componentDeck.takeFaceUpComponentById(componentId);
            if (component == null) return false;
        }
        
        try {
            // Add to ship's reserved components (Ship.reserveComponent now returns boolean)
            boolean reserved = player.getShip().reserveComponent(component);
            if (!reserved) {
                // Reservation failed (max limit reached), return component to face-up pile if we took it
                if (player.getHeldComponent() != component) {
                    componentDeck.returnToFaceUp(component);
                }
                return false;
            }
            
            if (player.getHeldComponent() == component) {
                player.clearHeldComponent(); // Component is now reserved on ship
            }
            
            // Fire event
            if (propertyChangeSupport != null) {
                ComponentReservedEvent event = new ComponentReservedEvent(
                    gameId,
                    component,
                    player,
                    componentDeck
                );
                propertyChangeSupport.firePropertyChange("eventPublished", null, event);
            }
            
            return true;
        } catch (Exception e) {
            // If reservation failed, return component to face-up pile if we took it
            if (player.getHeldComponent() != component) {
                componentDeck.returnToFaceUp(component);
            }
            return false;
        }
    }
    
    /**
     * Handles a player returning a component to the face-up pile
     */
    public boolean returnComponent(PlayerId playerId, String componentId) {
        if (currentPhase != GamePhase.BUILDING) {
            return false;
        }
        
        Player player = getPlayerById(playerId);
        if (player == null) return false;
        
        Component componentToReturn = null;
        
        // Check if it's the held component
        if (player.getHeldComponent() != null && player.getHeldComponent().getId().equals(componentId)) {
            componentToReturn = player.getHeldComponent();
            player.clearHeldComponent();
        } else {
            // Check if it's a reserved component on ship
            if (player.getShip() != null) {
                componentToReturn = player.getShip().releaseReservedComponent(componentId);
            }
        }
        
        if (componentToReturn == null) return false;
        
        componentDeck.returnToFaceUp(componentToReturn);
        
        // Fire event
        if (propertyChangeSupport != null) {
            ComponentOfferedEvent event = new ComponentOfferedEvent(
                gameId,
                componentToReturn,
                player,
                componentDeck
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
        
        return true;
    }
    
    /**
     * Gets a component by ID from available sources
     */
    public Component getComponentById(String componentId) {
        return componentDeck.findComponentById(componentId);
    }
    
    /**
     * Gets all available components for UI sync
     */
    public Map<String, Component> getAvailableComponents() {
        Map<String, Component> allComponents = componentDeck.getAvailableComponentsMap();
        Map<String, Component> availableComponents = new HashMap<>();
        
        // Filter out components held by players
        for (Map.Entry<String, Component> entry : allComponents.entrySet()) {
            Component component = entry.getValue();
            boolean isHeld = false;
            
            // Check if any player is holding this component
            for (Player player : players) {
                if (component.equals(player.getHeldComponent())) {
                    isHeld = true;
                    break;
                }
            }
            
            if (!isHeld) {
                availableComponents.put(entry.getKey(), component);
            }
        }
        
        return availableComponents;
    }
    
    /**
     * Handles ship validation for a player
     */
    public boolean validateShip(PlayerId playerId, String playerNickname, List<String> feedback) {
        Player player = getPlayerById(playerId);
        if (player == null || player.getShip() == null) return false;
        
        // Update ship stats and check validity
        player.getShip().updateStats();
        
        // Basic validation - ship must be structurally valid
        boolean isValid = player.getShip().isStructurallyValid();
        
        // Mark player as ready if validation passes
        player.setReady(isValid);
        
        // Fire event
        if (propertyChangeSupport != null) {
            ShipValidationEvent event = new ShipValidationEvent(
                gameId,
                playerId.toString(),
                playerNickname,
                isValid,
                feedback != null ? feedback : new ArrayList<>(),
                player,
                this
            );
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
        
        return isValid;
    }
    
    /**
     * Handles component removal from a player's ship and fires appropriate events
     */
    public boolean removeComponent(PlayerId playerId, it.polimi.ingsw.server.model.domain.ship.Position position, String reason) {
        Player player = getPlayerById(playerId);
        if (player == null || player.getShip() == null) return false;
        
        Component removedComponent = player.getShip().getComponentAt(position.getRow(), position.getCol());
        if (removedComponent == null) return false;
        
        // Remove component from ship (without firing event - Ship will be updated to not fire events)
        player.getShip().removeComponent(position, currentPhase);
        
        // Fire ComponentRemovedEvent from GameModel
        fireComponentRemovedEvent(playerId, player.getNickname(), removedComponent, position, reason);
        
        // Fire ShipStatsUpdatedEvent since ship stats changed
        fireShipStatsUpdatedEvent(playerId, player.getNickname(), player.getShip());
        
        return true;
    }
    
    /**
     * Updates player credits and fires appropriate events
     */
    public void updatePlayerCredits(PlayerId playerId, int newCredits) {
        Player player = getPlayerById(playerId);
        if (player == null) return;
        
        int oldCredits = player.getCredits();
        player.setCredits(newCredits); // Will be updated to not fire events
        
        // Fire PlayerCreditsChangedEvent from GameModel
        firePlayerCreditsChangedEvent(playerId, player.getNickname(), oldCredits, newCredits);
    }
    
    /**
     * Updates player held component and fires appropriate events
     */
    public void updatePlayerHeldComponent(PlayerId playerId, Component newComponent) {
        Player player = getPlayerById(playerId);
        if (player == null) return;
        
        Component oldComponent = player.getHeldComponent();
        player.setHeldComponent(newComponent); // Will be updated to not fire events
        
        // Fire PlayerComponentChangedEvent from GameModel
        firePlayerComponentChangedEvent(playerId, player.getNickname(), oldComponent, newComponent);
    }
    
    /**
     * Updates player ready status and fires appropriate events
     */
    public void updatePlayerReadyStatus(PlayerId playerId, boolean ready) {
        Player player = getPlayerById(playerId);
        if (player == null) return;
        
        player.setReady(ready); // Will be updated to not fire events
        
        // Fire PlayerReadyChangedEvent from GameModel
        firePlayerReadyChangedEvent(playerId, player.getNickname(), ready);
    }
    
    // === CENTRALIZED EVENT ORCHESTRATION METHODS ===
    
    /**
     * Fires ComponentRemovedEvent when a component is removed from a player's ship
     */
    public void fireComponentRemovedEvent(PlayerId playerId, String playerNickname, Component component, 
                                        it.polimi.ingsw.server.model.domain.ship.Position position, String reason) {
        if (propertyChangeSupport != null) {
            ComponentRemovedEvent event = new ComponentRemovedEvent(gameId, playerId, playerNickname, component, position, reason);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Fires ShipStatsUpdatedEvent when ship statistics are recalculated
     */
    public void fireShipStatsUpdatedEvent(PlayerId playerId, String playerNickname, Ship ship) {
        if (propertyChangeSupport != null) {
            ShipStatsUpdatedEvent event = new ShipStatsUpdatedEvent(gameId, playerId, playerNickname, ship);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Fires PlayerCreditsChangedEvent when player credits change
     */
    public void firePlayerCreditsChangedEvent(PlayerId playerId, String playerNickname, int oldCredits, int newCredits) {
        if (propertyChangeSupport != null) {
            PlayerCreditsChangedEvent event = new PlayerCreditsChangedEvent(gameId, playerId, playerNickname, oldCredits, newCredits);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Fires PlayerComponentChangedEvent when player held component changes
     */
    public void firePlayerComponentChangedEvent(PlayerId playerId, String playerNickname, Component oldComponent, Component newComponent) {
        if (propertyChangeSupport != null) {
            PlayerComponentChangedEvent event = new PlayerComponentChangedEvent(gameId, playerId, playerNickname, oldComponent, newComponent);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Fires PlayerReadyChangedEvent when player ready status changes
     */
    public void firePlayerReadyChangedEvent(PlayerId playerId, String playerNickname, boolean ready) {
        if (propertyChangeSupport != null) {
            PlayerReadyChangedEvent event = new PlayerReadyChangedEvent(gameId, playerId, playerNickname, ready);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Fires ComponentOfferedEvent when component is returned to face-up pile
     */
    public void fireComponentOfferedEvent(Component component, Player player) {
        if (propertyChangeSupport != null) {
            ComponentOfferedEvent event = new ComponentOfferedEvent(gameId, component, player, componentDeck);
            propertyChangeSupport.firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Gets the PropertyChangeSupport instance for event firing
     */
    public PropertyChangeSupport getPropertyChangeSupport() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        return propertyChangeSupport;
    }
    
    /**
     * Helper method to fire PropertyChangeSupport events
     */
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
    
    /**
     * Creates the starting cabin component for the specified player color.
     * Starting cabins are created directly and are not part of the drawable deck.
     */
    private Component getStartingCabinForPlayer(PlayerColor color) {
        // Create starting cabin directly - starting cabins have UNIVERSAL connectors on all sides
        String cabinId = "/assets/tiles/starting-cabin-" + color.name().toLowerCase() + ".jpg";
        
        // Create connectors map - starting cabins have UNIVERSAL connectors on all sides
        Map<it.polimi.ingsw.server.model.enums.ship.Direction, it.polimi.ingsw.server.model.enums.ship.ConnectorType> connectors = new HashMap<>();
        connectors.put(it.polimi.ingsw.server.model.enums.ship.Direction.UP, it.polimi.ingsw.server.model.enums.ship.ConnectorType.UNIVERSAL);
        connectors.put(it.polimi.ingsw.server.model.enums.ship.Direction.RIGHT, it.polimi.ingsw.server.model.enums.ship.ConnectorType.UNIVERSAL);
        connectors.put(it.polimi.ingsw.server.model.enums.ship.Direction.DOWN, it.polimi.ingsw.server.model.enums.ship.ConnectorType.UNIVERSAL);
        connectors.put(it.polimi.ingsw.server.model.enums.ship.Direction.LEFT, it.polimi.ingsw.server.model.enums.ship.ConnectorType.UNIVERSAL);
        
        // Create and return the starting cabin
        return new it.polimi.ingsw.server.model.domain.ship.components.Cabin(ComponentType.CABIN_START, connectors, cabinId);
    }


}
package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.flight.RewardSystem;
import it.polimi.ingsw.server.model.domain.flight.Route;
import it.polimi.ingsw.server.model.domain.general.config.FlightBoardConfig;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.general.config.RewardSystemConfig;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.general.config.GameConfig;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the current state of the game, including the current player, the flight board,
 * the adventure deck, the list of players, and the current game phase.
 */
public class GameModel {
    private final String gameId;
    private final GameLevel level;
    private final GameConfig config;
    private final List<Player> players;
    private final GameConfigurationManager configManager;
    private final int maxPlayers;
    
    private GamePhase currentPhase;
    private ComponentDeck componentDeck;
    private AdventureDeck adventureDeck;
    private FlightBoard flightBoard;
    private int currentPlayerIndex;
    private Player leadPlayer;
    private boolean isInitialized;
    private BuildingTimer buildingTimer;

    /**
     * Creates a new game model with the specified difficulty level, configuration, and number of players.
     * 
     * @param level The difficulty level of the game.
     * @param configManager The configuration manager for the game.
     * @param playerCount The number of players who will participate in the game.
     * @throws IllegalArgumentException if playerCount is not within valid range
     */
    public GameModel(GameLevel level, GameConfigurationManager configManager, int playerCount) {
        if (playerCount < 2 || playerCount > 4) {
            throw new IllegalArgumentException("Player count must be between 2 and 4");
        }

        this.gameId = UUID.randomUUID().toString();
        this.level = level;
        this.configManager = configManager;
        this.config = configManager.getConfigForLevel(level);
        this.players = new ArrayList<>();
        this.currentPhase = GamePhase.SETUP;
        this.currentPlayerIndex = 0;
        this.maxPlayers = playerCount;
        this.isInitialized = false;
        this.buildingTimer = new BuildingTimer(level);
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
        var gameConfig = configManager.getConfigForLevel(level);
        var shipGridConfig = gameConfig.shipGridConfig();
        var ship = new it.polimi.ingsw.server.model.domain.ship.Ship(level, shipGridConfig);
        
        // Assign ship to player
        player.setShip(ship);
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

        GameConfig gameCfg = configManager.getConfigForLevel(level);
        FlightBoardConfig fbCfg = gameCfg.flightBoardConfig();
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
        this.adventureDeck = configManager.createAdventureDeck(level);
        this.componentDeck = configManager.createComponentDeck(level);
        
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
    public boolean flipBuildingTimer(String playerId, boolean playerHasCompletedShip) {
        if (buildingTimer != null && currentPhase == GamePhase.BUILDING) {
            return buildingTimer.flipTimer(playerId, playerHasCompletedShip);
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

}
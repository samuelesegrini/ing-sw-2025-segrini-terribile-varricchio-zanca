package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Game session manager that handles multiple games.
 */
public class GameSessionManager {
    private static final Logger LOGGER = Logger.getLogger(GameSessionManager.class.getName());

    private final Map<String, GameSession> gameSessions;
    private final Map<String, String> playerToGameMap;
    private final GameConfigurationManager configManager;
    private final ServerNetworkManager networkManager;
    private final PlayerSessionRegistry playerRegistry;
    private final ExecutorService gameExecutor;

    public GameSessionManager(ServerNetworkManager networkManager,
                              PlayerSessionRegistry playerRegistry) {
        this.gameSessions = new ConcurrentHashMap<>();
        this.playerToGameMap = new ConcurrentHashMap<>();
        this.networkManager = networkManager;
        this.playerRegistry = playerRegistry;
        this.configManager = new GameConfigurationManager();
        this.gameExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("game-executor-" + t.getId());
            return t;
        });

        // Load game configurations
        try {
            configManager.loadAllConfigurations(
                    "/it/polimi/ingsw/json/components.json",
                    "/it/polimi/ingsw/json/adventure_cards.json",
                    "/it/polimi/ingsw/json/game_configurations.json"
            );
            System.out.println("Available game levels: " + configManager.getAllGameConfigs().keySet());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load game configurations", e);
            throw new RuntimeException("Failed to initialize game configurations", e);
        }
    }

    /**
     * Creates a new game session.
     */
    public String createGame(String creatorId, int maxPlayers, GameLevel gameLevel, String gameName) {
        // Check if player is already in a game
        if (playerToGameMap.containsKey(creatorId)) {
            LOGGER.warning("Player " + creatorId + " already in a game");
            return null;
        }

        String gameId = UUID.randomUUID().toString();
        GameSession session = new GameSession(gameId, gameName, creatorId,
                maxPlayers, gameLevel, configManager, playerRegistry);

        gameSessions.put(gameId, session);
        playerToGameMap.put(creatorId, gameId);

        LOGGER.info("Created game " + gameId + " by player " + creatorId);
        return gameId;
    }

    /**
     * Joins a player to an existing game.
     */
    public boolean joinGame(String gameId, String playerId) {
        // Check if player is already in a game
        if (playerToGameMap.containsKey(playerId)) {
            LOGGER.warning("Player " + playerId + " already in a game");
            return false;
        }

        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            LOGGER.warning("Game " + gameId + " not found");
            return false;
        }

        if (session.addPlayer(playerId)) {
            playerToGameMap.put(playerId, gameId);
            return true;
        }

        return false;
    }

    /**
     * Removes a player from their current game.
     */
    public boolean removePlayerFromGame(String gameId, String playerId) {
        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            return false;
        }

        if (session.removePlayer(playerId)) {
            playerToGameMap.remove(playerId);

            // Remove empty games
            if (session.getPlayerCount() == 0) {
                gameSessions.remove(gameId);
                LOGGER.info("Removed empty game " + gameId);
            }

            return true;
        }

        return false;
    }

    /**
     * Gets the game session for a specific game ID.
     */
    public GameSession getGameSession(String gameId) {
        return gameSessions.get(gameId);
    }

    /**
     * Gets the game session that a player is in.
     */
    public GameSession getGameSessionForPlayer(String playerId) {
        String gameId = playerToGameMap.get(playerId);
        if (gameId != null) {
            return gameSessions.get(gameId);
        }
        return null;
    }
    
    /**
     * Gets the game ID for a specific player.
     */
    public String getPlayerGameId(String playerId) {
        return playerToGameMap.get(playerId);
    }

    /**
     * Gets all games (both available to join and in progress).
     */
    public List<GameModel> getAvailableGames() {
        List<GameModel> allGames = new ArrayList<>();

        for (GameSession session : gameSessions.values()) {
            allGames.add(session.getGameModel());
        }

        return allGames;
    }

    /**
     * Shuts down the game session manager.
     */
    public void shutdown() {
        gameExecutor.shutdown();
        try {
            if (!gameExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                gameExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            gameExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
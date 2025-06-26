package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
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
    private final Map<PlayerId, String> playerToGameMap;
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
            t.setName("game-executor-" + t.threadId());
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
    public synchronized String createGame(PlayerId creatorId, int maxPlayers, GameLevel gameLevel, String gameName) {
        LOGGER.info("🎮 CREATE GAME REQUEST - Creator: " + creatorId + ", maxPlayers: " + maxPlayers + 
                   ", level: " + gameLevel + ", name: '" + gameName + "'");
        
        // Check if player is already in a game
        if (playerToGameMap.containsKey(creatorId)) {
            LOGGER.warning("❌ CREATE FAILED - Player " + creatorId + " already in a game");
            return null;
        }

        String gameId = UUID.randomUUID().toString();
        LOGGER.info("🆔 GAME ID GENERATED - New gameId: " + gameId);
        
        GameSession session = new GameSession(gameId, gameName, creatorId,
                maxPlayers, gameLevel, configManager, playerRegistry);
        LOGGER.info("🏗️ GAME SESSION CREATED - GameSession object created for gameId: " + gameId);

        gameSessions.put(gameId, session);
        playerToGameMap.put(creatorId, gameId);
        
        LOGGER.info("✅ GAME STORED - Game " + gameId + " stored in gameSessions map. Total games: " + gameSessions.size());
        LOGGER.info("🗺️ PLAYER MAPPED - Player " + creatorId + " mapped to game " + gameId);
        LOGGER.info("🎉 CREATE SUCCESS - Game " + gameId + " created by player " + creatorId);
        
        return gameId;
    }
    
    /**
     * Creates a new game session (legacy String overload).
     */
    public String createGame(String creatorIdString, int maxPlayers, GameLevel gameLevel, String gameName) {
        PlayerId creatorId = PlayerId.fromString(creatorIdString);
        return createGame(creatorId, maxPlayers, gameLevel, gameName);
    }

    /**
     * Joins a player to an existing game.
     */
    public synchronized boolean joinGame(String gameId, PlayerId playerId) {
        LOGGER.info("🔍 JOIN GAME ATTEMPT - Player " + playerId + " trying to join game: " + gameId);
        LOGGER.info("📊 CURRENT STATE - Total games: " + gameSessions.size() + ", Player already in game: " + playerToGameMap.containsKey(playerId));
        
        // Log all available game IDs for debugging
        if (gameSessions.isEmpty()) {
            LOGGER.warning("⚠️ NO GAMES AVAILABLE - gameSessions map is empty");
        } else {
            LOGGER.info("🎮 AVAILABLE GAMES: " + gameSessions.keySet());
        }
        
        // Check if player is already in a game
        if (playerToGameMap.containsKey(playerId)) {
            String currentGameId = playerToGameMap.get(playerId);
            LOGGER.warning("❌ JOIN FAILED - Player " + playerId + " already in game: " + currentGameId);
            return false;
        }

        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            LOGGER.severe("❌ JOIN FAILED - Game " + gameId + " not found in gameSessions map! Available games: " + gameSessions.keySet());
            
            // Additional debugging: check if gameId format is correct
            LOGGER.severe("🔍 DEBUG INFO - Requested gameId: '" + gameId + "' (length: " + gameId.length() + ")");
            if (!gameSessions.isEmpty()) {
                String firstAvailableGame = gameSessions.keySet().iterator().next();
                LOGGER.severe("🔍 COMPARISON - First available game: '" + firstAvailableGame + "' (length: " + firstAvailableGame.length() + ")");
                LOGGER.severe("🔍 EQUALS CHECK - gameId.equals(firstAvailable): " + gameId.equals(firstAvailableGame));
            }
            
            return false;
        }
        
        LOGGER.info("✅ GAME FOUND - Game " + gameId + " exists, attempting to add player");

        if (session.addPlayer(playerId)) {
            playerToGameMap.put(playerId, gameId);
            LOGGER.info("🎉 JOIN SUCCESS - Player " + playerId + " successfully joined game: " + gameId);
            return true;
        } else {
            LOGGER.warning("❌ JOIN FAILED - GameSession.addPlayer() returned false for player " + playerId + " and game " + gameId);
        }

        return false;
    }
    
    /**
     * Joins a player to an existing game (legacy String overload).
     */
    public boolean joinGame(String gameId, String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return joinGame(gameId, playerId);
    }

    /**
     * Removes a player from their current game.
     */
    public boolean removePlayerFromGame(String gameId, PlayerId playerId) {
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
     * Removes a player from their current game (legacy String overload).
     */
    public boolean removePlayerFromGame(String gameId, String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return removePlayerFromGame(gameId, playerId);
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
    public GameSession getGameSessionForPlayer(PlayerId playerId) {
        String gameId = playerToGameMap.get(playerId);
        if (gameId != null) {
            return gameSessions.get(gameId);
        }
        return null;
    }
    
    /**
     * Gets the game session that a player is in (legacy String overload).
     */
    public GameSession getGameSessionForPlayer(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getGameSessionForPlayer(playerId);
    }
    
    /**
     * Gets the game ID for a specific player.
     */
    public String getPlayerGameId(PlayerId playerId) {
        return playerToGameMap.get(playerId);
    }
    
    /**
     * Gets the game ID for a specific player (legacy String overload).
     */
    public String getPlayerGameId(String playerIdString) {
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getPlayerGameId(playerId);
    }

    /**
     * Gets all games (both available to join and in progress).
     */
    public synchronized List<GameModel> getAvailableGames() {
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
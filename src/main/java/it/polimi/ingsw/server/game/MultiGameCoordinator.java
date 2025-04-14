package it.polimi.ingsw.server.game;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService; // Needed for createGame
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.server.commands.CommandProcessor;
import it.polimi.ingsw.server.network.ServerClientHandler;
import it.polimi.ingsw.server.session.SessionInfo;
import it.polimi.ingsw.server.session.SessionManager;

/**
 * Manages the lifecycle of multiple game instances (`GameInstanceController`)
 * and routes incoming player connections (new joins and reconnections) to the correct game.
 * Does not enforce a maximum number of concurrent games.
 */
public class MultiGameCoordinator {

    private static final Logger logger = Logger.getLogger(MultiGameCoordinator.class.getName());

    // Map: gameId -> GameInstanceController. Stores active game instances. Thread-safe.
    private final Map<String, GameInstanceController> games = new ConcurrentHashMap<>();
    private final SessionManager sessionManager;
    private final ExecutorService gameLogicExecutor; // Shared executor for all game logic (ADR-0006)
    private final ExecutorService networkExecutor;   // Shared executor for network I/O tasks (ADR-0006)

    // TODO: Inject or load GameConfigurationManager properly
    private final GameConfigurationManager configManager = new GameConfigurationManager(); // Temporary default

    /**
     * Constructs a MultiGameCoordinator.
     *
     * @param gameLogicExecutor The executor service dedicated to running game logic tasks.
     * @param networkExecutor   The executor service dedicated to network I/O tasks.
     * @param sessionManager    The global session manager instance.
     */
    public MultiGameCoordinator(ExecutorService gameLogicExecutor, ExecutorService networkExecutor, SessionManager sessionManager) {
        this.gameLogicExecutor = gameLogicExecutor;
        this.networkExecutor = networkExecutor;
        this.sessionManager = sessionManager;
        logger.info("MultiGameCoordinator initialized.");
    }

    /**
     * Creates a new game instance with a unique ID.
     * Instantiates a new GameInstanceController for the game.
     *
     * @param requestedGameId Optional requested game ID. If null or empty, a new UUID is generated.
     * @param initialPlayerCount Placeholder for initial player count needed by GameModel constructor.
     * @return The newly created GameInstanceController, or null if a game with the specified ID already exists.
     */
    // TODO: Refine how initialPlayerCount is determined (e.g., from game settings/request)
    public GameInstanceController createGame(String requestedGameId, int initialPlayerCount) {
        String gameId = (requestedGameId == null || requestedGameId.trim().isEmpty()) ? UUID.randomUUID().toString() : requestedGameId;

        // 1. Atomically check and create the game instance if the ID is not already taken.
        AtomicReference<GameInstanceController> newGameRef = new AtomicReference<>(null);
        GameInstanceController existingOrNewGame = games.computeIfAbsent(gameId, id -> {
            logger.log(Level.INFO, "Creating new game instance with ID: {0}", id);

            // 2. Create dependencies for the new GameInstanceController
            try {
                // Use the actual GameModel constructor
                // Use LEVEL_II as the default level
                GameModel model = new GameModel(GameLevel.LEVEL_II, configManager, initialPlayerCount);
                RuleEngine rules = new RuleEngine(); // Assuming RuleEngine doesn't need GameModel yet
                ScoringEngine scoring = new ScoringEngine(); // Assuming ScoringEngine doesn't need GameModel yet
                CommandProcessor processor = new CommandProcessor(model); // CommandProcessor needs the model

                // 3. Create the GameInstanceController
                GameInstanceController controller = new GameInstanceController(id, model, rules, scoring, processor, sessionManager, gameLogicExecutor, networkExecutor);
                newGameRef.set(controller); // Store the newly created instance
                return controller;
            } catch (IllegalArgumentException e) {
                // Catch exceptions from GameModel constructor (e.g., invalid playerCount)
                logger.log(Level.SEVERE, "Failed to create GameModel for game " + id, e);
                return null; // Returning null prevents insertion into the map
            }
        });

        // Check if computeIfAbsent returned null (due to exception during creation)
        if (existingOrNewGame == null) {
            logger.log(Level.SEVERE, "Game instance creation failed internally for ID: {0}", gameId);
            return null;
        }

        // 4. Check if the instance returned by computeIfAbsent is the one we just created.
        if (existingOrNewGame == newGameRef.get()) {
            // It was newly created
            logger.log(Level.INFO, "Successfully created and registered game: {0}", gameId);
            return existingOrNewGame;
        } else {
            // A game with that ID already existed, computeIfAbsent returned the existing one.
            logger.log(Level.WARNING, "Game creation failed: Game with ID {0} already exists.", gameId);
            return null; // Indicate failure because the requested ID was taken
        }
    }

    /**
     * Handles a player's request to join an existing game or reconnect to it.
     * Uses SessionManager to validate the player and find existing sessions.
     *
     * @param gameId       The ID of the game the player wants to join/reconnect to.
     * @param playerName   The player's chosen nickname (used for new joins, potentially for identifying reconnections too).
     * @param sessionToken Potential session token for reconnection validation.
     * @param clientHandler The handler representing the player's network connection.
     * @return A CompletableFuture indicating whether the join/reconnect was successful (completes with true/false).
     *         Completes exceptionally if the game doesn't exist or another critical error occurs.
     */
    public CompletableFuture<Boolean> joinOrReconnectGame(String gameId, String playerName, String sessionToken, ServerClientHandler clientHandler) {
        // Convert playerName to PlayerId at the beginning
        PlayerId playerId;
        try {
            // Assuming PlayerId has a static factory method or constructor
            playerId = PlayerId.fromString(playerName);
        } catch (Exception e) {
            // Handle invalid player name/ID format if necessary
            String errorMsg = "Invalid player name format: " + playerName;
            logger.log(Level.WARNING, errorMsg, e);
            clientHandler.sendError(errorMsg, true);
            return CompletableFuture.completedFuture(false);
        }

        logger.log(Level.INFO, "Player {0} attempting to join/reconnect to game {1} (Token Provided: {2})",
                new Object[]{playerId, gameId, (sessionToken != null && !sessionToken.isEmpty())});

        // 1. Get the GameInstanceController for the given gameId.
        GameInstanceController game = games.get(gameId);
        if (game == null) {
            String errorMsg = "Player " + playerId + " failed to join/reconnect: Game " + gameId + " not found.";
            logger.log(Level.WARNING, errorMsg);
            // Send error back to the client trying to join
            clientHandler.sendError(errorMsg, true); // true indicates fatal error for this attempt
            return CompletableFuture.failedFuture(new IllegalArgumentException("Game not found: " + gameId));
        }

        // 2. Determine if it's a reconnection or new join attempt based on session token.
        if (sessionToken != null && !sessionToken.isEmpty()) {
            // --- Reconnection Attempt ---
            logger.log(Level.FINE, "Attempting reconnection for player {0} in game {1}", new Object[]{playerId, gameId});
            // Use validateReconnectionAttempt which checks token AND disconnected status
            if (sessionManager.validateReconnectionAttempt(playerId, sessionToken)) {
                logger.log(Level.INFO, "Session validated for reconnecting player {0} in game {1}", new Object[]{playerId, gameId});
                // Session is valid, delegate full reconnection process to GIC
                // Pass the correct PlayerId object to GIC method
                return game.processReconnectionRequest(playerId, sessionToken, clientHandler);
            } else {
                // Invalid session token or status for reconnection
                String errorMsg = "Reconnect failed for player " + playerId + ": Invalid session token or not disconnected.";
                logger.log(Level.WARNING, errorMsg);
                clientHandler.sendError(errorMsg, true);
                return CompletableFuture.completedFuture(false); // Indicate failure to reconnect
            }
        } else {
            // --- New Join Attempt ---
            logger.log(Level.FINE, "Attempting new join for player {0} in game {1}", new Object[]{playerId, gameId});

            // Basic validation (e.g., check if game is already full or started?)
            // TODO: Add max player check (requires info from GameModel/GIC)
            // TODO: Add check if game phase prevents joining (requires info from GameModel/GIC)

            // Attempt to register a new session using the PlayerId object.
            SessionInfo newSession = sessionManager.registerNewSession(playerId, gameId);
            if (newSession == null) {
                String errorMsg = "Join failed for player " + playerId + ": ID already in use or session registration failed.";
                logger.log(Level.WARNING, errorMsg);
                clientHandler.sendError(errorMsg, true);
                return CompletableFuture.completedFuture(false);
            }

            // Session registered successfully, associate player ID (as String) with the handler
            // Handler likely needs the String representation for network comms
            clientHandler.setPlayerId(playerId.toString());

            // Add the player to the GameInstanceController using PlayerId
            return game.addPlayer(playerId, clientHandler) // GIC.addPlayer needs PlayerId
                    .thenApply(success -> {
                        if (success) {
                            logger.log(Level.INFO, "New player {0} successfully added to game {1}.", new Object[]{playerId, gameId});
                        } else {
                            logger.log(Level.WARNING, "Failed to add player {0} to game {1} (GIC rejected).", new Object[]{playerId, gameId});
                            // Clean up session if GIC add failed, use PlayerId
                            sessionManager.removePlayerSession(playerId);
                            clientHandler.sendError("Failed to add player to game instance.", true);
                        }
                        return success;
                    })
                    .exceptionally(ex -> {
                        logger.log(Level.SEVERE, "Error during addPlayer call for player " + playerId + " in game " + gameId, ex);
                        // Clean up session on exception, use PlayerId
                        sessionManager.removePlayerSession(playerId);
                        clientHandler.sendError("Internal server error while adding player.", true);
                        return false;
                    });
        }
    }

    /**
     * Removes a finished or aborted game instance from the coordinator.
     * Also responsible for telling the GameInstanceController to shut down.
     *
     * @param gameId The ID of the game to remove.
     */
    public void removeGame(String gameId) {
        // 1. Remove the GameInstanceController from the map.
        GameInstanceController removedGame = games.remove(gameId);

        // 2. If removal was successful, tell the GIC to shut down.
        if (removedGame != null) {
            logger.log(Level.INFO, "Removing and shutting down game instance: {0}", gameId);
            try {
                removedGame.shutdown(); // Call shutdown logic within GIC
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during GameInstanceController shutdown for game " + gameId, e);
            }
            // Also remove associated sessions? GIC might do this on shutdown, or MGC can coordinate.
            // Example: Iterate players in removedGame.getModel() and call sessionManager.removePlayerSession()
        } else {
            logger.log(Level.WARNING, "Attempted to remove non-existent game: {0}", gameId);
        }
    }

    /**
     * Retrieves a specific game instance controller.
     *
     * @param gameId The ID of the game.
     * @return The GameInstanceController, or null if not found.
     */
    public GameInstanceController getGameInstance(String gameId) {
        return games.get(gameId);
    }

    /**
     * Handles notification that a client associated with a ServerClientHandler has disconnected.
     * Delegates the status update to the SessionManager and notifies the relevant GameInstanceController.
     *
     * @param clientHandler The handler whose client disconnected.
     */
    public void handleClientDisconnect(ServerClientHandler clientHandler) {
        String playerIdString = clientHandler.getPlayerId(); // Handler gives String ID
        if (playerIdString != null && !playerIdString.isEmpty()) {
            PlayerId playerId;
            try {
                playerId = PlayerId.fromString(playerIdString);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to parse PlayerId from handler during disconnect: " + playerIdString, e);
                return;
            }

            logger.log(Level.INFO, "Handling disconnect for player {0} (handler: {1})", new Object[]{playerId, clientHandler.getConnectionId()});

            // 1. Update session status via SessionManager using PlayerId
            sessionManager.registerDisconnection(playerId);

            // 2. Notify the GameInstanceController
            // Use PlayerId to find game ID
            String gameId = sessionManager.getGameIdForPlayer(playerId);
            if (gameId != null) {
                GameInstanceController game = games.get(gameId);
                if (game != null) {
                    // GIC might need notification, but SessionManager update is primary
                    logger.log(Level.FINE, "Notified game {0} about disconnection of player {1}", new Object[]{gameId, playerId});
                } else {
                    logger.log(Level.WARNING, "Game {0} not found to notify about disconnect of player {1}", new Object[]{gameId, playerId});
                }
            } else {
                logger.log(Level.WARNING, "Could not find game ID for disconnected player {0}", playerId);
            }
        } else {
            logger.log(Level.WARNING, "Handling disconnect for handler {0} with no associated player ID.", clientHandler.getConnectionId());
        }
    }

    /**
     * Periodically checks for and potentially removes inactive or empty games.
     * (Implementation depends on specific requirements for game cleanup).
     */
    public void cleanupInactiveGames() {
        logger.log(Level.FINE, "Running inactive game cleanup...");
        // Iterate over a copy of keys to avoid ConcurrentModificationException
        Set<String> gameIds = Set.copyOf(games.keySet());
        for (String gameId : gameIds) {
            GameInstanceController game = games.get(gameId);
            if (game != null) {
                // Define criteria for inactivity/emptiness
                // boolean isEmpty = game.getPlayerCount() == 0; // GIC needs getPlayerCount()
                // boolean isStale = (System.currentTimeMillis() - game.getLastActivityTime()) > STALE_TIMEOUT; // GIC needs last activity tracking
                // if (isEmpty || isStale) {
                //     logger.log(Level.INFO, "Cleaning up inactive/empty game: {0}", gameId);
                //     removeGame(gameId); // This calls shutdown on the GIC
                // }
            }
        }
    }

    /**
     * Routes a heartbeat message from a client to the SessionManager.
     *
     * @param playerIdString     The ID of the player sending the heartbeat.
     * @param sessionToken The player's session token for validation.
     */
    public void routeHeartbeat(String playerIdString, String sessionToken) {
        PlayerId playerId;
        try {
            playerId = PlayerId.fromString(playerIdString);
        } catch (Exception e) {
            logger.log(Level.FINE, "Invalid PlayerId format received in heartbeat: " + playerIdString, e);
            return;
        }
        logger.log(Level.FINEST, "Routing heartbeat for player {0}", playerId);
        // Use PlayerId object
        sessionManager.updateLastActivity(playerId, sessionToken);
    }

    /**
     * Shuts down the MultiGameCoordinator, including all active game instances.
     */
    public void shutdown() {
        logger.log(Level.INFO, "Shutting down MultiGameCoordinator...");
        // Iterate over a copy of keys and shut down each game instance
        Set<String> gameIds = Set.copyOf(games.keySet());
        for (String gameId : gameIds) {
            removeGame(gameId); // Ensures each GIC is shut down
        }
        games.clear();
        // Shutdown executors? Only if MGC *owns* them. If passed in, owner should shut down.
        logger.log(Level.INFO, "MultiGameCoordinator shutdown complete.");
    }

    // Potentially add methods to list available games, etc.
}
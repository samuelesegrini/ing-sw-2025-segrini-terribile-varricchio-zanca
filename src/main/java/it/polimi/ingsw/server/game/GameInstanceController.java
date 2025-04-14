package it.polimi.ingsw.server.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import it.polimi.ingsw.network.dto.*;
import it.polimi.ingsw.server.commands.AddPlayerCommand;
import it.polimi.ingsw.server.commands.CommandProcessor;
import it.polimi.ingsw.server.commands.CommandResult;
import it.polimi.ingsw.server.commands.PlaceComponentCommand;
import it.polimi.ingsw.server.network.ServerClientHandler;
import it.polimi.ingsw.server.session.SessionManager;

/**
 * Controls a single game instance.
 * Handles client actions asynchronously, orchestrates game flow (phases, card draws),
 * validates actions via RuleEngine, executes state changes via CommandProcessor,
 * and broadcasts updates to clients via ServerClientHandlers.
 */
public class GameInstanceController {

    private static final Logger logger = Logger.getLogger(GameInstanceController.class.getName());

    private final String gameId;
    private final GameModel model;
    private final RuleEngine rules;
    private final CommandProcessor commandProcessor;
    private final ScoringEngine scoringEngine;
    private final ExecutorService gameLogicExecutor; // Executor for GIC's own async tasks (if any beyond commands)
    private final ExecutorService networkExecutor; // Executor for broadcasting (passed or obtained elsewhere)
    private final SessionManager sessionManager; // Add SessionManager field

    // Map: PlayerId -> ServerClientHandler. Thread-safe.
    private final Map<PlayerId, ServerClientHandler> clientHandlers = new ConcurrentHashMap<>();

    /**
     * Constructs a GameInstanceController.
     *
     * @param gameId            Unique ID for this game instance.
     * @param model             The GameModel for this instance (it.polimi.ingsw...).
     * @param rules             The RuleEngine for validation.
     * @param scoringEngine     The ScoringEngine for final scoring.
     * @param commandProcessor  The CommandProcessor for state changes.
     * @param sessionManager    The global SessionManager instance.
     * @param gameLogicExecutor Executor for internal async logic.
     * @param networkExecutor   Executor for network broadcast operations.
     */
    public GameInstanceController(String gameId, GameModel model, RuleEngine rules, ScoringEngine scoringEngine, CommandProcessor commandProcessor, SessionManager sessionManager, ExecutorService gameLogicExecutor, ExecutorService networkExecutor) {
        this.gameId = gameId;
        this.model = model;
        this.rules = rules;
        this.scoringEngine = scoringEngine;
        this.commandProcessor = commandProcessor;
        this.sessionManager = sessionManager; // Initialize SessionManager
        this.gameLogicExecutor = gameLogicExecutor;
        this.networkExecutor = networkExecutor;
        logger.log(Level.INFO, "GameInstanceController created for game ID: {0}", gameId);
    }

    // --- Client Action Handling Methods ---

    /**
     * Processes a request from a player to acquire a component from the pool.
     *
     * @param playerIdString The ID (String representation) of the player making the request.
     * @param componentType The type of component requested (e.g., "ENGINE").
     * @return A CompletableFuture containing the CommandResult of the action.
     */
    public CompletableFuture<CommandResult> processComponentRequest(String playerIdString, String componentType) {
        logger.log(Level.FINER, "Processing component request from {0} for type {1}", new Object[]{playerIdString, componentType});
        PlayerId playerId = findPlayerIdFromString(playerIdString);
        if (playerId == null) {
            return CompletableFuture.completedFuture(CommandResult.failure("Unknown player ID: " + playerIdString));
        }

        // 1. Create an AcquireComponentCommand (Needs implementation)
        // AcquireComponentCommand command = new AcquireComponentCommand(playerId, componentType);
        // 2. Pre-validate
        // if (!command.canExecute(model)) { return CompletableFuture.completedFuture(CommandResult.failure("Cannot acquire component now.")); }
        // 3. Submit the command to the commandProcessor.
        // return commandProcessor.process(command);
        return CompletableFuture.completedFuture(CommandResult.failure("AcquireComponentCommand not implemented")); // Placeholder
    }

    /**
     * Processes a request from a player to place a component on their ship.
     *
     * @param playerIdString The ID (String representation) of the player making the request.
     * @param componentDTO The DTO of the component being placed.
     * @param position     The target position on the ship grid.
     * @return A CompletableFuture containing the CommandResult of the placement action.
     */
    public CompletableFuture<CommandResult> processPlacementRequest(String playerIdString, ComponentDTO componentDTO, Position position) {
        logger.log(Level.FINER, "Processing placement request from {0} for component {1} at {2}", new Object[]{playerIdString, componentDTO.getType(), position});
        PlayerId playerId = findPlayerIdFromString(playerIdString);
        if (playerId == null) {
            return CompletableFuture.completedFuture(CommandResult.failure("Unknown player ID: " + playerIdString));
        }

        // TODO: Convert ComponentDTO back to model Component to pass to command.
        // This requires knowing which component the DTO represents.
        // Maybe the client request should include a component identifier (from hand/pool)?
        // For now, assume we can get the Component model object.
        Component componentModel = null; // Placeholder!
        if (componentModel == null) {
            return CompletableFuture.completedFuture(CommandResult.failure("Could not identify component model from DTO."));
        }

        // 1. Create a PlaceComponentCommand
        PlaceComponentCommand command = new PlaceComponentCommand(playerId, componentModel, position, rules);
        // 2. Pre-validate
        // if (!command.canExecute(model)) { return CompletableFuture.completedFuture(CommandResult.failure("Invalid placement context.")); }

        // 3. Process command and handle result (including broadcasting)
        return commandProcessor.process(command)
                .thenComposeAsync(result -> {
                    if (result.isSuccess()) {
                        // If placement was successful, broadcast the ship update
                        ShipDTO updatedShip = getShipDTOForPlayer(playerIdString); // Generate updated DTO
                        return broadcastShipUpdate(playerIdString, updatedShip)
                                .thenApply(v -> result); // Return the original command result after broadcast
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                }, networkExecutor);
    }

    /**
     * Adds a new player to the game instance by processing an AddPlayerCommand.
     *
     * @param playerId      The PlayerId object for the new player.
     * @param clientHandler The network handler associated with this player.
     * @return A CompletableFuture<Boolean> indicating if the player was successfully added.
     */
    public CompletableFuture<Boolean> addPlayer(PlayerId playerId, ServerClientHandler clientHandler) {
        logger.log(Level.INFO, "Attempting to add player {0} to game {1}", new Object[]{playerId, gameId});

        // Pass PlayerId directly to the command constructor
        AddPlayerCommand command = new AddPlayerCommand(playerId);

        return commandProcessor.process(command)
                .thenApplyAsync(result -> {
                    if (result.isSuccess()) {
                        PlayerId addedPlayerId = result.getData(); // Command should return the PlayerId it used/created
                        if (addedPlayerId == null || !addedPlayerId.equals(playerId)) {
                            logger.log(Level.SEVERE, "AddPlayerCommand logic error for {0}. Expected ID {1}, got {2}",
                                    new Object[]{playerId.getNickname(), playerId, addedPlayerId});
                            return false;
                        }
                        clientHandler.setPlayerId(addedPlayerId.toString()); // Set handler ID as String
                        clientHandlers.put(addedPlayerId, clientHandler);
                        logger.log(Level.INFO, "Player {0} added to game {1} successfully.", new Object[]{addedPlayerId, gameId});
                        broadcastPlayerStatusUpdate(addedPlayerId.toString(), FlightStatus.RACING); // Broadcast using String ID
                        return true;
                    } else {
                        logger.log(Level.WARNING, "AddPlayerCommand failed for {0} in game {1}: {2}", new Object[]{playerId, gameId, result.getMessage()});
                        return false;
                    }
                }, gameLogicExecutor);
    }

    // --- Connection Management ---

    /**
     * Removes a client handler and potentially the player from the game model.
     *
     * @param playerIdString The ID (String representation) of the player whose handler should be removed.
     * @param broadcast Whether to broadcast the removal to other players.
     */
    public void removeClientHandler(String playerIdString, boolean broadcast) {
        PlayerId targetPlayerId = findPlayerIdFromString(playerIdString);
        if (targetPlayerId != null) {
            ServerClientHandler removedHandler = clientHandlers.remove(targetPlayerId);
            if (removedHandler != null) {
                logger.log(Level.INFO, "Removed client handler for player {0} from game {1}", new Object[]{playerIdString, gameId});
                // Consider closing connection via removedHandler.closeConnection();

                // TODO: Process a RemovePlayerCommand via commandProcessor

                if (broadcast) {
                    broadcastPlayerRemoval(playerIdString);
                }
            } else {
                logger.log(Level.WARNING, "Attempted to remove non-existent handler (found PlayerId but not handler) for player {0} from game {1}", new Object[]{playerIdString, gameId});
            }
        } else {
            logger.log(Level.WARNING, "Attempted to remove handler for unknown player ID {0} from game {1}", new Object[]{playerIdString, gameId});
        }
    }

    /**
     * Handles a player reconnection request.
     *
     * @param playerId       The PlayerId object of the reconnecting player.
     * @param sessionToken   The session token provided by the client.
     * @param clientHandler  The new network handler for the reconnected client.
     * @return A CompletableFuture indicating success (true) or failure (false) of the reconnection.
     */
    public CompletableFuture<Boolean> processReconnectionRequest(PlayerId playerId, String sessionToken, ServerClientHandler clientHandler) {
        logger.log(Level.INFO, "Processing reconnection request for player {0} in game {1}", new Object[]{playerId, gameId});

        // PlayerId already validated by MultiGameCoordinator using sessionManager.validateReconnectionAttempt

        clientHandler.setPlayerId(playerId.toString()); // Set handler ID as String
        ServerClientHandler oldHandler = clientHandlers.put(playerId, clientHandler);
        if (oldHandler != null && oldHandler != clientHandler) {
            logger.log(Level.WARNING, "Replacing existing handler for reconnecting player {0}", playerId);
            // Consider closing oldHandler.closeConnection();
        }

        // Mark as connected in SessionManager
        sessionManager.registerConnection(playerId);

        // Send full game state and broadcast status update
        return sendFullGameState(playerId.toString()) // Send state using String ID
                .thenComposeAsync(v -> broadcastPlayerStatusUpdate(playerId.toString(), FlightStatus.RACING), networkExecutor) // Broadcast using String ID
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    logger.log(Level.SEVERE, "Failed to process reconnection for player " + playerId, ex);
                    // Revert connection status on failure?
                    sessionManager.registerDisconnection(playerId);
                    clientHandlers.remove(playerId, clientHandler); // Remove the new handler if setup failed
                    return false;
                });
    }


    // --- Broadcasting Methods (Run on Network Executor) ---

    private CompletableFuture<Void> broadcast(CompletableFuture<?>... futures) {
        return CompletableFuture.allOf(futures);
    }

    public CompletableFuture<Void> broadcastGameState(GameStateDTO state) {
        logger.log(Level.FINE, "Broadcasting full game state to {0} clients in game {1}", new Object[]{clientHandlers.size(), gameId});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendGameState(state))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastPhaseTransition(GamePhaseDTO phase) {
        logger.log(Level.INFO, "Broadcasting phase transition to {0} in game {1}", new Object[]{phase.getPhaseName(), gameId});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendPhaseChange(phase))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastPlayerStatusUpdate(String affectedPlayerIdString, FlightStatus status) {
        logger.log(Level.INFO, "Broadcasting status update for player {0}: {1} in game {2}", new Object[]{affectedPlayerIdString, status, gameId});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendPlayerStatusUpdate(affectedPlayerIdString, status))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastShipUpdate(String affectedPlayerIdString, ShipDTO dto) {
        logger.log(Level.FINE, "Broadcasting ship update for player {0} in game {1}", new Object[]{affectedPlayerIdString, gameId});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendShipUpdate(affectedPlayerIdString, dto))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastComponentPoolUpdate(ComponentDeckDTO dto) {
        logger.log(Level.FINE, "Broadcasting component pool update in game {0}", gameId);
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendComponentPoolUpdate(dto))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastGameResults(Map<String, Integer> finalScores, String winner) {
        logger.log(Level.INFO, "Broadcasting game results for game {0}. Winner: {1}", new Object[]{gameId, winner});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .map(handler -> handler.sendGameResults(finalScores, winner))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    public CompletableFuture<Void> broadcastPlayerRemoval(String removedPlayerIdString) {
        logger.log(Level.INFO, "Broadcasting removal of player {0} in game {1}", new Object[]{removedPlayerIdString, gameId});
        CompletableFuture<?>[] futures = clientHandlers.values().stream()
                .filter(handler -> !handler.getPlayerId().equals(removedPlayerIdString))
                .map(handler -> handler.sendPlayerRemoval(removedPlayerIdString))
                .toArray(CompletableFuture<?>[]::new);
        return broadcast(futures);
    }

    // --- Specific Send Methods (Run on Network Executor) ---

    private CompletableFuture<Void> sendToPlayer(String playerIdString, java.util.function.Function<ServerClientHandler, CompletableFuture<Void>> sendAction) {
        PlayerId targetPlayerId = findPlayerIdFromString(playerIdString);
        if (targetPlayerId != null) {
            ServerClientHandler handler = clientHandlers.get(targetPlayerId);
            if (handler != null) {
                return sendAction.apply(handler)
                        .exceptionally(ex -> {
                            logger.log(Level.WARNING, "Failed to send message to player " + playerIdString, ex);
                            return null;
                        });
            } else {
                logger.log(Level.WARNING, "Cannot send message, no handler found in map for player ID {0}", playerIdString);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            logger.log(Level.WARNING, "Cannot send message, unknown player ID {0}", playerIdString);
            return CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<Void> sendFullGameState(String playerIdString) {
        logger.log(Level.FINE, "Sending full game state to player {0} in game {1}", new Object[]{playerIdString, gameId});
        GameStateDTO currentState = getFullGameStateDTO();
        return sendToPlayer(playerIdString, handler -> handler.sendGameState(currentState));
    }

    public CompletableFuture<Void> sendErrorToPlayer(String playerIdString, String message, boolean isFatal) {
        logger.log(Level.WARNING, "Sending error to player {0}: {1} (Fatal: {2})", new Object[]{playerIdString, message, isFatal});
        return sendToPlayer(playerIdString, handler -> handler.sendError(message, isFatal));
    }

    public CompletableFuture<Void> sendActionResultToPlayer(String playerIdString, ActionResultDTO result) {
        logger.log(Level.FINE, "Sending action result to player {0}: Success={1}", new Object[]{playerIdString, result.isSuccess()});
        return sendToPlayer(playerIdString, handler -> handler.sendActionResult(result));
    }

    // --- Game Flow Logic ---

    private void transitionToPhase(it.polimi.ingsw.model.enums.GamePhase newPhase) {
        logger.log(Level.INFO, "Transitioning game {0} to phase {1}", new Object[]{gameId, newPhase});
        // TODO: Create TransitionPhaseCommand
        // TransitionPhaseCommand command = new TransitionPhaseCommand(newPhase);
        // commandProcessor.process(command).thenRunAsync(() -> {
        //    GamePhaseDTO phaseDTO = new GamePhaseDTO(newPhase.name());
        //    // Add current player info if needed
        //    broadcastPhaseTransition(phaseDTO);
        // }, networkExecutor);

        // Placeholder broadcast
        GamePhaseDTO phaseDTO = new GamePhaseDTO(newPhase.name());
        broadcastPhaseTransition(phaseDTO);
    }

    public void startGame() {
        logger.log(Level.INFO, "Starting game {0}", gameId);
        // TODO: Process StartGameCommand if needed (e.g., to initialize decks)
        // GameModel already initializes decks/board in its constructor/initializeGame()
        // We just need to set the phase.
        if (model.getCurrentPhase() == it.polimi.ingsw.model.enums.GamePhase.SETUP) {
            transitionToPhase(it.polimi.ingsw.model.enums.GamePhase.BUILDING);
        } else {
            logger.log(Level.WARNING, "Attempted to start game {0} but not in SETUP phase.", gameId);
        }
    }

    // Called potentially by a command when a player signals readiness or timer ends
    public void checkAndEndBuildPhase() {
        logger.log(Level.FINE, "Checking if build phase can end for game {0}", gameId);
        if (model.getCurrentPhase() != it.polimi.ingsw.model.enums.GamePhase.BUILDING) {
            return; // Not in build phase
        }
        // TODO: Implement logic to check if all players are ready
        // boolean allReady = model.getPlayers().stream().allMatch(Player::isReady); // Requires Player.isReady()
        boolean allReady = true; // Placeholder
        if (allReady) {
            logger.log(Level.INFO, "Ending build phase for game {0}", gameId);
            transitionToPhase(it.polimi.ingsw.model.enums.GamePhase.FLIGHT);
        }
    }

    // Logic to run the flight phase (e.g., called after transitioning)
    private void runFlightPhase() {
        logger.log(Level.INFO, "Running flight phase for game {0}", gameId);
        // TODO: Implement flight phase loop
        // While (adventure cards remain and game not ended) {
        //    Draw card -> Create ResolveCardCommand -> process command
        //    Process command result (damage, rewards, player decisions)
        //    Broadcast updates
        // }
        // When finished:
        // transitionToPhase(it.polimi.ingsw.model.enums.GamePhase.END); // Or SCORING if separate phase
    }

    // Logic to run the scoring phase (e.g., called after transitioning)
    private void runScoringPhase() {
        logger.log(Level.INFO, "Running scoring phase for game {0}", gameId);
        // 1. Use ScoringEngine to calculate scores.
        // Assuming calculateFinalScores returns Map<PlayerId, Integer> or similar
        Map<String, Integer> scores = scoringEngine.calculateFinalScores(model); // Adapt based on ScoringEngine return type
        // 2. Determine the winner.
        String winnerIdString = scoringEngine.determineWinner(scores);
        // 3. Broadcast results.
        broadcastGameResults(scores, winnerIdString).thenRunAsync(() -> {
            logger.log(Level.INFO, "Game {0} finished. Winner: {1}", new Object[]{gameId, winnerIdString});
            // TODO: Trigger game cleanup in MultiGameCoordinator
            // multiGameCoordinator.removeGame(gameId); // Needs reference to MGC
        }, gameLogicExecutor);
    }


    // --- Utility and Conversion Methods ---

    /**
     * Gets the unique ID of this game instance.
     *
     * @return The game ID string.
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Finds a PlayerId object based on its string representation.
     * NOTE: This implementation is inefficient (linear scan). Consider a reverse map if performance critical.
     * @param playerIdString The string representation of the PlayerId (e.g., from PlayerId.toString()).
     * @return The matching PlayerId object, or null if not found.
     */
    private PlayerId findPlayerIdFromString(String playerIdString) {
        if (playerIdString == null) return null;
        for (PlayerId pid : clientHandlers.keySet()) {
            if (pid.toString().equals(playerIdString)) { // TODO: Verify PlayerId.toString() format
                return pid;
            }
        }
        logger.log(Level.FINEST, "Could not find PlayerId object for string: {0}", playerIdString);
        return null; // Not found
    }

    /**
     * Creates a DTO representing the full current game state.
     *
     * @return A GameStateDTO.
     */
    private GameStateDTO getFullGameStateDTO() {
        logger.log(Level.FINER, "Generating full game state DTO for game {0}", gameId);
        GameStateDTO dto = new GameStateDTO();

        // 1. Convert Game Phase
        if (model.getCurrentPhase() != null) {
            GamePhaseDTO phaseDTO = new GamePhaseDTO(model.getCurrentPhase().name());
            if (model.getCurrentPlayer() != null && model.getCurrentPlayer().getId() != null) {
                // phaseDTO.setCurrentPlayerId(model.getCurrentPlayer().getId().toString());
            }
            dto.setGamePhase(phaseDTO);
        }

        // 2. Convert Players list
        List<PlayerDTO> playerDTOs = model.getPlayers().stream()
                .map(this::convertPlayerToDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        dto.setPlayers(playerDTOs);

        // 3. Convert Component Pool
        ComponentDeckDTO poolDTO = convertComponentPoolToDTO(model.getComponentDeck());
        dto.setComponentPool(poolDTO);

        // 4. Convert Flight Board
        FlightBoardDTO boardDTO = convertFlightBoardToDTO(model.getFlightBoard());
        dto.setFlightBoard(boardDTO);

        // 5. Add other relevant top-level state
        if (model.getCurrentPlayer() != null && model.getCurrentPlayer().getId() != null) {
            dto.setCurrentTurnPlayerId(model.getCurrentPlayer().getId().toString());
        }

        logger.log(Level.FINEST, "Finished generating game state DTO for game {0}", gameId);
        return dto;
    }

    /**
     * Gets the ShipDTO for a specific player.
     *
     * @param playerIdString The player's ID string.
     * @return The ShipDTO, or null if player/ship not found.
     */
    private ShipDTO getShipDTOForPlayer(String playerIdString) {
        logger.log(Level.FINEST, "Generating ShipDTO for player {0}", playerIdString);
        PlayerId playerId = findPlayerIdFromString(playerIdString);
        if (playerId == null) {
            logger.log(Level.WARNING, "Cannot get ShipDTO: Player ID {0} not found.", playerIdString);
            return null;
        }

        Player player = model.getPlayerById(playerId);
        if (player == null) {
            logger.log(Level.WARNING, "Cannot get ShipDTO: Player {0} found in handlers but not in model.", playerIdString);
            return null;
        }

        Ship ship = player.getShip();
        if (ship == null) {
            logger.log(Level.WARNING, "Cannot get ShipDTO: Player {0} has no ship object.", playerIdString);
            return new ShipDTO();
        }

        ShipDTO dto = new ShipDTO();
        Map<PositionDTO, ComponentDTO> gridDTO = new HashMap<>();
        Component[][] board = ship.getBoard();
        if (board != null) {
            for (int r = 0; r < board.length; r++) {
                for (int c = 0; c < (board[r] != null ? board[r].length : 0); c++) {
                    Component component = board[r][c];
                    if (component != null) {
                        PositionDTO posDto = new PositionDTO(c, r);
                        ComponentDTO compDto = convertComponentToDTO(component);
                        if (compDto != null) {
                            gridDTO.put(posDto, compDto);
                        }
                    }
                }
            }
        }
        dto.setGrid(gridDTO);
        // dto.setValidStructure(ship.isValidStructure()); // TODO
        dto.setValidStructure(true); // Placeholder
        // ship.updateStats(); // If needed
        dto.setCrewCount(ship.getCrew());
        dto.setTotalEnginePower(ship.getEngines());
        dto.setBatteries(ship.getBatteries());
        return dto;
    }

    /**
     * Helper method to convert a model Component to its DTO representation.
     *
     * @param component The Component model object.
     * @return The corresponding ComponentDTO.
     */
    private ComponentDTO convertComponentToDTO(Component component) {
        if (component == null) return null;
        ComponentDTO dto = new ComponentDTO();
        if (component.getType() != null) {
            dto.setType(component.getType().name());
        } else {
            logger.log(Level.WARNING, "Component model object has null type!");
            dto.setType("UNKNOWN");
        }
        // TODO: Populate rotation
        // TODO: Populate type-specific properties (crew, power, cargo)
        return dto;
    }

    /**
     * Helper method to convert a model Player to its DTO representation.
     *
     * @param player The Player model object.
     * @return The corresponding PlayerDTO.
     */
    private PlayerDTO convertPlayerToDTO(Player player) {
        if (player == null) return null;
        PlayerId playerId = player.getId();
        if (playerId == null) {
            logger.log(Level.WARNING, "Player model object has null PlayerId!");
            return null;
        }
        String playerIdString = playerId.toString();
        logger.log(Level.FINEST, "Converting Player {0} to DTO", playerIdString);

        ShipDTO shipDTO = getShipDTOForPlayer(playerIdString);

        PlayerDTO dto = new PlayerDTO();
        dto.setPlayerId(playerIdString);
        dto.setPlayerName(playerId.getNickname());
        dto.setPlayerColor(player.getColor().name());

        // TODO: Get accurate connection status
        ServerClientHandler handler = clientHandlers.get(playerId);
        FlightStatus status = (handler != null) ? FlightStatus.RACING : FlightStatus.ABANDONED;
        dto.setStatus(status);

        // TODO: Populate credits, rank, ready status from Player model
        dto.setCredits(0); // Placeholder
        dto.setRank(0); // Placeholder
        dto.setShip(shipDTO);
        return dto;
    }

    /**
     * Helper method to convert the ComponentDeck model to its DTO representation.
     *
     * @param deck The ComponentDeck model object.
     * @return The corresponding ComponentPoolDTO.
     */
    private ComponentDeckDTO convertComponentPoolToDTO(it.polimi.ingsw.model.domain.general.ComponentDeck deck) {
        if (deck == null) {
            logger.log(Level.WARNING, "ComponentDeck model is null, cannot convert to DTO.");
            return new ComponentDeckDTO(); // Return empty DTO
        }
        ComponentDeckDTO dto = new ComponentDeckDTO();
        // TODO: Implement conversion based on ComponentDeck methods
        // dto.setAvailableComponents(deck.getCountsByType());
        // dto.setRemainingCount(deck.getRemainingCount());
        dto.setRemainingCount(0); // Placeholder
        dto.setAvailableComponents(new HashMap<>()); // Placeholder
        return dto;
    }

    /**
     * Helper method to convert the FlightBoard model to its DTO representation.
     *
     * @param board The FlightBoard model object.
     * @return The corresponding FlightBoardDTO.
     */
    private FlightBoardDTO convertFlightBoardToDTO(it.polimi.ingsw.model.domain.flight.FlightBoard board) {
        if (board == null) {
            logger.log(Level.WARNING, "FlightBoard model is null, cannot convert to DTO.");
            return new FlightBoardDTO(); // Return empty DTO
        }
        FlightBoardDTO dto = new FlightBoardDTO();
        // TODO: Implement conversion based on FlightBoard methods
        // dto.setTotalLength(board.getLength());
        // Map<String, Integer> positions = board.getPlayerPositions().entrySet().stream()
        //     .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        // dto.setPlayerPositions(positions);
        // dto.setRevealedAdventureCardTypes(board.getRevealedCards().stream().map(c -> c.getType().name()).collect(Collectors.toList()));
        dto.setTotalLength(0); // Placeholder
        dto.setPlayerPositions(new HashMap<>()); // Placeholder
        dto.setRevealedAdventureCardTypes(new ArrayList<>()); // Placeholder
        return dto;
    }

    /**
     * Shuts down resources associated with this game instance.
     * Called by MultiGameCoordinator when the game is removed.
     */
    public void shutdown() {
        logger.log(Level.INFO, "Shutting down GameInstanceController for game {0}", gameId);
        clientHandlers.values().forEach(ServerClientHandler::closeConnection);
        clientHandlers.clear();
        commandProcessor.shutdown();
    }
}
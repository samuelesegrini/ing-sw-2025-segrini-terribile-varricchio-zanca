package it.polimi.ingsw.server.network;

import it.polimi.ingsw.model.enums.flight.FlightStatus;
import it.polimi.ingsw.network.dto.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the server-side handling of a connection to a single client.
 * This interface abstracts the underlying network protocol (Socket, RMI).
 * Implementations are responsible for sending/receiving data asynchronously.
 */
public interface ServerClientHandler {

    /**
     * Gets the unique identifier for this connection.
     * @return The connection ID.
     */
    String getConnectionId();

    /**
     * Gets the player ID associated with this connection (after successful login/join).
     * @return The player ID, or null if not yet associated.
     */
    String getPlayerId();

    /**
     * Sets the player ID associated with this handler.
     * Typically called after successful authentication or game joining.
     * @param playerId The player ID.
     */
    void setPlayerId(String playerId);

    /**
     * Sends the full game state to the client asynchronously.
     * Used typically when a client joins or reconnects.
     *
     * @param state The GameStateDTO containing the full game state.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendGameState(GameStateDTO state);

    /**
     * Sends a notification about a game phase change to the client asynchronously.
     *
     * @param phase The new GamePhaseDTO.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendPhaseChange(GamePhaseDTO phase);

    /**
     * Sends an update about a player's connection status to the client asynchronously.
     *
     * @param affectedPlayerId The ID of the player whose status changed.
     * @param status The new FlightStatus.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendPlayerStatusUpdate(String affectedPlayerId, FlightStatus status);

    /**
     * Sends an update about a specific player's ship to the client asynchronously.
     *
     * @param affectedPlayerId The ID of the player whose ship was updated.
     * @param dto The updated ShipDTO.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendShipUpdate(String affectedPlayerId, ShipDTO dto);

    /**
     * Sends an update about the shared component pool to the client asynchronously.
     *
     * @param dto The updated ComponentPoolDTO.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendComponentPoolUpdate(ComponentDeckDTO dto);

    /**
     * Sends the final game results to the client asynchronously.
     *
     * @param scores Map of player IDs to their final scores.
     * @param winner Player ID of the winner.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendGameResults(Map<String, Integer> scores, String winner);

    /**
     * Sends a notification that a player has been removed from the game.
     *
     * @param removedPlayerId The ID of the player who was removed.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendPlayerRemoval(String removedPlayerId);

    /**
     * Sends an error message to the client asynchronously.
     *
     * @param message The error message string.
     * @param isFatal Indicates if the error requires client disconnection.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendError(String message, boolean isFatal);

    /**
     * Sends a confirmation or result of a player action.
     *
     * @param result The ActionResultDTO containing details of the action outcome.
     * @return A CompletableFuture that completes when the send operation is finished (or fails).
     */
    CompletableFuture<Void> sendActionResult(ActionResultDTO result);

    /**
     * Closes the connection to the client.
     * Implementations should handle resource cleanup.
     */
    void closeConnection();
}
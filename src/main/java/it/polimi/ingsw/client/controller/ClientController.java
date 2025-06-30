package it.polimi.ingsw.client.controller;

import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.ui.UI;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.newUI;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.common.message.*;
import it.polimi.ingsw.common.message.PongMessage;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.request.*;
import it.polimi.ingsw.common.message.response.ClientContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.ListGamesResponse;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.logging.*;

/**
 * Client controller that handles user actions and server messages.
 * Uses Simple Direct Model Architecture with UIContext dependency injection.
 */
public class ClientController {
    private static final Logger LOGGER = Logger.getLogger(ClientController.class.getName());

    private final NetworkClient networkClient;
    private final MessageHandler messageHandler;
    
    private UIContext uiContext;
    private final ClientState clientState;

    private newUI ui;
    
    // ENHANCED: Conflict resolution system
    private final Map<String, AtomicInteger> requestRetryCounters = new ConcurrentHashMap<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 200;

    public ClientController(NetworkClient networkClient, UIContext uiContext) {
        this.networkClient = networkClient;
        this.messageHandler = new MessageHandler();
        this.uiContext = uiContext;
        this.clientState = uiContext.getClientState();
    }
    
    public ClientController(NetworkClient networkClient, ClientState clientState) {
        this.networkClient = networkClient;
        this.messageHandler = new MessageHandler();
        this.uiContext = null; // Will be set by UIManager when context is created
        this.clientState = clientState;
    }
    
    public UIContext getUIContext() {
        return uiContext;
    }
    
    public void setUIContext(UIContext uiContext) {
        this.uiContext = uiContext;
    }
    
    public ClientState getClientState() {
        return clientState;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public newUI getUI() {
        return ui;
    }

    public void setUI(newUI ui) {
        this.ui = ui;
    }

    public String getPlayerId() {
        return clientState.getPlayerId();
    }
    
    public PlayerId getPlayerIdObject() {
        return clientState.getPlayerIdObject();
    }
    
    /**
     * Safely attempts to navigate to a view state using ViewNavigator.
     * ViewNavigator is REQUIRED for consistent state management.
     * 
     * @param viewState The target view state
     * @param context The navigation context/reason
     * @return true if navigation was successful, false otherwise
     */
    private boolean attemptNavigation(ClientState.ViewState viewState, String context) {
        if (uiContext == null || uiContext.getViewNavigator() == null) {
            LOGGER.severe("ViewNavigator not available - cannot navigate to " + viewState + 
                         ". This indicates a serious initialization problem.");
            return false;
        }
        
        boolean success = uiContext.getViewNavigator().navigateTo(viewState, context);
        if (!success) {
            String reason = uiContext.getViewNavigator().getNavigationFailureReason(viewState);
            LOGGER.warning("Failed to navigate to " + viewState + " - Reason: " + reason + " (Context: " + context + ")");
            
            // Show error to user for critical navigation failures
            if (uiContext.getNotificationService() != null) {
                uiContext.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Navigation Error", 
                        "Cannot navigate to " + viewState + ": " + reason,
                        it.polimi.ingsw.client.ui.NotificationType.WARNING
                    )
                );
            }
        }
        return success;
    }

    // Connection actions
    public CompletableFuture<Boolean> connect(String host, int port, boolean useSocket) {
        if (host == null || host.trim().isEmpty()) {
            LOGGER.warning("Invalid host provided for connection");
            return CompletableFuture.completedFuture(false);
        }
        
        if (port <= 0 || port > 65535) {
            LOGGER.warning("Invalid port provided for connection: " + port);
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Connecting to " + host + ":" + port + " using " +
                (useSocket ? "Socket" : "RMI"));

        return networkClient.connect(host, port, useSocket)
                .thenApply(connected -> {
                    clientState.setConnectionStatus(connected ? 
                        ClientState.ConnectionStatus.CONNECTED : 
                        ClientState.ConnectionStatus.FAILED);
                    if (connected) {
                        LOGGER.info("Successfully connected to server");
                    } else {
                        LOGGER.warning("Failed to connect to server");
                    }
                    return connected;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Connection failed with exception", ex);
                    clientState.setConnectionStatus(ClientState.ConnectionStatus.FAILED);
                    return false;
                });
    }

    public void disconnect() {
        networkClient.disconnect();
        clientState.setConnectionStatus(ClientState.ConnectionStatus.DISCONNECTED);
        clientState.setPlayerInfo(null, null);
        
        // Navigation handled by UI implementation
    }

    // Authentication actions
    public CompletableFuture<Boolean> login(String nickname) {
        if (!clientState.isConnected()) {
            LOGGER.warning("Cannot login: not connected to server");
            return CompletableFuture.completedFuture(false);
        }
        
        if (nickname == null || nickname.trim().isEmpty()) {
            LOGGER.warning("Cannot login: invalid nickname provided");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Logging in with nickname: " + nickname);


        LoginRequest request = new LoginRequest(nickname.trim());
        return networkClient.sendRequest(request)
                .thenApply(response -> {
                    if (response instanceof LoginResponse loginResp) {
                        if (loginResp.isSuccess()) {
                            clientState.setPlayerInfo(loginResp.getPlayerId(), loginResp.getNickname());
                            
                            // Navigation handled by UI response handler

                            LOGGER.info("Login successful for player: " + loginResp.getNickname());
                            // Request game list and handle the response to update the client state
                            requestGameList().thenAccept(gameListResponse -> {
                                if (gameListResponse instanceof ListGamesResponse glr) {
                                    clientState.setAvailableGames(glr.getGames());
                                }
                            });
                            return true;
                        } else {
                            LOGGER.warning("Login failed: " + loginResp.getErrorMessage());
                        }
                    } else {
                        LOGGER.warning("Login failed: unexpected response type");
                    }
                    return false;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Login failed with exception", ex);
                    return false;
                });
    }


    // Game actions
    public CompletableFuture<Boolean> createGame(String gameName, int maxPlayers, String gameLevel) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot create game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }
        
        if (gameName == null || gameName.trim().isEmpty()) {
            LOGGER.warning("Cannot create game: invalid game name");
            return CompletableFuture.completedFuture(false);
        }
        
        if (maxPlayers < 2 || maxPlayers > 4) {
            LOGGER.warning("Cannot create game: invalid player count " + maxPlayers);
            return CompletableFuture.completedFuture(false);
        }

        try {
            CreateGameRequest request = new CreateGameRequest(
                    maxPlayers,
                    GameLevel.valueOf(gameLevel),
                    gameName.trim()
            );

            return networkClient.sendRequest(request)
                    .thenApply(response -> {
                        if (response.isSuccess()) {
                            LOGGER.info("Game created successfully: " + gameName);
                        } else {
                            LOGGER.warning("Failed to create game: " + response.getErrorMessage());
                        }
                        return response.isSuccess();
                    })
                    .exceptionally(ex -> {
                        LOGGER.log(Level.WARNING, "Create game failed with exception", ex);
                        return false;
                    });
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Cannot create game: invalid game level " + gameLevel);
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> joinGame(String gameId) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot join game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }
        
        if (gameId == null || gameId.trim().isEmpty()) {
            LOGGER.warning("Cannot join game: invalid game ID");
            return CompletableFuture.completedFuture(false);
        }

        JoinGameRequest request = new JoinGameRequest(gameId.trim());
        return networkClient.sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Successfully joined game: " + gameId);
                        
                        // Navigation handled by UI response handler
                    } else {
                        LOGGER.warning("Failed to join game: " + response.getErrorMessage());
                        
                        // If join failed due to "Game not found", refresh the games list
                        if (response.getErrorMessage() != null && response.getErrorMessage().contains("Game not found")) {
                            LOGGER.info("🔄 AUTO-REFRESH - Join failed due to game not found, refreshing games list");
                            refreshGameList().thenAccept(refreshSuccess -> {
                                if (refreshSuccess) {
                                    LOGGER.info("✅ Games list refreshed after join failure");
                                } else {
                                    LOGGER.warning("❌ Failed to refresh games list after join failure");
                                }
                            });
                        }
                    }
                    return response.isSuccess();
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Join game failed with exception", ex);
                    return false;
                });
    }

    public CompletableFuture<Response> requestGameList() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot request game list: not authenticated");
            Response err = new ErrorResponse(null, "Not authenticated", ErrorResponse.AUTHENTICATION_ERROR);
            return CompletableFuture.completedFuture(err);
        }

        LOGGER.fine("Requesting game list from server");
        ListGamesRequest request = new ListGamesRequest();
        return networkClient.sendRequest(request)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Failed to request game list", ex);
                    return new ErrorResponse(request.getCorrelationId(), "Failed to send request: " + ex.getMessage(), ErrorResponse.INTERNAL_ERROR);
                });
    }

    public CompletableFuture<Boolean> refreshGameList() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot refresh game list: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Refreshing game list");
        return requestGameList()
                .thenApply(response -> {
                    if (response.isSuccess() && response instanceof ListGamesResponse listGamesResponse) {
                        clientState.setAvailableGames(listGamesResponse.getGames());
                        LOGGER.info("Game list refreshed successfully - " + listGamesResponse.getGames().size() + " games found");
                        return true;
                    } else {
                        LOGGER.warning("Failed to refresh game list: " + response.getErrorMessage());
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Refresh game list failed with exception", ex);
                    return false;
                });
    }

    // Generic request sending method
    public CompletableFuture<Response> sendRequest(Request request) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot send request: not authenticated");
            Response err = new ErrorResponse(null, "Not authenticated", ErrorResponse.AUTHENTICATION_ERROR);
            return CompletableFuture.completedFuture(err);
        }

        return networkClient.sendRequest(request)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Failed to send request: " + request.getClass().getSimpleName(), ex);
                    return new ErrorResponse(request.getCorrelationId(), "Failed to send request: " + ex.getMessage(), ErrorResponse.INTERNAL_ERROR);
                });
    }

    // Specific lobby action methods
    public CompletableFuture<Boolean> setPlayerReady(boolean ready) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot set player ready: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        SetPlayerReadyRequest request = new SetPlayerReadyRequest(ready);
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Player ready status set to: " + ready);
                        return true;
                    } else {
                        LOGGER.warning("Failed to set player ready: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> startGame() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot start game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        String gameId = clientState.getCurrentGameId();
        if (gameId == null) {
            LOGGER.warning("Cannot start game: not in a game");
            return CompletableFuture.completedFuture(false);
        }

        StartGameRequest request = new StartGameRequest(gameId);
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Game start requested successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to start game: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> leaveGame() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot leave game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        String gameId = clientState.getCurrentGameId();
        if (gameId == null) {
            LOGGER.warning("Cannot leave game: not in a game");
            return CompletableFuture.completedFuture(false);
        }

        LeaveGameRequest request = new LeaveGameRequest(gameId);
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Left game successfully");
                        
                        // Navigation handled by UI response handler
                        return true;
                    } else {
                        LOGGER.warning("Failed to leave game: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    // Ship building actions
    public CompletableFuture<Boolean> placeTile(String componentType, int row, int col, int rotation) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot place tile: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        if (componentType == null || componentType.trim().isEmpty()) {
            LOGGER.warning("Cannot place tile: invalid component type");
            return CompletableFuture.completedFuture(false);
        }

        if (row < 0 || row >= 5 || col < 0 || col >= 7) {
            LOGGER.warning("Cannot place tile: invalid position (" + row + ", " + col + ")");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Placing tile: " + componentType + " at (" + row + ", " + col + ") with rotation " + rotation);

        PlaceTileRequest request = new PlaceTileRequest(componentType.trim(), row, col, rotation);
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Tile placed successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to place tile: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> takeTile() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot take tile: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("[TAKETILE DEBUG] Taking random tile - Player: " + clientState.getPlayerId());
        return takeTileWithRetry(0);
    }
    
    /**
     * ENHANCED: Take tile with automatic retry for conflict resolution
     */
    private CompletableFuture<Boolean> takeTileWithRetry(int attempt) {
        String requestKey = "take_tile_" + clientState.getPlayerId();
        
        LOGGER.info("[TAKETILE DEBUG] Sending TakeTileRequest to server - attempt " + (attempt + 1));
        TakeTileRequest request = new TakeTileRequest();
        return sendRequest(request)
                .thenCompose(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("[TAKETILE DEBUG] Tile taken successfully - response: " + response.getClass().getSimpleName());
                        requestRetryCounters.remove(requestKey);
                        return CompletableFuture.completedFuture(true);
                    } else if (isConflictError(response) && attempt < MAX_RETRY_ATTEMPTS) {
                        // Component was taken by another player - retry with delay
                        LOGGER.info("Component conflict detected, retrying... (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
                        
                        if (uiContext != null && uiContext.getNotificationService() != null) {
                            uiContext.getNotificationService().showWarning("Component Conflict", 
                                "Another player took that component. Retrying...");
                        }
                        
                        return CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY_MS + (attempt * 100)); // Increasing delay
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).thenCompose(v -> takeTileWithRetry(attempt + 1));
                    } else {
                        LOGGER.warning("Failed to take tile: " + response.getErrorMessage());
                        requestRetryCounters.remove(requestKey);
                        
                        if (uiContext != null && uiContext.getNotificationService() != null) {
                            uiContext.getNotificationService().showError("Take Component Failed", 
                                response.getErrorMessage());
                        }
                        
                        return CompletableFuture.completedFuture(false);
                    }
                });
    }
    
    /**
     * ENHANCED: Determines if a response represents a conflict that should be retried
     */
    private boolean isConflictError(Response response) {
        if (!response.isSuccess() && response instanceof ErrorResponse errorResponse) {
            String errorMessage = errorResponse.getErrorMessage().toLowerCase();
            return errorMessage.contains("not available") || 
                   errorMessage.contains("already taken") ||
                   errorMessage.contains("conflict") ||
                   errorMessage.contains("concurrent access");
        }
        return false;
    }

    public CompletableFuture<Boolean> requestFaceUpTile(String tileId) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot request face up tile: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        if (tileId == null || tileId.trim().isEmpty()) {
            LOGGER.warning("Cannot request face up tile: invalid tile ID");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Requesting face up tile: " + tileId);
        return requestFaceUpTileWithRetry(tileId.trim(), 0);
    }
    
    /**
     * ENHANCED: Request face-up tile with automatic retry for conflict resolution
     */
    private CompletableFuture<Boolean> requestFaceUpTileWithRetry(String tileId, int attempt) {
        String requestKey = "face_up_" + tileId + "_" + clientState.getPlayerId();
        
        RequestFaceUpTileRequest request = new RequestFaceUpTileRequest(tileId);
        return sendRequest(request)
                .thenCompose(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Face up tile requested successfully");
                        requestRetryCounters.remove(requestKey);
                        return CompletableFuture.completedFuture(true);
                    } else if (isConflictError(response) && attempt < MAX_RETRY_ATTEMPTS) {
                        // Face-up component was taken by another player - retry with delay
                        LOGGER.info("Face-up component conflict detected, retrying... (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
                        
                        if (uiContext != null && uiContext.getNotificationService() != null) {
                            uiContext.getNotificationService().showWarning("Component Conflict", 
                                "Another player took that face-up component. Retrying...");
                        }
                        
                        return CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY_MS + (attempt * 100)); // Increasing delay
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).thenCompose(v -> requestFaceUpTileWithRetry(tileId, attempt + 1));
                    } else {
                        LOGGER.warning("Failed to request face up tile: " + response.getErrorMessage());
                        requestRetryCounters.remove(requestKey);
                        
                        if (uiContext != null && uiContext.getNotificationService() != null) {
                            uiContext.getNotificationService().showError("Face-up Component Failed", 
                                response.getErrorMessage());
                        }
                        
                        return CompletableFuture.completedFuture(false);
                    }
                });
    }

    public CompletableFuture<Boolean> returnTile(String tileId) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot return tile: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        if (tileId == null || tileId.trim().isEmpty()) {
            LOGGER.warning("Cannot return tile: invalid tile ID");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Returning tile: " + tileId);

        ReturnTileRequest request = new ReturnTileRequest(tileId.trim());
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Tile returned successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to return tile: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> reserveComponent(String tileId) {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot reserve component: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        if (tileId == null || tileId.trim().isEmpty()) {
            LOGGER.warning("Cannot reserve component: invalid tile ID");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Reserving component: " + tileId);

        ReserveTileRequest request = new ReserveTileRequest(tileId.trim());
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Component reserved successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to reserve component: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> flipBuildingTimer() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot flip building timer: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Flipping building timer");

        FlipBuildingTimerRequest request = new FlipBuildingTimerRequest();
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Building timer flipped successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to flip building timer: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    public CompletableFuture<Boolean> validateShip() {
        if (clientState.getPlayerId() == null) {
            LOGGER.warning("Cannot validate ship: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Validating ship");

        ValidateShipRequest request = new ValidateShipRequest();
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Ship validated successfully");
                        return true;
                    } else {
                        LOGGER.warning("Failed to validate ship: " + response.getErrorMessage());
                        return false;
                    }
                });
    }

    // Message handling
    public void handleMessage(Message message) {
        if (message == null) {
            LOGGER.warning("Received null message");
            return;
        }

        LOGGER.fine("Submitting message to handler: " + message.getClass().getSimpleName());
        // No need for a try-catch block here if the inner handler manages its own exceptions
        messageHandler.handle(message);
    }

    /**
     * Inner class to handle incoming messages.
     */
    private class MessageHandler {

        public void handle(Message message) {
            try {
                LOGGER.fine("Handling message: " + message.getClass().getSimpleName());

                if (message instanceof Response response) {
                    handleResponse(response);
                } else if (message instanceof Event event) {
                    handleEvent(event);
                } else if (message instanceof PingMessage pingMessage) {
                    // Unified ping handling for both Socket and RMI
                    LOGGER.finer("Ping message received, sending pong");
                    networkClient.sendMessage(new PongMessage());
                } else {
                    LOGGER.warning("Unhandled message type: " + message.getClass().getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling message: " + message.getClass().getSimpleName(), e);
            }
        }

        private void handleResponse(Response response) {
            LOGGER.info("📨 RESPONSE RECEIVED - Processing " + response.getClass().getSimpleName());
            // Create client context and let response handle itself
            ClientContext context = new ClientContextImpl();
            response.handleOnClient(context);
            LOGGER.info("✅ RESPONSE PROCESSED - " + response.getClass().getSimpleName() + " handled successfully");
        }

        private void handleEvent(Event event) {
            LOGGER.info("Received event: " + event.getEventType());

            try {
                // Create ClientEventContext and let each event handle itself
                ClientEventContext clientEventContext = new ClientEventContextImpl();
                event.handleOnClient(clientEventContext);
                
                LOGGER.fine("Successfully processed event: " + event.getEventType());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing event: " + event.getEventType(), e);
            }
        }
        
        /**
         * Implementation of ClientEventContext for events
         */
        private class ClientEventContextImpl implements ClientEventContext {
            @Override
            public void runOnUIThread(Runnable task) {
                if (uiContext != null && uiContext.getThreadService() != null) {
                    uiContext.getThreadService().runOnUIThread(task);
                } else {
                    // Fallback for early stages or if UI context is not fully initialized
                    task.run();
                }
            }
            
            @Override
            public ClientController getController() {
                return ClientController.this;
            }
            
            @Override
            public PlayerId getLocalPlayerId() {
                return clientState.getPlayerIdObject();
            }
            
            @Override
            public boolean isLocalPlayer(PlayerId playerId) {
                return playerId != null && clientState.getPlayerIdObject() != null && 
                       playerId.equals(clientState.getPlayerIdObject());
            }
            
            @Override
            public it.polimi.ingsw.client.core.ClientState getClientState() {
                return clientState;
            }
            
            @Override
            public NotificationService getNotificationService() {
                if (uiContext != null) {
                    return uiContext.getNotificationService();
                }
                return null;
            }
        }

    }

    /**
     * Implementation of ClientContext.
     * Simple Direct Model Architecture: Uses only ClientState.
     */
    private class ClientContextImpl implements ClientContext {

        @Override
        public PlayerId getPlayerId() {
            return clientState.getPlayerIdObject();
        }

        @Override
        public String getGameId() {
            return clientState.getCurrentGameId();
        }

        @Override
        public void showNotification(String title, String message, NotificationType type) {
            if (uiContext != null && uiContext.getNotificationService() != null) {
                it.polimi.ingsw.client.ui.Notification notification = 
                    new it.polimi.ingsw.client.ui.Notification(title, message, type);
                uiContext.getNotificationService().showNotification(notification);
            } else {
                LOGGER.warning("Notification service not available for: " + title + ": " + message);
            }
        }

        @Override
        public void showError(String title, String message) {
            if (uiContext != null && uiContext.getNotificationService() != null) {
                it.polimi.ingsw.client.ui.Notification notification = 
                    new it.polimi.ingsw.client.ui.Notification(title, message, NotificationType.ERROR);
                uiContext.getNotificationService().showNotification(notification);
            } else {
                LOGGER.severe("Error notification service not available for: " + title + ": " + message);
            }
        }
        
        @Override
        public ClientState getClientState() {
            return clientState;
        }
        
        @Override
        public ClientController getController() {
            return ClientController.this;
        }
    }
    
    /**
     * Gets the current view being displayed.
     * Returns null if no view is available.
     */
    public Object getCurrentView() {
        // This would typically return the current UI view
        // For now, return null as a placeholder
        return null;
    }
    
}
package it.polimi.ingsw.client.controller;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.message.*;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.event.GameCreatedEvent;
import it.polimi.ingsw.common.message.event.GameEndedEvent;
import it.polimi.ingsw.common.message.request.*;
import it.polimi.ingsw.common.message.response.ClientContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.ListGamesResponse;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

/**
 * Client controller that handles user actions and server messages.
 */
public class ClientController {
    private static final Logger LOGGER = Logger.getLogger(ClientController.class.getName());

    private final ClientModel model;
    private final NetworkClient networkClient;
    private final MessageHandler messageHandler;

    public ClientController(ClientModel model, NetworkClient networkClient) {
        this.model = model;
        this.networkClient = networkClient;
        this.messageHandler = new MessageHandler();
    }

    public ClientModel getModel() {
        return model;
    }

    public String getPlayerId() {
        return model.getPlayerId();
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

        model.setServerHost(host);
        model.setServerPort(port);

        return networkClient.connect(host, port, useSocket)
                .thenApply(connected -> {
                    model.setConnected(connected);
                    if (connected) {
                        model.setCurrentView(ClientModel.ViewState.LOGIN);
                        LOGGER.info("Successfully connected to server");
                    } else {
                        LOGGER.warning("Failed to connect to server");
                    }
                    return connected;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Connection failed with exception", ex);
                    model.setConnected(false);
                    return false;
                });
    }

    public void disconnect() {
        networkClient.disconnect();
        model.setConnected(false);
        model.setAuthenticated(false);
        model.setCurrentView(ClientModel.ViewState.CONNECTION);
    }

    // Authentication actions
    public CompletableFuture<Boolean> login(String nickname) {
        if (!model.isConnected()) {
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
                            model.setPlayerId(loginResp.getPlayerId());
                            model.setNickname(loginResp.getNickname());
                            model.setAuthenticated(true);
                            model.setCurrentView(ClientModel.ViewState.LOBBY);

                            LOGGER.info("Login successful for player: " + loginResp.getNickname());
                            // Request game list and handle the response to update the model
                            requestGameList().thenAccept(gameListResponse -> {
                                if (gameListResponse instanceof ListGamesResponse glr) {
                                    model.setAvailableGames(glr.getGames());
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
        if (!model.isAuthenticated()) {
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
        if (!model.isAuthenticated()) {
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
                    } else {
                        LOGGER.warning("Failed to join game: " + response.getErrorMessage());
                    }
                    return response.isSuccess();
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Join game failed with exception", ex);
                    return false;
                });
    }

    public CompletableFuture<Response> requestGameList() {
        if (!model.isAuthenticated()) {
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

    // Generic request sending method
    public CompletableFuture<Response> sendRequest(Request request) {
        if (!model.isAuthenticated()) {
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
        if (!model.isAuthenticated()) {
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
        if (!model.isAuthenticated()) {
            LOGGER.warning("Cannot start game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        String gameId = model.getCurrentGameId();
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
        if (!model.isAuthenticated()) {
            LOGGER.warning("Cannot leave game: not authenticated");
            return CompletableFuture.completedFuture(false);
        }

        String gameId = model.getCurrentGameId();
        if (gameId == null) {
            LOGGER.warning("Cannot leave game: not in a game");
            return CompletableFuture.completedFuture(false);
        }

        LeaveGameRequest request = new LeaveGameRequest(gameId);
        return sendRequest(request)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Left game successfully");
                        model.setCurrentView(ClientModel.ViewState.LOBBY);
                        return true;
                    } else {
                        LOGGER.warning("Failed to leave game: " + response.getErrorMessage());
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
                } else if (message instanceof PingMessage) {
                    handlePing((PingMessage) message);
                } else {
                    LOGGER.warning("Unhandled message type: " + message.getClass().getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling message: " + message.getClass().getSimpleName(), e);
            }
        }

        private void handleResponse(Response response) {
            // Create client context
            ClientContext context = new ClientContextImpl(model);
            response.handleOnClient(context);
        }

        private void handleEvent(Event event) {
            LOGGER.info("Received event: " + event.getEventType());

            try {
                // Update model based on event type
                switch (event.getEventType()) {
                    case GAME_CREATED -> {
                        if (event instanceof GameCreatedEvent gce) {
                            // Create proper game info with creator as first player
                            it.polimi.ingsw.common.PlayerInfo creatorInfo = new it.polimi.ingsw.common.PlayerInfo(
                                gce.getCreatorId(), gce.getCreatorNickname(), true);
                            java.util.List<it.polimi.ingsw.common.PlayerInfo> players = new ArrayList<>();
                            players.add(creatorInfo);
                            
                            GameInfo gameInfo = new GameInfo(
                                    gce.getGameId(),
                                    gce.getGameName(),
                                    gce.getMaxPlayers(),
                                    1,
                                    gce.getGameLevel(),
                                    players
                            );
                            
                            // If this is the creator, transition to game lobby
                            if (gce.getCreatorId().equals(model.getPlayerId())) {
                                LOGGER.info("Creator event received - transitioning to game lobby");
                                model.setCurrentGame(gameInfo);
                                model.setPlayersInLobby(players); // Update lobby player list
                                model.setCurrentView(ClientModel.ViewState.GAME_LOBBY);
                            } else {
                                // For other players, just add to available games
                                model.addAvailableGame(gameInfo);
                            }
                            LOGGER.fine("Processed game created event: " + gce.getGameName());
                        }
                    }
                    case PLAYER_JOINED_GAME -> {
                        if (event instanceof it.polimi.ingsw.common.message.event.PlayerJoinedGameEvent pjge) {
                            LOGGER.info("Player joined game event received: " + pjge.getPlayerNickname());
                            
                            // If we're in the same game, update our lobby player list
                            if (model.getCurrentGameInfo() != null && 
                                model.getCurrentGameInfo().getGameId().equals(pjge.getGameId())) {
                                
                                // Add new player to existing player list
                                java.util.List<it.polimi.ingsw.common.PlayerInfo> currentPlayers = 
                                    new ArrayList<>(model.getPlayersInLobby());
                                
                                // Check if player is already in the list (avoid duplicates)
                                boolean playerExists = currentPlayers.stream()
                                    .anyMatch(p -> p.getPlayerId().equals(pjge.getPlayerId()));
                                
                                if (!playerExists) {
                                    it.polimi.ingsw.common.PlayerInfo newPlayer = 
                                        new it.polimi.ingsw.common.PlayerInfo(pjge.getPlayerId(), pjge.getPlayerNickname(), false);
                                    currentPlayers.add(newPlayer);
                                    
                                    // Update model with new player list
                                    model.setPlayersInLobby(currentPlayers);
                                    
                                    // Update current game info with new player count
                                    it.polimi.ingsw.common.GameInfo updatedGameInfo = 
                                        new it.polimi.ingsw.common.GameInfo(
                                            model.getCurrentGameInfo().getGameId(),
                                            model.getCurrentGameInfo().getGameName(),
                                            model.getCurrentGameInfo().getMaxPlayers(),
                                            pjge.getCurrentPlayerCount(),
                                            model.getCurrentGameInfo().getGameLevel(),
                                            currentPlayers
                                        );
                                    model.setCurrentGame(updatedGameInfo);
                                    
                                    LOGGER.info("Updated lobby with new player: " + pjge.getPlayerNickname());
                                }
                            }
                        }
                    }
                    case PLAYER_READY_CHANGED -> {
                        if (event instanceof it.polimi.ingsw.common.message.event.PlayerReadyChangedEvent prce) {
                            LOGGER.info("Player ready status changed: " + prce.getPlayerNickname() + " = " + prce.isReady());
                            
                            // If we're in the same game, update the ready status
                            if (model.getCurrentGameInfo() != null && 
                                model.getCurrentGameInfo().getGameId().equals(prce.getGameId())) {
                                
                                model.setPlayerReadyStatus(prce.getPlayerId(), prce.isReady());
                                LOGGER.info("Updated ready status for " + prce.getPlayerNickname());
                            }
                        }
                    }
                    case GAME_ENDED -> {
                        if (event instanceof GameEndedEvent gee) {
                            model.removeAvailableGame(gee.getGameId());
                            LOGGER.fine("Removed ended game from available games: " + gee.getGameId());
                        }
                    }
                    default -> {
                        LOGGER.fine("Unhandled event type: " + event.getEventType());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing event: " + event.getEventType(), e);
            }
        }

        private void handlePing(PingMessage pingMessage) {
            // No need to log, just respond silently.
            networkClient.sendMessage(new PongMessage());
        }
    }

    /**
     * Implementation of ClientContext.
     */
    private class ClientContextImpl implements ClientContext {

        private final ClientModel model;

        public ClientContextImpl(ClientModel model) {
            this.model = model;
        }

        @Override
        public String getPlayerId() {
            return model.getPlayerId();
        }

        @Override
        public String getGameId() {
            return model.getCurrentGameId();
        }

        @Override
        public ClientModel getModel() {
            return model;
        }

        @Override
        public void showNotification(String title, String message, NotificationType type) {
            LOGGER.fine("UI Notification: " + title + " - " + message);
        }

        @Override
        public void showError(String title, String message) {
            LOGGER.severe("UI Error: " + title + " - " + message);
        }
    }
}
package it.polimi.ingsw.server.controller.notification;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
import it.polimi.ingsw.common.message.building.*; // Import client-facing building events
import it.polimi.ingsw.common.message.setup.GameSessionStateChangedEvent;
import it.polimi.ingsw.common.message.setup.PlayerJoinedGameSessionNotification;
import it.polimi.ingsw.common.message.setup.PlayerLeftGameSessionNotification;
import it.polimi.ingsw.common.message.setup.ServerGameListUpdateNotification;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.event.*; // Import your internal server events
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;


/**
 * Listens to internal server domain events and translates them into
 * network messages to be sent to clients.
 */
public class NotificationController {
    private static final Logger LOGGER = Logger.getLogger(NotificationController.class.getName());

    private final ServerNetworkManager networkManager;
    private final GameSessionManager sessionManager;
    private final PlayerSessionRegistry playerSessionRegistry;

    public NotificationController(EventBus serverEventBus, ServerNetworkManager networkManager,
                                  GameSessionManager sessionManager, PlayerSessionRegistry playerSessionRegistry) {
        this.networkManager = Objects.requireNonNull(networkManager);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.playerSessionRegistry = Objects.requireNonNull(playerSessionRegistry);
        Objects.requireNonNull(serverEventBus).register(this);
        LOGGER.info("NotificationController initialized and registered with server EventBus.");
    }

    // --- Existing Handlers (Login, Lobby, Game State) ---
    @MessageHandler
    public void onInternalLoginSuccess(InternalLoginSuccessEvent event) {
        LOGGER.fine("NC handling InternalLoginSuccessEvent for NetID: " + event.networkClientId());
        String welcomeMessage = "Welcome, " + event.nickname() + "!";
        ServerLoginResponse response = new ServerLoginResponse(event.gamePlayerId(), welcomeMessage);
        networkManager.sendMessageToClient(event.networkClientId(), response);
    }

    @MessageHandler
    public void onInternalPlayerReconnected(InternalPlayerReconnectedEvent event) {
        LOGGER.info("NC handling InternalPlayerReconnectedEvent for Player: " + event.nickname() + ", Session: " + event.sessionId());
        String welcomeMessage = "Welcome back, " + event.nickname() + "!";
        ServerLoginResponse loginResp = new ServerLoginResponse(event.gamePlayerId(), welcomeMessage);
        networkManager.sendMessageToClient(event.newNetworkClientId(), loginResp);

        GameSession session = sessionManager.getSession(event.sessionId());
        if (session != null) {
            GameSessionStateChangedEvent stateUpdate = new GameSessionStateChangedEvent(
                    event.sessionId(),
                    null,
                    session.getCurrentState(),
                    session.getAllRegisteredPlayersInfoDTOs()
            );
            networkManager.sendMessageToClient(event.newNetworkClientId(), stateUpdate);

            PlayerJoinedGameSessionNotification rejoinNotification = new PlayerJoinedGameSessionNotification(
                    event.sessionId(),
                    session.getPlayerInfoDTO(event.gamePlayerId()),
                    session.getActivePlayersInfoDTOs()
            );

            List<String> otherClientsInSession = playerSessionRegistry
                    .getNetworkClientIdsForSession(event.sessionId()).stream()
                    .filter(netId -> !netId.equals(event.newNetworkClientId()))
                    .toList();
            if(!otherClientsInSession.isEmpty()){
                networkManager.broadcastMessageToSessionClients(rejoinNotification, otherClientsInSession);
            }
            LOGGER.info("Sent rejoin notification to " + otherClientsInSession.size() + " other players in session " + event.sessionId());

        } else {
            LOGGER.warning("Session " + event.sessionId() + " not found when trying to send reconnected state for " + event.nickname());
        }
    }

    @MessageHandler
    public void onInternalPlayerTemporarilyDisconnected(InternalPlayerTemporarilyDisconnectedEvent event) {
        LOGGER.info("NC handling InternalPlayerTemporarilyDisconnectedEvent for Player: " + event.nickname() + ", Session: " + event.sessionId());
        GameSession session = sessionManager.getSession(event.sessionId());
        if (session != null) {
            PlayerLeftGameSessionNotification tempDiscoNotification = new PlayerLeftGameSessionNotification(
                    event.sessionId(),
                    event.gamePlayerId(),
                    event.nickname() + " (Lost Connection)",
                    session.getActivePlayersInfoDTOs(),
                    session.isCreator(event.gamePlayerId())
            );
            List<String> otherClientsInSession = playerSessionRegistry
                    .getNetworkClientIdsForSession(event.sessionId()).stream()
                    .filter(netId -> {
                        String gameId = session.getGameIdForCurrentNetId(netId);
                        return gameId == null || !gameId.equals(event.gamePlayerId());
                    })
                    .toList();

            if(!otherClientsInSession.isEmpty()){
                networkManager.broadcastMessageToSessionClients(tempDiscoNotification, otherClientsInSession);
            }
        }
    }

    @MessageHandler
    public void onInternalLoginFailed(InternalLoginFailedEvent event) {
        LOGGER.fine("NotificationController handling InternalLoginFailedEvent for NetID: " + event.networkClientId());
        ServerLoginResponse response = new ServerLoginResponse(event.reason());
        networkManager.sendMessageToClient(event.networkClientId(), response);
    }

    @MessageHandler
    public void onInternalGameLobbyListChanged(InternalGameLobbyListPotentiallyChangedEvent event) {
        LOGGER.info("NotificationController detected game lobby list change. Notifying relevant clients.");
        networkManager.broadcastMessageToAllClients(new ServerGameListUpdateNotification()); // This is good
        LOGGER.info("Broadcasted ServerGameListUpdateNotification to all clients.");
    }

    @MessageHandler
    public void onInternalGameCreated(InternalGameCreatedEvent event) {
        LOGGER.fine("NotificationController handling InternalGameCreatedEvent for session: " + event.sessionId());
        it.polimi.ingsw.common.message.setup.CreateGameResponseEvent clientResponse =
                new it.polimi.ingsw.common.message.setup.CreateGameResponseEvent(
                        event.sessionId(),
                        event.gameLobbyInfo(),
                        event.players()
                );
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), clientResponse);
        LOGGER.info("Sent CreateGameResponseEvent (success) to creator NetID: " + event.requestingNetworkClientId());
    }

    @MessageHandler
    public void onInternalGameCreationFailed(InternalGameCreationFailedEvent event) {
        LOGGER.fine("NotificationController handling InternalGameCreationFailedEvent for NetID: " + event.requestingNetworkClientId());
        it.polimi.ingsw.common.message.setup.CreateGameResponseEvent clientResponse =
                new it.polimi.ingsw.common.message.setup.CreateGameResponseEvent(event.reason());
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), clientResponse);
    }

    @MessageHandler
    public void onInternalPlayerJoinedMySession(InternalPlayerJoinedMySessionEvent event) {
        LOGGER.fine("NotificationController handling InternalPlayerJoinedMySessionEvent for session: " + event.sessionId());
        PlayerJoinedGameSessionNotification notification = new PlayerJoinedGameSessionNotification(
                event.sessionId(),
                event.newlyJoinedPlayerInfo(),
                event.allPlayersInSession()
        );
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId())
                .stream()
                .filter(netId -> !netId.equals(event.joiningPlayerNetworkClientId()))
                .toList();
        networkManager.broadcastMessageToSessionClients(notification, clientsInSession);
    }

    @MessageHandler
    public void onInternalGameJoined(InternalGameJoinedEvent event) {
        LOGGER.fine("NotificationController handling InternalGameJoinedEvent for NetID: " + event.requestingNetworkClientId());
        it.polimi.ingsw.common.message.setup.JoinGameResponseEvent clientResponse =
                new it.polimi.ingsw.common.message.setup.JoinGameResponseEvent(
                        event.sessionId(),
                        event.allPlayersInSession()
                );
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), clientResponse);

        GameSession session = sessionManager.getSession(event.sessionId());
        if (session != null) {
            GameSessionStateChangedEvent stateChangeForJoiner = new GameSessionStateChangedEvent(
                    event.sessionId(),
                    null,
                    session.getCurrentState(),
                    session.getAllRegisteredPlayersInfoDTOs()
            );
            networkManager.sendMessageToClient(event.requestingNetworkClientId(), stateChangeForJoiner);
        }
    }

    @MessageHandler
    public void onInternalGameJoinFailed(InternalGameJoinFailedEvent event) {
        LOGGER.fine("NotificationController handling InternalGameJoinFailedEvent for NetID: " + event.requestingNetworkClientId());
        it.polimi.ingsw.common.message.setup.JoinGameResponseEvent clientResponse =
                new it.polimi.ingsw.common.message.setup.JoinGameResponseEvent(event.sessionId(), event.reason());
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), clientResponse);
    }

    @MessageHandler
    public void onInternalPlayerLeftSession(InternalPlayerLeftSessionEvent event) {
        LOGGER.fine("NotificationController handling InternalPlayerLeftSessionEvent for session: " + event.sessionId() + ", player: " + event.leftPlayerNickname());
        PlayerLeftGameSessionNotification notification = new PlayerLeftGameSessionNotification(
                event.sessionId(),
                event.leftPlayerId(),
                event.leftPlayerNickname(),
                event.remainingPlayers(),
                event.wasHost()
        );
        List<String> remainingClientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        networkManager.broadcastMessageToSessionClients(notification, remainingClientsInSession);

        if (event.finalSessionStateIfChanged() == GameSessionState.ABORTED) {
            LOGGER.info("Session " + event.sessionId() + " aborted due to player leaving. Broadcasting state change.");
            GameSessionStateChangedEvent abortedEvent = new GameSessionStateChangedEvent(
                    event.sessionId(),
                    GameSessionState.LOBBY,
                    GameSessionState.ABORTED,
                    event.remainingPlayers()
            );
            networkManager.broadcastMessageToSessionClients(abortedEvent, remainingClientsInSession);
        }
    }

    @MessageHandler
    public void onInternalPlayerReadyStatusChanged(InternalPlayerReadyStatusChangedInSessionEvent event) {
        LOGGER.fine("NotificationController handling InternalPlayerReadyStatusChanged for session: " + event.sessionId());
        GameSessionStateChangedEvent notification = new GameSessionStateChangedEvent(
                event.sessionId(),
                GameSessionState.LOBBY,
                GameSessionState.LOBBY,
                event.updatedPlayersInSession()
        );
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        networkManager.broadcastMessageToSessionClients(notification, clientsInSession);
    }

    @MessageHandler
    public void onInternalGameStarted(InternalGameStartedInSessionEvent event) {
        LOGGER.fine("NotificationController handling InternalGameStarted for session: " + event.sessionId());
        GameSessionStateChangedEvent notification = new GameSessionStateChangedEvent(
                event.sessionId(),
                GameSessionState.LOBBY,
                GameSessionState.SHIP_BUILDING,
                event.playersInSession()
        );
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        networkManager.broadcastMessageToSessionClients(notification, clientsInSession);
    }

    @MessageHandler
    public void onInternalOperationError(InternalOperationErrorEvent event) {
        LOGGER.warning("NC handling InternalOperationErrorEvent for NetID: " + event.requestingNetworkClientId() + ", Reason: " + event.reason());
        ErrorMessage errorMsg = new ErrorMessage(event.reason(), ErrorMessage.ErrorType.ILLEGAL_ACTION);
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), errorMsg);
    }

    @MessageHandler
    public void onInternalSessionActualStateChanged(InternalSessionActualStateChangedEvent event) {
        LOGGER.info("NC: Session " + event.sessionId() + " actual state changed from " + event.oldState() + " to " + event.newState());
        GameSessionStateChangedEvent clientNotification = new GameSessionStateChangedEvent(
                event.sessionId(),
                event.oldState(),
                event.newState(),
                event.playersInSession()
        );
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientNotification, clientsInSession);
        }
    }
}
package it.polimi.ingsw.server.controller.notification;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
import it.polimi.ingsw.common.message.building.*; // Import client-facing building events
import it.polimi.ingsw.common.message.setup.GameSessionStateChangedEvent;
import it.polimi.ingsw.common.message.setup.PlayerJoinedGameSessionNotification;
import it.polimi.ingsw.common.message.setup.PlayerLeftGameSessionNotification;
import it.polimi.ingsw.common.message.setup.ServerGameListUpdateNotification;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.InfoMessage;
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
    public void onInternalGameSuspended(InternalGameSuspendedEvent event) {
        LOGGER.info("NC handling InternalGameSuspendedEvent for session: " + event.sessionId());
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            GameSession session = sessionManager.getSession(event.sessionId());
            if (session != null) {
                ErrorMessage suspendMsg = new ErrorMessage(
                        "Game '" + session.getGameName() + "' suspended: Only " + event.remainingActivePlayerNickname() + " remains active. Waiting for others to rejoin.",
                        ErrorMessage.ErrorType.GENERAL
                );
                networkManager.broadcastMessageToSessionClients(suspendMsg, clientsInSession);
            }
        }
    }

    @MessageHandler
    public void onInternalGameResumed(InternalGameResumedEvent event) {
        LOGGER.info("NC handling InternalGameResumedEvent for session: " + event.sessionId());
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            GameSession session = sessionManager.getSession(event.sessionId());
            if (session != null) {
                GameSessionStateChangedEvent resumedState = new GameSessionStateChangedEvent(
                        event.sessionId(),
                        session.getCurrentState(),
                        session.getCurrentState(),
                        event.activePlayers()
                );
                networkManager.broadcastMessageToSessionClients(resumedState, clientsInSession);
                InfoMessage info = new InfoMessage("Game Resumed", "Game activities have resumed in session '" + session.getGameName() + "'.");
                networkManager.broadcastMessageToSessionClients(info, clientsInSession);
            }
        }
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
    public void onInternalGameAborted(InternalGameAbortedEvent event) {
        LOGGER.info("NC handling InternalGameAbortedEvent for session: " + event.sessionId() + " Reason: " + event.reason());
        GameSession session = sessionManager.getSession(event.sessionId());
        GameSessionStateChangedEvent abortedNotification = new GameSessionStateChangedEvent(
                event.sessionId(),
                session != null ? session.getCurrentState() : null,
                GameSessionState.ABORTED,
                session != null ? session.getAllRegisteredPlayersInfoDTOs() : List.of()
        );
        List<String> clientsStillInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsStillInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(abortedNotification, clientsStillInSession);
            InfoMessage info = new InfoMessage("Game Aborted", "Session '" + (session != null ? session.getGameName() : event.sessionId()) + "' was aborted: " + event.reason());
            networkManager.broadcastMessageToSessionClients(info, clientsStillInSession);
        }
    }

    @MessageHandler
    public void onInternalOperationError(InternalOperationErrorEvent event) {
        LOGGER.warning("NC handling InternalOperationErrorEvent for NetID: " + event.requestingNetworkClientId() + ", Reason: " + event.reason());
        ErrorMessage errorMsg = new ErrorMessage(event.reason(), ErrorMessage.ErrorType.ILLEGAL_ACTION);
        networkManager.sendMessageToClient(event.requestingNetworkClientId(), errorMsg);
    }

    // --- NEW Handlers for Ship Building ---

    @MessageHandler
    public void onInternalComponentOfferedToPlayer(InternalComponentOfferedToPlayerEvent event) {
        LOGGER.fine("NC: Offering component " + event.component().componentType() + " to NetID: " + event.networkClientId());
        ComponentOfferedToPlayerEvent clientEvent = new ComponentOfferedToPlayerEvent(event.component());
        networkManager.sendMessageToClient(event.networkClientId(), clientEvent);
    }

    @MessageHandler
    public void onInternalComponentPlacedOnShip(InternalComponentPlacedOnShipEvent event) {
        LOGGER.fine("NC: Player " + event.gamePlayerId() + " placed component in session " + event.sessionId());
        ComponentPlacedEvent clientEvent = new ComponentPlacedEvent(
                event.gamePlayerId(),
                event.placedComponent().component(), // The ComponentDTO from PlacedComponentDTO
                event.placedComponent().position(),
                event.placedComponent().orientation()
        );
        // Broadcast to all in session so they can update their view of this player's ship
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
            // Also send the full ship board update, could be combined or separate
            PlayerShipUpdateEvent shipUpdate = new PlayerShipUpdateEvent(event.gamePlayerId(), event.updatedShipBoard());
            networkManager.broadcastMessageToSessionClients(shipUpdate, clientsInSession);
        }
    }

    @MessageHandler
    public void onInternalShipPlacementInvalid(InternalShipPlacementInvalidEvent event) {
        LOGGER.warning("NC: Invalid ship placement attempt by NetID: " + event.networkClientId());
        // This event is specific to the player who made the invalid attempt.
        // The ShipValidationResultEvent can be used for this, assuming it's designed to be sent to a single client
        // or its `playerId` field would match `event.networkClientId()`'s associated player.
        // For now, let's assume it needs a direct error or a specialized client event.
        // Using ErrorMessage for simplicity, but ShipValidationResultEvent is better if client UI needs detailed errors.
        ErrorMessage errorMsg = new ErrorMessage(
                "Invalid component placement: " + event.errors().get(0).message(), // Show first error
                ErrorMessage.ErrorType.VALIDATION
        );
        networkManager.sendMessageToClient(event.networkClientId(), errorMsg);
        // If you want to send all errors, create a specific client-facing event or enhance ErrorMessage.
        // Example with ShipValidationResultEvent (assuming client handles it):
        // ShipValidationResultEvent validationEvent = new ShipValidationResultEvent(
        // gamePlayerId, // Need to get gamePlayerId from networkClientId
        // false,
        // event.errors(),
        // somehowGetTheirCurrentShipBoardDTO() // Might be tricky or part of event
        // );
        // networkManager.sendMessageToClient(event.networkClientId(), validationEvent);
    }

    @MessageHandler
    public void onInternalComponentReserved(InternalComponentReservedEvent event) {
        LOGGER.fine("NC: Player " + event.gamePlayerId() + " reserved component in session " + event.sessionId());
        ComponentReservedEvent clientEvent = new ComponentReservedEvent(
                event.gamePlayerId(),
                event.reservedComponent(),
                event.reservedSlot()
        );
        // Send to the player who reserved it.
        // If others need to know (e.g., for counts of reserved items per player), then broadcast.
        GameSession session = sessionManager.getSession(event.sessionId());
        if (session != null) {
            String networkClientId = null; // Find networkClientId for gamePlayerId
            for(Map.Entry<String, String> entry : session.getCurrentNetworkIdToGameIdMap().entrySet()){ // Assuming GameSession exposes this map
                if(entry.getValue().equals(event.gamePlayerId())){
                    networkClientId = entry.getKey();
                    break;
                }
            }
            if (networkClientId != null) {
                networkManager.sendMessageToClient(networkClientId, clientEvent);
            }

            // Optionally, update all players' views of this player's board
            PlayerShipUpdateEvent shipUpdate = new PlayerShipUpdateEvent(event.gamePlayerId(), event.updatedShipBoard());
            List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
            if (!clientsInSession.isEmpty()) {
                networkManager.broadcastMessageToSessionClients(shipUpdate, clientsInSession);
            }
        }
    }

    @MessageHandler
    public void onInternalComponentReturnedToFaceUpPile(InternalComponentReturnedToFaceUpPileEvent event) {
        LOGGER.fine("NC: Player " + event.gamePlayerId() + " returned component to face-up pile in session " + event.sessionId());
        // Notify all players in the session about the returned component and the new state of the face-up pile.
        ComponentReturnedToPileEvent clientEvent = new ComponentReturnedToPileEvent(event.returnedComponent());
        AvailableComponentsUpdateEvent pileUpdateEvent = new AvailableComponentsUpdateEvent(event.currentFaceUpPile());

        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
            networkManager.broadcastMessageToSessionClients(pileUpdateEvent, clientsInSession);
        }
    }

    @MessageHandler
    public void onInternalBuildingTimerUpdated(InternalBuildingTimerUpdatedEvent event) {
        LOGGER.info("NC: Building timer updated in session " + event.sessionId() + " to stage " + event.timerStage());
        BuildingTimerUpdatedEvent clientEvent = new BuildingTimerUpdatedEvent(event.timerStage(), event.description());
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
        }
    }

    @MessageHandler
    public void onInternalPlayerFinishedBuilding(InternalPlayerFinishedBuildingEvent event) {
        LOGGER.info("NC: Player " + event.playerInfo().getNickname() + " finished building in session " + event.sessionId());
        PlayerFinishedBuildingEvent clientEvent = new PlayerFinishedBuildingEvent(event.playerInfo(), event.flightOrderPosition());
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
        }
    }

    @MessageHandler
    public void onInternalShipBoardCorrected(InternalShipBoardCorrectedEvent event) {
        LOGGER.info("NC: Ship board corrected for player " + event.gamePlayerId() + " in session " + event.sessionId());
        ShipCorrectedEvent clientEvent = new ShipCorrectedEvent(
                event.gamePlayerId(),
                event.correctedShipBoard(),
                event.penaltiesApplied()
        );
        // This could be sent just to the affected player, or to all if they need to see the corrected board.
        // For now, send to all.
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
        }
    }

    @MessageHandler
    public void onInternalShipBuildingPhaseEndedForAll(InternalShipBuildingPhaseEndedForAllEvent event) {
        LOGGER.info("NC: Ship building phase ended for all in session " + event.sessionId());
        ShipBuildingPhaseEndedEvent clientEvent = new ShipBuildingPhaseEndedEvent();
        List<String> clientsInSession = playerSessionRegistry.getNetworkClientIdsForSession(event.sessionId()).stream().toList();
        if (!clientsInSession.isEmpty()) {
            networkManager.broadcastMessageToSessionClients(clientEvent, clientsInSession);
        }
        // This might also trigger a GameSessionStateChangedEvent if the overall session state changes.
        // For example, to FLIGHT_PREPARATION. GameSession should publish that internal event.
    }

    // Helper method to get networkClientId from gamePlayerId within a session
    private String getNetworkClientIdForPlayer(String sessionId, String gamePlayerId) {
        GameSession session = sessionManager.getSession(sessionId);
        if (session != null) {
            // Need a way for GameSession to map gamePlayerId back to current networkClientId
            // This could be by iterating its internal currentNetworkIdToGameIdMap
            for (Map.Entry<String, String> entry : session.getCurrentNetworkIdToGameIdMap().entrySet()) {
                if (entry.getValue().equals(gamePlayerId)) {
                    return entry.getKey();
                }
            }
        }
        LOGGER.warning("Could not find networkClientId for gamePlayerId " + gamePlayerId + " in session " + sessionId);
        return null;
    }

    // Add to NotificationController.java
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
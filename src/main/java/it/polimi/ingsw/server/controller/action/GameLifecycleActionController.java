package it.polimi.ingsw.server.controller.action;

import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.setup.*;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.server.controller.CommandContext;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.event.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameLifecycleActionController {
    private static final Logger LOGGER = Logger.getLogger(GameLifecycleActionController.class.getName());

    public GameLifecycleActionController() {
        LOGGER.info("GameLifecycleActionController initialized.");
    }

    private String getLoggedInPlayerId(CommandContext<?> context) {
        return context.networkClientToGamePlayerMap().get(context.networkClientId());
    }
    private String getLoggedInPlayerNickname(CommandContext<?> context, String gamePlayerId) {
        return context.activePlayersByIdMap().get(gamePlayerId);
    }


    public void handleCreateGame(CommandContext<CreateGameRequestCommand> context) {
        String networkClientId = context.networkClientId();
        String gamePlayerId = getLoggedInPlayerId(context);

        if (gamePlayerId == null) {
            context.serverEventBus().post(new InternalGameCreationFailedEvent("", "Player not logged in.", networkClientId));
            return;
        }
        String nickname = getLoggedInPlayerNickname(context, gamePlayerId);
        if (nickname == null) {
            context.serverEventBus().post(new InternalGameCreationFailedEvent("", "Player identity inconsistency.", networkClientId));
            return;
        }

        GameSettingsDTO settings = context.command().getSettings();
        LOGGER.info("Handling CreateGameRequest from NetID: " + networkClientId + " (Player: " + nickname + ") with settings: " + settings);

        context.gameLogicExecutor().submit(() -> {
            GameSession newSession = context.sessionManager().createNewGameSession(
                    networkClientId, gamePlayerId, nickname, settings
            );

            if (newSession != null) {
                context.playerSessionRegistry().registerPlayerInSession(networkClientId, gamePlayerId, newSession.getSessionId());

                context.serverEventBus().post(new InternalGameCreatedEvent(
                        newSession.getSessionId(),
                        newSession.getLobbyInfo(),
                        newSession.getAllRegisteredPlayersInfoDTOs(), // Corrected
                        networkClientId
                ));
                context.serverEventBus().post(new InternalGameLobbyListPotentiallyChangedEvent());
            } else {
                context.serverEventBus().post(new InternalGameCreationFailedEvent(
                        settings.getGameName(), "Failed to create session instance.", networkClientId
                ));
            }
        });
    }


    public void handleJoinGame(CommandContext<JoinGameRequestCommand> context) {
        String networkClientId = context.networkClientId();
        String gamePlayerId = getLoggedInPlayerId(context);
        String sessionIdToJoin = context.command().getSessionId();

        if (gamePlayerId == null) {
            context.serverEventBus().post(new InternalGameJoinFailedEvent(sessionIdToJoin, "Player not logged in.", networkClientId));
            return;
        }
        String nickname = getLoggedInPlayerNickname(context, gamePlayerId);
        if (nickname == null) {
            context.serverEventBus().post(new InternalGameJoinFailedEvent(sessionIdToJoin, "Player identity inconsistency.", networkClientId));
            return;
        }

        LOGGER.info("Handling JoinGameRequest for session " + sessionIdToJoin + " from NetID: " + networkClientId + " (Player: " + nickname + ")");

        context.gameLogicExecutor().submit(() -> {
            GameSession session = context.sessionManager().getSession(sessionIdToJoin);
            if (session == null) {
                context.serverEventBus().post(new InternalGameJoinFailedEvent(sessionIdToJoin, "Game session not found.", networkClientId));
                return;
            }

            boolean joined = context.sessionManager().addPlayerToSession(sessionIdToJoin, networkClientId, gamePlayerId, nickname);

            if (joined) {
                context.playerSessionRegistry().registerPlayerInSession(networkClientId, gamePlayerId, sessionIdToJoin);
                PlayerInfoDTO joinedPlayerInfo = session.getAllRegisteredPlayersInfoDTOs().stream() // Corrected
                        .filter(p -> p.getPlayerId().equals(gamePlayerId)).findFirst().orElse(null);

                if (joinedPlayerInfo != null) {
                    context.serverEventBus().post(new InternalGameJoinedEvent(
                            sessionIdToJoin, joinedPlayerInfo,
                            session.getAllRegisteredPlayersInfoDTOs(), // Corrected
                            networkClientId
                    ));
                    context.serverEventBus().post(new InternalGameLobbyListPotentiallyChangedEvent());
                } else {
                    context.serverEventBus().post(new InternalGameJoinFailedEvent(sessionIdToJoin, "Internal error after joining.", networkClientId));
                }
            } else {
                String reason = "Failed to join: ";
                if (session.getTotalRegisteredPlayerCount() >= session.getMaxPlayers()) reason += "Lobby full."; // Corrected
                else if (session.getCurrentState() != it.polimi.ingsw.common.model.GameSessionState.LOBBY) reason += "Game not in LOBBY state.";
                else reason += "Unknown reason.";
                context.serverEventBus().post(new InternalGameJoinFailedEvent(sessionIdToJoin, reason, networkClientId));
            }
        });
    }

    public void handleLeaveGame(CommandContext<LeaveGameRequestCommand> context) {
        String networkClientId = context.networkClientId();
        String gamePlayerId = getLoggedInPlayerId(context);
        String sessionIdToLeave = context.command().getSessionId();
        PlayerSessionRegistry registry = context.playerSessionRegistry();

        String actualSessionId = registry.getSessionIdForNetworkClient(networkClientId);
        if (actualSessionId == null || !actualSessionId.equals(sessionIdToLeave)) {
            LOGGER.warning("NetID: " + networkClientId + " tried to leave session " + sessionIdToLeave + " but is in " + actualSessionId + " or none.");
            return;
        }

        LOGGER.info("Handling LeaveGameRequest for session " + sessionIdToLeave + " from NetID: " + networkClientId);

        context.gameLogicExecutor().submit(() -> {
            GameSession session = context.sessionManager().getSession(sessionIdToLeave);
            if (session != null && gamePlayerId !=null) {
                session.removePlayerHard(networkClientId); // Corrected
                registry.removePlayerFromAnySessionByNetworkId(networkClientId);
            } else {
                LOGGER.warning("Attempt to leave session " + sessionIdToLeave + " failed: session or player not found.");
                if (session == null) registry.removePlayerFromAnySessionByNetworkId(networkClientId);
            }
            context.serverEventBus().post(new InternalGameLobbyListPotentiallyChangedEvent());
        });
    }


    public void handleSetPlayerReady(CommandContext<SetPlayerReadyCommand> context) {
        String networkClientId = context.networkClientId();
        String sessionId = context.command().getSessionId();
        boolean isReady = context.command().isReady();
        PlayerSessionRegistry registry = context.playerSessionRegistry();

        String actualSessionId = registry.getSessionIdForNetworkClient(networkClientId);
        if (actualSessionId == null || !actualSessionId.equals(sessionId)) {
            LOGGER.warning("NetID: " + networkClientId + " tried to set ready for session " + sessionId + " but is in " + actualSessionId + " or none.");
            context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("You are not in the specified game session.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            return;
        }

        LOGGER.info("Handling SetPlayerReady(ready=" + isReady + ") for session " + sessionId + " from NetID: " + networkClientId);

        context.gameLogicExecutor().submit(() -> {
            GameSession session = context.sessionManager().getSession(sessionId);
            if (session != null) {
                if(session.getCurrentState() != it.polimi.ingsw.common.model.GameSessionState.LOBBY){
                    context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Cannot change ready status: game not in LOBBY.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
                    return;
                }
                session.setPlayerReady(networkClientId, isReady);
            } else {
                LOGGER.warning("SetPlayerReady for non-existent session " + sessionId);
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Game session not found.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            }
        });
    }


    public void handleStartGame(CommandContext<StartGameRequestCommand> context) {
        String networkClientId = context.networkClientId();
        String gamePlayerId = getLoggedInPlayerId(context);
        String sessionId = context.command().getSessionId();
        PlayerSessionRegistry registry = context.playerSessionRegistry();

        String actualSessionId = registry.getSessionIdForNetworkClient(networkClientId);
        if (actualSessionId == null || !actualSessionId.equals(sessionId)) {
            LOGGER.warning("NetID: " + networkClientId + " tried to start game for session " + sessionId + " but is in " + actualSessionId + " or none.");
            context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("You are not in the specified game session to start it.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            return;
        }
        if (gamePlayerId == null) {
            context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Not logged in.", ErrorMessage.ErrorType.AUTHENTICATION_FAILURE));
            return;
        }

        LOGGER.info("Handling StartGameRequest for session " + sessionId + " from NetID: " + networkClientId);

        context.gameLogicExecutor().submit(() -> {
            GameSession session = context.sessionManager().getSession(sessionId);
            if (session == null) {
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Game session not found.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
                return;
            }
            if (session.getCurrentState() != it.polimi.ingsw.common.model.GameSessionState.LOBBY) {
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Game is not in LOBBY state.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
                return;
            }
            if (!session.isCreator(gamePlayerId)) {
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Only the host can start the game.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
                return;
            }
            if (!session.checkAllActivePlayersReady()) { // Corrected
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Not all players are ready.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
                return;
            }

            boolean started = session.startGame(gamePlayerId); // Corrected
            if (started) {
                context.serverEventBus().post(new InternalGameLobbyListPotentiallyChangedEvent());
            } else {
                context.networkManager().sendMessageToClient(networkClientId, new ErrorMessage("Failed to start game (unknown reason).", ErrorMessage.ErrorType.SERVER_INTERNAL));
            }
        });
    }
}
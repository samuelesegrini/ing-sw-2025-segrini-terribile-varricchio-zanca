package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;

import java.util.List;

public record InternalGameCreatedEvent(String sessionId, GameLobbyInfoDTO gameLobbyInfo, List<PlayerInfoDTO> players, String requestingNetworkClientId) implements Message {}

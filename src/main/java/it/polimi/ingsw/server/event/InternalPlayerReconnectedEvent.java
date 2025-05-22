package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;

import java.util.List;

public record InternalPlayerReconnectedEvent(String sessionId, String gamePlayerId, String nickname, String newNetworkClientId, List<PlayerInfoDTO> currentPlayersInSession) implements Message {}

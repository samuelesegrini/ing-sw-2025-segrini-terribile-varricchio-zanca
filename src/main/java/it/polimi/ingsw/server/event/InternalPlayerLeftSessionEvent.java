package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.model.GameSessionState;

import java.util.List;

public record InternalPlayerLeftSessionEvent(String sessionId, String leftPlayerId,
                                             String leftPlayerNickname, List<PlayerInfoDTO> remainingPlayers,
                                             boolean wasHost, GameSessionState finalSessionStateIfChanged) implements Message {}


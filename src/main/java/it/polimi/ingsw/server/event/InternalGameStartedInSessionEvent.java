package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;

import java.util.List;

public record InternalGameStartedInSessionEvent(String sessionId, List<PlayerInfoDTO> playersInSession) implements Message {}

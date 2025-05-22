package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.message.Message;

public record InternalGameJoinFailedEvent(String sessionId, String reason, String requestingNetworkClientId) implements Message {}

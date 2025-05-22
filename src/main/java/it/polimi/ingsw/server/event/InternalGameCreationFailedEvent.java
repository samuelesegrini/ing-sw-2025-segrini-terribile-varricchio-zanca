package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.message.Message;

public record InternalGameCreationFailedEvent(String requestedGameName, String reason, String requestingNetworkClientId) implements Message {}

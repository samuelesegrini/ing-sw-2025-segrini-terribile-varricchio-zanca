package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.message.Message;

public record InternalLoginFailedEvent(String networkClientId, String reason) implements Message {}

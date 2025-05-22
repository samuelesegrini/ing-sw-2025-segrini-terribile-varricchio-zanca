package it.polimi.ingsw.server.event;
import it.polimi.ingsw.common.message.Message;

public record InternalLoginSuccessEvent(String networkClientId, String gamePlayerId, String nickname) implements Message {}



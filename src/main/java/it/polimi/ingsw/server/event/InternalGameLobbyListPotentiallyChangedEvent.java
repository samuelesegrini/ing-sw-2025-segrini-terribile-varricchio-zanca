package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.message.Message;

/** Signals that the global game list (joinable/running) might have changed and clients should be updated. */
public record InternalGameLobbyListPotentiallyChangedEvent() implements Message {}

package it.polimi.ingsw.common.protocol;

import java.io.Serializable;

/**
 * Everything a client can ask the server to do.
 *
 * <p>Client to server is a closed set of records, not a set of remote methods. RMI is a
 * transport for these messages, not a way to call the model from another machine
 * (architecture § 3.2). One consequence is that the whole controller can be tested without
 * a network: hand it commands, assert on the events it emits.
 *
 * <p>The set is sealed in two layers. Each layer is exhaustive, so a phase can switch over
 * the commands it owns and be told at compile time when a new one appears. The layers match
 * the phases of a game, which is what makes architecture § 3.3 structural rather than a
 * convention: a {@link BuildingCommand} arriving during the flight has nowhere to be
 * handled, and the compiler says so.
 *
 * <p><b>Nobody says who they are.</b> No command carries a nickname or a colour. The server
 * knows which session a message arrived on and looks up the player from that. A command
 * that named its own sender would be a command a client could forge — which matters most
 * for {@link FlightCommand.Answer}, whose payload names a player for reasons that predate
 * the network.
 */
public sealed interface Command extends Serializable
        permits LobbyCommand, BuildingCommand, PreparationCommand, FlightCommand {
}

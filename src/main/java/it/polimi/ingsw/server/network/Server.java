package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.transport.DefaultPorts;
import it.polimi.ingsw.common.transport.Doorway;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.common.transport.rmi.RmiServer;
import it.polimi.ingsw.common.transport.socket.SocketServer;
import it.polimi.ingsw.server.lobby.Lobby;
import it.polimi.ingsw.server.lobby.ServerSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * One lobby, listening two ways at once.
 *
 * <p>This is the whole of what requirement S5 needs, and it is four lines of it: both
 * transports are handed the same {@code lobby::welcome}, and from there nothing downstream can
 * tell which door a player came through. There is no branch to write, because there is nothing
 * left that differs.
 *
 * <p>The two ports are separate because RMI wants a registry of its own. A player picks a
 * transport at startup (requirement C4) and connects to the matching one; whether the person
 * sitting opposite them made the same choice is not something either of them can find out.
 */
public final class Server implements AutoCloseable {

    private final Lobby lobby;
    private final Doorway sockets;
    private final Doorway rmi;

    /**
     * Every door, so that opening and shutting them is written once rather than once each.
     *
     * <p>The two are also held by name, because a client has to be told which port to use and
     * "the first one" is not something a caller should have to know.
     */
    private final List<Doorway> doors;

    private Server(Lobby lobby, Doorway sockets, Doorway rmi) {
        this.lobby = lobby;
        this.sockets = sockets;
        this.rmi = rmi;
        this.doors = List.of(sockets, rmi);
    }

    /**
     * Starts a server on the default ports, with the bundled game data.
     *
     * @return a running server
     * @throws TransportException if either port is already in use
     */
    public static Server start() {
        return start(DefaultPorts.SOCKET, DefaultPorts.RMI);
    }

    /**
     * Starts a server on the given ports, with everything else left at its default.
     *
     * <p>Which means, among other things, that it keeps no games. A server that should
     * survive being stopped says so with {@link #start(int, int, ServerSettings)}, because
     * writing files is not something a caller should get without asking.
     *
     * @param socketPort where socket clients connect, or zero to be given a free port
     * @param rmiPort    where the RMI registry lives, or zero to be given a free port
     * @return a running server
     * @throws TransportException if either port is already in use
     */
    public static Server start(int socketPort, int rmiPort) {
        return start(socketPort, rmiPort, ServerSettings.defaults());
    }

    /**
     * Starts a server.
     *
     * @param socketPort where socket clients connect, or zero to be given a free port
     * @param rmiPort    where the RMI registry lives, or zero to be given a free port
     * @param settings   the catalogue, the shuffle, the clock, the disconnection policy, the
     *                   solo timeout, and where games are kept
     * @return a running server
     * @throws TransportException if either port is already in use
     */
    public static Server start(int socketPort, int rmiPort, ServerSettings settings) {
        Lobby lobby = new Lobby(settings);
        List<Doorway> opened = new ArrayList<>();
        try {
            opened.add(SocketServer.listening(socketPort, lobby::welcome, Liveness.DEFAULT));
            opened.add(RmiServer.listening(rmiPort, lobby::welcome, Liveness.DEFAULT));
            return new Server(lobby, opened.get(0), opened.get(1));
        } catch (TransportException failed) {
            // Half a server is worse than none: a client would connect to the door that opened
            // and then find nobody else could reach the other one. Written as a loop over what
            // actually opened, so that a third door would not need a third null check.
            opened.forEach(Doorway::close);
            lobby.close();
            throw failed;
        }
    }

    /**
     * Returns the port socket clients should connect to.
     *
     * @return the bound socket port
     */
    public int socketPort() {
        return sockets.port();
    }

    /**
     * Returns the port the RMI registry is on.
     *
     * @return the bound registry port
     */
    public int rmiPort() {
        return rmi.port();
    }

    /**
     * Returns the lobby everything connects to.
     *
     * @return the lobby
     */
    public Lobby lobby() {
        return lobby;
    }

    @Override
    public void close() {
        doors.forEach(Doorway::close);
        lobby.close();
    }
}

package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.transport.DefaultPorts;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.common.transport.rmi.RmiServer;
import it.polimi.ingsw.common.transport.socket.SocketServer;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.lobby.DisconnectionPolicy;
import it.polimi.ingsw.server.lobby.Lobby;

import java.time.InstantSource;
import java.util.Random;
import java.util.random.RandomGenerator;

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
    private final SocketServer sockets;
    private final RmiServer rmi;

    private Server(Lobby lobby, SocketServer sockets, RmiServer rmi) {
        this.lobby = lobby;
        this.sockets = sockets;
        this.rmi = rmi;
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
     * Starts a server on the given ports, with the bundled game data.
     *
     * @param socketPort where socket clients connect, or zero to be given a free port
     * @param rmiPort    where the RMI registry lives, or zero to be given a free port
     * @return a running server
     * @throws TransportException if either port is already in use
     */
    public static Server start(int socketPort, int rmiPort) {
        return start(socketPort, rmiPort, GameDataLoader.loadBundled(), new Random(),
                InstantSource.system(), DisconnectionPolicy.GAME_CARRIES_ON);
    }

    /**
     * Starts a server.
     *
     * @param socketPort      where socket clients connect, or zero to be given a free port
     * @param rmiPort         where the RMI registry lives, or zero to be given a free port
     * @param data            the tiles, cards and boards games will be made of
     * @param random          where the shuffling and the dice come from
     * @param clock           where hourglasses read the time
     * @param onDisconnection what a dropped connection does to a game in progress
     * @return a running server
     * @throws TransportException if either port is already in use
     */
    public static Server start(int socketPort, int rmiPort, GameData data, RandomGenerator random,
                               InstantSource clock, DisconnectionPolicy onDisconnection) {
        Lobby lobby = new Lobby(data, random, clock, onDisconnection);
        SocketServer sockets = null;
        try {
            sockets = SocketServer.listening(socketPort, lobby::welcome, Liveness.DEFAULT);
            RmiServer rmi = RmiServer.listening(rmiPort, lobby::welcome, Liveness.DEFAULT);
            return new Server(lobby, sockets, rmi);
        } catch (TransportException failed) {
            // Half a server is worse than none: a client would connect to the door that opened
            // and then find nobody else could reach the other one.
            if (sockets != null) {
                sockets.close();
            }
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
        sockets.close();
        rmi.close();
        lobby.close();
    }
}

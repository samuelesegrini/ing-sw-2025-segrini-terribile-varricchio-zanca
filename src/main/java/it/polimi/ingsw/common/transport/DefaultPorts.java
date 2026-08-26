package it.polimi.ingsw.common.transport;

/**
 * Where a server listens unless somebody says otherwise.
 *
 * <p>Shared, because both sides have to agree and neither owns the agreement. Putting them on
 * the server would mean a client reaching into it to find out where to dial — which the
 * layering test refuses, and rightly: a client that imports the server is a client that ships
 * with one.
 *
 * <p>Two ports rather than one because RMI wants a registry of its own. A player picks a
 * transport at startup and the port follows from it; asking somebody to remember which of two
 * numbers goes with which of two words is asking for a mistake.
 */
public final class DefaultPorts {

    /** Where a socket client connects. */
    public static final int SOCKET = 4321;

    /** Where an RMI client looks for the registry. */
    public static final int RMI = 4322;

    private DefaultPorts() {
    }
}

package it.polimi.ingsw.server.model.enums.ship;

public enum ConnectorType {
    /**
     * Connector type is Universal.
     */
    UNIVERSAL,

    /**
     * Connector type is Double.
     */
    DOUBLE,

    /**
     * Connector type is Single.
     */
    SINGLE,

    /**
     * Connector type is Plain.
     */
    PLAIN;

    /**
     * Checks if the connector type is compatible with the connector type given as argument.
     * @param other The other connector type
     * @return {@code true} if the connector type is compatible with the connector type given as argument,
     * {@code false} otherwise.
     */
    public boolean canConnectTo(ConnectorType other) {
        // PLAIN can only connect to PLAIN
        if (this == PLAIN || other == PLAIN) {
            return this == other;
        }

        // UNIVERSAL connects to anything except PLAIN
        if (this == UNIVERSAL || other == UNIVERSAL) {
            return true;
        }

        // Same type connectors can connect
        return this == other;
    }
}

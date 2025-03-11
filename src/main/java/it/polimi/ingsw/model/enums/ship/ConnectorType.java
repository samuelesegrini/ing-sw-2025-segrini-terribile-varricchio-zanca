package it.polimi.ingsw.model.enums.ship;

public enum ConnectorType {
    /**
     * Connector type is universal.
     */
    UNIVERSAL,

    /**
     * Connector type is double.
     */
    DOUBLE,

    /**
     * Connector type is single.
     */
    SINGLE,

    /**
     * Connector type is plain.
     */
    PLAIN;

    // returns true if the connector can be connected to the connector given, false otherwise

    /**
     * Checks if the connector type is compatible with the connector type given as argument.
     * @param other The other connector type
     * @return {@code true} if the connector type is compatible with the connector type given as argument,
     * {@code false} otherwise.
     */
    public boolean canConnectTo(ConnectorType other) {return false;}
}

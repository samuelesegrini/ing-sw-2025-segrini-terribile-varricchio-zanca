package it.polimi.ingsw.model.enums.ship;

public enum ConnectorType {
    UNIVERSAL, DOUBLE, SINGLE, PLAIN;

    // returns true if the connector can be connected to the connector given, false otherwise
    public boolean canConnectTo(ConnectorType other) {return false;}
}

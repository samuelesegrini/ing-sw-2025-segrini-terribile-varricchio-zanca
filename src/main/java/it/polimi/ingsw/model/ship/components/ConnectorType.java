package it.polimi.ingsw.model.ship.components;

public enum ConnectorType {
    UNIVERSAL, DOUBLE, SINGLE, PLAIN;

    public boolean canConnectTo(ConnectorType other) {return false;}
}

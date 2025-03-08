package it.polimi.ingsw.model.enums.ship;

public enum ConnectorType {
    UNIVERSAL, DOUBLE, SINGLE, PLAIN;

    public boolean canConnectTo(ConnectorType other) {return false;}
}

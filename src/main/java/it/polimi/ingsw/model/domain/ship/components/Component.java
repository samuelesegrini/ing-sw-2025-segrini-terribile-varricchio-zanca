package it.polimi.ingsw.model.ship.components;

import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.ship.*;
import java.util.Map;

public abstract class Component {
    private ComponentType type;
    private Direction facing;
    private Position position;
    private Map<Direction, ConnectorType> connectors;

    public void rotate() {}
    public void place(Position p, Ship s) {}
    public ComponentType getType() {return null;}
    public Direction getFacing() {return null;}
    public ConnectorType getConnectorAt(Direction direction) {return null;}
    public Map<Direction, ConnectorType> getConnectors() {return null;}
    public boolean canBePlacedAt(Position position) {return false;}
}
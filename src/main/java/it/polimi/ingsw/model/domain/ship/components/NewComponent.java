package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.NewShip;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public abstract class NewComponent {
    private ComponentType type;
    private Direction direction;
    private Position position;
    private Map<Direction, ConnectorType> connectors;


    public ComponentType getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Map<Direction, ConnectorType> getConnectors() {
        return connectors;
    }

    public ConnectorType getConnectorAt(Direction direction) {
        return connectors.get(direction);
    }


    /**
     * Rotate the component 90 degrees clockwise from the direction stored in the facing attribute
     */
    public void rotate() {
        direction = direction.rotateClockwise();
    }


    public void accept(ComponentVisitor visitor) {}

    // These methods are overridden by the subclasses
    public void count(NewShip s) {}
    public boolean check(NewShip s) {
        return false;
    }
}

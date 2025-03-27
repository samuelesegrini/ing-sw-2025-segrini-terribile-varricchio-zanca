package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.NewShip;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public abstract class NewComponent {
    protected ComponentType type;
    protected Direction direction;
    protected Position position;
    protected Map<Direction, ConnectorType> connectors;


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

    // Updates the ship's stats adding this component's contributions
    public void count(NewShip s) {}

    // Checks if the component is still connected to at least one other component, and other specific conditions
    public boolean check(NewShip s) {
        NewComponent[][] board = s.getBoard();

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int x = neighbor.getX();
            int y = neighbor.getY();

            // Checks if there's a component nearby and if they're connected (assumes the connection is legal)
            if (board[x][y] != null && board[x][y].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                return true;
            }
        }
        return false;
    }
}

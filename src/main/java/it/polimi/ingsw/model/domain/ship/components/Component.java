package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Grid;
import it.polimi.ingsw.model.domain.ship.ShipBoardLayout;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;
import java.util.Map;

public abstract class Component {
    private ComponentType type;
    //indicates the direction the component is facing, useful for tracking any rotations of the component.
    private Direction facing;
    private Position position;
    private Map<Direction,ConnectorType> connectors;


    /**
     * Rotate the component 90 degrees clockwise from the direction stored in the facing attribute
     */
    public void rotate() {
        this.facing = facing.rotateClockwise();
    }

    /**
     * Position the component in the ship at the specified location.
     * @param p coordinates on the ShipBoardLayout
     * @param s ship where to place the component
     * @see ShipBoardLayout
     */
    public void place(Position p, Ship s) {
        if (this.canBePlacedAt(p, s)) {
            s.addComponent(this, p);
        }
        else {
            System.out.println("Posizione non valida.");
        }
    }

    /**
     * It checks if a component can be placed at the specified location,
     * considering whether another component occupies the space
     * and if the input coordinates match a valid point on the grid.
     * @param p coordinates on the ShipBoardLayout
     * @return {@code true} if the component can be placed at the specified location, {@code false} otherwise
     * @see ShipBoardLayout
     */
    public boolean canBePlacedAt(Position p, Ship s) {
        ShipBoardLayout layout = s.getLayout();
        Grid<T> grid = s.getGrid();    // ERRORE DA SISTEMARE
        return (layout.isValidPosition(p)) && (!grid.containsKey((p)));
    }

    /**
     * Returns the type of the component.
     * @return The type of the component
     */
    public ComponentType getType() {
        return this.type;
    }

    /**
     * Returns the component's direction. If it hasn't been rotated
     * or has been rotated by multiples of 360 degrees, the direction will be UP.
     * @return direction of the component
     */
    public Direction getFacing() {
        return this.facing;
    }

    /**
     * Given a direction it returns the connector type
     * @param d direction of interest
     * @return Returns the connector type at the specified direction
     */
    public ConnectorType getConnectorAt(Direction d) {
        return this.connectors.get(d);
    }

    /**
     * Returns all the component connectors with their corresponding directions
     * @return Map that associates directions with connector types.
     */
    public Map<Direction, ConnectorType> getConnectors() {
        return this.connectors;
    }

    /**
     * Returns the component's position.
     * @return position of the component
     */
    public Position getPosition() {
        return this.position;
    }

    /**
     * Sets the given connector at the given direction.
     */
    public void setConnector(Direction d, ConnectorType c) {
        this.connectors.put(d,c);
    }


    public void accept(ComponentVisitor visitor) {}

    // These methods are overridden by the subclasses
    public void count(Ship s) {}
    public boolean check(Ship s) {
        return false;
    }
}
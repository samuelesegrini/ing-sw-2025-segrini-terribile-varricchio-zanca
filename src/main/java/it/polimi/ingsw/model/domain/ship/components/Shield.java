package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Set;

public class Shield extends Component{
    /**
     * Indicates the directions in which the shield provides protection.
     * @return the shielded directions
     */
    public Set<Direction> getProtectedDirections(){ return Set.of(); }

    /**
     * Checks if the shield protects in a specific direction.
     * @param d direction
     * @return true if the shield cover the specified direction, false otherwise
     */
    public boolean protectsFromDirection(Direction d){ return false; }
}
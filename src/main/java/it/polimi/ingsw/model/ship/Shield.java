package it.polimi.ingsw.model.ship;

public class Shield extends Component{
    /**
     * Indicates the directions in which the shield provides protection.
     * @return the shielded directions
     */
    public Set<Direction> getProtectedDirections(){};

    /**
     * Checks if the shield protects in a specific direction.
     * @param d direction
     * @return true if the shield cover the specified direction, false otherwise
     */
    public boolean protectsFromDirection(Direction d){};
}

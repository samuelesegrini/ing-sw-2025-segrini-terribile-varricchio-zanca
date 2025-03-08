package it.polimi.ingsw.model.ship;

import java.util.Set;

public class Shield extends Component{
    public Set<Direction> getProtectedDirections() {
        return null;
    }

    public boolean protectsFromDirection(Direction direction) {
        return false;
    }
}
package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Set;

public class Shield extends Component{
    public Set<Direction> getProtectedDirections() {
        return null;
    }

    public boolean protectsFromDirection(Direction direction) {
        return false;
    }
}
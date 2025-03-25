package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Grid;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.ship.Direction;

public class StructuralModule extends Component {
    @Override
    public boolean check(Ship s) {
        Grid<Component> grid = s.getGrid();

        for (Direction d :  Direction.values()) {
            if (grid.containsKey(this.getPosition().offsetBy(d))) {
                return true;
            }
        }
        return false;
    }
}
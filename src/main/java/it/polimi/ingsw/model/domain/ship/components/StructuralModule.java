package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public class StructuralModule extends Component {
    public StructuralModule(ComponentType type, Map<Direction, ConnectorType> connectors) {
        super(type, connectors);
    }
}
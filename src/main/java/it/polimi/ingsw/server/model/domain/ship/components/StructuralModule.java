package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class StructuralModule extends Component {
    private static final long serialVersionUID = 1L;
    
    public StructuralModule(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
    }
}
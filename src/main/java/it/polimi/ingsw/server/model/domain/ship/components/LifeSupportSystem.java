package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.enums.crew.AlienColor;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class LifeSupportSystem extends Component {
    private AlienColor supportedAlienColor;


    public LifeSupportSystem(ComponentType type, Map<Direction, ConnectorType> connectors) {
        super(type, connectors);
    }


    public AlienColor getSupportedAlienColor() {
        return supportedAlienColor;
    }

    public void setSupportedAlienColor(AlienColor supportedAlienColor) {
        this.supportedAlienColor = supportedAlienColor;
    }
}

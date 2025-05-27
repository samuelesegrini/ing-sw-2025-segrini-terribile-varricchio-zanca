package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Shield extends Component {
    private Set<Direction> protectedDirections;
    boolean charged;


    public Shield(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
        this.charged = false;

        this.protectedDirections = new HashSet<>();
        this.protectedDirections.addAll(connectors.keySet());
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useShield(this.getShip(), this);
    }


    public Set<Direction> getProtectedDirections() {
        return protectedDirections;
    }

    public void setProtectedDirections(Set<Direction> protectedDirections) {
        this.protectedDirections = protectedDirections;
    }

    public boolean isCharged() {
        return charged;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }
}

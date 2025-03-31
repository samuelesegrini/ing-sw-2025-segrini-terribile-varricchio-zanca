package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Set;

public class Shield extends Component {
    private Set<Direction> protectedDirections;
    boolean charged;

    @Override
    public void accept(ComponentVisitor v) {
        v.useShield(super.ship, this);
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

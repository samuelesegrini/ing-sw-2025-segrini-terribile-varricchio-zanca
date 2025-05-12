package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;


    public Battery(ComponentType type, Map<Direction, ConnectorType> connectors, int maxBatteries) {
        super(type, connectors);
        this.maxBatteries = maxBatteries;
        this.currentBatteries = maxBatteries;
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useBattery(this.getShip(), this, v.getQuantity());
    }

    @Override
    public void count(Ship ship) {
        ship.setBatteries(ship.getBatteries() + currentBatteries);
    }


    public int getMaxBatteries() {
        return maxBatteries;
    }

    public void setMaxBatteries(int maxBatteries) {
        this.maxBatteries = maxBatteries;
    }

    public int getCurrentBatteries() {
        return currentBatteries;
    }

    public void setCurrentBatteries(int currentBatteries) {
        this.currentBatteries = currentBatteries;
    }
}

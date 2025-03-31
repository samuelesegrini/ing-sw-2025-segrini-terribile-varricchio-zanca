package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

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
    public void accept(ComponentVisitor v) {
        int quantity = 0;    // Input da utente
        v.useBattery(super.ship, this, quantity);
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

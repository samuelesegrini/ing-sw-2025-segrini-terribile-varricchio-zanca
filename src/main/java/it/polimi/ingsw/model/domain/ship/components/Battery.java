package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;

    @Override
    public void accept(ComponentVisitor v) {
        int quantity = 0;    // Input da utente
        v.useBattery(super.ship, this, quantity);
    }

    @Override
    public void count(Ship s) {
        s.setBatteries(s.getBatteries() + currentBatteries);
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

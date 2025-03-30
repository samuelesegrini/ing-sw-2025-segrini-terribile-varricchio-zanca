package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;

    @Override
    public void accept(ComponentVisitor v) {
        int quantity = 0;    // Input da utente
        v.useBattery(this, quantity);
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














/*

VECCHIA IMPLEMENTAZIONE

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;

    /**
     * Returns the number of batteries in the battery component.
     * @return number of remaining batteries

    public int getCurrentBatteries(){ return 0; }
    public void setCurrentBatteries(int currentBatteries){ this.currentBatteries = currentBatteries;}
    public int getMaxBatteries(){ return maxBatteries; }

    /**
     * Check if the component it has at least one battery stored.
     * @return {@code true} if there is at least one battery in the component, {@code false} otherwise.

    public boolean canUseBattery(){ return false; }

    /**
     * Decrements the number of batteries by the value passed as a parameter.
     * @param numberOfBatteries Indicates the number of batteries consumed

    public void useBattery(int numberOfBatteries){}

    /**
     * It fills the battery components to full capacity.

    public void resetBatteries(){}
}

 */
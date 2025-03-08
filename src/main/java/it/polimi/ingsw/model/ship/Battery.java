package it.polimi.ingsw.model.ship;

public class Battery {
    private int maxBatteries;
    private int currentBatteries;

    public int getCurrentBatteries() {
        return 0;
    }

    //returns true if it has at least one battery
    public boolean canUseBattery() {
        return false;
    }

    //decrements the number of batteries by the value passed as a parameter.
    public void useBattery(int numberOfBatteries) {

    }

    //at the start of the game, it fills the battery compartments to full capacity
    public void resetBatteries() {

    }

}

package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.components.Component;

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;

    /**
     * Returns the number of batteries in the battery component.
     * @return number of remaining batteries
     */
    public int getCurrentBatteries(){ return 0; }

    /**
     * Check if the component it has at least one battery stored.
     * @return {@code true} if there is at least one battery in the component, {@code false} otherwise.
     */
    public boolean canUseBattery(){ return false; }

    /**
     * Decrements the number of batteries by the value passed as a parameter.
     * @param numberOfBatteries Indicates the number of batteries consumed
    */
    public void useBattery(int numberOfBatteries){}

    /**
     * It fills the battery components to full capacity.
     */
    public void resetBatteries(){}
}

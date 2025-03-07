package it.polimi.ingsw.model.ship;

public class Battery extends Component {
    private int maxBatteries;
    private int currentBatteries;

    /**
     * Returns the number of batteries in the battery component.
     * @return number of remaining batteries
     */
    public int getCurrentBatteries(){};

    /**
     * Check if the component it has at least one battery stored.
     * @return true if there is at least one battery in the component, false otherwise.
     */
    public boolean canUseBattery(){};

    /**
     * Decrements the number of batteries by the value passed as a parameter.
     * @param numberOfBatteries Indicates the number of batteries consumed
    */
    public void useBattery(int numberOfBatteries){};

    /**
     * It fills the battery components to full capacity.
     */
    public void resetBatteries(){};
}

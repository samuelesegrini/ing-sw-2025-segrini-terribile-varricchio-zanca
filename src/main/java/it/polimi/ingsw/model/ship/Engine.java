package it.polimi.ingsw.model.ship;

public class Engine extends Component{
    private boolean isDouble;
    public boolean canBePlacedAt (Ship s, Position p);
    //the exhaust pipe must point to the rear of the space ship (toward the
    //player) and no component can sit on the square behind the engine.
    public Direction getExhaustDirection();
    //calculate the power contribution considering that double engine have twice the power, but at the
    //expense of consuming a battery.
    public int getPowerContribution (boolean usingBattery);
}


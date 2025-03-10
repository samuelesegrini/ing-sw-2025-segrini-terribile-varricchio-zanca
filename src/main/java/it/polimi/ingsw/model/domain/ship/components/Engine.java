package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Engine extends Component{
    private boolean isDouble;

    /**
     * Verifies that the engine points to the rear of the spaceship
     * and that no component is placed on the square behind the engine.
     * @param s ship where to place the engine
     * @param p coordinates on the ShipBoardLayout
     * @return {@code true} if both conditions for the engine placement are met, {@code false} otherwise
     */
    public boolean canBePlacedAt (Ship s, Position p){ return false; }

    /**
     * Returns the direction the exhaust is facing.
     * @return the direction the exhaust is facing
     */
    public Direction getExhaustDirection(){ return null; }

    /**
     * Calculate the power contribution considering that double engine have twice the power.
     * @param usingBattery indicates whether a battery has been spent to enable double engine
     * @return the power contribution
     */
    public int getPowerContribution (boolean usingBattery){ return 0; }
}


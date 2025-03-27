package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.NewShip;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Engine extends NewComponent {
    private boolean isDouble;

    @Override
    public void accept(ComponentVisitor v) {
        v.useEngine(this);
    }

    @Override
    public void count(NewShip s) {
        int power;
        // Chi verifica che venga usata una batteria?
        if (isDouble) {
            power = 2;
        }
        else {
            power = 1;
        }

        s.setEngines(s.getEngines() + power);
    }

    @Override
    public boolean check(NewShip s) {
        NewComponent[][] board = s.getBoard();
        boolean result = false;

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int x = neighbor.getX();
            int y = neighbor.getY();

            if (board[x][y] != null && board[x][y].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                result = true;
            }
        }

        Direction exhaust = direction.getOpposite();
        int x = position.offsetBy(exhaust).getX();
        int y = position.offsetBy(exhaust).getY();

        if (board[x][y] != null) {
            result = false;    // Exhaust pipe is blocked
        }

        return result;
    }


    public boolean isDouble() {
        return isDouble;
    }

    public void setDouble(boolean isDouble) {
        this.isDouble = isDouble;
    }
}























/*
public class Engine extends Component{
    private boolean isDouble;

    /**
     * Verifies that the engine points to the rear of the spaceship
     * and that no component is placed on the square behind the engine.
     * @param s ship where to place the engine
     * @param p coordinates on the ShipBoardLayout
     * @return {@code true} if both conditions for the engine placement are met, {@code false} otherwise

public boolean canBePlacedAt (Ship s, Position p){ return false; }

        /**
         * Returns the direction the exhaust is facing.
         * @return the direction the exhaust is facing

        public Direction getExhaustDirection(){ return null; }

        /**
         * Calculate the power contribution considering that double engine have twice the power.
         * @param usingBattery indicates whether a battery has been spent to enable double engine
         * @return the power contribution

        public int getPowerContribution (boolean usingBattery){ return 0; }
}
 */
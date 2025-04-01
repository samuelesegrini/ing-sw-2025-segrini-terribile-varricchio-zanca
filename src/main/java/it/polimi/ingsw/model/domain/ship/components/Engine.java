package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public class Engine extends Component {
    private boolean charged;


    public Engine(ComponentType type, Map<Direction, ConnectorType> connectors) {
        super(type, connectors);
        this.charged = false;
    }


    @Override
    public void accept(ComponentVisitor v) {
        v.useEngine(super.ship, this);
    }

    @Override
    public void count(Ship ship) {
        int power;

        if (super.getType() == ComponentType.ENGINE_DOUBLE && charged) {
            power = 2;
        } else if (super.getType() == ComponentType.ENGINE_SINGLE) {
            power = 1;
        }
        else {
            power = 0;
        }

        ship.setEngines(ship.getEngines() + power);
    }

    @Override
    public boolean check(Ship ship) {
        Component[][] board = ship.getBoard();
        boolean result = false;

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (board[row][col] != null && board[row][col].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                result = true;
            }
        }

        Direction exhaust = direction.getOpposite();
        int row = position.offsetBy(exhaust).getRow();
        int col = position.offsetBy(exhaust).getCol();

        if (board[row][col] != null) {
            result = false;    // Exhaust pipe is blocked
        }

        return result;
    }


    public boolean isCharged() {
        return charged;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }
}

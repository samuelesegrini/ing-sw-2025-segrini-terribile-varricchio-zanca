package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class Cannon extends Component {
    private boolean charged;


    public Cannon(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
        this.charged = false;
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useCannon(this.getShip(), this);
    }

    @Override
    public void count(Ship ship) {
        int power;

        if (super.getType() == ComponentType.CANNON_DOUBLE && charged) {
            power = 2;
        } else if (super.getType() == ComponentType.CANNON_SINGLE) {
            power = 1;
        }
        else {
            power = 0;
        }

        ship.setCannons(ship.getCannons() + power);
    }

    @Override
    public boolean check(Ship ship) {
        Component[][] board = ship.getBoard();
        boolean result = false;

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                if (board[row][col] != null && board[row][col].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                    result = true;
                }
            }
        }

        Direction barrel = direction;
        int row = position.offsetBy(barrel).getRow();
        int col = position.offsetBy(barrel).getCol();

        if (board[row][col] != null) {
            result = false;    // Barrel is blocked
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

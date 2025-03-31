package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Cannon extends Component {
    private boolean charged;

    @Override
    public void accept(ComponentVisitor v) {
        v.useCannon(super.ship, this);
    }

    @Override
    public void count(Ship s) {
        int power;

        if (super.getType() == ComponentType.CANNON_DOUBLE && charged) {
            power = 2;
        } else if (super.getType() == ComponentType.CANNON_SINGLE) {
            power = 1;
        }
        else {
            power = 0;
        }

        s.setCannons(s.getCannons() + power);
    }

    @Override
    public boolean check(Ship s) {
        Component[][] board = s.getBoard();
        boolean result = false;

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int x = neighbor.getX();
            int y = neighbor.getY();

            if (board[x][y] != null && board[x][y].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                result = true;
            }
        }

        Direction barrel = direction;
        int x = position.offsetBy(barrel).getX();
        int y = position.offsetBy(barrel).getY();

        if (board[x][y] != null) {
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

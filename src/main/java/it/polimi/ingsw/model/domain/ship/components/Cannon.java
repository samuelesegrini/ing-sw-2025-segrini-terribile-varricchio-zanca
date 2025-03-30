package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Cannon extends Component {
    private boolean isDouble;

    @Override
    public void accept(ComponentVisitor v) {
        v.useCannon(this);
    }

    @Override
    public void count(Ship s) {
        int power;
        // Chi verifica che venga usata una batteria?
        if (isDouble) {
            power = 2;
        } else {
            power = 1;
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


    public boolean isDouble() {
        return isDouble;
    }

    public void setDouble(boolean isDouble) {
        this.isDouble = isDouble;
    }
}
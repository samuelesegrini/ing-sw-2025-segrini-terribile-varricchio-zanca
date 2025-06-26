package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class Cannon extends Component {

    public Cannon(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useCannon(this.getShip(), this);
    }

    @Override
    public void count(Ship ship) {
        // Galaxy Trucker: Cannons provide base power without batteries
        // Double cannons need batteries during combat calculation to function at full power
        int basePower = switch (super.getType()) {
            case CANNON_SINGLE -> 1;
            case CANNON_DOUBLE -> 1; // Base power, can be boosted with batteries during combat
            default -> 0;
        };

        ship.setCannons(ship.getCannons() + basePower);
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


}

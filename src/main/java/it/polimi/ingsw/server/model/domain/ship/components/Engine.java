package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class Engine extends Component {
    private static final long serialVersionUID = 1L;

    public Engine(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useEngine(this.getShip(), this);
    }

    @Override
    public void count(Ship ship) {
        // Galaxy Trucker: Single engines work without batteries, double engines require batteries
        // Double engines provide 0 base power and need batteries to function at all
        int basePower = switch (super.getType()) {
            case ENGINE_SINGLE -> 1;
            case ENGINE_DOUBLE -> 0; // Require batteries for ANY power
            default -> 0;
        };

        ship.setEngines(ship.getEngines() + basePower);
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


}

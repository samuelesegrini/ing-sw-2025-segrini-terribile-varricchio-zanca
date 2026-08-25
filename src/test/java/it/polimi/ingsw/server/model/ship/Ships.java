package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.goods.GoodsBank;
import it.polimi.ingsw.server.model.player.PlayerColor;

import java.util.Map;
import java.util.Set;

/**
 * Ships for tests.
 *
 * <p>Almost every test wants the same thing: a rectangular board with no forbidden cells,
 * a starting cabin in the middle, and a bank deep enough not to matter. Spelling that out
 * in each test class buried the point of the test and meant a change to the constructor
 * rippled through every file that had one.
 */
public final class Ships {

    /** Where the starting cabin sits on a fixture board. */
    public static final Position CABIN = new Position(2, 2);

    private Ships() {
    }

    /** Returns a bank deep enough that no test runs into a shortage by accident. */
    public static GoodsBank deepBank() {
        return new GoodsBank(Map.of(GoodColor.RED, 99, GoodColor.YELLOW, 99,
                GoodColor.GREEN, 99, GoodColor.BLUE, 99));
    }

    /** Returns a bank holding exactly the cubes given, and nothing of any other colour. */
    public static GoodsBank bankOf(Map<GoodColor, Integer> stock) {
        return new GoodsBank(stock);
    }

    /** Returns a square board with every cell usable and the cabin in the middle. */
    public static ShipBoardSpec openBoard(int size, int reservationSlots) {
        return new ShipBoardSpec(size, size, 5, 4, CABIN, reservationSlots, Set.of());
    }

    /** Returns a five by five ship, so printed columns run 4 to 8 and rows 5 to 9. */
    public static Ship openShip() {
        return openShip(deepBank());
    }

    /** Returns a five by five ship drawing on the given bank. */
    public static Ship openShip(GoodsBank bank) {
        return new Ship(openBoard(5, 0), Tiles.startingCabin(PlayerColor.BLUE), bank);
    }

    /** Welds a tile of the given kind, with capacity when its kind needs one. */
    public static void put(Ship ship, Position cell, ComponentKind kind, Rotation rotation) {
        ship.place(cell, Tiles.of(kind, kind.hasCapacity() ? 2 : 0), rotation);
    }

    /** Welds a tile of the given kind, unrotated. */
    public static void put(Ship ship, Position cell, ComponentKind kind) {
        put(ship, cell, kind, Rotation.NONE);
    }
}

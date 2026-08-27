package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.AdventureDeck;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.board.DeckComposition;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Building sites for tests.
 *
 * <p>Every building test needs the same three collaborators wired together, and none of
 * them cares how. Spelling the wiring out in each test class buried what each test was
 * actually about, and meant a change to one constructor rippled through four files.
 */
public final class BuildingFixtures {

    /** Where the starting cabin sits on the fixture board. */
    public static final Position CABIN = new Position(2, 2);

    /** The cell directly ahead of the starting cabin, where most tests put their first tile. */
    public static final Position AHEAD = new Position(1, 2);

    /** The cell to port of the starting cabin. */
    public static final Position PORT = new Position(2, 1);

    private static final long SEED = 20250825L;

    private BuildingFixtures() {
    }

    /**
     * A builder and the pool it draws from.
     *
     * @param builder the player building
     * @param pool    the heap they share with everyone else
     */
    public record Site(ShipBuilder builder, ComponentPool pool) {
    }

    /** Returns interchangeable tiles, universal on every side so any of them welds anywhere. */
    public static List<ComponentTile> plainTiles(int count) {
        List<ComponentTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(new ComponentTile("tile-" + i, ComponentKind.STRUCTURAL_MODULE,
                    Tiles.allSides(Connector.UNIVERSAL), 0));
        }
        return tiles;
    }

    /** Returns cards of one level, enough to deal any pile a test needs. */
    private static List<AdventureCardIdentity> cards(CardLevel level, int count) {
        List<AdventureCardIdentity> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(new AdventureCardIdentity(level + "-card-" + i, AdventureCardType.OPEN_SPACE, level, false));
        }
        return cards;
    }

    /** Returns a level II deck: four piles of one level I card and two level II cards. */
    public static AdventureDeck levelTwoDeck() {
        Map<CardLevel, List<AdventureCardIdentity>> pools = new EnumMap<>(CardLevel.class);
        pools.put(CardLevel.LEVEL_I, cards(CardLevel.LEVEL_I, 20));
        pools.put(CardLevel.LEVEL_II, cards(CardLevel.LEVEL_II, 20));
        return AdventureDeck.deal(
                new DeckComposition(4, Map.of(CardLevel.LEVEL_I, 1, CardLevel.LEVEL_II, 2), false),
                pools, new Random(SEED));
    }

    /** Returns a test flight deck: one pile of eight, and nothing anyone may scout. */
    public static AdventureDeck testFlightDeck() {
        return AdventureDeck.deal(
                new DeckComposition(1, Map.of(CardLevel.LEVEL_I, 8), true),
                Map.of(CardLevel.LEVEL_I, cards(CardLevel.LEVEL_I, 20)), new Random(SEED));
    }

    /** Returns a site on a level II deck, with the given number of reservation slots. */
    public static Site site(int reservationSlots) {
        return site(reservationSlots, levelTwoDeck());
    }

    /** Returns a site on a given deck, with the given number of reservation slots. */
    public static Site site(int reservationSlots, AdventureDeck deck) {
        ComponentPool pool = new ComponentPool(plainTiles(8), new Random(SEED));
        Ship ship = new Ship(new ShipBoardSpec(5, 5, 5, 4, CABIN, reservationSlots, true, Set.of()),
                Tiles.startingCabin(PlayerColor.GREEN), Ships.deepBank());
        return new Site(new ShipBuilder(ship, pool, deck), pool);
    }
}

package it.polimi.ingsw.server.data;

import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.board.GameLevel;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.StartingCabinTile;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.player.PlayerColor;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The whole printed game: every board, every tile, every card.
 *
 * <p>Loaded once at startup and shared by all games on the server. Nothing in here
 * changes while a game runs, which is why one copy can serve many tables.
 *
 * @param levels         the two flight configurations in scope
 * @param tiles          the 152 tiles that go into the shared pool
 * @param startingCabins the four cabins handed out at setup, by player colour
 * @param cards          the identity of every adventure card
 * @param bankStock      how many cubes of each colour the game ships with
 */
public record GameData(Map<GameLevel, LevelSpec> levels,
                       List<ComponentTile> tiles,
                       Map<PlayerColor, StartingCabinTile> startingCabins,
                       List<AdventureCardIdentity> cards,
                       Map<GoodColor, Integer> bankStock) {

    /**
     * Validates the catalogue and takes defensive copies.
     *
     * @throws GameDataException if a level or a starting cabin is missing, or two tiles
     *                           or two cards share an identifier
     */
    public GameData {
        Map<GameLevel, LevelSpec> levelCopy = new EnumMap<>(GameLevel.class);
        levelCopy.putAll(levels);
        for (GameLevel level : GameLevel.values()) {
            if (!levelCopy.containsKey(level)) {
                throw new GameDataException("no board data for level " + level);
            }
        }
        levels = Map.copyOf(levelCopy);

        tiles = List.copyOf(tiles);
        requireDistinctIds(tiles.stream().map(ComponentTile::id).toList(), "component tile");

        Map<PlayerColor, StartingCabinTile> cabinCopy = new EnumMap<>(PlayerColor.class);
        cabinCopy.putAll(startingCabins);
        for (PlayerColor color : PlayerColor.values()) {
            if (!cabinCopy.containsKey(color)) {
                throw new GameDataException("no starting cabin for the " + color + " player");
            }
        }
        startingCabins = Map.copyOf(cabinCopy);

        cards = List.copyOf(cards);
        requireDistinctIds(cards.stream().map(AdventureCardIdentity::id).toList(), "adventure card");

        Map<GoodColor, Integer> bank = new EnumMap<>(GoodColor.class);
        bank.putAll(bankStock);
        for (GoodColor color : GoodColor.values()) {
            if (!bank.containsKey(color)) {
                throw new GameDataException("no bank stock given for " + color + " cubes");
            }
        }
        bankStock = Map.copyOf(bank);
    }

    private static void requireDistinctIds(List<String> ids, String what) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                throw new GameDataException("two " + what + " entries share the identifier " + id);
            }
        }
    }

    /**
     * Returns the specification of one flight configuration.
     *
     * @param level the flight configuration
     * @return its boards and rules
     */
    public LevelSpec level(GameLevel level) {
        return levels.get(level);
    }

    /**
     * Returns every tile of one kind.
     *
     * @param kind the kind to filter by
     * @return the matching tiles, in catalogue order
     */
    public List<ComponentTile> tilesOfKind(ComponentKind kind) {
        return tiles.stream().filter(tile -> tile.kind() == kind).toList();
    }

    /**
     * Returns the pool a flight draws its level I or level II cards from.
     *
     * @param level the card level
     * @return the matching cards, in catalogue order
     */
    public List<AdventureCardIdentity> cardsOfLevel(CardLevel level) {
        return cards.stream().filter(card -> card.level() == level).toList();
    }

    /**
     * Returns the cards bearing the L mark, which make up the test flight deck.
     *
     * @return the test flight cards, in catalogue order
     */
    public List<AdventureCardIdentity> testFlightCards() {
        return cards.stream().filter(AdventureCardIdentity::testFlight).toList();
    }
}

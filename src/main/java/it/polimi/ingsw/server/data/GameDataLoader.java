package it.polimi.ingsw.server.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.server.model.adventure.card.AbandonedShipCard;
import it.polimi.ingsw.server.model.adventure.card.AbandonedStationCard;
import it.polimi.ingsw.server.model.adventure.card.SlaversCard;
import it.polimi.ingsw.server.model.adventure.card.CombatLine;
import it.polimi.ingsw.server.model.adventure.card.EpidemicCard;
import it.polimi.ingsw.server.model.adventure.card.CombatPenalty;
import it.polimi.ingsw.server.model.adventure.card.CombatZoneCard;
import it.polimi.ingsw.server.model.adventure.card.MeteorSwarmCard;
import it.polimi.ingsw.server.model.adventure.card.PiratesCard;
import it.polimi.ingsw.server.model.adventure.card.OpenSpaceCard;
import it.polimi.ingsw.server.model.adventure.card.PlanetsCard;
import it.polimi.ingsw.server.model.adventure.card.ThreatPattern;
import it.polimi.ingsw.server.model.adventure.card.SmugglersCard;
import it.polimi.ingsw.server.model.adventure.card.StardustCard;
import it.polimi.ingsw.server.model.board.DeckComposition;
import it.polimi.ingsw.server.model.board.FlightBoardSpec;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.server.model.board.LevelRules;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.board.RewardTable;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.StartingCabinTile;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.Position;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the bundled JSON into an immutable {@link GameData} catalogue.
 *
 * <p>Every failure names the entry at fault. A rule that reads a wrong number is much
 * harder to diagnose than a startup that refuses to begin, so nothing here falls back
 * to a default when a field is missing or malformed.
 */
public final class GameDataLoader {

    private static final String COMPONENTS_FILE = "components.json";
    private static final String CARDS_FILE = "adventure-cards.json";
    private static final String BOARDS_FILE = "boards.json";

    private final GameDataSource source;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a loader reading from the given source.
     *
     * @param source where the data files come from, never {@code null}
     */
    public GameDataLoader(GameDataSource source) {
        this.source = source;
    }

    /**
     * Loads the catalogue shipped with the application.
     *
     * @return the game data
     * @throws GameDataException if any file is missing or malformed
     */
    public static GameData loadBundled() {
        return new GameDataLoader(GameDataSource.bundled()).load();
    }

    /**
     * Loads and validates the whole catalogue.
     *
     * @return the game data
     * @throws GameDataException if any file is missing or malformed
     */
    public GameData load() {
        List<ComponentTile> tiles = new ArrayList<>();
        Map<PlayerColor, StartingCabinTile> startingCabins = new EnumMap<>(PlayerColor.class);
        readTiles(tiles, startingCabins);
        Map<String, AdventureCard> playable = new LinkedHashMap<>();
        List<AdventureCardIdentity> cards = readCards(playable);
        return new GameData(readLevels(), tiles, startingCabins, cards, readBankStock(), playable);
    }

    // ---------------------------------------------------------------- components

    private void readTiles(List<ComponentTile> tiles, Map<PlayerColor, StartingCabinTile> startingCabins) {
        JsonNode root = read(COMPONENTS_FILE);
        for (JsonNode entry : array(root, "tiles", COMPONENTS_FILE)) {
            String id = text(entry, "id", COMPONENTS_FILE);
            String where = COMPONENTS_FILE + " entry " + id;
            ComponentKind kind = enumValue(ComponentKind.class, text(entry, "kind", where), where);
            Map<Direction, Connector> connectors = readConnectors(entry, where);

            try {
                if (kind == ComponentKind.STARTING_CABIN) {
                    PlayerColor color = enumValue(PlayerColor.class, text(entry, "playerColor", where), where);
                    StartingCabinTile previous =
                            startingCabins.put(color, new StartingCabinTile(id, color, connectors));
                    if (previous != null) {
                        throw new GameDataException(where + ": a second starting cabin for the " + color + " player");
                    }
                } else {
                    tiles.add(new ComponentTile(id, kind, connectors, optionalInt(entry, "capacity")));
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new GameDataException(where + ": " + e.getMessage(), e);
            }
        }
    }

    private Map<Direction, Connector> readConnectors(JsonNode entry, String where) {
        JsonNode node = required(entry, "connectors", where);
        Map<Direction, Connector> connectors = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            connectors.put(side, enumValue(Connector.class, text(node, side.name(), where), where));
        }
        return connectors;
    }

    // ---------------------------------------------------------------- cards

    private List<AdventureCardIdentity> readCards(Map<String, AdventureCard> playable) {
        List<AdventureCardIdentity> cards = new ArrayList<>();
        for (JsonNode entry : array(read(CARDS_FILE), "cards", CARDS_FILE)) {
            String id = text(entry, "id", CARDS_FILE);
            String where = CARDS_FILE + " entry " + id;
            try {
                AdventureCardIdentity identity = new AdventureCardIdentity(
                        id,
                        enumValue(AdventureCardType.class, text(entry, "type", where), where),
                        enumValue(CardLevel.class, text(entry, "level", where), where),
                        required(entry, "testFlight", where).asBoolean());
                cards.add(identity);
                playable.put(id, buildCard(identity, entry, where));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new GameDataException(where + ": " + e.getMessage(), e);
            }
        }
        return cards;
    }

    /**
     * Builds a card's rules from its printed values.
     *
     * <p>Every card type has rules, and a test asserts it: a card in the data with no
     * implementation would quietly do nothing when it was turned over. The switch is
     * exhaustive over {@code AdventureCardType} with no default, so adding a type to the
     * enum stops this compiling until somebody writes its card.
     *
     * @param identity the card's identity
     * @param entry    its data
     * @param where    where to say the fault is
     * @return the card's rules
     */
    private AdventureCard buildCard(AdventureCardIdentity identity, JsonNode entry, String where) {
        return switch (identity.type()) {
            case ABANDONED_SHIP -> new AbandonedShipCard(
                    identity,
                    integer(entry, "crewCost", where),
                    integer(entry, "credits", where),
                    integer(entry, "flightDays", where));
            case ABANDONED_STATION -> new AbandonedStationCard(
                    identity,
                    integer(entry, "minimumCrew", where),
                    readGoods(required(entry, "goods", where), where),
                    integer(entry, "flightDays", where));
            case SLAVERS -> new SlaversCard(
                    identity,
                    integer(entry, "firepower", where),
                    integer(entry, "credits", where),
                    integer(entry, "crewPenalty", where),
                    integer(entry, "flightDays", where));
            case SMUGGLERS -> new SmugglersCard(
                    identity,
                    integer(entry, "firepower", where),
                    readGoods(required(entry, "goods", where), where),
                    integer(entry, "goodsPenalty", where),
                    integer(entry, "flightDays", where));
            case PIRATES -> new PiratesCard(
                    identity,
                    integer(entry, "firepower", where),
                    integer(entry, "credits", where),
                    readThreats(array(entry, "shots", where), where),
                    integer(entry, "flightDays", where));
            case METEOR_SWARM -> new MeteorSwarmCard(
                    identity, readThreats(array(entry, "meteors", where), where));
            case PLANETS -> new PlanetsCard(
                    identity, readPlanets(array(entry, "planets", where), where),
                    integer(entry, "flightDays", where));
            case COMBAT_ZONE -> new CombatZoneCard(
                    identity, readCombatLines(array(entry, "lines", where), where));
            // Three cards carry no printed values at all: what they do is the same every time.
            case OPEN_SPACE -> new OpenSpaceCard(identity);
            case STARDUST -> new StardustCard(identity);
            case EPIDEMIC -> new EpidemicCard(identity);
        };
    }

    private static List<ThreatPattern> readThreats(JsonNode node, String where) {
        List<ThreatPattern> threats = new ArrayList<>();
        for (JsonNode threat : node) {
            threats.add(new ThreatPattern(
                    enumValue(HitKind.class, text(threat, "kind", where), where),
                    enumValue(Direction.class, text(threat, "from", where), where)));
        }
        return threats;
    }

    private static List<CombatLine> readCombatLines(JsonNode node, String where) {
        List<CombatLine> lines = new ArrayList<>();
        for (JsonNode line : node) {
            JsonNode penalty = required(line, "penalty", where);
            lines.add(new CombatLine(
                    enumValue(ShipAttribute.class, text(line, "attribute", where), where),
                    readCombatPenalty(penalty, where)));
        }
        return lines;
    }

    private static CombatPenalty readCombatPenalty(JsonNode node, String where) {
        String kind = text(node, "kind", where);
        return switch (kind) {
            case "FLIGHT_DAYS" -> new CombatPenalty.LoseFlightDays(integer(node, "value", where));
            case "CREW" -> new CombatPenalty.LoseCrew(integer(node, "value", where));
            case "GOODS" -> new CombatPenalty.LoseGoods(integer(node, "value", where));
            case "CANNON_FIRE" -> new CombatPenalty.TakeFire(
                    readThreats(array(node, "shots", where), where));
            default -> throw new GameDataException(where + ": '" + kind + "' is not a combat penalty");
        };
    }

    private static List<Map<GoodColor, Integer>> readPlanets(JsonNode node, String where) {
        List<Map<GoodColor, Integer>> planets = new ArrayList<>();
        for (JsonNode planet : node) {
            planets.add(readGoods(required(planet, "goods", where), where));
        }
        return planets;
    }

    private static Map<GoodColor, Integer> readGoods(JsonNode node, String where) {
        Map<GoodColor, Integer> goods = new EnumMap<>(GoodColor.class);
        for (GoodColor color : GoodColor.values()) {
            goods.put(color, integer(node, color.name(), where));
        }
        return goods;
    }

    // ---------------------------------------------------------------- boards

    private Map<GoodColor, Integer> readBankStock() {
        String where = BOARDS_FILE + " bank";
        JsonNode goods = required(required(read(BOARDS_FILE), "bank", BOARDS_FILE), "goods", where);
        Map<GoodColor, Integer> stock = new EnumMap<>(GoodColor.class);
        for (GoodColor color : GoodColor.values()) {
            stock.put(color, integer(goods, color.name(), where));
        }
        return stock;
    }

    private Map<GameLevel, LevelSpec> readLevels() {
        JsonNode levels = required(read(BOARDS_FILE), "levels", BOARDS_FILE);
        Map<GameLevel, LevelSpec> specs = new EnumMap<>(GameLevel.class);
        for (GameLevel level : GameLevel.values()) {
            String where = BOARDS_FILE + " level " + level;
            JsonNode node = required(levels, level.name(), BOARDS_FILE);
            try {
                specs.put(level, new LevelSpec(
                        level,
                        readFlightBoard(required(node, "flightBoard", where), where),
                        readShipBoard(required(node, "shipBoard", where), where),
                        readRules(required(node, "rules", where), where)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new GameDataException(where + ": " + e.getMessage(), e);
            }
        }
        return specs;
    }

    private FlightBoardSpec readFlightBoard(JsonNode node, String where) {
        List<Integer> starts = new ArrayList<>();
        for (JsonNode start : required(node, "startingPositions", where)) {
            starts.add(start.asInt());
        }
        return new FlightBoardSpec(
                integer(node, "routeLength", where),
                starts,
                integer(node, "hourglassSpaces", where),
                readRewards(required(node, "rewards", where), where),
                readDeck(required(node, "deck", where), where));
    }

    private RewardTable readRewards(JsonNode node, String where) {
        List<Integer> finishOrder = new ArrayList<>();
        for (JsonNode credits : required(node, "finishOrder", where)) {
            finishOrder.add(credits.asInt());
        }
        JsonNode prices = required(node, "goodsPrices", where);
        Map<GoodColor, Integer> goodsPrices = new EnumMap<>(GoodColor.class);
        for (GoodColor color : GoodColor.values()) {
            goodsPrices.put(color, integer(prices, color.name(), where));
        }
        return new RewardTable(
                finishOrder,
                integer(node, "prettiestShip", where),
                integer(node, "lostComponentPenalty", where),
                goodsPrices);
    }

    private DeckComposition readDeck(JsonNode node, String where) {
        JsonNode perPile = required(node, "cardsPerPile", where);
        Map<CardLevel, Integer> cardsPerPile = new LinkedHashMap<>();
        for (CardLevel level : CardLevel.values()) {
            if (perPile.has(level.name())) {
                cardsPerPile.put(level, perPile.get(level.name()).asInt());
            }
        }
        return new DeckComposition(
                integer(node, "piles", where),
                cardsPerPile,
                required(node, "testFlightCardsOnly", where).asBoolean());
    }

    private ShipBoardSpec readShipBoard(JsonNode node, String where) {
        Set<Position> forbidden = new LinkedHashSet<>();
        for (JsonNode cell : required(node, "forbidden", where)) {
            forbidden.add(readPosition(cell, where));
        }
        return new ShipBoardSpec(
                integer(node, "rows", where),
                integer(node, "columns", where),
                integer(node, "firstPrintedRow", where),
                integer(node, "firstPrintedColumn", where),
                readPosition(required(node, "startingCabin", where), where),
                integer(node, "reservationSlots", where),
                forbidden);
    }

    private Position readPosition(JsonNode node, String where) {
        return new Position(integer(node, "row", where), integer(node, "column", where));
    }

    private LevelRules readRules(JsonNode node, String where) {
        return new LevelRules(
                required(node, "hourglass", where).asBoolean(),
                required(node, "componentReservation", where).asBoolean(),
                required(node, "cardPilePeeking", where).asBoolean(),
                required(node, "aliens", where).asBoolean(),
                required(node, "illegalShipCreditPenalty", where).asBoolean());
    }

    // ---------------------------------------------------------------- parsing helpers

    private JsonNode read(String file) {
        try (InputStream stream = source.open(file)) {
            return mapper.readTree(stream);
        } catch (IOException e) {
            throw new GameDataException("could not read game data file " + file, e);
        }
    }

    private static JsonNode array(JsonNode parent, String field, String where) {
        JsonNode node = required(parent, field, where);
        if (!node.isArray()) {
            throw new GameDataException(where + ": '" + field + "' should be a list");
        }
        return node;
    }

    private static JsonNode required(JsonNode parent, String field, String where) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            throw new GameDataException(where + ": missing '" + field + "'");
        }
        return node;
    }

    private static String text(JsonNode parent, String field, String where) {
        return required(parent, field, where).asText();
    }

    private static int integer(JsonNode parent, String field, String where) {
        JsonNode node = required(parent, field, where);
        if (!node.canConvertToInt()) {
            throw new GameDataException(where + ": '" + field + "' should be a whole number, got " + node);
        }
        return node.asInt();
    }

    private static int optionalInt(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        return node == null || node.isNull() ? 0 : node.asInt();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String where) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new GameDataException(
                    where + ": '" + value + "' is not one of " + List.of(type.getEnumConstants()), e);
        }
    }
}

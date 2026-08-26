package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.GameLevel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Where the picture of a thing lives.
 *
 * <p>The game already names everything it owns — a tile is {@code battery_L-S}, a card is
 * {@code abandoned-ship_lvl2_01} — and the artwork is filed under those same names. So this is
 * a lookup and not a naming scheme: nothing here decides what a component is called, and a tile
 * added to the data with no picture is a missing file rather than a silent blank.
 *
 * <p>Kept apart from anything that draws, so that the question "is there a picture for every
 * component in the game?" can be asked by a test on a machine with no screen — which is what
 * the machine that builds this is.
 */
public final class Artwork {

    private static final String MANIFEST = "/data/assets.json";

    private final Map<String, String> tiles;
    private final Map<String, String> cards;

    private Artwork(Map<String, String> tiles, Map<String, String> cards) {
        this.tiles = Map.copyOf(tiles);
        this.cards = Map.copyOf(cards);
    }

    /**
     * Reads the manifest that ships with the game.
     *
     * @return the artwork
     * @throws UncheckedIOException if the manifest is missing or unreadable, which is a broken
     *                              build rather than something to carry on from
     */
    public static Artwork bundled() {
        try (InputStream source = Artwork.class.getResourceAsStream(MANIFEST)) {
            if (source == null) {
                throw new UncheckedIOException(
                        new IOException("no asset manifest at " + MANIFEST));
            }
            JsonNode manifest = new ObjectMapper().readTree(source);
            return new Artwork(section(manifest, "tiles"), section(manifest, "cards"));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the asset manifest could not be read", unreadable);
        }
    }

    /**
     * Returns the picture of a component.
     *
     * @param tileId the component's id, as the data and the projection both spell it
     * @return the resource path, or empty when nothing is filed under that name
     */
    public Optional<String> ofTile(String tileId) {
        return Optional.ofNullable(tiles.get(tileId));
    }

    /**
     * Returns the picture of an adventure card.
     *
     * @param cardId the card's id
     * @return the resource path, or empty when nothing is filed under that name
     */
    public Optional<String> ofCard(String cardId) {
        return Optional.ofNullable(cards.get(cardId));
    }

    /**
     * Returns the empty ship board a player builds on.
     *
     * <p>Not in the manifest because there is one per level and nothing needs to look it up by
     * name — the level is enough.
     *
     * @param level which game
     * @return the resource path
     */
    public String ofShipBoard(GameLevel level) {
        return "/assets/cardboard/ship-grid_lvl-" + printed(level) + ".jpg";
    }

    /**
     * Returns the route board the markers move along.
     *
     * @param level which game
     * @return the resource path
     */
    public String ofFlightBoard(GameLevel level) {
        return "/assets/cardboard/flight-board_lvl-" + printed(level) + ".png";
    }

    /**
     * How many components the game knows a picture for.
     *
     * @return the count
     */
    public int tileCount() {
        return tiles.size();
    }

    /**
     * How many adventure cards the game knows a picture for.
     *
     * @return the count
     */
    public int cardCount() {
        return cards.size();
    }

    /**
     * The number printed on the box for a level.
     *
     * <p>The test flight is board 1 and the full game is board 2. The artwork carries a third
     * that the rules we implement never reach.
     */
    private static String printed(GameLevel level) {
        return level == GameLevel.TEST_FLIGHT ? "1" : "2";
    }

    /**
     * Pulls one {@code "name": "path"} section out of the manifest.
     *
     * @param manifest the parsed manifest
     * @param name     which section
     * @return the names and their paths, empty when the section is not there
     */
    private static Map<String, String> section(JsonNode manifest, String name) {
        Map<String, String> found = new HashMap<>();
        JsonNode section = manifest.get(name);
        if (section == null) {
            return found;
        }
        section.fields().forEachRemaining(entry ->
                found.put(entry.getKey(), entry.getValue().asText()));
        return found;
    }
}

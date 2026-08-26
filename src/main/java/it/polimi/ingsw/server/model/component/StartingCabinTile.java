package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.PlayerColor;
import java.util.Map;

/**
 * The cabin a player's ship is built around.
 *
 * <p>Kept apart from {@link ComponentTile} because it behaves differently in the two
 * ways that matter: it is handed out at setup rather than drawn from the pool, and it
 * can never house an alien — the manual's explanation being that the paint smells odd
 * (p.18).
 *
 * @param id          the tile's identifier, taken from its artwork file name
 * @param playerColor the colour this cabin belongs to
 * @param connectors  what each of the four printed sides carries
 */
public record StartingCabinTile(String id, PlayerColor playerColor, Map<Direction, Connector> connectors)
        implements Tile {

    /**
     * Validates the cabin and takes a defensive copy of its connectors.
     *
     * @throws IllegalArgumentException if the identifier is blank or a side is missing
     * @throws NullPointerException     if the colour or the connector map is {@code null}
     */
    public StartingCabinTile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a starting cabin needs an identifier");
        }
        if (playerColor == null) {
            throw new NullPointerException(id + ": a starting cabin belongs to a player colour");
        }
        connectors = ComponentTile.sidesOf(id, connectors);
    }

    /**
     * Returns what this cabin is, for code that treats every welded piece alike.
     *
     * @return always {@link ComponentKind#STARTING_CABIN}
     */
    @Override
    public ComponentKind kind() {
        return ComponentKind.STARTING_CABIN;
    }

    /**
     * Returns the capacity printed on this piece.
     *
     * <p>A cabin holds crew rather than slots or charges, so it has none. How many
     * people fit is a rule, not a printed number.
     *
     * @return always zero
     */
    @Override
    public int capacity() {
        return 0;
    }
}

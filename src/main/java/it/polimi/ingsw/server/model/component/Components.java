package it.polimi.ingsw.server.model.component;

import java.util.Arrays;

/**
 * Shared guards for the {@link ShipComponent} variants.
 *
 * <p>Every variant checks on construction that it was handed a tile of a kind it can
 * actually represent. Without that, {@link ShipComponent#place} could be bypassed and a
 * cabin could end up wrapped as a cannon, which would fail much later and much less
 * clearly.
 */
final class Components {

    private Components() {
        // Guard holder, never instantiated.
    }

    /**
     * Checks that a tile is one of the kinds a variant accepts.
     *
     * @param tile     the tile being wrapped
     * @param accepted the kinds this variant represents
     * @throws IllegalArgumentException if the tile is of any other kind
     * @throws NullPointerException     if the tile is {@code null}
     */
    static void require(Tile tile, ComponentKind... accepted) {
        if (tile == null) {
            throw new NullPointerException("a ship component needs a tile");
        }
        if (!Arrays.asList(accepted).contains(tile.kind())) {
            throw new IllegalArgumentException(
                    tile.id() + " is a " + tile.kind() + ", not one of " + Arrays.toString(accepted));
        }
    }
}

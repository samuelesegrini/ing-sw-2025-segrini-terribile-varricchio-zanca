package it.polimi.ingsw.server.model.goods;

/**
 * The four colours of trade goods, ordered from most to least valuable.
 *
 * <p>Ordering is load-bearing: a card that takes goods always takes the most valuable
 * ones first (manual p.11), so the declaration order is the order of loss.
 */
public enum GoodColor {

    /** Hazardous material. Only a special hold will take it. Worth four credits. */
    RED,

    /** Worth three credits. */
    YELLOW,

    /** Worth two credits. */
    GREEN,

    /** Worth one credit. */
    BLUE;

    /**
     * Tells whether a cube of this colour needs a special cargo hold.
     *
     * @return {@code true} only for {@link #RED}
     */
    public boolean requiresSpecialHold() {
        return this == RED;
    }
}

package it.polimi.ingsw.common.game;

/**
 * The kinds of adventure a flight can run into.
 *
 * <p>Each type is specified in {@code docs/specs/game-rules.md} section 8. The eight
 * that appear in the test flight are those {@link #inTestFlight()} accepts.
 *
 * <p>Sabotage is deliberately absent: it is a level III card, and level III is out of
 * scope by {@code requirements.pdf} section 2.1. The manual describes it next to
 * Epidemic on page 19, which makes the two easy to confuse.
 */
public enum AdventureCardType {

    /** Two to four planets to land on, each with its own goods. */
    PLANETS,

    /** Trade crew for credits. Only the first taker benefits. */
    ABANDONED_SHIP,

    /** Trade flight days for goods, if the crew is large enough. Costs no crew. */
    ABANDONED_STATION,

    /** An enemy that pays in goods. */
    SMUGGLERS,

    /** An enemy that pays in credits and fires on whoever loses. */
    PIRATES,

    /** An enemy that pays in credits and takes crew from whoever loses. */
    SLAVERS,

    /** Declare engine power and advance that many empty spaces. */
    OPEN_SPACE,

    /** Meteors from listed directions, resolved against every ship at once. */
    METEOR_SWARM,

    /** Three lines, each penalising whoever is weakest in one attribute. */
    COMBAT_ZONE,

    /** One flight day lost per exposed connector. */
    STARDUST,

    /** One crew member lost from every occupied cabin joined to another occupied cabin. */
    EPIDEMIC;

    /**
     * Tells whether this type appears among the eight cards of the test flight deck.
     *
     * <p>Manual pages 12 and 13 describe exactly these eight; the rest are introduced
     * with the complete game on page 19.
     *
     * @return {@code true} if a card of this type can turn up in a test flight
     */
    public boolean inTestFlight() {
        return this != PIRATES && this != SLAVERS && this != EPIDEMIC;
    }
}

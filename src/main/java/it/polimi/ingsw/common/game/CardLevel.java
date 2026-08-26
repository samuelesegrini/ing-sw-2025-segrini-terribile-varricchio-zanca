package it.polimi.ingsw.common.game;

/**
 * The difficulty band printed on an adventure card, shown as rocket flames.
 *
 * <p>Distinct from the flight level: a level II flight draws from both card levels,
 * one level I card and two level II cards in each of its four piles (manual p.16).
 * Level III cards exist in the physical game but no flight in scope uses them.
 */
public enum CardLevel {

    /** One flame. Eight of these also carry the L mark and make up the test flight deck. */
    LEVEL_I,

    /** Two flames. */
    LEVEL_II
}

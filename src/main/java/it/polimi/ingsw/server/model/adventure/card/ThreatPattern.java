package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.HitKind;

/**
 * One threat printed on a card: what it is and which side it comes from.
 *
 * <p>The line it lands on is not printed — that is what the dice decide, once, for every
 * ship the threat applies to.
 *
 * <p>Shared by the three cards that send things at ships. The direction convention was
 * checked against the artwork: a meteor drawn with its flame trailing upward is arriving
 * from the bow, and the level I combat zone's shots from below match the manual's
 * "provenienti da dietro". Getting that backwards would invert every meteor in the game.
 *
 * @param kind what is coming
 * @param from the side it arrives from
 */
public record ThreatPattern(HitKind kind, Direction from) {

    /**
     * Validates the threat.
     *
     * @throws NullPointerException if either part is {@code null}
     */
    public ThreatPattern {
        if (kind == null || from == null) {
            throw new NullPointerException("a threat needs a kind and a direction");
        }
    }

    /**
     * Tells whether this is a meteor rather than cannon fire.
     *
     * @return {@code true} for either size of meteor
     */
    public boolean isMeteor() {
        return kind == HitKind.SMALL_METEOR || kind == HitKind.BIG_METEOR;
    }
}

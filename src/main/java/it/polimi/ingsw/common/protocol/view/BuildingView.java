package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.PlayerColor;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The shipyard, as it stands.
 *
 * <p>Almost all of this is public: the heap, the discard pile, who has finished, which
 * start spaces are still free. Two things are not, and they are the reason this view is
 * built per recipient rather than once for everybody — the tile in a player's hand, and
 * the cards they have peeked at. Sending either to the table would give away information
 * the manual keeps private (p.7, p.17).
 *
 * @param faceDownRemaining how many tiles are still face down on the table
 * @param faceUpPile        the tiles lying face up, which anyone may take
 * @param hand              the tile <em>this</em> player is holding, {@code null} when their hands are empty
 * @param scouted           the pile <em>this</em> player is looking at, empty unless they are
 * @param hourglassSpace    which space the timer is on, {@code null} when the level has no timer
 * @param hourglassSpaces   how many spaces the timer has, zero when the level has none
 * @param secondsRemaining  what is left of the current turn of the glass, zero when it is not running
 * @param finished          who has declared their ship done
 * @param freeStartSpaces   the start spaces nobody has claimed
 */
public record BuildingView(int faceDownRemaining, List<TileView> faceUpPile, TileView hand,
                           List<AdventureCardIdentity> scouted, Integer hourglassSpace,
                           int hourglassSpaces, long secondsRemaining, Set<PlayerColor> finished,
                           List<Integer> freeStartSpaces) implements Serializable {

    /**
     * Takes defensive copies of the collections.
     *
     * @throws IllegalArgumentException if a count is negative
     */
    public BuildingView {
        if (faceDownRemaining < 0 || hourglassSpaces < 0 || secondsRemaining < 0) {
            throw new IllegalArgumentException("the shipyard cannot hold a negative amount of anything");
        }
        faceUpPile = List.copyOf(faceUpPile);
        scouted = List.copyOf(scouted);
        finished = Set.copyOf(finished);
        freeStartSpaces = List.copyOf(freeStartSpaces);
    }

    /**
     * Returns the tile this player is holding.
     *
     * @return the tile in hand, or empty when their hands are free
     */
    public Optional<TileView> handIfAny() {
        return Optional.ofNullable(hand);
    }
}

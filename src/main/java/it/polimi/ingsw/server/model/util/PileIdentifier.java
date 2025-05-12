package it.polimi.ingsw.server.model.util;

import it.polimi.ingsw.server.model.enums.GameLevel;
import java.util.Arrays;

/**
 * Identifies the different piles of components or cards on the game board.
 * Each pile has a position and can be either predictable (contents known) or unpredictable.
 */
public enum PileIdentifier {
    // Predictable piles (contents known in advance)
    BOTTOM_LEFT(0, true),
    BOTTOM_CENTER(1, true),
    BOTTOM_RIGHT(2, true),
    
    // Special value for unidentified piles
    UNKNOWN(-1, false);
    
    private final int index;
    private final boolean predictable;

    /**
     * Creates a new pile identifier.
     *
     * @param index The numeric index of this pile
     * @param predictable Whether the pile's contents are predictable
     */
    PileIdentifier(int index, boolean predictable) {
        this.index = index;
        this.predictable = predictable;
    }

    /**
     * Gets the numeric index of this pile.
     *
     * @return The pile's index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Determines if this pile has predictable contents.
     *
     * @return true if the pile's contents are predictable, false otherwise
     */
    public boolean isPredictable() {
        return predictable;
    }

    /**
     * Gets the pile identifier corresponding to a specific index.
     *
     * @param index The index to look up
     * @return The matching pile identifier or UNKNOWN if no match
     */
    public static PileIdentifier fromIndex(int index) {
        for (PileIdentifier pile : values()) {
            if (pile.index == index) {
                return pile;
            }
        }
        return UNKNOWN;
    }

    /**
     * Gets all predictable piles available for a specific game level.
     *
     * @param level The game difficulty level
     * @return Array of predictable pile identifiers
     */
    public static PileIdentifier[] getPredictablePiles(GameLevel level) {
        return Arrays.stream(values())
                    .filter(PileIdentifier::isPredictable)
                    .toArray(PileIdentifier[]::new);
    }
}
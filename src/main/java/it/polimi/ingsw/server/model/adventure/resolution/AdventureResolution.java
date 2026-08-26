package it.polimi.ingsw.server.model.adventure.resolution;

import java.util.Optional;

/**
 * An adventure card being resolved, one decision at a time.
 *
 * <p>A controller drives this without knowing which card it is: ask what is
 * {@link #pending()}, hand back an answer with {@link #submit}, repeat until nothing is
 * pending. Cards that need no input from anybody resolve on construction and report
 * nothing pending from the start.
 *
 * <p>This is deliberately not a visitor. A visitor over eleven card types concentrates
 * every card's logic in one class — the previous implementation's grew to 1147 lines —
 * and makes adding a card an edit to a switch that already handles ten other things.
 * Here a card owns its own resolution, so adding one means adding a file.
 */
public interface AdventureResolution {

    /**
     * Returns the decision the card is waiting on.
     *
     * @return the outstanding prompt, or empty when the card is finished
     */
    Optional<PlayerPrompt> pending();

    /**
     * Applies a player's answer and advances to the next decision.
     *
     * @param choice the player's answer
     * @throws IllegalStateException    if the card is already finished
     * @throws IllegalArgumentException if the answer is from the wrong player, or does not
     *                                  answer the outstanding question
     */
    void submit(PlayerChoice choice);

    /**
     * Tells whether the card is done.
     *
     * @return {@code true} when nothing is pending
     */
    default boolean isComplete() {
        return pending().isEmpty();
    }
}

package it.polimi.ingsw.server.model.adventure.resolution;

import java.util.Optional;

/**
 * A card that asks nobody anything.
 *
 * <p>Stardust counts exposed connectors, Epidemic empties one bunk in every joined cabin.
 * Neither offers a choice, so both simply happen the moment the card is turned over.
 *
 * <p>Like {@link TurnByTurnResolution}, the work is done the first time the resolution is
 * asked what is pending rather than when it is constructed. That keeps one rule for every
 * card: building a resolution is free, and turning the card over is the first question.
 */
public abstract class AutomaticResolution implements AdventureResolution {

    private boolean applied;

    /**
     * Applies the card's effects. Called exactly once.
     */
    protected abstract void apply();

    @Override
    public final Optional<PlayerPrompt> pending() {
        if (!applied) {
            applied = true;
            apply();
        }
        return Optional.empty();
    }

    @Override
    public final void submit(PlayerChoice choice) {
        throw new IllegalStateException("this card asks nobody anything, so there is nothing to answer");
    }
}

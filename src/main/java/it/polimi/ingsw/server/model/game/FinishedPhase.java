package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;

import java.util.Optional;

/**
 * Nothing more will happen.
 *
 * <p>A phase rather than a null, so that a command arriving after the end is refused with a
 * sentence like every other refusal instead of falling off the end of a state machine.
 */
final class FinishedPhase implements Phase {

    @Override
    public GamePhase name() {
        return GamePhase.FINISHED;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        return new Reaction.Refused("this game is over");
    }

    @Override
    public Optional<Phase> next() {
        return Optional.empty();
    }
}

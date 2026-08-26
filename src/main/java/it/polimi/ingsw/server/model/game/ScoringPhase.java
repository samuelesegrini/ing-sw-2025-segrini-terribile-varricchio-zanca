package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.server.model.flight.FlightScorer;

import java.util.Optional;

/**
 * Settling the ledger, which takes no input from anybody.
 *
 * <p>A phase all the same, rather than something the flight does on its way out, because the
 * scoring rules are worth a place of their own and because there is exactly one moment at
 * which they may run. It accepts nothing: by this point every decision has been made and the
 * only thing left is arithmetic.
 */
final class ScoringPhase implements Phase {

    private final Game game;

    ScoringPhase(Game game) {
        this.game = game;
        game.settle(FlightScorer.settle(game.flight()));
    }

    @Override
    public GamePhase name() {
        return GamePhase.SCORING;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        return new Reaction.Refused("the flight is over and the credits are being counted");
    }

    @Override
    public Optional<Phase> next() {
        return Optional.of(new FinishedPhase());
    }
}

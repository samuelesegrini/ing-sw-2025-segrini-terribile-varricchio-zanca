package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.Optional;

/**
 * Pulling the illegal parts off, and choosing which half to keep when that breaks a ship.
 *
 * <p>Everybody works on their own ship at once, so again there are no turns. A player whose
 * ship was legal to begin with has nothing to do here, and the phase does not wait for them
 * to say so.
 *
 * <p>What makes this phase awkward is that the fix can make things worse: removing a component
 * can leave a ship in two pieces, and the manual then has the player choose one and lose the
 * rest (p.10). So "am I finished" is not "is my ship legal" but "is my ship legal <em>and</em>
 * in one piece", and a player can be legal, then broken, then legal again.
 */
final class ValidationPhase implements Phase {

    private final Game game;

    ValidationPhase(Game game) {
        this.game = game;
    }

    @Override
    public GamePhase name() {
        return GamePhase.VALIDATION;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        Ship ship = game.ships().get(player);
        try {
            return switch (command) {
                case PreparationCommand.RemoveComponent remove -> {
                    // Free in the trial flight, a credit each in the full game. The board
                    // says which, rather than this switching on the level.
                    boolean charged = game.level().shipBoard().chargesForCorrections();
                    if ((charged ? ship.discard(remove.cell()) : ship.correct(remove.cell()))
                            .isEmpty()) {
                        yield new Reaction.Refused("there is nothing welded at " + remove.cell());
                    }
                    yield Reaction.Accepted.quietly();
                }
                case PreparationCommand.KeepPiece keep -> {
                    if (ship.isWhole()) {
                        yield new Reaction.Refused("this ship is in one piece; there is nothing to choose");
                    }
                    ship.keepFragment(keep.piece());
                    yield Reaction.Accepted.quietly();
                }
                default -> new Reaction.Refused("the ships are being checked over");
            };
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
    }

    @Override
    public Optional<Phase> next() {
        boolean everyShipCanFly = game.ships().values().stream()
                .allMatch(ship -> ship.isWhole() && ship.validate().isLegal());
        return everyShipCanFly ? Optional.of(new CrewPhase(game)) : Optional.empty();
    }
}

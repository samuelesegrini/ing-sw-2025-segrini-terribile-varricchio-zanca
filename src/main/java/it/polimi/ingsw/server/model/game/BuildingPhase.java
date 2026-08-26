package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.server.model.building.ShipBuilder;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The shipyard: everybody building at once, and nobody waiting their turn.
 *
 * <p>This is the one phase with no order of play at all (manual p.5). Commands arrive
 * interleaved from every player and each is answered on its own, which is why the aggregate
 * being single-threaded matters here more than anywhere else: two players reaching for the
 * same face-up tile is resolved by which command arrived first, and never by which thread
 * happened to run.
 *
 * <p><b>An illegal action is refused by the model, not re-checked here.</b> {@link ShipBuilder}
 * already knows that a player cannot weld what they are not holding, and says so in a sentence
 * worth reading. Restating those rules here would be writing them twice and getting one of
 * them wrong; instead the exception is turned into a refusal. That works because the builder
 * checks before it changes anything, so a refused command leaves the shipyard as it was.
 */
final class BuildingPhase implements Phase {

    private final Game game;

    BuildingPhase(Game game) {
        this.game = game;
    }

    @Override
    public GamePhase name() {
        return GamePhase.BUILDING;
    }

    @Override
    public Reaction apply(PlayerColor player, Command command) {
        if (!(command instanceof BuildingCommand building)) {
            return new Reaction.Refused("the ships are still being built");
        }
        ShipBuilder builder = game.builders().get(player);
        if (builder.hasFinished() && !(command instanceof BuildingCommand.FlipTimer)) {
            // Turning the glass is the one thing left to do once a ship is finished, and the
            // manual gives that job specifically to players who have finished (p.17).
            return new Reaction.Refused("this ship is finished and is on the starting line");
        }
        try {
            return run(player, builder, building);
        } catch (RuntimeException refused) {
            return new Reaction.Refused(Reasons.from(refused));
        }
    }

    private Reaction run(PlayerColor player, ShipBuilder builder, BuildingCommand command) {
        return switch (command) {
            case BuildingCommand.DrawFromPool ignored -> done(() -> builder.drawFaceDown());
            case BuildingCommand.TakeFaceUp take -> done(() -> builder.takeFaceUp(take.tileId()));
            case BuildingCommand.TakeReserved take -> done(() -> builder.takeReserved(take.tileId()));
            case BuildingCommand.ReturnToPool ignored -> done(builder::returnToPool);
            case BuildingCommand.Reserve ignored -> done(builder::reserve);
            case BuildingCommand.PlaceInHand place ->
                    done(() -> builder.attach(place.cell(), place.rotation()));
            case BuildingCommand.AdjustPlacement adjust ->
                    done(() -> builder.adjust(adjust.cell(), adjust.rotation()));
            case BuildingCommand.Weld ignored -> {
                if (builder.unweldedCell().isEmpty()) {
                    // The builder treats this as harmless, because finishing welds whatever
                    // happens to be down. From a client it is a command that did nothing, and
                    // a command that does nothing has to say so rather than look like it
                    // worked.
                    yield new Reaction.Refused("there is nothing waiting to be welded");
                }
                yield done(builder::weld);
            }
            case BuildingCommand.ScoutPile scout -> done(() ->
                    game.peeked().put(player, List.copyOf(builder.scout(scout.pile()))));
            case BuildingCommand.PutPileBack ignored -> done(() -> {
                builder.putPileBack();
                game.peeked().remove(player);
            });
            case BuildingCommand.FlipTimer ignored ->
                    done(() -> game.timer().flip(builder.hasFinished()));
            case BuildingCommand.FinishBuilding finish -> done(() -> finish(player, builder, finish));
        };
    }

    private static Reaction done(Runnable action) {
        action.run();
        return Reaction.Accepted.quietly();
    }

    private void finish(PlayerColor player, ShipBuilder builder, BuildingCommand.FinishBuilding finish) {
        builder.finish();
        game.peeked().remove(player);
        // The claim happens after the builder has accepted the finish, so a ship that cannot
        // be declared done does not take a place on the starting line on its way to being
        // refused.
        game.starts().claim(player, finish.startSpaceIfAny()
                .map(OptionalInt::of)
                .orElseGet(OptionalInt::empty));
    }

    @Override
    public Optional<Phase> next() {
        boolean everyoneDone = game.builders().values().stream().allMatch(ShipBuilder::hasFinished);
        if (!everyoneDone && !game.timer().isBuildingOver()) {
            return Optional.empty();
        }
        if (!everyoneDone) {
            // The last of the glass has run out. Whoever is still building stops where they
            // are, holding whatever they were holding (p.17) — which the builder turns into a
            // returned tile or a reservation, and either way charges them for it.
            forceEverybodyToStop();
        }
        return Optional.of(new ValidationPhase(game));
    }

    private void forceEverybodyToStop() {
        game.colours().forEach(player -> {
            ShipBuilder builder = game.builders().get(player);
            if (!builder.hasFinished()) {
                builder.finish();
                game.peeked().remove(player);
                game.starts().claim(player, OptionalInt.empty());
            }
        });
    }
}

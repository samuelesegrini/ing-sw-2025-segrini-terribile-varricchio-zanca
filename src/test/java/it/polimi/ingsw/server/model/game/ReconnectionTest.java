package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a game does about somebody who is not there.
 *
 * <p>Before this, nothing. The disconnection was recorded, shown in everybody's view, and
 * described in three comments as a turn being skipped — and then the game sat waiting for an
 * answer that was never going to come. One dropped laptop froze a table of four for good.
 *
 * <p>The rules here are deliberately dull, because a rule applied on somebody's behalf while
 * they cannot argue has to be one they would not argue with: take nothing, spend nothing, risk
 * nothing. What it must never be is <em>nothing at all</em>.
 */
class ReconnectionTest {

    private static final GameData DATA = GameDataLoader.loadBundled();
    private static final Instant START = Instant.parse("2026-08-27T10:00:00Z");

    /** A clock somebody can push forward, for the waiting. */
    private static final class Movable implements InstantSource {

        private Instant now = START;

        @Override
        public Instant instant() {
            return now;
        }

        void moveOn(Duration by) {
            now = now.plus(by);
        }
    }

    /**
     * A phase that overrides nothing, standing in for the next one somebody writes.
     *
     * <p>Deliberately minimal: the only thing under test is what {@link Phase} does by
     * default when it is asked to finish itself for a player who is not there.
     */
    private static final class Unattended implements Phase {

        @Override
        public GamePhase name() {
            return GamePhase.VALIDATION;
        }

        @Override
        public Reaction apply(PlayerColor player, Command command) {
            return new Reaction.Refused("nothing happens here");
        }

        @Override
        public Optional<Phase> next() {
            return Optional.empty();
        }
    }

    private final Movable clock = new Movable();

    private Game fourPlayers(Duration soloTimeout) {
        return Game.create("game-1", GameLevel.LEVEL_II,
                List.of(new Seat("samuele", PlayerColor.BLUE),
                        new Seat("chiara", PlayerColor.GREEN),
                        new Seat("marco", PlayerColor.RED),
                        new Seat("giulia", PlayerColor.YELLOW)),
                DATA, new Random(20260827L), clock, soloTimeout);
    }

    /** Ticks until nothing more happens, collecting everything the game said. */
    private static List<Event> settle(Game game) {
        List<Event> told = new ArrayList<>();
        for (int tick = 0; tick < 40; tick++) {
            List<Event> fresh = game.tick();
            if (fresh.isEmpty()) {
                return told;
            }
            told.addAll(fresh);
        }
        return told;
    }

    /** Gets everybody airborne, with all four still connected. */
    private Game aFlightInProgress() {
        Game game = fourPlayers(Duration.ofMinutes(2));
        for (PlayerColor player : List.of(PlayerColor.BLUE, PlayerColor.GREEN,
                PlayerColor.RED, PlayerColor.YELLOW)) {
            game.apply(player, new BuildingCommand.FinishBuilding(null));
        }
        settle(game);
        for (PlayerColor player : List.of(PlayerColor.BLUE, PlayerColor.GREEN,
                PlayerColor.RED, PlayerColor.YELLOW)) {
            game.apply(player, new it.polimi.ingsw.common.protocol.PreparationCommand
                    .FinishPreparation());
        }
        settle(game);
        return game;
    }

    @Nested
    @DisplayName("a turn nobody is there to take")
    class Skipping {

        @Test
        @DisplayName("disconnectedPlayer_hasTheirPromptSkipped")
        void disconnectedPlayerHasTheirPromptSkipped() {
            Game game = aFlightInProgress();
            assertEquals(GamePhase.FLIGHT, game.phase(), "the fleet should have launched");

            // Whoever the first card asks, take their laptop away.
            PlayerColor asked = game.viewFor(PlayerColor.BLUE).pendingIfAny()
                    .orElseThrow(() -> new AssertionError("no card asked anybody anything"))
                    .player();
            game.connectionChanged(asked, false);

            List<Event> told = settle(game);

            assertTrue(told.stream().anyMatch(event ->
                            event instanceof GameEvent.TurnSkipped skipped
                                    && skipped.player() == asked),
                    "the game should have answered for them, and said so");
            assertTrue(game.viewFor(PlayerColor.BLUE).pendingIfAny()
                            .map(prompt -> prompt.player() != asked)
                            .orElse(true),
                    "and it should not still be waiting on somebody who is not there");
        }

        @Test
        @DisplayName("a whole table of absentees still reaches the end rather than stalling")
        void everybodyAway() {
            // The worst case, and the one the old code turned into a permanent stall.
            Game game = aFlightInProgress();
            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));

            settle(game);

            assertNotEquals(GamePhase.BUILDING, game.phase());
            assertTrue(game.viewFor(PlayerColor.BLUE).pendingIfAny()
                            .map(prompt -> prompt.player() == PlayerColor.BLUE)
                            .orElse(true),
                    "the only question left should be for the only player still here");
        }

        @Test
        @DisplayName("an absent builder stops building where they stand, so the yard can close")
        void skippingTheShipyard() {
            Game game = fourPlayers(Duration.ofMinutes(2));
            assertEquals(GamePhase.BUILDING, game.phase());

            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));
            game.apply(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            settle(game);

            assertNotEquals(GamePhase.BUILDING, game.phase(),
                    "a shipyard that never closes is a game that never starts");
        }

        @Test
        @DisplayName("and is stopped once, however many times the clock asks")
        void theShipyardClosesOncePerPlayer() {
            Game game = fourPlayers(Duration.ofMinutes(2));
            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));
            game.apply(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            long stopped = settle(game).stream()
                    .filter(event -> event instanceof GameEvent.TurnSkipped skipped
                            && skipped.what().equals("stopped building where they were"))
                    .count();

            // The tick runs many times over; a phase that reported closing the yard on every
            // one of them would narrate the same fact to four clients until the glass ran out.
            assertEquals(3, stopped,
                    "each absent builder is stopped once, not once per tick");
        }
    }

    @Nested
    @DisplayName("finishing a phase on somebody's behalf")
    class FinishingForSomebody {

        @Test
        @DisplayName("a phase that has not said what it means refuses, rather than being skipped")
        void theDefaultIsToRefuse() {
            Reaction answer = new Unattended().finishFor(PlayerColor.BLUE);

            // The point of the default. A phase added later that genuinely can be finished for
            // an absent player has to say so; one that says nothing is not quietly treated as
            // though it had been dealt with.
            assertInstanceOf(Reaction.Refused.class, answer,
                    "silence is not consent to skip a phase");
        }

        @Test
        @DisplayName("and says why, because a refusal that does not is not readable")
        void theRefusalSaysWhy() {
            Reaction answer = new Unattended().finishFor(PlayerColor.BLUE);

            assertFalse(((Reaction.Refused) answer).reason().isBlank());
        }
    }

    @Nested
    @DisplayName("coming back")
    class ComingBack {

        @Test
        @DisplayName("rejoiningWithTheSameNickname_restoresFullState")
        void rejoiningWithTheSameNicknameRestoresFullState() {
            Game game = aFlightInProgress();
            game.connectionChanged(PlayerColor.GREEN, false);
            settle(game);

            GameView whileAway = game.viewFor(PlayerColor.GREEN);
            assertFalse(whileAway.players().stream()
                            .filter(player -> player.colour() == PlayerColor.GREEN)
                            .findFirst().orElseThrow().connected(),
                    "they should be shown as away");

            game.connectionChanged(PlayerColor.GREEN, true);
            GameView back = game.viewFor(PlayerColor.GREEN);

            assertTrue(back.players().stream()
                            .filter(player -> player.colour() == PlayerColor.GREEN)
                            .findFirst().orElseThrow().connected());
            assertEquals(game.phase(), back.phase(), "the whole picture, not a fragment of it");
            assertEquals(4, back.players().size());
            assertNotEquals(0, back.players().stream()
                    .filter(player -> player.colour() == PlayerColor.GREEN)
                    .findFirst().orElseThrow().ship().outline().size(),
                    "including their own ship");
        }
    }

    @Nested
    @DisplayName("the last player standing")
    class Alone {

        @Test
        @DisplayName("a game with one player left says it is waiting, and for how long")
        void suspends() {
            Game game = aFlightInProgress();

            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));
            List<Event> told = settle(game);

            assertTrue(told.stream().anyMatch(event ->
                            event instanceof GameEvent.GameSuspended suspended
                                    && suspended.secondsRemaining() == 120),
                    "the wait should be announced, with a number on it: " + told);
        }

        @Test
        @DisplayName("and stops waiting the moment somebody comes back")
        void resumes() {
            Game game = aFlightInProgress();
            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));
            settle(game);

            game.connectionChanged(PlayerColor.RED, true);
            List<Event> told = settle(game);

            assertTrue(told.stream().anyMatch(event -> event instanceof GameEvent.GameResumed),
                    "coming back should be worth saying: " + told);
        }

        @Test
        @DisplayName("soloTimeout_awardsTheWin")
        void soloTimeoutAwardsTheWin() {
            Game game = aFlightInProgress();
            List.of(PlayerColor.GREEN, PlayerColor.RED, PlayerColor.YELLOW)
                    .forEach(player -> game.connectionChanged(player, false));
            settle(game);

            clock.moveOn(Duration.ofMinutes(3));
            settle(game);

            assertEquals(GamePhase.FINISHED, game.phase(), "the game should be over");
            List<it.polimi.ingsw.common.game.ScoreSheet> scores =
                    game.viewFor(PlayerColor.BLUE).scoresIfAny().orElseThrow();
            assertEquals(PlayerColor.BLUE, scores.get(0).player(),
                    "the player who was still there should have won: " + scores);
            assertTrue(scores.get(0).finishedTheFlight(),
                    "and they finished, because they were the only ship still flying");
        }

        @Test
        @DisplayName("the wait is a number somebody chose, not one baked into the game")
        void theTimeoutIsConfigurable() {
            Game impatient = Game.create("game-2", GameLevel.LEVEL_II,
                    List.of(new Seat("samuele", PlayerColor.BLUE),
                            new Seat("chiara", PlayerColor.GREEN)),
                    DATA, new Random(20260827L), clock, Duration.ofSeconds(30));

            impatient.connectionChanged(PlayerColor.GREEN, false);
            List<Event> told = settle(impatient);

            assertTrue(told.stream().anyMatch(event ->
                            event instanceof GameEvent.GameSuspended suspended
                                    && suspended.secondsRemaining() == 30),
                    "a game told to wait thirty seconds should say thirty: " + told);
        }

        @Test
        @DisplayName("a game still full waits for nobody")
        void nobodyIsMissing() {
            Game game = aFlightInProgress();

            List<Event> told = settle(game);

            assertFalse(told.stream().anyMatch(event -> event instanceof GameEvent.GameSuspended),
                    "nothing was wrong and the game announced a problem anyway");
        }
    }
}

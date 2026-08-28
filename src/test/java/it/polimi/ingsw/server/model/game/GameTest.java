package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the aggregate lets happen exactly what the phase allows, and nothing else.
 *
 * <p>The failures worth catching here are the quiet ones: a command that belongs to another
 * phase being applied anyway, a refused command that changed something on its way to being
 * refused, and a projection that shows one player what another is holding.
 *
 * <p>Components involved: {@link Game}, {@link PlayerColor}, {@link BuildingCommand}.
 */
class GameTest {

    private static final Position BESIDE_THE_CABIN = new Position(2, 4);

    private static Reaction send(Game game, PlayerColor player, Command command) {
        return game.apply(player, command);
    }

    private static void accepted(Game game, PlayerColor player, Command command) {
        Reaction reaction = send(game, player, command);
        assertInstanceOf(Reaction.Accepted.class, reaction,
                () -> command + " was refused: " + reaction);
    }

    private static String refused(Game game, PlayerColor player, Command command) {
        Reaction reaction = send(game, player, command);
        return assertInstanceOf(Reaction.Refused.class, reaction,
                () -> command + " was accepted, and should not have been").reason();
    }

    @Nested
    @DisplayName("opening")
    class Opening {

        @Test
        @DisplayName("a new game is a shipyard")
        void gamesStartInTheShipyard() {
            Game game = Games.levelTwo();

            assertEquals(GamePhase.BUILDING, game.phase());
            assertEquals(2, game.seats().size());
            assertTrue(game.isConnected(PlayerColor.RED));
        }

        @Test
        @DisplayName("a table has two to four places")
        void impossibleTables() {
            assertEquals("a game seats two to four players, not 1",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                            () -> Game.create("g", it.polimi.ingsw.common.game.GameLevel.LEVEL_II,
                                    List.of(new Seat("alone", PlayerColor.RED)),
                                    it.polimi.ingsw.server.data.GameDataLoader.loadBundled(),
                                    new java.util.Random(1), new Games.Hand())).getMessage());
        }

        @Test
        @DisplayName("somebody who is not playing is not playing")
        void strangers() {
            Game game = Games.levelTwo();

            assertEquals("there is no GREEN player in this game",
                    refused(game, PlayerColor.GREEN, new BuildingCommand.DrawFromPool()));
        }
    }

    @Nested
    @DisplayName("the shipyard")
    class Shipyard {

        @Test
        @DisplayName("a command from another phase is refused, and says which phase this is")
        void theWrongPhase() {
            Game game = Games.levelTwo();

            assertEquals("the ships are still being built",
                    refused(game, PlayerColor.RED, new FlightCommand.GiveUp()));
            assertEquals("the ships are still being built",
                    refused(game, PlayerColor.RED, new LobbyCommand.ListGames()));
        }

        @Test
        @DisplayName("draw, put down, turn, weld")
        void buildingOneTile() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.RED,
                    new BuildingCommand.PlaceInHand(BESIDE_THE_CABIN, Rotation.NONE));
            accepted(game, PlayerColor.RED,
                    new BuildingCommand.AdjustPlacement(BESIDE_THE_CABIN, Rotation.CLOCKWISE_90));
            accepted(game, PlayerColor.RED, new BuildingCommand.Weld());

            assertTrue(game.viewFor(PlayerColor.RED).players().get(0).ship()
                    .cells().containsKey(BESIDE_THE_CABIN));
        }

        @Test
        @DisplayName("a refused command leaves the shipyard exactly as it was")
        void refusalsChangeNothing() {
            Game game = Games.levelTwo();
            GameView before = game.viewFor(PlayerColor.RED);

            String reason = refused(game, PlayerColor.RED, new BuildingCommand.Weld());

            assertFalse(reason.isBlank(), "a refusal has to say why");
            assertEquals(before, game.viewFor(PlayerColor.RED),
                    "nothing may change on the way to being refused");
        }

        @Test
        @DisplayName("two players build at the same time without noticing each other")
        void everybodyAtOnce() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.BLUE, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.RED,
                    new BuildingCommand.PlaceInHand(BESIDE_THE_CABIN, Rotation.NONE));
            accepted(game, PlayerColor.BLUE,
                    new BuildingCommand.PlaceInHand(BESIDE_THE_CABIN, Rotation.NONE));
            accepted(game, PlayerColor.RED, new BuildingCommand.Weld());
            accepted(game, PlayerColor.BLUE, new BuildingCommand.Weld());

            assertEquals(GamePhase.BUILDING, game.phase(), "neither has finished");
        }

        @Test
        @DisplayName("a tile somebody rejected is there for anybody to take")
        void thePileIsPublic() {
            Game game = Games.levelTwo();
            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.RED, new BuildingCommand.ReturnToPool());

            String discarded = game.viewFor(PlayerColor.BLUE).buildingIfAny().orElseThrow()
                    .faceUpPile().get(0).tileId();

            accepted(game, PlayerColor.BLUE, new BuildingCommand.TakeFaceUp(discarded));

            assertEquals(discarded, game.viewFor(PlayerColor.BLUE).buildingIfAny().orElseThrow()
                    .handIfAny().orElseThrow().tileId());
        }

        @Test
        @DisplayName("a player sees their own hand and nobody else's")
        void handsArePrivate() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());

            assertTrue(game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .handIfAny().isPresent());
            assertTrue(game.viewFor(PlayerColor.BLUE).buildingIfAny().orElseThrow()
                    .handIfAny().isEmpty(),
                    "the blue player is not holding anything, whatever the red player is");
        }

        @Test
        @DisplayName("everybody finishing ends the shipyard")
        void finishingTogether() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            assertEquals(GamePhase.BUILDING, game.phase(), "one of two is not everybody");

            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            assertNotEquals(GamePhase.BUILDING, game.phase());
        }

        @Test
        @DisplayName("a finished ship is not still being built")
        void nothingAfterFinishing() {
            Game game = Games.levelTwo();
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));

            assertEquals("this ship is finished and is on the starting line",
                    refused(game, PlayerColor.RED, new BuildingCommand.DrawFromPool()));
        }

        @Test
        @DisplayName("the last of the glass stops everybody, finished or not")
        void whenTimeRunsOut() {
            Games.Hand clock = new Games.Hand();
            Game game = Games.levelTwo(clock);
            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());

            // Three turns of a ninety-second glass, with the last one turned by somebody who
            // has finished — which is the only player the manual lets turn it (p.17).
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            clock.pass(Duration.ofSeconds(91));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FlipTimer());
            clock.pass(Duration.ofSeconds(91));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FlipTimer());
            clock.pass(Duration.ofSeconds(91));

            game.tick();

            assertNotEquals(GamePhase.BUILDING, game.phase(),
                    "time ran out and the shipyard should have closed");
        }

        @Test
        @DisplayName("somebody still building may not start the final countdown")
        void theLastFlipIsRestricted() {
            Games.Hand clock = new Games.Hand();
            Game game = Games.levelTwo(clock);

            clock.pass(Duration.ofSeconds(91));
            accepted(game, PlayerColor.RED, new BuildingCommand.FlipTimer());
            clock.pass(Duration.ofSeconds(91));

            assertFalse(refused(game, PlayerColor.RED, new BuildingCommand.FlipTimer()).isBlank(),
                    "the last turn belongs to somebody who has finished");
        }
    }

    @Nested
    @DisplayName("getting to the starting line")
    class Preparation {

        @Test
        @DisplayName("two ships with nothing wrong with them go straight to crewing")
        void nothingToRepair() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            assertEquals(GamePhase.CREW_PLACEMENT, game.phase(),
                    "a ship that is only a starting cabin is a legal ship");
        }

        @Test
        @DisplayName("crewing everybody launches the fleet")
        void launching() {
            Game game = crewing();

            accepted(game, PlayerColor.RED, new PreparationCommand.FinishPreparation());
            assertEquals(GamePhase.CREW_PLACEMENT, game.phase());

            accepted(game, PlayerColor.BLUE, new PreparationCommand.FinishPreparation());

            assertEquals(GamePhase.FLIGHT, game.phase());
            assertTrue(game.viewFor(PlayerColor.RED).flightIfAny().isPresent());
        }

        @Test
        @DisplayName("a crewed ship waits for the others rather than carrying on")
        void waitingToLaunch() {
            Game game = crewing();
            accepted(game, PlayerColor.RED, new PreparationCommand.FinishPreparation());

            assertEquals("this ship is crewed and waiting to launch",
                    refused(game, PlayerColor.RED, new PreparationCommand.FinishPreparation()));
        }

        @Test
        @DisplayName("declaring ready fills the cabins that are still empty")
        void theDefaultIsPeople() {
            Game game = crewing();

            accepted(game, PlayerColor.RED, new PreparationCommand.FinishPreparation());
            accepted(game, PlayerColor.BLUE, new PreparationCommand.FinishPreparation());

            assertTrue(game.viewFor(PlayerColor.RED).players().get(0).ship().cells().values()
                    .stream().anyMatch(cell -> cell.humans() > 0),
                    "the starting cabin should have people in it");
        }

        @Test
        @DisplayName("giving up takes a ship off the route")
        void givingUp() {
            Game game = crewing();
            accepted(game, PlayerColor.RED, new PreparationCommand.FinishPreparation());
            accepted(game, PlayerColor.BLUE, new PreparationCommand.FinishPreparation());

            send(game, PlayerColor.RED, new FlightCommand.GiveUp());

            assertTrue(game.viewFor(PlayerColor.BLUE).players().get(0).retired());
            assertEquals("this ship has already left the route",
                    refused(game, PlayerColor.RED, new FlightCommand.GiveUp()));
        }

        private Game crewing() {
            Game game = Games.levelTwo();
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            assertEquals(GamePhase.CREW_PLACEMENT, game.phase());
            return game;
        }
    }

    @Nested
    @DisplayName("setting tiles aside and looking at cards")
    class LevelTwoPrivileges {

        @Test
        @DisplayName("a reserved tile never welded is charged for at the end")
        void reservingIsABet() {
            Game game = Games.levelTwo();

            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.RED, new BuildingCommand.Reserve());
            assertEquals(1, game.viewFor(PlayerColor.RED).players().get(0).ship().reserved().size(),
                    "reserved tiles sit where everyone can see them");

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));

            assertEquals(1, game.viewFor(PlayerColor.RED).players().get(0).ship().lostComponents(),
                    "a tile set aside and never used costs the same as one thrown away (p.7)");
        }

        @Test
        @DisplayName("a reserved tile can be taken back")
        void takingItBack() {
            Game game = Games.levelTwo();
            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());
            accepted(game, PlayerColor.RED, new BuildingCommand.Reserve());
            String reserved = game.viewFor(PlayerColor.RED).players().get(0).ship()
                    .reserved().get(0).tileId();

            accepted(game, PlayerColor.RED, new BuildingCommand.TakeReserved(reserved));

            assertEquals(reserved, game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .handIfAny().orElseThrow().tileId());
        }

        @Test
        @DisplayName("the cards a player peeks at are theirs alone, and go back when they let go")
        void peekingIsPrivate() {
            Game game = Games.levelTwo();
            weldAnything(game, PlayerColor.RED);

            accepted(game, PlayerColor.RED, new BuildingCommand.ScoutPile(0));

            assertFalse(game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .scouted().isEmpty(), "the red player picked the pile up");
            assertTrue(game.viewFor(PlayerColor.BLUE).buildingIfAny().orElseThrow()
                    .scouted().isEmpty(), "and the blue player is not looking over their shoulder");

            accepted(game, PlayerColor.RED, new BuildingCommand.PutPileBack());

            assertTrue(game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .scouted().isEmpty());
        }

        @Test
        @DisplayName("finishing while holding a pile puts it back")
        void finishingLetsGoOfThePile() {
            Game game = Games.levelTwo();
            weldAnything(game, PlayerColor.RED);
            accepted(game, PlayerColor.RED, new BuildingCommand.ScoutPile(0));

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));

            assertTrue(game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .scouted().isEmpty());
        }

        @Test
        @DisplayName("on level II a player picks their place on the starting line")
        void choosingAStartSpace() {
            Game game = Games.levelTwo();
            List<Integer> free = game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .freeStartSpaces();

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(free.get(1)));

            assertFalse(game.viewFor(PlayerColor.BLUE).buildingIfAny().orElseThrow()
                    .freeStartSpaces().contains(free.get(1)),
                    "a space somebody took is not free any more");
        }

        /**
         * Welds one tile, whatever it is.
         *
         * <p>A player may not look at the card piles before they have started building, which
         * is the manual's rule and not something worth working around (p.17).
         */
        private void weldAnything(Game game, PlayerColor player) {
            accepted(game, player, new BuildingCommand.DrawFromPool());
            accepted(game, player, new BuildingCommand.PlaceInHand(BESIDE_THE_CABIN, Rotation.NONE));
            accepted(game, player, new BuildingCommand.Weld());
        }
    }

    @Nested
    @DisplayName("crewing")
    class Crewing {

        @Test
        @DisplayName("people can be put in a cabin one at a time")
        void boardingPeople() {
            Game game = crewing();
            Position cabin = onlyCabin(game);

            accepted(game, PlayerColor.RED, new PreparationCommand.BoardCrew(cabin, null));

            assertEquals(2, game.viewFor(PlayerColor.RED).players().get(0).ship()
                    .cells().get(cabin).humans());
        }

        @Test
        @DisplayName("an alien needs life support welded to the cabin, not merely near it")
        void aliensNeedLifeSupport() {
            Game game = crewing();
            Position cabin = onlyCabin(game);

            String reason = refused(game, PlayerColor.RED,
                    new PreparationCommand.BoardCrew(cabin, AlienColor.PURPLE));

            assertFalse(reason.isBlank(), "a ship with no life support has no alien berths");
        }

        @Test
        @DisplayName("a command from another phase is refused during crewing too")
        void theWrongPhase() {
            Game game = crewing();

            assertEquals("the ships are being crewed",
                    refused(game, PlayerColor.RED, new BuildingCommand.DrawFromPool()));
        }

        private Position onlyCabin(Game game) {
            return game.viewFor(PlayerColor.RED).players().get(0).ship().cells().entrySet().stream()
                    .filter(cell -> cell.getValue().tile().kind().isCabin())
                    .findFirst().orElseThrow().getKey();
        }

        private Game crewing() {
            Game game = Games.levelTwo();
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            assertEquals(GamePhase.CREW_PLACEMENT, game.phase());
            return game;
        }
    }

    @Nested
    @DisplayName("seats")
    class Seats {

        @Test
        @DisplayName("a seat needs a name and a colour")
        void incompleteSeats() {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> new Seat("  ", PlayerColor.RED));
            org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                    () -> new Seat("samuele", null));
        }
    }

    @Nested
    @DisplayName("repairs")
    class Repairs {

        @Test
        @DisplayName("a ship with an engine facing the wrong way cannot launch until it is fixed")
        void anIllegalShipHoldsTheFleetUp() {
            Game game = Games.levelTwo();
            Position badEngine = weldFirst(game, PlayerColor.RED,
                    ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_180);

            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            assertEquals(GamePhase.VALIDATION, game.phase(),
                    "an engine pointing forwards is not a ship anybody may fly");
            assertFalse(game.viewFor(PlayerColor.RED).players().get(0).ship()
                    .validation().isLegal());

            accepted(game, PlayerColor.RED, new PreparationCommand.RemoveComponent(badEngine));

            assertEquals(GamePhase.CREW_PLACEMENT, game.phase());
        }

        @Test
        @DisplayName("throwing a component away costs its owner, whatever it was for")
        void repairsAreNotFree() {
            Game game = Games.levelTwo();
            Position badEngine = weldFirst(game, PlayerColor.RED,
                    ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_180);
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            accepted(game, PlayerColor.RED, new PreparationCommand.RemoveComponent(badEngine));

            assertEquals(1, game.viewFor(PlayerColor.RED).players().get(0).ship().lostComponents(),
                    "a component pulled off is a component lost, and it is counted at the end");
        }

        @Test
        @DisplayName("there is nothing to remove from an empty square, and nothing to choose from a whole ship")
        void nonsenseRepairs() {
            Game game = Games.levelTwo();
            weldFirst(game, PlayerColor.RED, ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_180);
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            assertEquals("there is nothing welded at Position[row=0, column=0]",
                    refused(game, PlayerColor.RED,
                            new PreparationCommand.RemoveComponent(new Position(0, 0))));
            // Asked of the blue player, whose ship is still only its starting cabin. The red
            // ship may well be in two pieces: welding does not check that a tile's connectors
            // meet what it is next to, so a component can be welded on and still not be joined
            // to anything — which is the manual's behaviour, not an oversight.
            assertEquals("this ship is in one piece; there is nothing to choose",
                    refused(game, PlayerColor.BLUE,
                            new PreparationCommand.KeepPiece(java.util.Set.of(new Position(2, 3)))));
        }

        /**
         * Draws until a component of the wanted kind turns up, then welds it beside the cabin.
         *
         * <p>The pool is the real one, so which tile comes up next is a fact about the seed
         * rather than something to arrange. Anything unwanted goes on the discard pile, which
         * is what a player does with it too.
         */
        private Position weldFirst(Game game, PlayerColor player, ComponentKind wanted,
                                   Rotation rotation) {
            for (int attempt = 0; attempt < 200; attempt++) {
                accepted(game, player, new BuildingCommand.DrawFromPool());
                ComponentKind drawn = game.viewFor(player).buildingIfAny().orElseThrow()
                        .handIfAny().orElseThrow().kind();
                if (drawn != wanted) {
                    accepted(game, player, new BuildingCommand.ReturnToPool());
                    continue;
                }
                accepted(game, player, new BuildingCommand.PlaceInHand(BESIDE_THE_CABIN, rotation));
                accepted(game, player, new BuildingCommand.Weld());
                return BESIDE_THE_CABIN;
            }
            throw new AssertionError("the pool never produced a " + wanted);
        }
    }

    @Nested
    @DisplayName("the test flight")
    class TestFlight {

        @Test
        @DisplayName("start spaces go in finishing order, and asking for one is refused")
        void finishingOrderDecides() {
            Game game = Games.testFlight();

            assertEquals("start spaces are handed out in finishing order on this board, not chosen",
                    refused(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(2)));

            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            assertEquals(List.of(2), game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .freeStartSpaces(),
                    "whoever finishes first takes the space at the front (p.8)");
        }

        @Test
        @DisplayName("there is no hourglass on the test flight")
        void noTimer() {
            Game game = Games.testFlight();

            assertEquals(0, game.viewFor(PlayerColor.RED).buildingIfAny().orElseThrow()
                    .hourglassSpaces());
            assertFalse(refused(game, PlayerColor.RED, new BuildingCommand.FlipTimer()).isBlank(),
                    "there is nothing to turn");
        }
    }

    @Nested
    @DisplayName("who is here")
    class Connections {

        @Test
        @DisplayName("a player who has dropped is shown as gone, and the game carries on")
        void droppingOut() {
            Game game = Games.levelTwo();

            game.connectionChanged(PlayerColor.BLUE, false);

            assertFalse(game.isConnected(PlayerColor.BLUE));
            assertFalse(game.viewFor(PlayerColor.RED).players().get(1).connected(),
                    "the others need to know why nobody is taking that player's turn");
            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());

            game.connectionChanged(PlayerColor.BLUE, true);
            assertTrue(game.isConnected(PlayerColor.BLUE));
        }
    }
}

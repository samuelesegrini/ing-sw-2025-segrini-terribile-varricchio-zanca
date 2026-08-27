package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test flight: a shorter game on a smaller board, with most of the fiddly parts taken out.
 *
 * <p>It is not a difficulty setting. The manual's trial flight teaches the game by removing what
 * needs explaining — the sand timer, setting a component aside, looking at a pile of cards before
 * anybody else, the aliens — and by not charging a beginner for a mistake they were always going
 * to make.
 *
 * <p>Every one of those is something the server has to <em>refuse</em>, because the commands
 * still exist and a client can still send them. The board data has been right since M1; what was
 * never checked is whether asking to reserve a tile in a trial flight gets an answer or gets
 * away with it. So these go through {@code apply}, which is the door a real client knocks on,
 * rather than reaching past it.
 */
class TestFlightTest {

    private static final LevelSpec TRIAL =
            GameDataLoader.loadBundled().level(GameLevel.TEST_FLIGHT);
    private static final LevelSpec FULL =
            GameDataLoader.loadBundled().level(GameLevel.LEVEL_II);

    /** Sends a command as a player and insists it was refused, returning the reason. */
    private static String refused(Game game, PlayerColor player, Command command) {
        return assertInstanceOf(Reaction.Refused.class, game.apply(player, command),
                () -> command.getClass().getSimpleName() + " was allowed in a trial flight")
                .reason();
    }

    /** Sends a command as a player and insists it was accepted. */
    private static void accepted(Game game, PlayerColor player, Command command) {
        assertInstanceOf(Reaction.Accepted.class, game.apply(player, command),
                () -> command.getClass().getSimpleName() + " was refused");
    }

    @Nested
    @DisplayName("the board")
    class TheBoard {

        @Test
        @DisplayName("is the small one: eighteen squares, eighteen spaces, four starts")
        void theLevelOneBoard() {
            long buildable = (long) TRIAL.shipBoard().rows() * TRIAL.shipBoard().columns()
                    - TRIAL.shipBoard().forbidden().size();

            assertEquals(18, buildable, "the trial flight ship is eighteen squares");
            assertEquals(18, TRIAL.flightBoard().routeLength());
            assertEquals(List.of(4, 2, 1, 0), TRIAL.flightBoard().startingPositions());
        }

        @Test
        @DisplayName("pays four, three, two and one to finish, and two for the prettiest ship")
        void theRewards() {
            assertEquals(List.of(4, 3, 2, 1), TRIAL.flightBoard().rewards().finishOrder());
            assertEquals(2, TRIAL.flightBoard().rewards().prettiestShip());
        }

        @Test
        @DisplayName("has no hourglass at all, rather than one that has already run out")
        void noTimer() {
            assertEquals(0, TRIAL.flightBoard().hourglassSpaces());
            assertTrue(FULL.flightBoard().hourglassSpaces() > 0, "the full game does have one");
        }

        @Test
        @DisplayName("has nowhere to set a component aside")
        void noReservation() {
            assertEquals(0, TRIAL.shipBoard().reservationSlots());
            assertFalse(TRIAL.shipBoard().allowsReservation());
            assertTrue(FULL.shipBoard().allowsReservation(), "the full game does allow it");
        }
    }

    @Nested
    @DisplayName("the deck")
    class TheDeck {

        @Test
        @DisplayName("deck_isTheEightMarkedCards")
        void deckIsTheEightMarkedCards() {
            assertEquals(1, TRIAL.flightBoard().deck().piles(),
                    "one pile, because there is nothing to choose between");
            assertEquals(8, TRIAL.flightBoard().deck().cardsPerPile().get(CardLevel.LEVEL_I),
                    "the trial flight is eight cards long");
            assertTrue(TRIAL.flightBoard().deck().testFlightCardsOnly(),
                    "a level II card in a trial flight would be a card the rules have not "
                            + "taught yet");
        }

        @Test
        @DisplayName("and the eight really are cards bearing the mark")
        void theCardsAreTheMarkedOnes() {
            Game game = Games.testFlight();
            GameView seen = game.viewFor(PlayerColor.RED);

            assertEquals(GameLevel.TEST_FLIGHT, seen.level());
            long marked = GameDataLoader.loadBundled().cards().stream()
                    .filter(card -> card.testFlight())
                    .count();
            assertTrue(marked >= 8,
                    "there are not eight cards bearing the L mark to draw from, only " + marked);
        }
    }

    @Nested
    @DisplayName("what a trial flight refuses")
    class Refusals {

        @Test
        @DisplayName("reservationCommand_isRejectedInTestFlight")
        void reservationCommandIsRejectedInTestFlight() {
            Game game = Games.testFlight();
            accepted(game, PlayerColor.RED, new BuildingCommand.DrawFromPool());

            String reason = refused(game, PlayerColor.RED, new BuildingCommand.Reserve());

            assertTrue(reason.toLowerCase().contains("aside")
                            || reason.toLowerCase().contains("reserv"),
                    "the refusal should name what was asked for: " + reason);
        }

        @Test
        @DisplayName("the hourglass cannot be flipped, because there is no hourglass")
        void noFlippingTheTimer() {
            Game game = Games.testFlight();

            String reason = refused(game, PlayerColor.RED, new BuildingCommand.FlipTimer());

            assertFalse(reason.isBlank(), "a refusal with no reason is a shrug");
        }

        @Test
        @DisplayName("nobody may look at a pile of cards early")
        void noPeeking() {
            Game game = Games.testFlight();

            String reason = refused(game, PlayerColor.RED, new BuildingCommand.ScoutPile(0));

            assertFalse(reason.isBlank(), "a refusal with no reason is a shrug");
        }

        @Test
        @DisplayName("no alien may come aboard, whatever is welded to the cabin")
        void noAliens() {
            Game game = Games.testFlight();
            accepted(game, PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            accepted(game, PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));

            String reason = refused(game, PlayerColor.RED, new PreparationCommand.BoardCrew(
                    TRIAL.shipBoard().startingCabin(), AlienColor.PURPLE));

            assertFalse(reason.isBlank(), "a refusal with no reason is a shrug");
        }
    }

    @Nested
    @DisplayName("mistakes are free")
    class Forgiveness {

        @Test
        @DisplayName("throwing a component off an illegal ship costs nothing at the end")
        void noPenaltyForCorrecting() {
            // Two different penalties, and only one of them is waived. A component lost *in
            // flight* still costs a credit in a trial flight — the manual's table says so for
            // both levels. What is free is throwing one off to make an illegal ship legal.
            assertEquals(1, TRIAL.flightBoard().rewards().lostComponentPenalty(),
                    "a component shot off still costs a credit, even in a trial flight");
            assertFalse(TRIAL.shipBoard().chargesForCorrections(),
                    "but putting a mistake right does not");
            assertTrue(FULL.shipBoard().chargesForCorrections(),
                    "and in the full game it does");
        }

        @Test
        @DisplayName("and the ship really is not charged for it, not merely told it will not be")
        void theCorrectionIsActuallyFree() {
            Ship trial = new Ship(TRIAL.shipBoard(),
                    Tiles.startingCabin(PlayerColor.RED), Ships.deepBank());
            Ship full = new Ship(FULL.shipBoard(),
                    Tiles.startingCabin(PlayerColor.RED), Ships.deepBank());
            Position spare = new Position(2, 4);
            Ships.put(trial, spare, ComponentKind.STRUCTURAL_MODULE);
            Ships.put(full, spare, ComponentKind.STRUCTURAL_MODULE);

            trial.correct(spare);
            full.discard(spare);

            assertEquals(0, trial.lostComponentCount(),
                    "a beginner is not charged for a ship they were always going to build wrong");
            assertEquals(1, full.lostComponentCount());
        }
    }

    @Nested
    @DisplayName("choosing one")
    class ChoosingATrialFlight {

        @Test
        @DisplayName("is the creating player's choice, and the game is built to match")
        void theLevelIsChosenWhenTheGameIsMade() {
            assertEquals(GameLevel.TEST_FLIGHT,
                    Games.testFlight().viewFor(PlayerColor.RED).level());
            assertEquals(GameLevel.LEVEL_II,
                    Games.levelTwo().viewFor(PlayerColor.RED).level());
        }

        @Test
        @DisplayName("and the ship a player is given is the small board, not the big one")
        void theShipMatchesTheLevel() {
            GameView trial = Games.testFlight().viewFor(PlayerColor.RED);
            GameView full = Games.levelTwo().viewFor(PlayerColor.RED);

            long trialSquares = trial.players().get(0).ship().outline().size();
            long fullSquares = full.players().get(0).ship().outline().size();

            assertEquals(18, trialSquares);
            assertEquals(27, fullSquares);
        }
    }
}

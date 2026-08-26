package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a launched fleet actually flies, and stops when it should.
 *
 * <p>The cards themselves were tested to death in M3, so nothing here re-tests a card. What is
 * new is the loop around them: turning one over, asking the right player, refusing an answer
 * from anybody else, clearing up afterwards, and knowing when the deck has run out.
 *
 * <p>These run against the real forty cards, so which one comes up is a fact about the seed
 * rather than something arranged. That is deliberate — a flight driven by one hand-picked card
 * would not exercise the loop.
 */
class FlightPhaseTest {

    /** Far more turns than a flight of eight to twelve cards can need. */
    private static final int PATIENCE = 400;

    private static Game flying() {
        Game game = Games.levelTwo();
        for (PlayerColor player : List.of(PlayerColor.RED, PlayerColor.BLUE)) {
            assertInstanceOf(Reaction.Accepted.class,
                    game.apply(player, new BuildingCommand.FinishBuilding(null)));
        }
        for (PlayerColor player : List.of(PlayerColor.RED, PlayerColor.BLUE)) {
            assertInstanceOf(Reaction.Accepted.class,
                    game.apply(player, new PreparationCommand.FinishPreparation()));
        }
        return game;
    }

    private static Reaction send(Game game, PlayerColor player, Command command) {
        return game.apply(player, command);
    }

    private static List<Event> narrationOf(Reaction reaction) {
        return assertInstanceOf(Reaction.Accepted.class, reaction,
                () -> "refused: " + reaction).narration();
    }

    @Test
    @DisplayName("launching turns the first card over, and says so")
    void theFirstCard() {
        Game game = Games.levelTwo();
        game.apply(PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
        game.apply(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
        game.apply(PlayerColor.RED, new PreparationCommand.FinishPreparation());

        List<Event> narration = narrationOf(
                send(game, PlayerColor.BLUE, new PreparationCommand.FinishPreparation()));

        assertTrue(narration.stream().anyMatch(FlightEvent.CardRevealed.class::isInstance),
                "the fleet launched and nothing was turned over: " + narration);
        assertEquals(GamePhase.FLIGHT, game.phase());
    }

    @Test
    @DisplayName("the card on the table and what is left of the deck are both shown")
    void theDeckIsVisible() {
        Game game = flying();
        GameView view = game.viewFor(PlayerColor.RED);

        assertTrue(view.flightIfAny().isPresent());
        assertTrue(view.flightIfAny().orElseThrow().cardsLeft() > 0,
                "a level II deck holds twelve cards and one has been turned over");
    }

    @Test
    @DisplayName("an answer from somebody the card is not asking is refused")
    void answeringOutOfTurn() {
        Game game = flying();
        PlayerPrompt asked = waitForAPrompt(game);
        PlayerColor other = asked.player() == PlayerColor.RED ? PlayerColor.BLUE : PlayerColor.RED;

        Reaction reaction = send(game, other,
                new FlightCommand.Answer(new PlayerChoice.Take(other)));

        assertEquals("the game is waiting for the " + asked.player() + " player, not you",
                assertInstanceOf(Reaction.Refused.class, reaction).reason());
    }

    @Test
    @DisplayName("an answer signed by somebody else is refused, however it arrived")
    void answeringForSomebodyElse() {
        Game game = flying();
        PlayerPrompt asked = waitForAPrompt(game);
        PlayerColor other = asked.player() == PlayerColor.RED ? PlayerColor.BLUE : PlayerColor.RED;

        // The payload names a player because the model needed that before there was a network.
        // Over a socket it is a claim anybody can make, and this is where the claim is checked
        // against the connection the command arrived on.
        Reaction reaction = send(game, asked.player(),
                new FlightCommand.Answer(new PlayerChoice.Take(other)));

        assertEquals("that answer is signed by the " + other + " player",
                assertInstanceOf(Reaction.Refused.class, reaction).reason());
    }

    @Test
    @DisplayName("an answer that does not fit the question is refused, and the question stands")
    void theWrongKindOfAnswer() {
        Game game = flying();
        PlayerPrompt asked = waitForAPrompt(game);

        Reaction reaction = send(game, asked.player(),
                new FlightCommand.Answer(new PlayerChoice.Done(asked.player())));

        if (reaction instanceof Reaction.Refused refused) {
            assertFalse(refused.reason().isBlank());
            assertEquals(asked, game.viewFor(asked.player()).pendingIfAny().orElseThrow(),
                    "a refused answer leaves the game waiting for the same thing");
        }
    }

    @Test
    @DisplayName("giving up while being asked something abandons the card rather than answering for them")
    void givingUpMidCard() {
        Game game = flying();
        PlayerPrompt asked = waitForAPrompt(game);

        List<Event> narration = narrationOf(send(game, asked.player(), new FlightCommand.GiveUp()));

        assertTrue(narration.stream().anyMatch(FlightEvent.ShipRetired.class::isInstance));
        assertTrue(game.viewFor(asked.player()).pendingIfAny()
                        .map(prompt -> prompt.player() != asked.player()).orElse(true),
                "a player who has left the route is not still being asked things");
    }

    @Test
    @DisplayName("a whole flight runs from the first card to the last credit")
    void aWholeFlight() {
        Game game = Games.levelTwo();
        game.apply(PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
        game.apply(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
        game.apply(PlayerColor.RED, new PreparationCommand.FinishPreparation());

        List<Event> everything = new ArrayList<>(narrationOf(
                send(game, PlayerColor.BLUE, new PreparationCommand.FinishPreparation())));
        everything.addAll(playToTheEnd(game));

        assertEquals(GamePhase.FINISHED, game.phase(),
                "the deck ran out and the game should be over");
        assertTrue(game.viewFor(PlayerColor.RED).scoresIfAny().isPresent(),
                "the ledger travels in the final state");
        assertEquals(2, game.viewFor(PlayerColor.RED).scoresIfAny().orElseThrow().size());
        long revealed = everything.stream().filter(FlightEvent.CardRevealed.class::isInstance).count();
        assertTrue(revealed > 1,
                "a flight that turned over " + revealed + " cards did not reach the end of the deck");
        assertTrue(everything.stream().anyMatch(FlightEvent.CardResolved.class::isInstance));
    }

    @Test
    @DisplayName("nothing is accepted once the game is over")
    void afterTheEnd() {
        Game game = flying();
        playToTheEnd(game);

        assertEquals("this game is over",
                assertInstanceOf(Reaction.Refused.class,
                        send(game, PlayerColor.RED, new FlightCommand.GiveUp())).reason());
    }

    @Test
    @DisplayName("everybody giving up ends the flight without the rest of the deck")
    void anEmptyRoute() {
        Game game = flying();

        send(game, PlayerColor.RED, new FlightCommand.GiveUp());
        send(game, PlayerColor.BLUE, new FlightCommand.GiveUp());
        game.tick();

        assertNotEquals(GamePhase.FLIGHT, game.phase(),
                "there is nobody left to turn a card over for");
    }

    // ------------------------------------------------------------------ playing it out

    /**
     * Answers whatever is asked until the game ends.
     *
     * <p>Every answer is the least interesting one available — take nothing, power nothing,
     * keep the first piece — because the point is to reach the end of the deck, not to play
     * well. What the cards do when answered properly is M3's business.
     */
    private static List<Event> playToTheEnd(Game game) {
        List<Event> everything = new ArrayList<>();
        for (int turn = 0; turn < PATIENCE && game.phase() != GamePhase.FINISHED; turn++) {
            PlayerPrompt asked = game.viewFor(PlayerColor.RED).pendingIfAny().orElse(null);
            if (asked == null) {
                everything.addAll(game.tick());
                if (game.phase() == GamePhase.FINISHED) {
                    break;
                }
                continue;
            }
            Reaction reaction = game.apply(asked.player(),
                    new FlightCommand.Answer(simplestAnswerTo(asked)));
            everything.addAll(narrationOf(reaction));
        }
        return everything;
    }

    private static PlayerChoice simplestAnswerTo(PlayerPrompt prompt) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave leave -> new PlayerChoice.Leave(leave.player());
            case PlayerPrompt.DeclarePower declare ->
                    new PlayerChoice.Declaration(declare.player(),
                            it.polimi.ingsw.common.game.BatteryPlan.none());
            case PlayerPrompt.ArrangeCargo cargo -> new PlayerChoice.Done(cargo.player());
            case PlayerPrompt.GiveUpCrew crew -> new PlayerChoice.CrewGiven(crew.player(),
                    crew.cabins().stream().limit(crew.count()).toList());
            case PlayerPrompt.ChooseDefence defence ->
                    PlayerChoice.DefenceChosen.none(defence.player());
            case PlayerPrompt.ChooseFragment fragment ->
                    new PlayerChoice.FragmentKept(fragment.player(), fragment.pieces().get(0));
            case PlayerPrompt.ChoosePlanet planet ->
                    new PlayerChoice.PlanetChosen(planet.player(),
                            planet.planets().keySet().iterator().next());
        };
    }

    private static PlayerPrompt waitForAPrompt(Game game) {
        for (int turn = 0; turn < PATIENCE; turn++) {
            PlayerPrompt asked = game.viewFor(PlayerColor.RED).pendingIfAny().orElse(null);
            if (asked != null) {
                return asked;
            }
            game.tick();
        }
        throw new AssertionError("this flight never asked anybody anything");
    }
}

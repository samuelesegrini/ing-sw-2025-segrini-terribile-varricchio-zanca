package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.view.ShipView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that what a player types becomes the answer the card is waiting for.
 *
 * <p>A pure function of a prompt, a line and a board, so there is no server here. What it has to
 * get right is that the same word means different things depending on what is being asked —
 * {@code keep} chooses a piece of a broken ship and sets a tile aside in the shipyard,
 * {@code leave} declines an offer and flies past a planet — and that a line it cannot use is
 * refused with something a person can act on rather than sent to be refused less helpfully.
 */
class FlightAnswersTest {

    private static final PlayerColor ME = PlayerColor.RED;
    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);

    /** The level II board: rows printed from 5, columns from 4. */
    private static final ShipView SHIP = new ShipView(5, 7, 5, 4,
            Set.of(CABIN, HOLD), Map.of(), List.of(), List.of(), 0,
            new ShipAttributes(0, 0, 0), ValidationReport.legal());

    private static FlightAnswers.Reading read(String line, PlayerPrompt prompt) {
        return FlightAnswers.read(Typed.of(line), prompt, SHIP, ME);
    }

    private static PlayerChoice answer(String line, PlayerPrompt prompt) {
        return assertInstanceOf(FlightAnswers.Reading.Answer.class, read(line, prompt),
                () -> line + " was not read as an answer").choice();
    }

    private static String refusal(String line, PlayerPrompt prompt) {
        return assertInstanceOf(FlightAnswers.Reading.Wrong.class, read(line, prompt),
                () -> line + " was not refused").why();
    }

    @Nested
    @DisplayName("an offer")
    class Offers {

        private final PlayerPrompt prompt =
                new PlayerPrompt.TakeOrLeave(ME, "4 credits", 1);

        @Test
        @DisplayName("is taken or left")
        void takingAndLeaving() {
            assertEquals(new PlayerChoice.Take(ME), answer("take", prompt));
            assertEquals(new PlayerChoice.Leave(ME), answer("leave", prompt));
            assertEquals(new PlayerChoice.Take(ME), answer("yes", prompt),
                    "somebody will type yes");
        }

        @Test
        @DisplayName("and anything else belongs to somebody other than this question")
        void notAnAnswer() {
            assertInstanceOf(FlightAnswers.Reading.NotForUs.class, read("power", prompt));
        }
    }

    @Nested
    @DisplayName("a declaration")
    class Declarations {

        private final PlayerPrompt prompt = new PlayerPrompt.DeclarePower(
                ME, ShipAttribute.FIREPOWER, Set.of(HOLD), 2);

        @Test
        @DisplayName("with nothing powered is still a declaration")
        void declaringAsYouAre() {
            assertEquals(new PlayerChoice.Declaration(ME, BatteryPlan.none()),
                    answer("power", prompt));
        }

        @Test
        @DisplayName("names components in the numbers printed on the board")
        void poweringSomething() {
            assertEquals(new PlayerChoice.Declaration(ME, BatteryPlan.powering(HOLD)),
                    answer("power 7 8", prompt));
        }

        @Test
        @DisplayName("of something a charge would not help is a typo, and is said so")
        void poweringNothingUseful() {
            assertTrue(refusal("power 7 7", prompt).contains("a charge would do nothing"),
                    "the prompt already said what a charge would power");
        }

        @Test
        @DisplayName("cannot spend charges the ship does not have")
        void overspending() {
            PlayerPrompt broke = new PlayerPrompt.DeclarePower(
                    ME, ShipAttribute.FIREPOWER, Set.of(HOLD, CABIN), 1);

            assertTrue(refusal("power 7 8 7 7", broke).contains("2 charges and you have 1"));
        }

        @Test
        @DisplayName("needs a row and a column for each component, not half a pair")
        void halfACoordinate() {
            assertTrue(refusal("power 7", prompt).contains("row and a column"));
        }

        @Test
        @DisplayName("of a square that is not on the board says which squares are")
        void offTheBoard() {
            assertTrue(refusal("power 1 1", prompt).contains("rows 5-9"));
        }
    }

    @Nested
    @DisplayName("stowing")
    class Stowing {

        private final PlayerPrompt prompt = new PlayerPrompt.ArrangeCargo(
                ME, Map.of(GoodColor.BLUE, 2), Set.of(HOLD));

        @Test
        @DisplayName("a cube names a colour and a hold")
        void loading() {
            assertEquals(PlayerChoice.CargoStowed.load(ME, HOLD, GoodColor.BLUE),
                    answer("load blue 7 8", prompt));
        }

        @Test
        @DisplayName("a colour can be shortened, because nobody wants to type yellow twice")
        void abbreviatedColours() {
            assertEquals(PlayerChoice.CargoStowed.load(ME, HOLD, GoodColor.GREEN),
                    answer("load g 7 8", prompt));
            assertEquals(PlayerChoice.CargoStowed.load(ME, HOLD, GoodColor.YELLOW),
                    answer("load yel 7 8", prompt));
        }

        @Test
        @DisplayName("a cube between holds needs both of them")
        void moving() {
            assertEquals(PlayerChoice.CargoStowed.move(ME, HOLD, CABIN, GoodColor.BLUE),
                    answer("move 7 8 7 7 blue", prompt));
            assertTrue(refusal("move 7 8 blue", prompt).contains("two holds"));
        }

        @Test
        @DisplayName("throwing one overboard needs a hold and a colour")
        void jettisoning() {
            assertEquals(PlayerChoice.CargoStowed.jettison(ME, HOLD, GoodColor.RED),
                    answer("drop 7 8 red", prompt));
            assertTrue(refusal("drop 7 8", prompt).contains("hold and a colour"));
        }

        @Test
        @DisplayName("and saying so is what ends it")
        void finishing() {
            assertEquals(new PlayerChoice.Done(ME), answer("done", prompt));
        }

        @Test
        @DisplayName("a colour nobody recognises lists the ones that exist")
        void unknownColour() {
            assertTrue(refusal("load mauve 7 8", prompt).contains("red, yellow, green or blue"));
        }
    }

    @Nested
    @DisplayName("an incoming shot")
    class Shots {

        private final PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(
                ME, new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 7), CABIN, Set.of(HOLD));

        @Test
        @DisplayName("can be taken")
        void takingIt() {
            assertEquals(PlayerChoice.DefenceChosen.none(ME), answer("hit", prompt));
        }

        @Test
        @DisplayName("or stopped with something that could stop it")
        void stoppingIt() {
            assertEquals(PlayerChoice.DefenceChosen.using(ME, HOLD), answer("shield 7 8", prompt));
        }

        @Test
        @DisplayName("but not with something that could not")
        void stoppingItWithNonsense() {
            assertTrue(refusal("shield 7 7", prompt).contains("cannot stop this one"));
        }
    }

    @Nested
    @DisplayName("crew and pieces and planets")
    class TheRest {

        @Test
        @DisplayName("crew is named one cabin per person, and the count has to match")
        void givingUpCrew() {
            PlayerPrompt prompt = new PlayerPrompt.GiveUpCrew(ME, 2, Set.of(CABIN));

            assertEquals(new PlayerChoice.CrewGiven(ME, List.of(CABIN, CABIN)),
                    answer("crew 7 7 7 7", prompt),
                    "a cabin holds two, and both of them can go");
            assertTrue(refusal("crew 7 7", prompt).contains("name 2 cabins"));
        }

        @Test
        @DisplayName("a piece of a broken ship is chosen by number, not by typing every square")
        void keepingAPiece() {
            PlayerPrompt prompt = new PlayerPrompt.ChooseFragment(
                    ME, List.of(Set.of(CABIN), Set.of(HOLD)));

            assertEquals(new PlayerChoice.FragmentKept(ME, Set.of(HOLD)), answer("keep 1", prompt));
            assertTrue(refusal("keep 9", prompt).contains("0 to 1"));
        }

        @Test
        @DisplayName("a planet is landed on by number, or flown past")
        void landing() {
            PlayerPrompt prompt = new PlayerPrompt.ChoosePlanet(
                    ME, Map.of(0, Map.of(GoodColor.BLUE, 2), 2, Map.of(GoodColor.RED, 1)), 1);

            assertEquals(new PlayerChoice.PlanetChosen(ME, 2), answer("planet 2", prompt));
            assertEquals(new PlayerChoice.Leave(ME), answer("leave", prompt));
            assertTrue(refusal("planet 1", prompt).contains("which planet"),
                    "planet one has somebody on it already and is not in the offer");
        }
    }

    @Test
    @DisplayName("the same word means different things depending on what is being asked")
    void contextDecides() {
        // The reason answers are read against the prompt rather than a table of verbs.
        // Two pieces, because a ship in one piece is never asked which to keep.
        PlayerPrompt fragment =
                new PlayerPrompt.ChooseFragment(ME, List.of(Set.of(CABIN), Set.of(HOLD)));
        PlayerPrompt planet = new PlayerPrompt.ChoosePlanet(ME, Map.of(0, Map.of()), 1);
        PlayerPrompt offer = new PlayerPrompt.TakeOrLeave(ME, "salvage", 1);

        assertInstanceOf(PlayerChoice.FragmentKept.class, answer("keep 0", fragment));
        assertInstanceOf(PlayerChoice.Leave.class, answer("leave", planet));
        assertInstanceOf(PlayerChoice.Leave.class, answer("leave", offer));
        assertInstanceOf(FlightAnswers.Reading.NotForUs.class, read("keep 0", offer),
                "there is no piece to keep when somebody is offering you salvage");
    }
}

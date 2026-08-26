package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a player can follow a flight and make a decision from what is drawn.
 *
 * <p>Two things here are load-bearing rather than cosmetic. Route positions are shown as they
 * are — absolute, going up for ever — because a ship a whole lap behind the leader is out of the
 * flight, and that is invisible if positions wrap. And a prompt shows only the choices it
 * carries, because a screen offering something the server would refuse teaches the wrong game.
 */
class FlightRenderersTest {

    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);

    private static final ShipView SHIP = new ShipView(5, 7, 5, 4,
            Set.of(CABIN, HOLD), Map.of(), List.of(), List.of(), 0,
            new ShipAttributes(0, 0, 0), ValidationReport.legal());

    private static List<PlayerView> players() {
        return List.of(
                new PlayerView("samuele", PlayerColor.RED, true, false, 12, SHIP),
                new PlayerView("chiara", PlayerColor.BLUE, true, false, 4, SHIP));
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }

    @Nested
    @DisplayName("the route")
    class Route {

        @Test
        @DisplayName("shows where everybody is and who plays first")
        void positionsAndOrder() {
            Map<PlayerColor, Integer> positions = new LinkedHashMap<>();
            positions.put(PlayerColor.RED, 9);
            positions.put(PlayerColor.BLUE, 6);
            FlightView flight = new FlightView(24, positions,
                    List.of(PlayerColor.RED, PlayerColor.BLUE), null, 7);

            String screen = joined(RouteRenderer.render(flight, players()));

            assertTrue(screen.contains("24 spaces"));
            assertTrue(screen.contains("7 cards left"));
            assertTrue(screen.contains("samuele"));
            assertTrue(screen.contains("space 9"));
            assertTrue(screen.contains("leading"));
            assertTrue(screen.contains("3 behind"), "how far back decides whether to spend a battery");
        }

        @Test
        @DisplayName("shows a lapped ship as a bigger number, not as the same one again")
        void lapping() {
            // A whole lap behind is out of the flight. Positions modulo the board would show
            // that player as being in front.
            Map<PlayerColor, Integer> positions = new LinkedHashMap<>();
            positions.put(PlayerColor.RED, 30);
            positions.put(PlayerColor.BLUE, 5);
            FlightView flight = new FlightView(24, positions,
                    List.of(PlayerColor.RED, PlayerColor.BLUE), null, 3);

            String screen = joined(RouteRenderer.render(flight, players()));

            assertTrue(screen.contains("space 30"), "a lap round is a bigger number");
            assertTrue(screen.contains("25 behind"));
        }

        @Test
        @DisplayName("names the card on the table, when there is one")
        void theCard() {
            FlightView between = new FlightView(24, Map.of(), List.of(), null, 3);
            FlightView during = new FlightView(24, Map.of(), List.of(),
                    new AdventureCardIdentity("pirates_lvl2", AdventureCardType.PIRATES,
                            CardLevel.LEVEL_II, false), 3);

            assertFalse(joined(RouteRenderer.render(between, players())).contains("on the table"));
            assertTrue(joined(RouteRenderer.render(during, players())).contains("pirates"));
        }

        @Test
        @DisplayName("says who has left the flight, since they are not on the route any more")
        void retiredPlayers() {
            List<PlayerView> withARetirement = List.of(
                    new PlayerView("samuele", PlayerColor.RED, true, false, 12, SHIP),
                    new PlayerView("chiara", PlayerColor.BLUE, true, true, 4, SHIP));
            FlightView flight = new FlightView(24, Map.of(PlayerColor.RED, 9),
                    List.of(PlayerColor.RED), null, 3);

            String screen = joined(RouteRenderer.render(flight, withARetirement));

            assertTrue(screen.contains("chiara"));
            assertTrue(screen.contains("out of the flight"));
        }
    }

    @Nested
    @DisplayName("a question")
    class Prompts {

        @Test
        @DisplayName("shows only the components that could stop this particular shot")
        void onlyLegalDefences() {
            PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(PlayerColor.RED,
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 7), CABIN, Set.of(HOLD));

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("small meteor from the north"));
            assertTrue(screen.contains("rolled 7"));
            assertTrue(screen.contains("it would strike 7,7"));
            assertTrue(screen.contains("7,8"), "and what could stop it");
            assertTrue(screen.contains("'shield"));
        }

        @Test
        @DisplayName("says plainly when nothing can be done, rather than offering a choice")
        void nothingToBeDone() {
            PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(PlayerColor.RED,
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7), CABIN, Set.of());

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("nothing aboard can stop it"));
            assertFalse(screen.contains("'shield"), "offering a choice the server would refuse "
                    + "teaches the wrong game");
        }

        @Test
        @DisplayName("a shot that misses says so")
        void aMiss() {
            PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(PlayerColor.RED,
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 2), null, Set.of());

            assertTrue(joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED))
                    .contains("misses your ship"));
        }

        @Test
        @DisplayName("a declaration says how many charges are left and what they would power")
        void declaring() {
            PlayerPrompt prompt = new PlayerPrompt.DeclarePower(
                    PlayerColor.RED, ShipAttribute.FIREPOWER, Set.of(HOLD), 2);

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("2 battery charges left"));
            assertTrue(screen.contains("7,8"));
            assertTrue(screen.contains("'power"));
        }

        @Test
        @DisplayName("crew is counted rather than declared, and says so")
        void declaringCrew() {
            PlayerPrompt prompt = new PlayerPrompt.DeclarePower(
                    PlayerColor.RED, ShipAttribute.CREW, Set.of(), 2);

            assertTrue(joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED))
                    .contains("counted, not declared"));
        }

        @Test
        @DisplayName("planets are listed with what is on them and what landing costs")
        void planets() {
            PlayerPrompt prompt = new PlayerPrompt.ChoosePlanet(PlayerColor.RED,
                    Map.of(0, Map.of(GoodColor.BLUE, 2), 1, Map.of(GoodColor.RED, 1)), 2);

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("2 flight days"));
            assertTrue(screen.contains("2 blue"));
            assertTrue(screen.contains("1 red"));
            assertTrue(screen.contains("'planet"));
        }

        @Test
        @DisplayName("a broken ship's pieces are numbered, so nobody types out every square")
        void pieces() {
            PlayerPrompt prompt = new PlayerPrompt.ChooseFragment(PlayerColor.RED,
                    List.of(Set.of(CABIN), Set.of(HOLD)));

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("0)"));
            assertTrue(screen.contains("1)"));
            assertTrue(screen.contains("'keep <n>'"));
        }

        @Test
        @DisplayName("somebody else's question is summed up, not spelled out")
        void watchingSomebodyElse() {
            PlayerPrompt prompt = new PlayerPrompt.TakeOrLeave(PlayerColor.BLUE, "salvage", 1);

            String screen = joined(PromptRenderer.render(prompt, SHIP, PlayerColor.RED));

            assertTrue(screen.contains("waiting for BLUE"));
            assertFalse(screen.contains("'take'"),
                    "the details of another ship's decision are not this player's to read");
        }
    }

    @Nested
    @DisplayName("the ledger")
    class Scores {

        @Test
        @DisplayName("shows every line the manual scores separately, separately")
        void theBreakdown() {
            List<ScoreSheet> sheets = List.of(
                    new ScoreSheet(PlayerColor.RED, true, 4, 4, 9, 12, 1),
                    new ScoreSheet(PlayerColor.BLUE, false, 0, 0, 2, 3, 6));

            String screen = joined(ScoreRenderer.render(sheets, players()));

            assertTrue(screen.contains("finish"));
            assertTrue(screen.contains("prettiest"));
            assertTrue(screen.contains("goods"));
            assertTrue(screen.contains("lost"));
            assertTrue(screen.contains("-1"), "losses are a subtraction, not folded into a total");
            assertTrue(screen.contains("-6"));
        }

        @Test
        @DisplayName("a player who did not finish is not shown a finishing bonus of zero")
        void notFinishing() {
            List<ScoreSheet> sheets = List.of(
                    new ScoreSheet(PlayerColor.BLUE, false, 0, 0, 2, 3, 0));

            String screen = joined(ScoreRenderer.render(sheets, players()));

            assertTrue(screen.contains("—"), "a dash reads as 'not applicable'; a nought reads "
                    + "as 'you earned nothing', which is a different thing");
        }

        @Test
        @DisplayName("says who won, and whether they made anything doing it")
        void winning() {
            String rich = joined(ScoreRenderer.render(
                    List.of(new ScoreSheet(PlayerColor.RED, true, 4, 4, 9, 12, 1)), players()));
            String poor = joined(ScoreRenderer.render(
                    List.of(new ScoreSheet(PlayerColor.RED, false, 0, 0, 0, 0, 8)), players()));

            assertTrue(rich.contains("samuele wins with"));
            assertTrue(poor.contains("having lost money on the trip"),
                    "the manual is quite happy to let somebody win a flight they lost money on");
        }
    }
}

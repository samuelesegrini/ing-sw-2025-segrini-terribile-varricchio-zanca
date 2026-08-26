package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.protocol.view.ShipView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The buttons a question allows, and what each of them answers.
 *
 * <p>Every prompt carries its own options — which cabins hold crew, which components could stop
 * this shot, which planets are still free — so this turns them into things to press and nothing
 * else. A button that is not here is a choice the server would refuse, and offering it would
 * teach a player the wrong game.
 *
 * <p>A pure function of a prompt and a board, which is why it is here and not inside a pane: it
 * can be checked against every kind of question on a machine with no screen.
 */
public final class PromptChoices {

    /**
     * Something a player can press.
     *
     * @param label  what it says
     * @param choice what it answers, {@code null} for a button the screen handles itself
     * @param cell   the square it refers to, {@code null} when it refers to none
     */
    public record Option(String label, PlayerChoice choice, Position cell) {

        /**
         * A button that answers outright.
         *
         * @param label  what it says
         * @param choice what it sends
         * @return the option
         */
        public static Option answering(String label, PlayerChoice choice) {
            return new Option(label, choice, null);
        }

        /**
         * A button that refers to a square, for the questions answered by picking several.
         *
         * @param label  what it says
         * @param cell   which square
         * @return the option
         */
        public static Option pointingAt(String label, Position cell) {
            return new Option(label, null, cell);
        }

        /**
         * Tells whether pressing this sends something on its own.
         *
         * @return {@code true} when it answers by itself
         */
        public boolean answersOutright() {
            return choice != null;
        }
    }

    private PromptChoices() {
    }

    /**
     * Says what the question is, in a sentence a player can act on.
     *
     * @param prompt what is being asked
     * @param ship   the board being asked about, for its printed numbers
     * @return the question
     */
    public static String question(PlayerPrompt prompt, ShipView ship) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave offer ->
                    "You are offered " + offer.description() + ", for "
                            + days(offer.flightDays()) + ".";
            case PlayerPrompt.DeclarePower power -> power.attribute() == ShipAttribute.CREW
                    ? "Crew is counted, not declared."
                    : "Declare your " + words(power.attribute().name()) + ". "
                            + power.chargesAvailable() + " battery "
                            + (power.chargesAvailable() == 1 ? "charge" : "charges") + " left.";
            case PlayerPrompt.ArrangeCargo cargo -> cargo.offered().isEmpty()
                    ? "There is nothing left to take."
                    : "On offer: " + goods(cargo.offered()) + ".";
            case PlayerPrompt.GiveUpCrew crew ->
                    "You must give up " + crew.count()
                            + (crew.count() == 1 ? " crew member." : " crew.");
            case PlayerPrompt.ChooseDefence defence ->
                    words(defence.hit().kind().name()) + " from the "
                            + defence.hit().from().name().toLowerCase()
                            + ", rolled " + defence.hit().diceSum() + ". "
                            + defence.targetIfAny()
                                    .map(cell -> "It would strike "
                                            + printed(ship, cell) + ".")
                                    .orElse("It misses your ship.");
            case PlayerPrompt.ChooseFragment ignored ->
                    "Your ship has come apart. Choose the piece to keep; the rest is lost.";
            case PlayerPrompt.ChoosePlanet planet ->
                    "Planets still free, at " + days(planet.flightDays()) + ".";
        };
    }

    /**
     * Returns the buttons this question allows.
     *
     * @param prompt what is being asked
     * @param ship   the board being asked about
     * @param you    which player this client is
     * @return the options, in the order they should be offered
     */
    public static List<Option> of(PlayerPrompt prompt, ShipView ship, PlayerColor you) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave ignored -> List.of(
                    Option.answering("Take it", new PlayerChoice.Take(you)),
                    Option.answering("Leave it", new PlayerChoice.Leave(you)));
            case PlayerPrompt.DeclarePower power -> declaring(power, ship, you);
            case PlayerPrompt.ArrangeCargo cargo -> stowing(cargo, ship, you);
            case PlayerPrompt.GiveUpCrew crew -> crew.cabins().stream()
                    .sorted(byPrintedOrder())
                    .map(cabin -> Option.pointingAt("Cabin " + printed(ship, cabin), cabin))
                    .toList();
            case PlayerPrompt.ChooseDefence defence -> defending(defence, ship, you);
            case PlayerPrompt.ChooseFragment fragment -> pieces(fragment, you);
            case PlayerPrompt.ChoosePlanet planet -> landing(planet, you);
        };
    }

    private static List<Option> declaring(PlayerPrompt.DeclarePower power, ShipView ship,
                                          PlayerColor you) {
        if (power.attribute() == ShipAttribute.CREW) {
            return List.of(Option.answering("Go on",
                    new PlayerChoice.Declaration(you, BatteryPlan.none())));
        }
        List<Option> options = new ArrayList<>();
        options.add(Option.answering("Declare as you are",
                new PlayerChoice.Declaration(you, BatteryPlan.none())));
        power.activatable().stream()
                .sorted(byPrintedOrder())
                .forEach(cell -> options.add(
                        Option.pointingAt("Power " + printed(ship, cell), cell)));
        return List.copyOf(options);
    }

    private static List<Option> stowing(PlayerPrompt.ArrangeCargo cargo, ShipView ship,
                                        PlayerColor you) {
        List<Option> options = new ArrayList<>();
        options.add(Option.answering("Finished stowing", new PlayerChoice.Done(you)));
        cargo.offered().forEach((colour, howMany) -> {
            if (howMany > 0) {
                cargo.holds().stream()
                        .sorted(byPrintedOrder())
                        .forEach(hold -> options.add(Option.answering(
                                "Put " + colour.name().toLowerCase() + " in "
                                        + printed(ship, hold),
                                PlayerChoice.CargoStowed.load(you, hold, colour))));
            }
        });
        return List.copyOf(options);
    }

    private static List<Option> defending(PlayerPrompt.ChooseDefence defence, ShipView ship,
                                          PlayerColor you) {
        List<Option> options = new ArrayList<>();
        options.add(Option.answering("Take the hit", PlayerChoice.DefenceChosen.none(you)));
        defence.options().stream()
                .sorted(byPrintedOrder())
                .forEach(cell -> options.add(Option.answering(
                        "Stop it with " + printed(ship, cell),
                        PlayerChoice.DefenceChosen.using(you, cell))));
        return List.copyOf(options);
    }

    private static List<Option> pieces(PlayerPrompt.ChooseFragment fragment, PlayerColor you) {
        List<Option> options = new ArrayList<>();
        for (int piece = 0; piece < fragment.pieces().size(); piece++) {
            Set<Position> cells = fragment.pieces().get(piece);
            options.add(Option.answering(
                    "Keep the piece with " + cells.size()
                            + (cells.size() == 1 ? " component" : " components"),
                    new PlayerChoice.FragmentKept(you, cells)));
        }
        return List.copyOf(options);
    }

    private static List<Option> landing(PlayerPrompt.ChoosePlanet planet, PlayerColor you) {
        List<Option> options = new ArrayList<>();
        planet.planets().forEach((number, goods) -> options.add(Option.answering(
                "Land on " + number + " (" + goods(goods) + ")",
                new PlayerChoice.PlanetChosen(you, number))));
        options.add(Option.answering("Fly on", new PlayerChoice.Leave(you)));
        return List.copyOf(options);
    }

    /**
     * Orders squares the way a player reads them: along a row, then down.
     *
     * <p>The prompts carry sets, and a set has no order — so without this the buttons would
     * move about between one question and the next.
     */
    private static java.util.Comparator<Position> byPrintedOrder() {
        return java.util.Comparator.comparingInt(Position::row)
                .thenComparingInt(Position::column);
    }

    private static String printed(ShipView ship, Position cell) {
        return ship.printedRow(cell.row()) + "," + ship.printedColumn(cell.column());
    }

    private static String days(int flightDays) {
        return flightDays == 1 ? "1 flight day" : flightDays + " flight days";
    }

    private static String goods(Map<GoodColor, Integer> goods) {
        return goods.isEmpty() ? "nothing" : goods.entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey().name().toLowerCase())
                .reduce((left, right) -> left + ", " + right)
                .orElse("nothing");
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

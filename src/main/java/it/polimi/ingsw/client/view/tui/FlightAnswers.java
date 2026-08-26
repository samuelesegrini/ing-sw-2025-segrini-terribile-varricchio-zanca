package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.view.ShipView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Turning a typed line into the answer the outstanding question wants.
 *
 * <p>Which question is outstanding decides what a word means. {@code keep} chooses a piece of a
 * broken ship during a flight and sets a tile aside in the shipyard; {@code leave} declines an
 * offer and flies past a planet. Reading commands against the prompt rather than against a
 * global table is what lets both be true without either being renamed.
 *
 * <p>Nothing here decides anything about the game. It refuses what it cannot parse and hands
 * everything else to the server, which refuses what it cannot allow — the client's job is to
 * make a legal answer easy to type, not to work out whether it is a good one.
 *
 * <p>A pure function of a prompt, a line and a board, so it is tested without a server.
 */
public final class FlightAnswers {

    /** What came of reading a line against the outstanding question. */
    public sealed interface Reading {

        /**
         * The line is an answer.
         *
         * @param choice what to send
         */
        record Answer(PlayerChoice choice) implements Reading {
        }

        /**
         * The line was meant as an answer but does not work.
         *
         * @param why what to tell the player
         */
        record Wrong(String why) implements Reading {
        }

        /**
         * The line is not an answer at all, and belongs to whatever else handles it.
         */
        record NotForUs() implements Reading {
        }
    }

    private FlightAnswers() {
    }

    /**
     * Reads a line as an answer to the outstanding question.
     *
     * @param typed  what was typed
     * @param prompt what the card is asking
     * @param ship   the board being asked about, for its printed numbers
     * @param you    which player this client is
     * @return the answer, a complaint, or nothing if the line was not an answer
     */
    public static Reading read(Typed typed, PlayerPrompt prompt, ShipView ship, PlayerColor you) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave ignored -> takeOrLeave(typed, you);
            case PlayerPrompt.DeclarePower power -> declare(typed, power, ship, you);
            case PlayerPrompt.ArrangeCargo cargo -> stow(typed, cargo, ship, you);
            case PlayerPrompt.GiveUpCrew crew -> giveUpCrew(typed, crew, ship, you);
            case PlayerPrompt.ChooseDefence defence -> defend(typed, defence, ship, you);
            case PlayerPrompt.ChooseFragment fragment -> keepAPiece(typed, fragment, you);
            case PlayerPrompt.ChoosePlanet planet -> land(typed, planet, you);
        };
    }

    // ------------------------------------------------------------------ one question each

    private static Reading takeOrLeave(Typed typed, PlayerColor you) {
        if (typed.is("take", "yes")) {
            return new Reading.Answer(new PlayerChoice.Take(you));
        }
        if (typed.is("leave", "no")) {
            return new Reading.Answer(new PlayerChoice.Leave(you));
        }
        return new Reading.NotForUs();
    }

    private static Reading declare(Typed typed, PlayerPrompt.DeclarePower power,
                                   ShipView ship, PlayerColor you) {
        if (!typed.is("power", "declare")) {
            return new Reading.NotForUs();
        }
        List<Position> powered = new ArrayList<>();
        for (int at = 0; at + 1 < typed.words().size(); at += 2) {
            Optional<Position> cell = Coordinates.on(ship, typed.number(at), typed.number(at + 1));
            if (cell.isEmpty()) {
                return new Reading.Wrong("that is not a square on your ship — "
                        + Coordinates.range(ship));
            }
            powered.add(cell.orElseThrow());
        }
        if (typed.words().size() % 2 != 0) {
            return new Reading.Wrong("name each component as a row and a column");
        }
        Set<Position> notPowerable = new LinkedHashSet<>(powered);
        notPowerable.removeAll(power.activatable());
        if (!notPowerable.isEmpty()) {
            // The prompt already says what a charge would help, so this is a typo rather than a
            // rule the player has not learnt.
            return new Reading.Wrong("a charge would do nothing at "
                    + notPowerable.stream().map(cell -> Coordinates.printed(ship, cell))
                            .reduce((left, right) -> left + " " + right).orElse(""));
        }
        if (powered.size() > power.chargesAvailable()) {
            return new Reading.Wrong("that would take " + powered.size() + " charges and you have "
                    + power.chargesAvailable());
        }
        return new Reading.Answer(new PlayerChoice.Declaration(you,
                new BatteryPlan(Set.copyOf(powered))));
    }

    private static Reading stow(Typed typed, PlayerPrompt.ArrangeCargo cargo,
                                ShipView ship, PlayerColor you) {
        if (typed.is("done", "finished")) {
            return new Reading.Answer(new PlayerChoice.Done(you));
        }
        if (typed.is("load")) {
            Optional<GoodColor> colour = colour(typed.word(0));
            Optional<Position> hold = Coordinates.on(ship, typed.number(1), typed.number(2));
            if (colour.isEmpty()) {
                return new Reading.Wrong("which cube? " + colours());
            }
            if (hold.isEmpty()) {
                return new Reading.Wrong("which hold? give a row and a column");
            }
            return new Reading.Answer(PlayerChoice.CargoStowed.load(
                    you, hold.orElseThrow(), colour.orElseThrow()));
        }
        if (typed.is("move")) {
            Optional<Position> from = Coordinates.on(ship, typed.number(0), typed.number(1));
            Optional<Position> to = Coordinates.on(ship, typed.number(2), typed.number(3));
            Optional<GoodColor> colour = colour(typed.word(4));
            if (from.isEmpty() || to.isEmpty()) {
                return new Reading.Wrong("moving needs two holds: 'move <row> <col> <row> <col> <colour>'");
            }
            if (colour.isEmpty()) {
                return new Reading.Wrong("which cube? " + colours());
            }
            return new Reading.Answer(PlayerChoice.CargoStowed.move(
                    you, from.orElseThrow(), to.orElseThrow(), colour.orElseThrow()));
        }
        if (typed.is("drop", "jettison")) {
            Optional<Position> hold = Coordinates.on(ship, typed.number(0), typed.number(1));
            Optional<GoodColor> colour = colour(typed.word(2));
            if (hold.isEmpty() || colour.isEmpty()) {
                return new Reading.Wrong("throwing one overboard needs a hold and a colour");
            }
            return new Reading.Answer(PlayerChoice.CargoStowed.jettison(
                    you, hold.orElseThrow(), colour.orElseThrow()));
        }
        return new Reading.NotForUs();
    }

    private static Reading giveUpCrew(Typed typed, PlayerPrompt.GiveUpCrew crew,
                                      ShipView ship, PlayerColor you) {
        if (!typed.is("crew", "give")) {
            return new Reading.NotForUs();
        }
        List<Position> cabins = new ArrayList<>();
        for (int at = 0; at + 1 < typed.words().size(); at += 2) {
            Optional<Position> cabin = Coordinates.on(ship, typed.number(at), typed.number(at + 1));
            if (cabin.isEmpty()) {
                return new Reading.Wrong("that is not a square on your ship");
            }
            cabins.add(cabin.orElseThrow());
        }
        if (cabins.size() != crew.count()) {
            // One cabin per person, and a cabin may be named twice if two of its people go.
            return new Reading.Wrong("name " + crew.count() + " cabin"
                    + (crew.count() == 1 ? "" : "s") + ", one for each person");
        }
        return new Reading.Answer(new PlayerChoice.CrewGiven(you, List.copyOf(cabins)));
    }

    private static Reading defend(Typed typed, PlayerPrompt.ChooseDefence defence,
                                  ShipView ship, PlayerColor you) {
        if (typed.is("hit", "take")) {
            return new Reading.Answer(PlayerChoice.DefenceChosen.none(you));
        }
        if (!typed.is("shield", "use", "stop")) {
            return new Reading.NotForUs();
        }
        Optional<Position> component = Coordinates.on(ship, typed.number(0), typed.number(1));
        if (component.isEmpty()) {
            return new Reading.Wrong("which component? give a row and a column");
        }
        if (!defence.options().contains(component.orElseThrow())) {
            return new Reading.Wrong(Coordinates.printed(ship, component.orElseThrow())
                    + " cannot stop this one");
        }
        return new Reading.Answer(PlayerChoice.DefenceChosen.using(you, component.orElseThrow()));
    }

    private static Reading keepAPiece(Typed typed, PlayerPrompt.ChooseFragment fragment,
                                      PlayerColor you) {
        if (!typed.is("keep")) {
            return new Reading.NotForUs();
        }
        OptionalInt which = typed.number(0);
        if (which.isEmpty() || which.getAsInt() < 0 || which.getAsInt() >= fragment.pieces().size()) {
            return new Reading.Wrong("which piece? 0 to " + (fragment.pieces().size() - 1));
        }
        return new Reading.Answer(new PlayerChoice.FragmentKept(
                you, fragment.pieces().get(which.getAsInt())));
    }

    private static Reading land(Typed typed, PlayerPrompt.ChoosePlanet planet, PlayerColor you) {
        if (typed.is("leave", "no")) {
            return new Reading.Answer(new PlayerChoice.Leave(you));
        }
        if (!typed.is("planet", "land")) {
            return new Reading.NotForUs();
        }
        OptionalInt which = typed.number(0);
        if (which.isEmpty() || !planet.planets().containsKey(which.getAsInt())) {
            return new Reading.Wrong("which planet? " + planet.planets().keySet().stream()
                    .map(String::valueOf)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("none are free"));
        }
        return new Reading.Answer(new PlayerChoice.PlanetChosen(you, which.getAsInt()));
    }

    // ------------------------------------------------------------------ reading a colour

    private static Optional<GoodColor> colour(Optional<String> word) {
        return word.flatMap(spoken -> {
            for (GoodColor colour : GoodColor.values()) {
                String name = colour.name().toLowerCase(Locale.ROOT);
                if (name.equals(spoken.toLowerCase(Locale.ROOT))
                        || name.startsWith(spoken.toLowerCase(Locale.ROOT))) {
                    return Optional.of(colour);
                }
            }
            return Optional.empty();
        });
    }

    private static String colours() {
        return "red, yellow, green or blue";
    }
}

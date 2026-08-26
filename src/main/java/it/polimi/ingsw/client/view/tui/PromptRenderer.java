package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GoodColor;
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
 * The question a card is asking, and only the answers that are legal.
 *
 * <p>Every prompt carries its own options — which cabins hold crew, which components could stop
 * this shot, which planets are still free — so there is nothing to work out here and nothing to
 * guess. That is not a convenience: a screen that offered a choice the server would refuse
 * would be a screen that taught players the wrong game.
 *
 * <p>Cells are named in the numbers printed on the board, because that is what a player can see
 * and what a meteor roll gives them.
 */
public final class PromptRenderer {

    private PromptRenderer() {
    }

    /**
     * Draws the outstanding question.
     *
     * @param prompt what is being asked
     * @param ship   the board of whoever is being asked, for its printed numbers
     * @param you    which player this client is
     * @return the lines to print
     */
    public static List<String> render(PlayerPrompt prompt, ShipView ship, PlayerColor you) {
        if (prompt.player() != you) {
            return List.of("  waiting for " + prompt.player() + " to " + summarise(prompt));
        }
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.addAll(asked(prompt, ship));
        return List.copyOf(lines);
    }

    /**
     * Says in a few words what somebody else is being asked.
     *
     * <p>Enough that a player waiting knows whether to expect a moment or a while, and no more:
     * the details of another ship's decision are not this player's business to read.
     *
     * @param prompt what is being asked
     * @return a phrase
     */
    public static String summarise(PlayerPrompt prompt) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave ignored -> "take an offer or leave it";
            case PlayerPrompt.DeclarePower power -> "declare " + words(power.attribute().name());
            case PlayerPrompt.ArrangeCargo ignored -> "stow what they have taken";
            case PlayerPrompt.GiveUpCrew crew -> "give up " + crew.count() + " crew";
            case PlayerPrompt.ChooseDefence ignored -> "answer an incoming shot";
            case PlayerPrompt.ChooseFragment ignored -> "choose which piece of their ship to keep";
            case PlayerPrompt.ChoosePlanet ignored -> "choose a planet";
        };
    }

    private static List<String> asked(PlayerPrompt prompt, ShipView ship) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave offer -> List.of(
                    "You are offered " + offer.description() + ".",
                    "  it costs " + days(offer.flightDays()),
                    "  'take' or 'leave'");
            case PlayerPrompt.DeclarePower power -> declaring(power, ship);
            case PlayerPrompt.ArrangeCargo cargo -> stowing(cargo, ship);
            case PlayerPrompt.GiveUpCrew crew -> List.of(
                    "You must give up " + crew.count()
                            + (crew.count() == 1 ? " crew member." : " crew."),
                    "  cabins with somebody in them: " + cells(crew.cabins(), ship),
                    "  'crew <row> <col>' — name one cabin per person");
            case PlayerPrompt.ChooseDefence defence -> defending(defence, ship);
            case PlayerPrompt.ChooseFragment fragment -> choosingAPiece(fragment, ship);
            case PlayerPrompt.ChoosePlanet planet -> landing(planet);
        };
    }

    private static List<String> declaring(PlayerPrompt.DeclarePower power, ShipView ship) {
        List<String> lines = new ArrayList<>();
        lines.add("Declare your " + words(power.attribute().name()) + ".");
        if (power.attribute() == ShipAttribute.CREW) {
            lines.add("  nothing to decide: crew is counted, not declared");
            lines.add("  'power' to go on");
            return List.copyOf(lines);
        }
        lines.add("  " + power.chargesAvailable() + " battery "
                + (power.chargesAvailable() == 1 ? "charge" : "charges") + " left");
        if (power.activatable().isEmpty()) {
            lines.add("  nothing aboard that a charge would help");
        } else {
            lines.add("  a charge each would power: " + cells(power.activatable(), ship));
        }
        lines.add("  'power <row> <col> …' to spend them, or 'power' to declare as you are");
        return List.copyOf(lines);
    }

    private static List<String> stowing(PlayerPrompt.ArrangeCargo cargo, ShipView ship) {
        List<String> lines = new ArrayList<>();
        lines.add(cargo.offered().isEmpty()
                ? "There is nothing left to take."
                : "On offer: " + goods(cargo.offered()));
        lines.add("  your holds: " + (cargo.holds().isEmpty()
                ? "none — there is nowhere to put anything"
                : cells(cargo.holds(), ship)));
        lines.add("  'load <colour> <row> <col>', 'move <row> <col> <row> <col> <colour>',");
        lines.add("  'drop <row> <col> <colour>', or 'done' when you have finished");
        return List.copyOf(lines);
    }

    private static List<String> defending(PlayerPrompt.ChooseDefence defence, ShipView ship) {
        List<String> lines = new ArrayList<>();
        lines.add(words(defence.hit().kind().name()) + " from the "
                + defence.hit().from().name().toLowerCase()
                + ", rolled " + defence.hit().diceSum() + ".");
        lines.add(defence.targetIfAny()
                .map(cell -> "  it would strike " + Coordinates.printed(ship, cell))
                .orElse("  it misses your ship altogether"));
        if (defence.options().isEmpty()) {
            lines.add("  nothing aboard can stop it");
            lines.add("  'hit' to take it");
        } else {
            lines.add("  could stop it, at a charge each: " + cells(defence.options(), ship));
            lines.add("  'shield <row> <col>' to use one, or 'hit' to take it");
        }
        return List.copyOf(lines);
    }

    private static List<String> choosingAPiece(PlayerPrompt.ChooseFragment fragment, ShipView ship) {
        List<String> lines = new ArrayList<>();
        lines.add("Your ship has come apart. Choose the piece to keep; the rest is lost.");
        for (int piece = 0; piece < fragment.pieces().size(); piece++) {
            Set<Position> cells = fragment.pieces().get(piece);
            lines.add("  " + piece + ")  " + cells.size()
                    + (cells.size() == 1 ? " component:  " : " components:  ")
                    + cells(cells, ship));
        }
        lines.add("  'keep <n>'");
        return List.copyOf(lines);
    }

    private static List<String> landing(PlayerPrompt.ChoosePlanet planet) {
        List<String> lines = new ArrayList<>();
        lines.add("Planets still free, at " + days(planet.flightDays()) + ":");
        planet.planets().forEach((number, goods) ->
                lines.add("  " + number + ")  " + goods(goods)));
        lines.add("  'planet <n>' to land, or 'leave' to fly on");
        return List.copyOf(lines);
    }

    private static String days(int flightDays) {
        return flightDays == 1 ? "1 flight day" : flightDays + " flight days";
    }

    private static String goods(Map<GoodColor, Integer> goods) {
        if (goods.isEmpty()) {
            return "nothing";
        }
        return goods.entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey().name().toLowerCase())
                .reduce((left, right) -> left + ", " + right)
                .orElse("nothing");
    }

    private static String cells(Set<Position> cells, ShipView ship) {
        return cells.stream()
                .map(cell -> Coordinates.printed(ship, cell))
                .sorted()
                .reduce((left, right) -> left + "  " + right)
                .orElse("none");
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

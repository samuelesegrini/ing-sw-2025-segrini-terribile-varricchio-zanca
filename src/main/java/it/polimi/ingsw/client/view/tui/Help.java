package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GamePhase;

import java.util.ArrayList;
import java.util.List;

/**
 * What you can type, and what it does.
 *
 * <p>Reachable from anywhere by typing {@code help}, and it shows the commands that are legal
 * <em>now</em> rather than all of them. A reference that listed every command in the game would
 * be a reference a player had to filter in their head while a timer ran.
 */
public final class Help {

    /** One command and what it does. */
    private record Line(String form, String meaning) {

        String rendered() {
            return "  " + String.format("%-22s", form) + meaning;
        }
    }

    private static final List<Line> ALWAYS = List.of(
            new Line("help", "this list"),
            new Line("look [player]", "show a ship — yours, or somebody else's"),
            new Line("scores", "the ledger, once there is one"),
            new Line("quit", "leave"));

    private static final List<Line> LOBBY = List.of(
            new Line("name <nickname>", "claim a name, or take back a seat you dropped out of"),
            new Line("games", "list the games waiting for players"),
            new Line("new <2-4> [test]", "open a game; add 'test' for the test flight"),
            new Line("join <game>", "take a seat at one"),
            new Line("leave", "give up a seat before the game starts"));

    private Help() {
    }

    /**
     * Lists what can be typed in the lobby.
     *
     * @return the lines to print
     */
    public static List<String> inTheLobby() {
        return render("In the lobby", LOBBY);
    }

    /**
     * Lists what can be typed during a game.
     *
     * @param phase where the game has got to
     * @return the lines to print
     */
    public static List<String> during(GamePhase phase) {
        return render("Now (" + phase.name().toLowerCase().replace('_', ' ') + ")",
                commandsDuring(phase));
    }

    private static List<Line> commandsDuring(GamePhase phase) {
        return switch (phase) {
            case BUILDING -> List.of(
                    new Line("draw", "take the top tile off the heap"),
                    new Line("take <tile>", "take one from the discard pile"),
                    new Line("pile", "put the tile in hand on the discard pile"),
                    new Line("keep", "set the tile in hand aside for later"),
                    new Line("back <tile>", "take back one you set aside"),
                    new Line("put <row> <col> [turns]", "put the tile in hand down"),
                    new Line("turn <row> <col> <turns>", "move or turn it before welding"),
                    new Line("weld", "make it part of the ship"),
                    new Line("peek <pile>", "look at a pile of cards (level II)"),
                    new Line("drop", "put the pile back"),
                    new Line("flip", "turn the hourglass"),
                    new Line("done [space]", "finish, and take a place on the starting line"));
            case VALIDATION -> List.of(
                    new Line("scrap <row> <col>", "throw away a component that cannot stay"),
                    new Line("keep <row> <col> …", "choose which piece of a broken ship to fly"));
            case CREW_PLACEMENT -> List.of(
                    new Line("crew <row> <col>", "put two people in a cabin"),
                    new Line("alien <row> <col> <p|b>", "put a purple or brown alien in one"),
                    new Line("done", "fill the rest with people and launch"));
            case FLIGHT -> List.of(
                    new Line("route", "where everybody is, and who plays first"),
                    new Line("take / leave", "accept or decline what a card is offering"),
                    new Line("power [<row> <col> …]", "declare, powering these components"),
                    new Line("load <colour> <r> <c>", "take a cube a card is offering"),
                    new Line("move <r> <c> <r> <c> <col>", "shift a cube between holds"),
                    new Line("drop <r> <c> <colour>", "throw one overboard"),
                    new Line("done", "finished stowing"),
                    new Line("crew <r> <c> …", "give up crew, one cabin per person"),
                    new Line("shield <row> <col>", "put something in front of a shot"),
                    new Line("hit", "take the shot"),
                    new Line("keep <n>", "choose which piece of a broken ship to fly on"),
                    new Line("planet <n>", "land on a planet"),
                    new Line("give up", "leave the route"));
            case SCORING, FINISHED -> List.of(
                    new Line("scores", "the final ledger, line by line"),
                    new Line("route", "where everybody finished"));
            case LOBBY -> List.of();
        };
    }

    private static List<String> render(String title, List<Line> commands) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        commands.forEach(command -> lines.add(command.rendered()));
        if (!commands.isEmpty()) {
            lines.add("");
        }
        lines.add("Always");
        ALWAYS.forEach(command -> lines.add(command.rendered()));
        return List.copyOf(lines);
    }
}

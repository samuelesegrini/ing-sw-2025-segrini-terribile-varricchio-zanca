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

    private static final List<Verb> ALWAYS = List.of(
            Verb.of("help", "this list", "help", "?"),
            Verb.of("look [player]", "show a ship — yours, or somebody else's", "look"),
            Verb.of("yard", "the shipyard, while it is open", "yard", "pool", "shipyard"),
            Verb.of("scores", "the ledger, once there is one", "scores", "ledger"),
            Verb.of("quit", "leave", "quit", "exit"));

    private static final List<Verb> LOBBY = List.of(
            Verb.of("name <nickname>", "claim a name, or take back a seat you dropped out of", "name", "login"),
            Verb.of("games", "list the games waiting for players", "games", "list"),
            Verb.of("new <2-4> [test]", "open a game; add 'test' for the test flight", "new", "create"),
            Verb.of("join <game>", "take a seat at one", "join"),
            Verb.of("leave", "give up a seat before the game starts", "leave"));

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

    /**
     * Returns every word a player can type in a phase, and what each one does.
     *
     * <p>Package-visible so that the vocabulary can be checked against the code that matches
     * it. A verb accepted by a handler and missing from here is a command nobody can find, and
     * one declared here and matched by nothing is a line of help pointing at nothing; neither
     * shows up in ordinary use, so a test reads both and compares them.
     *
     * @param phase where the game has got to
     * @return the verbs legal in it
     */
    static List<Verb> verbsDuring(GamePhase phase) {
        return commandsDuring(phase);
    }

    /**
     * Returns the words a player can always type, whatever is happening.
     *
     * @return the verbs
     */
    static List<Verb> always() {
        return ALWAYS;
    }

    /**
     * Returns the words a player can type before they are in a game.
     *
     * @return the verbs
     */
    static List<Verb> inTheLobbyVerbs() {
        return LOBBY;
    }

    /**
     * Names what can be typed now, on one line, for a hint after an unrecognised word.
     *
     * <p>Read from the same declaration the help itself is printed from. Written out by hand
     * these went stale without anybody noticing: the hint for the validation phase, whose whole
     * job is putting a broken ship right, listed only {@code scrap} and left out the command
     * that chooses which piece to keep.
     *
     * @param phase where the game has got to
     * @return the forms, quoted and comma separated
     */
    static String oneLine(GamePhase phase) {
        return commandsDuring(phase).stream()
                .map(verb -> "'" + verb.form() + "'")
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static List<Verb> commandsDuring(GamePhase phase) {
        return switch (phase) {
            case BUILDING -> List.of(
                    Verb.of("draw", "take the top tile off the heap", "draw"),
                    Verb.of("take <tile>", "take one from the discard pile", "take"),
                    Verb.of("pile", "put the tile in hand on the discard pile", "pile", "return"),
                    Verb.of("keep", "set the tile in hand aside for later", "keep", "reserve"),
                    Verb.of("back <tile>", "take back one you set aside", "back"),
                    Verb.of("put <row> <col> [turns]", "put the tile in hand down", "put", "place"),
                    Verb.of("turn <row> <col> <turns>", "move or turn it before welding", "turn", "move"),
                    Verb.of("weld", "make it part of the ship", "weld"),
                    Verb.of("peek <pile>", "look at a pile of cards (level II)", "peek", "scout"),
                    Verb.of("drop", "put the pile back", "drop"),
                    Verb.of("flip", "turn the hourglass", "flip"),
                    Verb.of("done [space]", "finish, and take a place on the starting line", "done", "finish"));
            case VALIDATION -> List.of(
                    Verb.of("scrap <row> <col>", "throw away a component that cannot stay", "scrap", "remove"),
                    Verb.of("keep <n>", "choose which piece of a broken ship to fly", "keep"));
            case CREW_PLACEMENT -> List.of(
                    Verb.of("crew <row> <col>", "put two people in a cabin", "crew", "people"),
                    Verb.of("alien <row> <col> <p|b>", "put a purple or brown alien in one", "alien"),
                    Verb.of("done", "fill the rest with people and launch", "done", "ready", "finish"));
            case FLIGHT -> List.of(
                    Verb.of("route", "where everybody is, and who plays first", "route", "board"),
                    Verb.of("take / leave", "accept or decline what a card is offering", "take", "yes", "leave", "no"),
                    Verb.of("power [<row> <col> …]", "declare, powering these components", "power", "declare"),
                    Verb.of("load <colour> <r> <c>", "take a cube a card is offering", "load"),
                    Verb.of("move <r> <c> <r> <c> <col>", "shift a cube between holds", "move"),
                    Verb.of("drop <r> <c> <colour>", "throw one overboard", "drop", "jettison"),
                    Verb.of("done", "finished stowing", "done", "finished"),
                    Verb.of("crew <r> <c> …", "give up crew, one cabin per person", "crew", "give"),
                    Verb.of("shield <row> <col>", "put something in front of a shot", "shield", "use", "stop"),
                    Verb.of("hit", "take the shot", "hit", "take"),
                    Verb.of("keep <n>", "choose which piece of a broken ship to fly on", "keep"),
                    Verb.of("planet <n>", "land on a planet", "planet", "land"),
                    Verb.of("give up", "leave the route", "give", "quitflight"));
            case SCORING, FINISHED -> List.of(
                    Verb.of("scores", "the final ledger, line by line", "scores", "ledger"),
                    Verb.of("route", "where everybody finished", "route", "board"));
            case LOBBY -> List.of();
        };
    }

    private static List<String> render(String title, List<Verb> commands) {
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

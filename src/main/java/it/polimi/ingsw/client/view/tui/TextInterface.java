package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.client.view.UserInterface;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * Playing by typing.
 *
 * <p>A loop: read a line, work out what it means in the phase the game is actually in, send a
 * command, print whatever has arrived since. Deliberately thin — everything worth looking at is
 * in a renderer, which can be tested without a terminal.
 *
 * <p>Reading and drawing both happen here, on one thread, and events arrive on another. Nothing
 * is shared but the {@link ClientState}, which is built for exactly that. So a line typed while
 * a card is being resolved is read against whatever the state says <em>at the moment it is
 * read</em>, which is also what a player is looking at.
 */
public final class TextInterface implements UserInterface {

    private final ClientState state;
    private final ServerLink server;
    private final BufferedReader in;
    private final PrintStream out;

    private final Object screen = new Object();

    private int narrationShown;
    private boolean stopped;
    private boolean promptShowing;

    /**
     * Builds a text interface over a pair of streams.
     *
     * <p>The streams are arguments rather than {@code System.in} and {@code System.out} so that
     * a test can drive a whole session without a terminal.
     *
     * @param state  everything the client knows
     * @param server where to send commands
     * @param in     where the player types
     * @param out    where to draw
     */
    public TextInterface(ClientState state, ServerLink server, BufferedReader in, PrintStream out) {
        this.state = state;
        this.server = server;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        print(List.of("Galaxy Trucker", "Type 'help' at any point.", ""));
        // Most of what a player waits for happens while they are not typing: three other people
        // finishing their ships, a card being turned over, somebody's laptop closing. A loop
        // that only drew after a line was read would show none of it until the player pressed
        // Enter to find out whether anything had happened.
        state.onChange(this::showWhatArrived);
        while (!stopped) {
            drainNarration();
            prompt();
            String line = readLine();
            if (line == null) {
                return;
            }
            synchronized (screen) {
                // They pressed Enter, so the terminal has moved past the prompt whether or not
                // anything was typed.
                promptShowing = false;
            }
            handle(Typed.of(line));
            // Refusals are printed from the narration, along with everything else, because a
            // stream each event appears in exactly once is precisely what "say this once" wants.
            // Reading the state's last refusal after every command printed the same complaint
            // again for every command that followed it.
            drainNarration();
        }
    }

    /**
     * Draws whatever has arrived, from the thread it arrived on.
     *
     * <p>Interleaves with a half-typed prompt, which is what every line-based client does and
     * is a great deal better than silence. The prompt is written again afterwards so that
     * somebody who was reading it still has it in front of them.
     */
    private void showWhatArrived() {
        synchronized (screen) {
            if (stopped) {
                return;
            }
            List<String> fresh = pendingNarration();
            if (fresh.isEmpty()) {
                return;
            }
            print(fresh);
            prompt();
        }
    }

    /**
     * Writes the prompt, unless one is already sitting there.
     *
     * <p>Two threads want to draw it — the loop after every command, and the listener after
     * anything arrives — and both are right to. Remembering whether one is already on screen is
     * cheaper than working out which of them should have been the one to write it.
     */
    private void prompt() {
        synchronized (screen) {
            if (promptShowing) {
                return;
            }
            out.print(LobbyRenderer.prompt(state) + " > ");
            out.flush();
            promptShowing = true;
        }
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException gone) {
            throw new UncheckedIOException("cannot read from the terminal", gone);
        }
    }

    // ------------------------------------------------------------------ what a line means

    private void handle(Typed typed) {
        if (typed.isBlank()) {
            return;
        }
        if (typed.is("quit", "exit")) {
            stopped = true;
            server.close();
            return;
        }
        if (typed.is("help", "?")) {
            print(state.game().map(game -> Help.during(game.phase())).orElseGet(Help::inTheLobby));
            return;
        }
        if (typed.is("look")) {
            look(typed.word(0));
            return;
        }
        if (state.game().isEmpty()) {
            inTheLobby(typed);
            return;
        }
        GameView game = state.game().orElseThrow();
        if (typed.is("yard", "pool", "shipyard")) {
            game.buildingIfAny().ifPresentOrElse(
                    yard -> print(ShipyardRenderer.render(yard)),
                    () -> print(List.of("  the shipyard closed a while ago")));
            return;
        }
        if (game.phase() == GamePhase.BUILDING) {
            inTheShipyard(typed, game);
            return;
        }
        // The remaining phases arrive with #49. Until then a player is told plainly rather than
        // left typing into silence.
        print(List.of("  not yet: '" + typed.verb() + "' belongs to the "
                + game.phase().name().toLowerCase().replace('_', ' ') + " phase"));
    }

    // ------------------------------------------------------------------ the shipyard

    private void inTheShipyard(Typed typed, GameView game) {
        if (typed.is("draw")) {
            sendThenShowHand(new BuildingCommand.DrawFromPool());
        } else if (typed.is("take")) {
            requireArgument(typed.word(0).orElse(""), "a tile to take",
                    tile -> sendThenShowHand(new BuildingCommand.TakeFaceUp(tile)));
        } else if (typed.is("back")) {
            requireArgument(typed.word(0).orElse(""), "one of the tiles you set aside",
                    tile -> sendThenShowHand(new BuildingCommand.TakeReserved(tile)));
        } else if (typed.is("pile", "return")) {
            send(new BuildingCommand.ReturnToPool());
        } else if (typed.is("keep", "reserve")) {
            send(new BuildingCommand.Reserve());
        } else if (typed.is("put", "place")) {
            place(typed, game);
        } else if (typed.is("turn", "move")) {
            adjust(typed, game);
        } else if (typed.is("weld")) {
            sendThenShowShip(new BuildingCommand.Weld());
        } else if (typed.is("peek", "scout")) {
            peek(typed);
        } else if (typed.is("drop")) {
            send(new BuildingCommand.PutPileBack());
        } else if (typed.is("flip")) {
            send(new BuildingCommand.FlipTimer());
        } else if (typed.is("done", "finish")) {
            send(new BuildingCommand.FinishBuilding(
                    typed.number(0).isPresent() ? typed.number(0).getAsInt() : null));
        } else {
            print(List.of("  ? '" + typed.verb() + "' is not something you can do in the "
                    + "shipyard; type 'help'"));
        }
    }

    private void place(Typed typed, GameView game) {
        cellFrom(typed, game).ifPresent(cell ->
                sendThenShowShip(new BuildingCommand.PlaceInHand(cell, turnsFrom(typed))));
    }

    private void adjust(Typed typed, GameView game) {
        cellFrom(typed, game).ifPresent(cell ->
                sendThenShowShip(new BuildingCommand.AdjustPlacement(cell, turnsFrom(typed))));
    }

    private void peek(Typed typed) {
        if (typed.number(0).isEmpty()) {
            print(List.of("  ? which pile? 'peek 0', 'peek 1', 'peek 2'"));
            return;
        }
        send(new BuildingCommand.ScoutPile(typed.number(0).getAsInt()));
        state.game().flatMap(GameView::buildingIfAny)
                .ifPresent(yard -> print(ShipyardRenderer.render(yard)));
    }

    /**
     * Reads a square from what somebody typed, in the numbers printed on their board.
     *
     * <p>Wrong coordinates are the expensive mistake in this phase: a tile welded three squares
     * from where it was meant is discovered during validation and paid for with a component. So
     * a pair that is not on the board is refused here, with the range that would have worked,
     * rather than sent to a server that would refuse it less helpfully.
     */
    private Optional<Position> cellFrom(Typed typed, GameView game) {
        ShipView ship = myShip(game);
        Optional<Position> cell = Coordinates.on(ship, typed.number(0), typed.number(1));
        if (cell.isEmpty()) {
            print(List.of("  ? which square? give a row and a column — "
                    + Coordinates.range(ship)));
        }
        return cell;
    }

    /**
     * Reads how far to turn a tile, in quarter turns.
     *
     * <p>Nothing given means upright, which is what somebody who did not mention turning meant.
     * Anything outside nought to three wraps, because a player who typed five quarter turns
     * meant one and should not be told off for it.
     */
    private static Rotation turnsFrom(Typed typed) {
        int quarters = typed.number(2).orElse(0);
        return Rotation.values()[Math.floorMod(quarters, Rotation.values().length)];
    }

    private ShipView myShip(GameView game) {
        return game.players().stream()
                .filter(player -> player.colour() == game.you())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("this game has no seat for us"))
                .ship();
    }

    private void sendThenShowHand(Command command) {
        send(command);
        state.game().flatMap(GameView::buildingIfAny)
                .map(ShipyardRenderer::hand)
                .filter(lines -> !lines.isEmpty())
                .ifPresent(this::print);
    }

    /**
     * Sends something that changes the ship, then draws it.
     *
     * <p>With the loose-tile note if there is one, which is what makes putting a tile down and
     * welding it look different on screen: after a placement the note says where it is and that
     * it can still be moved, and after a weld the note is gone.
     */
    private void sendThenShowShip(Command command) {
        send(command);
        state.game().ifPresent(after -> {
            ShipView ship = myShip(after);
            print(ShipRenderer.render(ship));
            after.buildingIfAny()
                    .map(yard -> ShipyardRenderer.loose(yard, ship))
                    .filter(lines -> !lines.isEmpty())
                    .ifPresent(this::print);
        });
    }

    private void inTheLobby(Typed typed) {
        if (typed.is("name", "login")) {
            requireArgument(typed.rest(), "a nickname",
                    nickname -> send(new LobbyCommand.Login(nickname)));
        } else if (typed.is("games", "list")) {
            send(new LobbyCommand.ListGames());
            // Asking and then having to ask again to see the answer is not asking.
            print(LobbyRenderer.render(state.openGames()));
        } else if (typed.is("new", "create")) {
            create(typed);
        } else if (typed.is("join")) {
            requireArgument(typed.word(0).orElse(""), "a game to join",
                    game -> send(new LobbyCommand.JoinGame(game)));
        } else if (typed.is("leave")) {
            send(new LobbyCommand.LeaveGame());
        } else {
            print(List.of("  ? '" + typed.verb() + "' is not something you can do here; "
                    + "type 'help'"));
        }
    }

    private void create(Typed typed) {
        int seats = typed.number(0).orElse(0);
        if (seats < 2 || seats > 4) {
            print(List.of("  ? how many players? 'new 4', or 'new 2 test' for the test flight"));
            return;
        }
        GameLevel level = typed.word(1).filter(word -> word.equalsIgnoreCase("test")).isPresent()
                ? GameLevel.TEST_FLIGHT
                : GameLevel.LEVEL_II;
        send(new LobbyCommand.CreateGame(level, seats));
    }

    private void requireArgument(String value, String what, java.util.function.Consumer<String> then) {
        if (value == null || value.isBlank()) {
            print(List.of("  ? that needs " + what));
            return;
        }
        then.accept(value.strip());
    }

    // ------------------------------------------------------------------ looking

    private void look(Optional<String> who) {
        Optional<GameView> game = state.game();
        if (game.isEmpty()) {
            print(LobbyRenderer.render(state.openGames()));
            return;
        }
        Optional<PlayerView> player = whose(game.orElseThrow(), who);
        if (player.isEmpty()) {
            print(List.of("  ? there is nobody called " + who.orElse("that") + " in this game"));
            return;
        }
        print(ShipRenderer.describe(player.orElseThrow().ship(),
                player.orElseThrow().nickname() + " (" + player.orElseThrow().colour() + ")"));
    }

    /**
     * Finds whose ship to show.
     *
     * <p>Every ship is public in this game (requirement G6), so anybody may be named. With
     * nobody named it is your own, which is what somebody typing {@code look} on its own means.
     */
    private Optional<PlayerView> whose(GameView game, Optional<String> named) {
        if (named.isEmpty()) {
            PlayerColor mine = game.you();
            return game.players().stream()
                    .filter(player -> player.colour() == mine)
                    .findFirst();
        }
        String wanted = named.orElseThrow();
        return game.players().stream()
                .filter(player -> player.nickname().equalsIgnoreCase(wanted)
                        || player.colour().name().equalsIgnoreCase(wanted))
                .findFirst();
    }

    // ------------------------------------------------------------------ printing

    private void send(Command command) {
        server.send(command);
        // Give the server a moment to answer, so that the next thing the player sees is the
        // consequence of what they typed rather than the prompt again. A tenth of a second is
        // below what anybody notices and above a loopback round trip.
        settle();
    }

    private void settle() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainNarration() {
        synchronized (screen) {
            List<String> fresh = pendingNarration();
            if (!fresh.isEmpty()) {
                print(fresh);
            }
        }
    }

    /**
     * Takes the narration nobody has drawn yet.
     *
     * <p>Called with the screen held, so that two threads cannot each decide they are the one
     * to print an event.
     */
    private List<String> pendingNarration() {
        List<it.polimi.ingsw.common.protocol.Event> fresh = state.narrationAfter(narrationShown);
        narrationShown += fresh.size();
        return fresh.stream()
                .map(NarrationRenderer::render)
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .toList();
    }

    private void print(List<String> lines) {
        synchronized (screen) {
            if (promptShowing) {
                // Something arrived while a prompt was waiting to be typed into. Break the line
                // first, or the message lands on the end of it.
                out.println();
                promptShowing = false;
            }
            lines.forEach(out::println);
            out.flush();
        }
    }
}

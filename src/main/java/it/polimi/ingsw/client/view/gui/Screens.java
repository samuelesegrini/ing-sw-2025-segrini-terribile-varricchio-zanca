package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.protocol.Command;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

/**
 * The windows, and what a player can do in each.
 *
 * <p>Controllers in the sense the requirement means: they turn a click into a command and put
 * what the server says back on the screen. There is no rule here that a player could break by
 * finding a way to press two buttons at once — every refusal comes from the server, and every
 * screen redraws from the state it is sent.
 *
 * <p>Each screen is built once and updated in place. Rebuilding on every change would throw
 * away a half-typed nickname each time somebody else drew a tile.
 */
final class Screens {

    private final ClientState state;
    private final java.util.function.Consumer<Command> outbox;
    private final Map<Screen, Parent> built = new EnumMap<>(Screen.class);

    private final TileImages images = new TileImages(Artwork.bundled());
    private ShipyardPane shipyard;
    private TextField nickname;
    private Label loginTrouble;
    private ListView<String> tables;
    private Label lobbyTrouble;

    /**
     * Prepares the windows.
     *
     * @param state  what the client knows
     * @param outbox where commands go — a consumer rather than the connection, because that is
     *               all any of these need and because a test can then watch what they send
     */
    Screens(ClientState state, java.util.function.Consumer<Command> outbox) {
        this.state = state;
        this.outbox = outbox;
    }

    /**
     * Returns the window for a screen, building it the first time it is asked for.
     *
     * @param screen which screen
     * @return its root
     */
    Parent rootFor(Screen screen) {
        return built.computeIfAbsent(screen, this::build);
    }

    /**
     * Refreshes whatever is currently up.
     *
     * @param screen which screen is showing
     */
    void update(Screen screen) {
        switch (screen) {
            case LOGIN -> loginTrouble.setText(state.lastRefusal().orElse(""));
            case LOBBY -> updateLobby();
            case SHIPYARD -> shipyard.redraw();
            default -> {
                // The playing screens arrive with #51 and #52. Until then their placeholder
                // says which one is missing rather than showing an empty window.
            }
        }
    }

    private Parent build(Screen screen) {
        return switch (screen) {
            case LOGIN -> login();
            case LOBBY -> lobby();
            case SHIPYARD -> shipyard();
            default -> notBuiltYet(screen);
        };
    }

    // ------------------------------------------------------------------ who are you

    private Parent login() {
        nickname = new TextField();
        nickname.setPromptText("your name");
        nickname.setMaxWidth(260);
        loginTrouble = trouble();

        Button enter = new Button("Play");
        enter.setDefaultButton(true);
        enter.setOnAction(clicked -> claimName());
        nickname.setOnAction(typed -> claimName());

        VBox box = column(new Label("Galaxy Trucker"), nickname, enter, loginTrouble);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void claimName() {
        String wanted = nickname.getText().trim();
        if (wanted.isEmpty()) {
            loginTrouble.setText("a name would help");
            return;
        }
        outbox.accept(new it.polimi.ingsw.common.protocol.LobbyCommand.Login(wanted));
    }

    // ------------------------------------------------------------------ choosing a table

    private Parent lobby() {
        tables = new ListView<>();
        tables.setMaxWidth(420);
        tables.setPrefHeight(240);
        lobbyTrouble = trouble();

        Button join = new Button("Join");
        join.setOnAction(clicked -> joinSelected());

        Button openTwo = new Button("Open a table for 2");
        openTwo.setOnAction(clicked -> open(2, false));
        Button openFour = new Button("Open a table for 4");
        openFour.setOnAction(clicked -> open(4, false));
        Button openTest = new Button("Test flight for 2");
        openTest.setOnAction(clicked -> open(2, true));

        HBox opening = new HBox(10, openTwo, openFour, openTest);
        opening.setAlignment(Pos.CENTER);

        VBox box = column(new Label("Tables waiting for players"), tables, join, opening,
                lobbyTrouble);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void updateLobby() {
        // Replacing the list wholesale would drop whatever the player had selected, so the
        // selection is put back afterwards. Tables come and go while somebody is deciding.
        String chosen = tables.getSelectionModel().getSelectedItem();
        tables.getItems().setAll(state.openGames().stream()
                .map(game -> game.gameId() + "   " + game.players().size() + " of "
                        + game.seats() + "   " + words(game.level().name()))
                .toList());
        if (chosen != null) {
            tables.getSelectionModel().select(chosen);
        }
        lobbyTrouble.setText(state.lastRefusal().orElse(""));
    }

    private void joinSelected() {
        String chosen = tables.getSelectionModel().getSelectedItem();
        if (chosen == null) {
            lobbyTrouble.setText("choose a table first, or open one");
            return;
        }
        outbox.accept(new it.polimi.ingsw.common.protocol.LobbyCommand.JoinGame(
                chosen.split("\\s+")[0]));
    }

    private void open(int players, boolean testFlight) {
        outbox.accept(new it.polimi.ingsw.common.protocol.LobbyCommand.CreateGame(
                testFlight
                        ? it.polimi.ingsw.common.game.GameLevel.TEST_FLIGHT
                        : it.polimi.ingsw.common.game.GameLevel.LEVEL_II,
                players));
    }

    // ------------------------------------------------------------------ building a ship

    private Parent shipyard() {
        shipyard = new ShipyardPane(state, outbox, images);
        return shipyard;
    }

    // ------------------------------------------------------------------ not built yet

    /**
     * A screen that says which part of the game is still missing.
     *
     * <p>Better than an empty window: somebody running this before #51 and #52 land should be
     * told what they are looking at rather than left to guess whether it has crashed.
     */
    private Parent notBuiltYet(Screen screen) {
        VBox box = column(new Label(SceneRouter.titleOf(screen)),
                new Label("This screen is not built yet. The game itself is running — "
                        + "the text interface can play it."));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ------------------------------------------------------------------ bits and pieces

    private static Label trouble() {
        Label label = new Label();
        label.setWrapText(true);
        label.setMaxWidth(420);
        return label;
    }

    private static VBox column(javafx.scene.Node... children) {
        VBox box = new VBox(14, children);
        box.setPadding(new Insets(40));
        return box;
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

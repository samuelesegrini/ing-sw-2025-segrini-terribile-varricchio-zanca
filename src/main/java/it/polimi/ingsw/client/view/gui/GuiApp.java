package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.state.ClientState;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The window, and the one place that knows what is currently in it.
 *
 * <p>It holds no rules and decides nothing about the game. What to show follows from the
 * client's state through {@link SceneRouter}, which is a pure function tested on a machine with
 * no screen; what a player does is turned into a command and sent. Everything in between
 * belongs to the server.
 *
 * <p>State changes arrive on the transport's thread and JavaFX will not be touched from there,
 * so every redraw is handed to {@link Platform#runLater}.
 */
public final class GuiApp extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;

    private ClientState state;
    private ServerLink server;
    private Screens screens;
    private Stage stage;
    private Screen showing;

    /**
     * Constructs the application.
     *
     * <p>Called by JavaFX, which is why it is public and takes nothing: everything this needs
     * arrives through {@link GraphicalInterface#handover()}.
     */
    public GuiApp() {
        // Constructed by JavaFX itself.
    }

    @Override
    public void start(Stage primary) {
        GraphicalInterface.Handover handover = GraphicalInterface.handover();
        this.state = handover.state();
        this.server = handover.server();
        this.screens = new Screens(state, server);
        this.stage = primary;

        primary.setScene(new Scene(screens.rootFor(Screen.LOGIN), WIDTH, HEIGHT));
        primary.setTitle(SceneRouter.titleOf(Screen.LOGIN));
        showing = Screen.LOGIN;

        state.onChange(() -> Platform.runLater(this::refresh));
        primary.setOnCloseRequest(closing -> handover.closed().countDown());
        primary.show();
        refresh();
    }

    /**
     * Puts the right screen up, and keeps it there if it is already right.
     *
     * <p>Rebuilding a scene that has not changed would throw away what the player was doing —
     * a half-typed nickname, a scrolled list — every time anybody else did anything.
     */
    private void refresh() {
        Screen wanted = SceneRouter.screenFor(state.nickname(), state.game());
        if (wanted != showing) {
            stage.getScene().setRoot(screens.rootFor(wanted));
            stage.setTitle(SceneRouter.titleOf(wanted));
            showing = wanted;
        }
        screens.update(showing);
    }

    @Override
    public void stop() {
        GraphicalInterface.Handover handover = GraphicalInterface.handover();
        handover.closed().countDown();
    }
}

package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.client.view.UserInterface;
import javafx.application.Application;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

/**
 * Playing in a window.
 *
 * <p>The other implementation of {@link UserInterface}, and the reason there is an interface at
 * all: the transport and the way of playing are chosen at startup and nothing afterwards can
 * find out which was picked.
 *
 * <p>JavaFX insists on starting itself — {@code Application.launch} takes over the thread it is
 * called on and hands control back only when the last window closes. So this class is a way of
 * getting the state and the connection to an {@link GuiApp} that JavaFX will construct on its
 * own, and then waiting.
 */
public final class GraphicalInterface implements UserInterface {

    /**
     * Where {@link GuiApp} finds what it needs.
     *
     * <p>Static because JavaFX constructs the application itself and there is nowhere to pass
     * anything in. One client per process, so one of these.
     */
    private static volatile Handover handover;

    private final ClientState state;
    private final ServerLink server;

    /**
     * Prepares a window.
     *
     * @param state  what the client knows
     * @param server where to send what the player does
     */
    public GraphicalInterface(ClientState state, ServerLink server) {
        this.state = state;
        this.server = server;
    }

    /**
     * What a starting {@link GuiApp} needs to know.
     *
     * @param state  what the client knows
     * @param server where to send what the player does
     * @param closed counted down when the last window is gone
     */
    record Handover(ClientState state, ServerLink server, CountDownLatch closed) {
    }

    /**
     * Returns what the running client handed over.
     *
     * @return the handover
     * @throws IllegalStateException if JavaFX started without a client behind it
     */
    static Handover handover() {
        Handover waiting = handover;
        if (waiting == null) {
            throw new IllegalStateException(
                    "the window started without a client; run ClientMain rather than GuiApp");
        }
        return waiting;
    }

    @Override
    public void run() {
        CountDownLatch closed = new CountDownLatch(1);
        handover = new Handover(state, server, closed);
        try {
            Application.launch(GuiApp.class);
            closed.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            handover = null;
            Platform.exit();
        }
    }
}

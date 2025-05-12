package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.ui.UIThreadHandler;

/**
 * Simple implementation of UIThreadHandler for terminal UI (TUI) that
 * runs operations directly on the calling thread. Since TUI doesn't have
 * the same thread constraints as a GUI, we can just execute operations directly.
 */
public class DirectThreadHandler implements UIThreadHandler {

    @Override
    public void runOnUIThread(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }
} 
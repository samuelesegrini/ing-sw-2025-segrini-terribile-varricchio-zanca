package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.ui.UIThreadHandler;
import javafx.application.Platform;

/**
 * JavaFX-specific implementation of UIThreadHandler that runs operations
 * on the JavaFX application thread.
 */
public class JavaFXThreadHandler implements UIThreadHandler {

    @Override
    public void runOnUIThread(Runnable runnable) {
        if (runnable == null) return;
        
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
} 
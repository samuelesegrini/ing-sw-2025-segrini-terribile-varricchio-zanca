package it.polimi.ingsw.client.ui;


import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.gui.GuiManager;
import it.polimi.ingsw.client.ui.tui.TuiManager;

/**
 * UI Manager that creates and manages the appropriate UI based on type.
 */
public class UIManager {
    private final UIType type;
    private final ClientController controller;
    private UI ui;

    public UIManager(UIType type, ClientController controller) {
        this.type = type;
        this.controller = controller;
        createUI();
    }

    private void createUI() {
        switch (type) {
            case GUI -> ui = new GuiManager(controller);
            case TUI -> ui = new TuiManager(controller);
        }
    }

    public void start() {
        ui.start();
    }

    public void shutdown() {
        if (ui != null) {
            ui.shutdown();
        }
    }

    public boolean isRunning() {
        if (ui == null) {
            return false;
        }
        return ui.isRunning();
    }
}
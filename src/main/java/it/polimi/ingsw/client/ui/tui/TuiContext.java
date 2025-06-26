package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.AbstractUIContext;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.core.ViewNavigatorImpl;
import it.polimi.ingsw.client.ui.core.UIThreadService;

/**
 * TUI-specific implementation of UIContext.
 * Provides terminal-specific services and components.
 */
public class TuiContext extends AbstractUIContext {
    
    private final TuiConsole console;
    private TuiManager tuiManager;
    
    public TuiContext(ClientController controller, TuiConsole console) {
        super(controller);
        this.console = console;
    }
    
    public void setTuiManager(TuiManager tuiManager) {
        this.tuiManager = tuiManager;
    }
    
    public TuiConsole getConsole() {
        return console;
    }
    
    public TuiManager getTuiManager() {
        return tuiManager;
    }
    
    @Override
    protected NotificationService createNotificationService() {
        return new TuiNotificationService(console);
    }
    
    @Override
    protected ViewNavigator createViewNavigator() {
        return new ViewNavigatorImpl(clientState);
    }
    
    @Override
    protected UIThreadService createThreadService() {
        return new TuiThreadService();
    }
}
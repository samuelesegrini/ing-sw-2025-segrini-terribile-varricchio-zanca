package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.UI;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.core.UIView;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.tui.views.*;
import it.polimi.ingsw.client.ui.core.UIContextProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * New TUI manager using the unified architecture.
 */
public class TuiManager implements ViewNavigator.ViewStateChangeListener, UI {
    
    private static final Logger LOGGER = Logger.getLogger(TuiManager.class.getName());
    
    private final TuiContext context;
    private final TuiConsole console;
    private final Map<ClientState.ViewState, UIView> views;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private UIView currentView;
    
    public TuiManager(ClientController controller) {
        this.console = new TuiConsole();
        this.context = new TuiContext(controller, console);
        UIContextProvider.setCurrent(this.context);
        
        // Set the UI context on the controller so it can access ViewNavigator
        controller.setUIContext(this.context);
        
        // Set this TuiManager in the context for access by views
        this.context.setTuiManager(this);
        
        this.views = new HashMap<>();
        
        initializeViews();
        
        // Set up view navigation listener
        context.getViewNavigator().addViewStateChangeListener(this);
    }
    
    protected UIView createView(ClientState.ViewState viewState, UIContext context) {
        return switch (viewState) {
            case CONNECTION -> new TuiConnectionView( (TuiContext) context);
            case LOGIN -> new TuiLoginView((TuiContext) context);
            case LOBBY -> new TuiLobbyView((TuiContext) context);
            case GAME_LOBBY -> new TuiGameLobbyView((TuiContext) context);
            case BUILDING -> new TuiGameView(context.getController(), (TuiContext) context);
            default -> throw new IllegalArgumentException("Unknown view state: " + viewState);
        };
    }

    private void initializeViews() {
        views.put(ClientState.ViewState.CONNECTION, createView(ClientState.ViewState.CONNECTION, context));
        views.put(ClientState.ViewState.LOGIN, createView(ClientState.ViewState.LOGIN, context));
        views.put(ClientState.ViewState.LOBBY, createView(ClientState.ViewState.LOBBY, context));
        views.put(ClientState.ViewState.GAME_LOBBY, createView(ClientState.ViewState.GAME_LOBBY, context));
        views.put(ClientState.ViewState.BUILDING, createView(ClientState.ViewState.BUILDING, context));
        
        LOGGER.info("Initialized " + views.size() + " TUI views");
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("Starting TUI Manager");
            
            try {
                // Show the initial view
                navigateToCurrentView();
                
                // Keep the manager running
                while (running.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                LOGGER.severe("Error in TUI Manager: " + e.getMessage());
                e.printStackTrace();
            } finally {
                shutdown();
            }
        }
    }
    
    private void navigateToCurrentView() {
        ClientState.ViewState targetState = context.getClientState().getCurrentView();
        showView(targetState);
    }
    
    private void showView(ClientState.ViewState viewState) {
        // Hide the current view
        if (currentView != null && currentView.isActive()) {
            currentView.hide();
        }
        
        // Show the new view
        UIView newView = views.get(viewState);
        if (newView != null) {
            currentView = newView;
            newView.show();
            LOGGER.info("Switched to view: " + viewState);
        } else {
            LOGGER.warning("No view found for state: " + viewState);
        }
    }
    
    @Override
    public void onViewStateChanged(ClientState.ViewState oldState, ClientState.ViewState newState) {
        showView(newState);
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("Shutting down TUI Manager");
            
            try {
                // Dispose all views
                for (UIView view : views.values()) {
                    view.dispose();
                }
                
                // Shutdown console
                console.shutdown();
                
                // Shutdown thread service
                context.getThreadService().shutdown();
                
                LOGGER.info("TUI Manager shut down successfully");
                
            } catch (Exception e) {
                LOGGER.severe("Error during TUI Manager shutdown: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void showError(String title, String message) {
        console.println("[ERROR] " + title + ": " + message);
    }
    
    @Override
    public void showInfo(String title, String message) {
        console.println("[INFO] " + title + ": " + message);
    }
    
    /**
     * Prints a line to the console.
     */
    public void println(String message) {
        console.println(message);
    }
    
    /**
     * Prints text to the console without a newline.
     */
    public void print(String message) {
        console.print(message);
    }
    
    /**
     * Clears the specified number of lines from the console.
     */
    public void clearLines(int lines) {
        console.clearLines(lines);
    }
    
    /**
     * Clears the console screen.
     */
    public void clear() {
        console.clearScreen();
    }
}
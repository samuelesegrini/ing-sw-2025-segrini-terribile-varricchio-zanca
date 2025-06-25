package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.UI;
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
        this.views = new HashMap<>();
        
        initializeViews();
        
        // Set up view navigation listener
        context.getViewNavigator().addViewStateChangeListener(this);
    }
    
    private void initializeViews() {
        TuiConnectionView connectionView = new TuiConnectionView();
        connectionView.initialize(context);
        views.put(ClientState.ViewState.CONNECTION, connectionView);
        
        TuiLoginView loginView = new TuiLoginView(context);
        views.put(ClientState.ViewState.LOGIN, loginView);

        TuiLobbyView lobbyView = new TuiLobbyView(context);
        views.put(ClientState.ViewState.LOBBY, lobbyView);

        TuiGameLobbyView gameLobbyView = new TuiGameLobbyView(context);
        views.put(ClientState.ViewState.GAME_LOBBY, gameLobbyView);
        
        TuiShipBuildingView shipBuildingView = new TuiShipBuildingView(context);
        views.put(ClientState.ViewState.GAME, shipBuildingView);
        
        TuiFlightView flightView = new TuiFlightView(context);
        views.put(ClientState.ViewState.GAME, shipBuildingView);
        
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
}
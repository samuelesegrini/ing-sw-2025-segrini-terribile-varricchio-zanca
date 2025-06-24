package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.UI;
import it.polimi.ingsw.client.ui.core.UIView;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.gui.views.GuiConnectionView;
import it.polimi.ingsw.client.ui.gui.views.GuiGameLobbyView;
import it.polimi.ingsw.client.ui.gui.views.GuiLobbyView;
import it.polimi.ingsw.client.ui.gui.views.GuiLoginView;
import it.polimi.ingsw.client.ui.gui.views.GuiShipBuildingView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.scene.text.Font;
import it.polimi.ingsw.client.ui.core.UIContextProvider;

/**
 * New GUI manager using the unified architecture.
 * Replaces the old GUI manager with cleaner separation of concerns.
 */
public class GuiManager extends Application implements ViewNavigator.ViewStateChangeListener, UI {
    
    private static final Logger LOGGER = Logger.getLogger(GuiManager.class.getName());
    
    private static ClientController staticController;
    private static volatile boolean initialized = false;
    
    private GuiContext context;
    private Stage primaryStage;
    private Map<ClientModel.ViewState, UIView> views;
    private UIView currentView;
    
    public GuiManager() {}
    
    public GuiManager(ClientController controller) {
        staticController = controller;
    }
    
    public static void launch(ClientController controller) {
        staticController = controller;
        if (!initialized) {
            initialized = true;
            // Launch JavaFX application in a separate thread
            Thread fxThread = new Thread(() -> Application.launch(GuiManager.class));
            fxThread.setDaemon(false);
            fxThread.start();
        }
    }
    
    @Override
    public void start() {
        if (staticController != null) {
            launch(staticController);
        }
    }
    
    @Override
    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        // Load custom font globally for the GUI
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/OrgovanRounded.ttf"), 16);
        } catch (Exception e) {
            System.err.println("Could not load OrgovanRounded font: " + e.getMessage());
        }
        
        // Create GUI context
        context = new GuiContext(staticController, primaryStage);
        
        // Set the current UI context for global access (needed for message handling)
        UIContextProvider.setCurrent(context);
        
        // Initialize views
        initializeViews();
        
        // Set up view navigation listener
        context.getViewNavigator().addViewStateChangeListener(this);
        
        // Configure stage
        primaryStage.setTitle("Galaxy Trucker");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        // Show initial view
        navigateToCurrentView();
        
        LOGGER.info("GUI Manager started successfully");
    }
    
    private void initializeViews() {
        views = new HashMap<>();
        
        // Create and initialize all views
        GuiConnectionView connectionView = new GuiConnectionView(primaryStage);
        connectionView.initialize(context);
        views.put(ClientModel.ViewState.CONNECTION, connectionView);
        
        GuiLoginView loginView = new GuiLoginView(primaryStage);
        loginView.initialize(context);
        views.put(ClientModel.ViewState.LOGIN, loginView);
        
        GuiLobbyView lobbyView = new GuiLobbyView(primaryStage);
        lobbyView.initialize(context);
        views.put(ClientModel.ViewState.LOBBY, lobbyView);

        GuiGameLobbyView gameLobbyView = new GuiGameLobbyView(primaryStage);
        gameLobbyView.initialize(context);
        views.put(ClientModel.ViewState.GAME_LOBBY, gameLobbyView);
        
        GuiShipBuildingView shipBuildingView = new GuiShipBuildingView(primaryStage, staticController);
        shipBuildingView.initialize(context);
        views.put(ClientModel.ViewState.GAME, shipBuildingView);
        
        // TODO: Add other views
    }
    
    private void navigateToCurrentView() {
        ClientModel.ViewState targetState = context.getModel().getCurrentView();
        showView(targetState);
    }
    
    private void showView(ClientModel.ViewState viewState) {
        // Hide current view
        if (currentView != null && currentView.isActive()) {
            currentView.hide();
        }
        
        // Show new view
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
    public void onViewStateChanged(ClientModel.ViewState oldState, ClientModel.ViewState newState) {
        Platform.runLater(() -> showView(newState));
    }
    
    public boolean isRunning() {
        return primaryStage != null && primaryStage.isShowing();
    }
    
    public void shutdown() {
        if (primaryStage != null) {
            Platform.runLater(() -> {
                // Dispose all views
                for (UIView view : views.values()) {
                    view.dispose();
                }
                
                // Close stage
                primaryStage.close();
                
                // Shutdown thread service
                context.getThreadService().shutdown();
                
                LOGGER.info("GUI Manager shut down successfully");
            });
        }
    }
}
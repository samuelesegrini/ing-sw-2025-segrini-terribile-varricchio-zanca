package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.ui.AbstractUserInterface;
import it.polimi.ingsw.client.view.ViewModelAwareController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX implementation of the UserInterface
 */
public class JavaFXGUI extends AbstractUserInterface {
    private static final Logger LOGGER = Logger.getLogger(JavaFXGUI.class.getName());
    
    private Stage primaryStage;
    
    public JavaFXGUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    @Override
    public void initialize() {
        // Set up any JavaFX-specific initialization
        primaryStage.setOnCloseRequest(event -> {
            if (clientController != null) {
                clientController.requestShutdown();
            }
            Platform.exit();
        });
    }
    
    @Override
    public void start() {
        // Start with the initial view
        LOGGER.info("Starting JavaFXGUI - ViewModel: " + (viewModel != null ? "OK" : "NULL"));
        
        Platform.runLater(() -> {
            LOGGER.info("Inside Platform.runLater for start() method");
            
            if (viewModel != null) {
                // Force loading the initial view
                LOGGER.info("Forcing initial view with LOGIN_SCREEN state");
                viewModel.setAppStatus(ClientViewModel.AppStatus.LOGIN_SCREEN);
                
                // Directly call handleAppStatusChange to ensure view is loaded
                LOGGER.info("Directly calling handleAppStatusChange to ensure view is loaded");
                handleAppStatusChange(ClientViewModel.AppStatus.LOGIN_SCREEN);
            } else {
                LOGGER.severe("ViewModel is NULL during start - cannot set initial state!");
                // Show fallback screen
                Label errorLabel = new Label("Error: Application not properly initialized");
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                Scene scene = new Scene(new StackPane(errorLabel), 400, 200);
                primaryStage.setScene(scene);
            }
            
            LOGGER.info("Showing primary stage");
            primaryStage.show();
        });
    }
    
    @Override
    protected void configureViewModelListeners() {
        // Set up listeners for ViewModel changes
        if (viewModel != null) {
            viewModel.appStatusProperty().addListener((obs, oldStatus, newStatus) -> {
                LOGGER.info("ViewModel status changed: " + oldStatus + " -> " + newStatus);
                Platform.runLater(() -> handleAppStatusChange(newStatus));
            });
        }
    }
    
    @Override
    public void handleAppStatusChange(ClientViewModel.AppStatus newStatus) {
        LOGGER.info("JavaFX GUI handling view transition to AppStatus: " + newStatus);
        try {
            Parent root = null;
            String fxmlPath = null;
            String title = "Galaxy Trucker"; // Default title

            switch (newStatus) {
                case NOT_CONNECTED:
                case LOGIN_SCREEN:
                case CONNECTING:
                case LOGGING_IN:
                case DISCONNECTED:
                    fxmlPath = "/it/polimi/ingsw/client/view/ConnectionView.fxml";
                    title = "Galaxy Trucker - Connect";
                    break;

                case LOGGED_IN_BROWSING_LOBBIES:
                    fxmlPath = "/it/polimi/ingsw/client/view/LobbyBrowserView.fxml";
                    title = "Galaxy Trucker - Lobbies (" + viewModel.loggedInNicknameProperty().get() + ")";
                    break;

                case GAME_LOBBY:
                    fxmlPath = "/it/polimi/ingsw/client/view/GameLobbyView.fxml";
                    String gameName = (viewModel.currentLobbyInfoProperty().get() != null) ? 
                            viewModel.currentLobbyInfoProperty().get().getGameName() : "Unknown Game";
                    title = "Galaxy Trucker - Lobby: " + gameName;
                    break;

                case GAME_BUILDING:
                    fxmlPath = "/it/polimi/ingsw/client/view/BuildingView.fxml";
                    title = "Galaxy Trucker - Building";
                    break;
                    
                case GAME_FLIGHT:
                    fxmlPath = "/it/polimi/ingsw/client/view/FlightView.fxml";
                    title = "Galaxy Trucker - Flight";
                    break;
                    
                case GAME_FINISHED:
                    fxmlPath = "/it/polimi/ingsw/client/view/FinishedView.fxml";
                    title = "Galaxy Trucker - Game Over";
                    break;

                default:
                    LOGGER.warning("Unhandled AppStatus: " + newStatus + ". Displaying placeholder.");
                    Label placeholder = new Label("App Status: " + newStatus + "\nUI not implemented yet.");
                    root = new StackPane(placeholder, new Label(viewModel.statusMessageProperty().get()));
                    title = "Galaxy Trucker - " + newStatus;
                    break;
            }

            if (fxmlPath != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                root = loader.load();
                
                // Get controller and inject ViewModel using ViewModelAwareController interface
                ViewModelAwareController controller = (ViewModelAwareController) loader.getController();
                controller.setViewModel(viewModel);
                controller.setClientApp(clientController);
                controller.setPrimaryStage(primaryStage);
                LOGGER.fine("ViewModel injected into " + controller.getClass().getSimpleName());
            }

            if (root != null) {
                Scene currentScene = primaryStage.getScene();
                if (currentScene == null) {
                    currentScene = new Scene(root);
                    
                    // Add application stylesheet
                    String cssPath = getClass().getResource("/it/polimi/ingsw/client/view/styles.css").toExternalForm();
                    currentScene.getStylesheets().add(cssPath);
                    
                    primaryStage.setScene(currentScene);
                } else {
                    currentScene.setRoot(root);
                }
                primaryStage.setTitle(title);
                primaryStage.sizeToScene();
                primaryStage.show();
            } else {
                LOGGER.severe("Root pane is null after attempting to load UI for status: " + newStatus);
                showError("Fatal Error", "Could not load UI for state " + newStatus);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading FXML for status: " + newStatus, e);
            showError("Fatal Error", "Exception loading UI for state " + newStatus + "\n" + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during view transition for status: " + newStatus, e);
            showError("Fatal Error", "Unexpected error during view transition for state " + newStatus + "\n" + e.getMessage());
        }
    }
    
    @Override
    public void showConnectionPrompt() {
        // In JavaFX, the connection UI is loaded through FXML
        // This will be triggered by setting the appropriate AppStatus
        if (viewModel != null) {
            viewModel.setAppStatus(ClientViewModel.AppStatus.LOGIN_SCREEN);
        }
    }
    
    @Override
    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
        Platform.runLater(() -> {
            try {
                if (primaryStage != null && primaryStage.isShowing()) {
                    primaryStage.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during JavaFX shutdown", e);
            }
        });
    }
} 
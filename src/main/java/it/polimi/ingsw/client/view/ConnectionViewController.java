package it.polimi.ingsw.client.view;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;

// Updated import for the ViewModelAwareController interface
import it.polimi.ingsw.client.view.ViewModelAwareController;

import java.util.Objects;
import java.util.logging.Logger;


public class ConnectionViewController implements ViewModelAwareController {
    private static final Logger LOGGER = Logger.getLogger(ConnectionViewController.class.getName());

    @FXML private VBox rootPane;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField nicknameField;
    @FXML private ComboBox<String> techComboBox;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator connectionProgress;

    private GameClientController clientController; // Need controller to trigger the connection setup
    private ClientViewModel viewModel; // Injected by controller


    @FXML
    public void initialize() {
        LOGGER.info("ConnectionViewController initialize called.");
        hostField.setText("localhost");
        portField.setText("12345");
        techComboBox.getItems().addAll("Socket", "RMI"); // RMI is placeholder
        techComboBox.setValue("Socket");
        
        // Initialize the progress indicator as invisible
        if (connectionProgress != null) {
            connectionProgress.setVisible(false);
        }
    }

    /**
     * Sets the ViewModel and binds UI elements to ViewModel properties.
     * Called by GameClientController after loading the FXML.
     * @param viewModel The ClientViewModel instance.
     */
    @Override
    public void setViewModel(ClientViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
        LOGGER.info("ConnectionViewController received ViewModel. Binding data.");

        // Bind status label text to ViewModel's statusMessage
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Bind connect button disable state based on ViewModel's app status
        // Button is disabled while connecting or logging in
        connectButton.disableProperty().bind(
                viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                        .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
        );
        
        // Show progress indicator during connection and login attempts
        if (connectionProgress != null) {
            connectionProgress.visibleProperty().bind(
                viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                    .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
            );
        }

        // Disable input fields during connection and login
        hostField.disableProperty().bind(
            viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
        );
        portField.disableProperty().bind(
            viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
        );
        nicknameField.disableProperty().bind(
            viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
        );
        techComboBox.disableProperty().bind(
            viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.CONNECTING)
                .or(viewModel.appStatusProperty().isEqualTo(ClientViewModel.AppStatus.LOGGING_IN))
        );
    }

    /**
     * Sets the client controller reference
     * @param clientController The game client controller instance
     */
    @Override
    public void setClientApp(GameClientController clientController) {
        this.clientController = Objects.requireNonNull(clientController);
        LOGGER.info("ConnectionViewController received GameClientController reference.");
    }

    @Override
    public void setPrimaryStage(javafx.stage.Stage stage) {
        // Not used in this controller but required by interface
    }

    public VBox getRootPane() {
        return rootPane; // Might be needed by controller for scene management
    }

    @FXML
    private void handleConnectButtonAction() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String technology = techComboBox.getValue();

        if (host.isEmpty() || portStr.isEmpty() || nickname.isEmpty()) {
            viewModel.setStatusMessage("Error: All fields (Host, Port, Nickname) must be filled.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                viewModel.setStatusMessage("Error: Port must be a number between 1 and 65535.");
                return;
            }
        } catch (NumberFormatException e) {
            viewModel.setStatusMessage("Error: Port must be a valid number.");
            return;
        }

        if (nickname.length() < 3 || nickname.length() > 20) {
            viewModel.setStatusMessage("Error: Nickname must be 3-20 characters.");
            return;
        }

        if (clientController != null) {
            // Delegate the connection and login process to GameClientController/LoginHandler
            // Status updates ("Connecting...", "Logging in...") are handled by ViewModel
            // based on the calls from ClientLoginHandler.
            LOGGER.info("ConnectionViewController calling GameClientController.setupNetworkAndConnect...");
            clientController.setupNetworkAndConnect(host, port, nickname, technology);
        } else {
            // This shouldn't happen if App is structured correctly, but defensive check
            LOGGER.severe("GameClientController reference not set in ConnectionViewController. Cannot proceed.");
            viewModel.setStatusMessage("Internal error: Controller reference missing.");
        }
    }
}
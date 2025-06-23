package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ProgressIndicator;

/**
 * GUI view for server connection.
 * Migrated to new unified architecture.
 */
public class GuiConnectionView extends BaseUIView {

    private Stage stage;
    private TextField hostnameField;
    private TextField portField;
    private Button connectButton;
    private Label statusLabel;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    public GuiConnectionView(Stage stage) {
        this.stage = stage;
    }

    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.CONNECTION;
    }

    @Override
    public String getTitle() {
        return "Server Connection";
    }

    @Override
    protected void onShow() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("Galaxy Trucker - " + getTitle());
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    @Override
    protected void onHide() {
        // GUI views don't need special hiding logic - handled by scene switching
    }

    @Override
    protected void onRefresh() {
        updateConnectionStatus();
    }

    private Scene createScene() {
        // Root StackPane to allow overlaying loading indicator
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();

        // Main content VBox
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.getStyleClass().addAll("panel-dark-glass", "connection-content-container");

        // Add stylesheet for UITest look
        Scene scene = new Scene(root, 800, 600);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }

        // Logo (placeholder, can be replaced with actual logo)
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/assets/images/logo.png"));
            logoView.setImage(logo);
            logoView.setFitWidth(300);
            logoView.setPreserveRatio(true);
            logoView.getStyleClass().add("login-logo-imageview");
        } catch (Exception e) {
            // fallback text if logo not found
            Label logoPlaceholder = new Label("GALAXY TRUCKER");
            logoPlaceholder.getStyleClass().add("login-logo-placeholder");
            logoView = new ImageView(logoPlaceholder.snapshot(null, null));
            logoView.setFitWidth(350);
            logoView.setPreserveRatio(true);
        }

        // Title
        Label title = new Label("Connect to Server");
        title.getStyleClass().add("label-title");
        title.setAlignment(Pos.CENTER);

        // Server input form
        VBox formBox = new VBox(18);
        formBox.setAlignment(Pos.CENTER);
        formBox.setMaxWidth(400);

        Label hostnameLabel = new Label("Server Hostname:");
        hostnameLabel.getStyleClass().add("login-form-label");
        hostnameField = new TextField("localhost");
        hostnameField.getStyleClass().add("text-field-styled");
        hostnameField.setPrefWidth(300);

        Label portLabel = new Label("Port:");
        portLabel.getStyleClass().add("login-form-label");
        portField = new TextField("12345");
        portField.getStyleClass().add("text-field-styled");
        portField.setPrefWidth(300);

        formBox.getChildren().addAll(hostnameLabel, hostnameField, portLabel, portField);

        // Connect button
        connectButton = new Button("CONNECT");
        connectButton.getStyleClass().addAll("button", "button-primary", "login-connect-button");
        connectButton.setMaxWidth(300);
        connectButton.setDefaultButton(true);
        connectButton.setOnAction(e -> handleConnect());
        if (context != null) {
            connectButton.disableProperty().bind(context.getModel().connectedProperty());
            hostnameField.disableProperty().bind(context.getModel().connectedProperty());
            portField.disableProperty().bind(context.getModel().connectedProperty());
        }

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");
        errorLabel.setVisible(false);

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("progress-indicator-styled");
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(80, 80);

        // Status label (for connection status)
        statusLabel = new Label();
        statusLabel.setFont(Font.font(14));
        statusLabel.setAlignment(Pos.CENTER);

        content.getChildren().addAll(logoView, title, formBox, connectButton, errorLabel, statusLabel);
        root.getChildren().addAll(content, loadingIndicator);
        javafx.scene.layout.StackPane.setAlignment(loadingIndicator, Pos.CENTER);

        return scene;
    }

    private void handleConnect() {
        String hostname = hostnameField.getText().trim();
        String portText = portField.getText().trim();
        if (hostname.isEmpty()) {
            showError("Hostname cannot be empty");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Port must be a valid number");
            return;
        }
        showLoading(true);
        context.getController().connect(hostname, port, true)
            .thenAccept(success -> {
                showLoading(false);
                if (success) {
                    // On success, the model's view will change, triggering navigation.
                } else {
                    showError("Could not connect to the server. Please check the address and port.");
                }
            });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        if (isLoading) {
            errorLabel.setVisible(false);
        }
    }

    private void updateConnectionStatus() {
        if (context.getModel().isConnected()) {
            statusLabel.setText("Connected to server");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("Not connected");
            statusLabel.setTextFill(Color.RED);
        }
    }
}
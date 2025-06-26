package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ProgressIndicator;

/**
 * GUI view for user login.
 * Migrated to new unified architecture.
 */
public class GuiLoginView extends BaseUIView {
    private Stage stage;
    private TextField nicknameField;
    private Button loginButton;
    private Label statusLabel;
    private ProgressIndicator loadingIndicator;
    private Label errorLabel;

    public GuiLoginView(Stage stage) {
        this.stage = stage;
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.LOGIN;
    }

    @Override
    public String getTitle() {
        return "Login";
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
        // Clear loading state when hiding this view
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }

    @Override
    protected void onRefresh() {
        updateLoginStatus();
    }

    private Scene createScene() {
        // Root StackPane to allow overlaying loading indicator
        StackPane root = new StackPane();

        // Main content VBox (acts as a card)
        VBox card = new VBox(24);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(48, 36, 48, 36));
        card.getStyleClass().addAll("panel-dark-glass", "login-content-container");
        card.setMaxWidth(420);

        // Add stylesheet for UITest look
        Scene scene = new Scene(root, 800, 600);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }

        // Title
        Label title = new Label("Sign in to Galaxy Trucker");
        title.getStyleClass().add("label-title");
        title.setAlignment(Pos.CENTER);

        // Subtitle
        Label subtitle = new Label("Enter your nickname to continue");
        subtitle.getStyleClass().add("label-header");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #B0C4DE;");
        subtitle.setAlignment(Pos.CENTER);

        // Form section
        VBox formBox = new VBox(12);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setMaxWidth(340);

        Label nicknameLabel = new Label("Nickname");
        nicknameLabel.getStyleClass().add("login-form-label");
        nicknameLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #E0E0FF; -fx-font-weight: bold;");

        nicknameField = new TextField();
        nicknameField.setPromptText("Enter nickname (3-20 characters)");
        nicknameField.getStyleClass().add("text-field-styled");
        nicknameField.setPrefWidth(320);
        nicknameField.setStyle("-fx-prompt-text-fill: #E0E0E0; -fx-text-fill: white;");

        formBox.getChildren().addAll(nicknameLabel, nicknameField);

        // Login button
        loginButton = new Button("Sign In");
        loginButton.getStyleClass().addAll("button", "button-primary", "login-connect-button");
        loginButton.setMaxWidth(320);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> handleLogin());
        if (context != null) {
            loginButton.disableProperty().bind(context.getClientState().authenticatedProperty());
            nicknameField.disableProperty().bind(context.getClientState().authenticatedProperty());
        }

        // Requirements label (smaller, muted, below the button)
        Label requirementsLabel = new Label("Letters, numbers, underscore and hyphen only");
        requirementsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0A0A0; -fx-padding: 4 0 0 2;");
        requirementsLabel.setAlignment(Pos.CENTER_LEFT);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");
        errorLabel.setVisible(false);

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("progress-indicator-styled");
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(80, 80);

        // Status label
        statusLabel = new Label();
        statusLabel.setFont(Font.font(14));
        statusLabel.setAlignment(Pos.CENTER);

        card.getChildren().setAll(title, subtitle, formBox, loginButton, requirementsLabel, errorLabel, statusLabel);
        root.getChildren().setAll(card, loadingIndicator);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);

        return scene;
    }

    private void handleLogin() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            showError("Please enter a nickname");
            return;
        }
        // Validate nickname format (3-20 chars, letters, numbers, _ or -)
        if (!nickname.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            showError("Nickname must be 3-20 characters: letters, numbers, underscore, or hyphen.");
            return;
        }
        showLoading(true);
        context.getController().login(nickname)
            .thenAccept(success -> {
                showLoading(false);
                if (!success) {
                    showError("The nickname might be taken or is invalid. Please try another one.");
                }
                // On success, view navigation is handled automatically by the model change.
            });
    }

    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    private void showLoading(boolean isLoading) {
        javafx.application.Platform.runLater(() -> {
            loadingIndicator.setVisible(isLoading);
            if (isLoading) {
                errorLabel.setVisible(false);
            }
        });
    }

    private void updateLoginStatus() {
        javafx.application.Platform.runLater(() -> {
            if (context.getClientState().isLoggedIn()) {
                statusLabel.setText("Logged in as: " + context.getClientState().getCurrentNickname());
                statusLabel.setTextFill(Color.GREEN);
            } else {
                statusLabel.setText("");
            }
        });
    }
}
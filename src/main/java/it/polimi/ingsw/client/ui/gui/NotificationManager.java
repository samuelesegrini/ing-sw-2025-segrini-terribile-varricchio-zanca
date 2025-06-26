package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import javafx.scene.control.Alert;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

/**
 * Notification manager for showing in-game notifications.
 */
public class NotificationManager implements NotificationService {
    private final VBox notificationContainer;

    public NotificationManager() {
        this.notificationContainer = new VBox(5);
        

        notificationContainer.setAlignment(Pos.TOP_RIGHT);
        notificationContainer.setPadding(new Insets(10));
        notificationContainer.setMouseTransparent(true);
    }

    public void show(Notification notification) {
        javafx.application.Platform.runLater(() -> {
            NotificationPane pane = new NotificationPane(notification);
            notificationContainer.getChildren().add(pane);

            // Auto-remove after duration
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(5),
                    e -> notificationContainer.getChildren().remove(pane)
            ));
            timeline.play();

            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), pane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
    }

    @Override
    public void showInfo(String title, String message) {
        show(new Notification(title, message, NotificationType.INFO));
    }

    @Override
    public void showSuccess(String title, String message) {
        show(new Notification(title, message, NotificationType.SUCCESS));
    }

    @Override
    public void showWarning(String title, String message) {
        show(new Notification(title, message, NotificationType.WARNING));
    }

    @Override
    public void showError(String title, String message) {
        show(new Notification(title, message, NotificationType.ERROR));
    }
    
    /**
     * Shows detailed validation errors with comprehensive Galaxy Trucker rule violations
     * @param title The error title
     * @param errors List of validation errors
     * @param warnings List of validation warnings (optional)
     */
    public void showValidationErrors(String title, List<String> errors, List<String> warnings) {
        if (errors.isEmpty()) {
            return;
        }
        
        javafx.application.Platform.runLater(() -> {
            ValidationNotificationPane pane = new ValidationNotificationPane(title, errors, warnings);
            notificationContainer.getChildren().add(pane);
            
            // Longer duration for validation errors since they contain important details
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(10), // Extended time to read validation details
                    e -> notificationContainer.getChildren().remove(pane)
            ));
            timeline.play();
            
            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), pane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
    }
    
    /**
     * Shows placement hints and suggestions for component placement
     * @param suggestions List of placement suggestions
     */
    public void showPlacementHints(List<String> suggestions) {
        if (suggestions.isEmpty()) {
            return;
        }
        
        javafx.application.Platform.runLater(() -> {
            PlacementHintPane pane = new PlacementHintPane(suggestions);
            notificationContainer.getChildren().add(pane);
            
            // Medium duration for hints
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(7),
                    e -> notificationContainer.getChildren().remove(pane)
            ));
            timeline.play();
            
            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), pane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
    }

    @Override
    public void showLoading(String message) {
        // Could show a loading overlay
        showInfo("Loading", message);
    }

    @Override
    public void hideLoading() {
        // Could hide loading overlay
    }

    @Override
    public void showNotification(Notification notification) {
        show(notification);
    }

    @Override
    public boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK;
    }
    
    /**
     * Gets the notification container for overlay purposes
     * @return The notification container VBox
     */
    public VBox getNotificationContainer() {
        return notificationContainer;
    }

    private class NotificationPane extends HBox {
        public NotificationPane(Notification notification) {
            setPadding(new Insets(10));
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);

            // Icon based on type
            Label icon = new Label(getIcon(notification.getType()));
            icon.setStyle("-fx-font-size: 20px;");

            // Content
            VBox content = new VBox(2);
            Label title = new Label(notification.getTitle());
            title.setStyle("-fx-font-weight: bold;");
            Label message = new Label(notification.getMessage());
            message.setWrapText(true);
            content.getChildren().addAll(title, message);

            getChildren().addAll(icon, content);

            // Style based on type
            setStyle(getStyleForType(notification.getType()));
        }

        private String getIcon(NotificationType type) {
            return switch (type) {
                case INFO -> "ℹ";
                case SUCCESS -> "✓";
                case WARNING -> "⚠";
                case ERROR -> "✗";
                case CRITICAL -> "🚨";
            };
        }

        private String getStyleForType(NotificationType type) {
            String baseStyle = "-fx-background-radius: 5; -fx-padding: 10; ";
            return baseStyle + switch (type) {
                case INFO -> "-fx-background-color: #3498db;";
                case SUCCESS -> "-fx-background-color: #2ecc71;";
                case WARNING -> "-fx-background-color: #f39c12;";
                case ERROR -> "-fx-background-color: #e74c3c;";
                case CRITICAL -> "-fx-background-color: #8e44ad;"; // Purple for critical
            };
        }
    }
    
    /**
     * Specialized notification pane for displaying detailed validation errors
     */
    private class ValidationNotificationPane extends VBox {
        public ValidationNotificationPane(String title, List<String> errors, List<String> warnings) {
            setPadding(new Insets(12));
            setSpacing(8);
            setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 5; -fx-max-width: 400px;");
            
            // Header with icon and title
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            
            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
            
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            
            header.getChildren().addAll(icon, titleLabel);
            getChildren().add(header);
            
            // Error messages
            if (!errors.isEmpty()) {
                Label errorsHeader = new Label("Validation Errors:");
                errorsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12px;");
                getChildren().add(errorsHeader);
                
                VBox errorsList = new VBox(3);
                for (String error : errors) {
                    Label errorLabel = new Label("• " + error);
                    errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
                    errorLabel.setWrapText(true);
                    errorsList.getChildren().add(errorLabel);
                }
                
                // Scrollable if too many errors
                if (errors.size() > 5) {
                    ScrollPane scrollPane = new ScrollPane(errorsList);
                    scrollPane.setMaxHeight(120);
                    scrollPane.setStyle("-fx-background-color: transparent;");
                    getChildren().add(scrollPane);
                } else {
                    getChildren().add(errorsList);
                }
            }
            
            // Warning messages (if any)
            if (warnings != null && !warnings.isEmpty()) {
                Label warningsHeader = new Label("Suggestions:");
                warningsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffe4b5; -fx-font-size: 12px;");
                getChildren().add(warningsHeader);
                
                VBox warningsList = new VBox(3);
                for (String warning : warnings) {
                    Label warningLabel = new Label("• " + warning);
                    warningLabel.setStyle("-fx-text-fill: #ffe4b5; -fx-font-size: 11px;");
                    warningLabel.setWrapText(true);
                    warningsList.getChildren().add(warningLabel);
                }
                getChildren().add(warningsList);
            }
        }
    }
    
    /**
     * Specialized notification pane for displaying placement hints
     */
    private class PlacementHintPane extends VBox {
        public PlacementHintPane(List<String> suggestions) {
            setPadding(new Insets(12));
            setSpacing(8);
            setStyle("-fx-background-color: #3498db; -fx-background-radius: 5; -fx-max-width: 350px;");
            
            // Header with icon and title
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            
            Label icon = new Label("💡");
            icon.setStyle("-fx-font-size: 20px;");
            
            Label titleLabel = new Label("Placement Hints");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            
            header.getChildren().addAll(icon, titleLabel);
            getChildren().add(header);
            
            // Suggestions list
            VBox suggestionsList = new VBox(3);
            for (String suggestion : suggestions) {
                Label suggestionLabel = new Label("• " + suggestion);
                suggestionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
                suggestionLabel.setWrapText(true);
                suggestionsList.getChildren().add(suggestionLabel);
            }
            getChildren().add(suggestionsList);
        }
    }
}
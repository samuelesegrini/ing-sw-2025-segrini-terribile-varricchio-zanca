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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Notification manager for showing in-game notifications.
 */
public class NotificationManager implements NotificationService {
    private final VBox notificationContainer;
    private final Queue<Notification> notificationQueue;

    public NotificationManager() {
        this.notificationContainer = new VBox(5);
        this.notificationQueue = new LinkedList<>();

        notificationContainer.setAlignment(Pos.TOP_RIGHT);
        notificationContainer.setPadding(new Insets(10));
        notificationContainer.setMouseTransparent(true);
    }

    public void show(Notification notification) {
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
            };
        }

        private String getStyleForType(NotificationType type) {
            String baseStyle = "-fx-background-radius: 5; -fx-padding: 10; ";
            return baseStyle + switch (type) {
                case INFO -> "-fx-background-color: #3498db;";
                case SUCCESS -> "-fx-background-color: #2ecc71;";
                case WARNING -> "-fx-background-color: #f39c12;";
                case ERROR -> "-fx-background-color: #e74c3c;";
            };
        }
    }
}
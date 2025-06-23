package it.polimi.ingsw.client.ui;

/**
 * Notification class.
 */
public class Notification {
    private final String title;
    private final String message;
    private final NotificationType type;
    private final long timestamp;

    public Notification(String title, String message, NotificationType type) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public long getTimestamp() { return timestamp; }
}

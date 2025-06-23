package it.polimi.ingsw.client.ui;

/**
 * Enum for UI types.
 */
public enum UIType {
    TUI("Text User Interface"),
    GUI("Graphical User Interface");

    private final String displayName;

    UIType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

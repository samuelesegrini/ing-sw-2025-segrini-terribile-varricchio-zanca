package it.polimi.ingsw.client.ui.tui;

/**
 * Base interface for TUI views.
 */
public interface TuiView {
    
    /**
     * Display the view to the user.
     */
    void display();
    
    /**
     * Handle user input for this view.
     * @param input The user input string
     */
    void handleInput(String input);
    
    /**
     * Get the prompt message for this view.
     * @return The prompt string
     */
    String getPrompt();
    
    /**
     * Refresh the view with updated data.
     */
    void refresh();
}
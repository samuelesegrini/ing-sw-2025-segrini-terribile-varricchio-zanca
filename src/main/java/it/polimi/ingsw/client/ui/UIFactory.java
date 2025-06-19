package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.ui.gui.JavaFXGUI;
import it.polimi.ingsw.client.ui.tui.TUI;
//import it.polimi.ingsw.client.ui.tui.TerminalUI;
import javafx.stage.Stage;

/**
 * Factory class for creating UI components based on the selected UI mode.
 * Provides centralized instantiation of UI interfaces and thread handlers.
 */
public class UIFactory {
    
    /**
     * Creates a UserInterface instance based on the specified UI mode.
     * 
     * @param mode The UI mode (GUI or TUI)
     * @param primaryStage The JavaFX stage (required for GUI mode only)
     * @return A concrete UserInterface implementation
     * @throws IllegalArgumentException if the primaryStage is null in GUI mode or if an unsupported mode is provided
     */
    public static UserInterface createUserInterface(GameClientController.UIMode mode, Stage primaryStage) {
        switch (mode) {
            case GUI:
                if (primaryStage == null) {
                    throw new IllegalArgumentException("Primary stage cannot be null for GUI mode");
                }
                return new JavaFXGUI(primaryStage);
                
            case TUI:
                //return new TerminalUI();
                try {
                    return new TUI();
                } catch (Exception e) {
                    System.err.println("Error initializing TUI: " + e.getMessage());
                    e.printStackTrace();
                    throw new IllegalArgumentException("Failed to initialize TUI", e);
                }
                
            default:
                throw new IllegalArgumentException("Unsupported UI mode: " + mode);
        }
    }
    
    /**
     * Creates an appropriate UIThreadHandler implementation for the specified UI mode.
     * This is used to handle threading concerns specific to each UI type.
     * 
     * @param mode The UI mode (GUI or TUI)
     * @return A UIThreadHandler implementation (JavaFXThreadHandler for GUI, DirectThreadHandler for TUI)
     * @throws IllegalArgumentException if an unsupported mode is provided or if mode is null
     */
    public static UIThreadHandler createThreadHandler(GameClientController.UIMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("UI mode cannot be null");
        }
        
        switch (mode) {
            case GUI:
                return new it.polimi.ingsw.client.ui.gui.JavaFXThreadHandler();
                
            case TUI:
                return new it.polimi.ingsw.client.ui.tui.DirectThreadHandler();
                
            default:
                throw new IllegalArgumentException("Unsupported UI mode: " + mode);
        }
    }
}
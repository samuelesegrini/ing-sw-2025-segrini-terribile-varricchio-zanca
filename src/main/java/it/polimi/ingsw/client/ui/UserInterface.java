package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;

/**
 * Interface defining the contract for user interfaces in the Galaxy Trucker client application.
 * Provides abstraction to support both GUI (JavaFX) and TUI (Terminal) implementations
 * while maintaining consistent functionality.
 */
public interface UserInterface {
    /**
     * Initializes the user interface components and prepares for rendering.
     * This should be called before start().
     */
    void initialize();
    
    /**
     * Starts the user interface and begins user interaction.
     * This is called after initialization and dependency injection.
     */
    void start();
    
    /**
     * Sets the client controller reference for application-level operations.
     * Must be called before initialize().
     * 
     * @param clientController The game client controller instance
     * @throws NullPointerException if clientController is null
     */
    void setClientController(GameClientController clientController);
    
    /**
     * Sets the view model for data binding and state observation.
     * Must be called before initialize().
     * 
     * @param viewModel The client view model
     * @throws NullPointerException if viewModel is null
     */
    void setViewModel(ClientViewModel viewModel);
    
    /**
     * Handles changes in application status by updating the UI accordingly.
     * Called when transitioning between different application states.
     * 
     * @param newStatus The new application status
     */
    void handleAppStatusChange(ClientViewModel.AppStatus newStatus);
    
    /**
     * Displays a connection prompt for entering server connection details.
     * This is typically called when the application first starts or reconnection is needed.
     */
    void showConnectionPrompt();
    
    /**
     * Displays an error message to the user in a UI-appropriate way.
     * 
     * @param title The error title/header
     * @param message The detailed error message
     */
    void showError(String title, String message);
    
    /**
     * Displays an informational message to the user in a UI-appropriate way.
     * 
     * @param title The information title/header
     * @param message The detailed information message
     */
    void showInfo(String title, String message);
    
    /**
     * Performs cleanup operations before application shutdown.
     * Releases any resources held by the UI implementation.
     */
    void shutdown();
} 
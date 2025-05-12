package it.polimi.ingsw.client.view;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import javafx.stage.Stage;

/**
 * Interface for view controllers that need access to the ClientViewModel.
 * All FXML controllers should implement this interface to allow for uniform view model injection.
 */
public interface ViewModelAwareController {
    
    /**
     * Sets the ViewModel for this controller.
     * Should be called after the controller is loaded but before it's used.
     * 
     * @param viewModel The ClientViewModel instance
     */
    void setViewModel(ClientViewModel viewModel);
    
    /**
     * Sets the client controller reference for controllers that need to interact with the app.
     * Default implementation does nothing - controllers should override if needed.
     * 
     * @param clientController The GameClientController instance
     */
    default void setClientApp(GameClientController clientController) {
        // Default implementation does nothing
        // Controllers that need the reference should override this method
    }
    
    /**
     * Sets the primary stage reference for controllers that need to open dialogs.
     * Default implementation does nothing - controllers should override if needed.
     * 
     * @param primaryStage The primary Stage instance
     */
    default void setPrimaryStage(Stage primaryStage) {
        // Default implementation does nothing
        // Controllers that need to open dialogs should override this method
    }
} 
package it.polimi.ingsw.client;

import it.polimi.ingsw.client.model.ClientViewModel;

/**
 * Interface for controllers that need access to the ClientViewModel.
 * This is typically implemented by JavaFX controllers to receive
 * and bind to the application's view model.
 */
public interface ViewModelAwareController {
    /**
     * Sets the view model for this controller.
     * Implementations should use this to establish data bindings
     * and listen for relevant state changes.
     * 
     * @param viewModel The client view model instance
     */
    void setViewModel(ClientViewModel viewModel);
} 
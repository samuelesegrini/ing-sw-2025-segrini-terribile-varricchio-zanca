package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;

import java.util.logging.Logger;

/**
 * Abstract implementation of the UserInterface that provides common functionality
 * for both GUI and TUI implementations.
 * 
 * This class manages shared state and behavior, reducing code duplication in concrete
 * UI implementations while enforcing consistent handling of controller and view model
 * references.
 */
public abstract class AbstractUserInterface implements UserInterface {
    protected static final Logger LOGGER = Logger.getLogger(AbstractUserInterface.class.getName());
    
    protected GameClientController clientController;
    protected ClientViewModel viewModel;

    /**
     * Sets the client controller reference.
     * Stores the reference for use by concrete implementations.
     * 
     * @param clientController The game client controller instance
     */
    @Override
    public void setClientController(GameClientController clientController) {
        this.clientController = clientController;
    }
    
    /**
     * Sets the view model and configures view model listeners.
     * Stores the reference and calls the template method for listener setup.
     * 
     * @param viewModel The client view model
     */
    @Override
    public void setViewModel(ClientViewModel viewModel) {
        this.viewModel = viewModel;
        configureViewModelListeners();
    }
    
    /**
     * Template method for configuring view model listeners.
     * Concrete implementations should override this to set up specific listeners
     * that respond to view model state changes.
     */
    protected abstract void configureViewModelListeners();
    
    /**
     * Performs cleanup operations before application shutdown.
     * This default implementation logs the shutdown event.
     * Concrete implementations should override this if they need to
     * release specific resources.
     */
    @Override
    public void shutdown() {
        LOGGER.fine("AbstractUserInterface shutdown called");
    }
} 
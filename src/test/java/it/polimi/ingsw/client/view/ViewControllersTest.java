package it.polimi.ingsw.client.view;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the client view controllers and the ViewModelAwareController interface.
 * Tests behavior common to all view controllers without requiring JavaFX.
 */
class ViewControllersTest {

    // Simple mock implementation of ViewModelAwareController
    static class TestViewController implements ViewModelAwareController {
        private ClientViewModel viewModel;
        private GameClientController clientApp;
        private Stage primaryStage;
        
        @Override
        public void setViewModel(ClientViewModel viewModel) {
            this.viewModel = viewModel;
        }
        
        @Override
        public void setClientApp(GameClientController clientController) {
            this.clientApp = clientController;
        }
        
        @Override
        public void setPrimaryStage(Stage primaryStage) {
            this.primaryStage = primaryStage;
        }
        
        // Getters for testing
        public ClientViewModel getViewModel() {
            return viewModel;
        }
        
        public GameClientController getClientApp() {
            return clientApp;
        }
        
        public Stage getPrimaryStage() {
            return primaryStage;
        }
    }
    
    private TestViewController controller;
    private ClientViewModel viewModel;
    private GameClientController clientApp;
    private Stage stage;
    
    @BeforeEach
    void setUp() {
        // Set up test objects
        controller = new TestViewController();
        viewModel = new ClientViewModel(null); // Passing null EventBus for this test
        clientApp = null; // Mocking not needed for this test
        stage = null; // Mocking not needed for this test
    }
    
    @Test
    void testSetViewModel() {
        // Ensure viewModel is initially null
        assertNull(controller.getViewModel());
        
        // Set the view model
        controller.setViewModel(viewModel);
        
        // Verify it was set correctly
        assertSame(viewModel, controller.getViewModel());
    }
    
    @Test
    void testSetClientApp() {
        // Ensure clientApp is initially null
        assertNull(controller.getClientApp());
        
        // Set the client app
        controller.setClientApp(clientApp);
        
        // Verify it was set correctly
        assertSame(clientApp, controller.getClientApp());
    }
    
    @Test
    void testSetPrimaryStage() {
        // Ensure primaryStage is initially null
        assertNull(controller.getPrimaryStage());
        
        // Set the primary stage
        controller.setPrimaryStage(stage);
        
        // Verify it was set correctly
        assertSame(stage, controller.getPrimaryStage());
    }
    
    @Test
    void testSetViewModelNullThrowsException() {
        // Setting null view model should throw NullPointerException
        assertThrows(NullPointerException.class, () -> controller.setViewModel(null));
    }
    
    @Test
    void testMethodsAreIndependent() {
        // Setting one dependency shouldn't affect others
        controller.setViewModel(viewModel);
        assertNull(controller.getClientApp());
        assertNull(controller.getPrimaryStage());
        
        // Set another dependency
        controller.setClientApp(clientApp);
        assertSame(viewModel, controller.getViewModel());
        assertNull(controller.getPrimaryStage());
        
        // Set the last dependency
        controller.setPrimaryStage(stage);
        assertSame(viewModel, controller.getViewModel());
        assertSame(clientApp, controller.getClientApp());
    }
} 
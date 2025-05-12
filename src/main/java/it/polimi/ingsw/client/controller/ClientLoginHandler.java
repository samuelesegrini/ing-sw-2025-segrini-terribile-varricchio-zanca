package it.polimi.ingsw.client.controller;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel; // Import ViewModel
import it.polimi.ingsw.client.network.ClientNetworkManager;
import it.polimi.ingsw.client.ui.UIFactory;
import it.polimi.ingsw.client.ui.UIThreadHandler;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
import it.polimi.ingsw.common.message.system.ClientLoginRequest;
import it.polimi.ingsw.common.message.system.ErrorMessage; // Keep for potential logging/re-dispatching
import it.polimi.ingsw.common.message.system.ServerLoginResponse;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;

public class ClientLoginHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientLoginHandler.class.getName());
    private ClientNetworkManager networkManager;
    private final EventBus clientEventBus;
    private final ClientViewModel clientViewModel;
    private ExecutorService backgroundTaskExecutor;
    private final UIThreadHandler uiThreadHandler;

    private String lastAttemptedNickname = null; // Still useful temporarily during login attempt

    public ClientLoginHandler(EventBus clientEventBus, ClientViewModel clientViewModel) {
        this.clientEventBus = Objects.requireNonNull(clientEventBus);
        this.clientViewModel = Objects.requireNonNull(clientViewModel);
        this.clientEventBus.register(this); // Register to listen for server responses
        
        // Create the appropriate UI thread handler based on ClientViewModel's GUI mode
        this.uiThreadHandler = UIFactory.createThreadHandler(ClientViewModel.isGUIMode() ? 
            GameClientController.UIMode.GUI : GameClientController.UIMode.TUI);
            
        LOGGER.info("ClientLoginHandler initialized and registered with EventBus.");
    }

    public void setNetworkManager(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // Add setter for background task executor
    public void setBackgroundTaskExecutor(ExecutorService backgroundTaskExecutor) {
        this.backgroundTaskExecutor = Objects.requireNonNull(backgroundTaskExecutor);
    }

    /**
     * Initiates the network connection and sends the login request.
     * Updates ViewModel state based on connection and request outcome.
     * Operations are performed on a background thread to avoid blocking the UI.
     */
    public void initiateConnectionAndLogin(String host, int port, String nickname) {
        if (this.networkManager == null) {
            LOGGER.severe("NetworkManager is null in ClientLoginHandler. Cannot connect.");
            clientViewModel.handleConnectionFailed("Internal error: Network manager not available.");
            return;
        }
        
        if (this.backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null in ClientLoginHandler. Cannot connect in background.");
            clientViewModel.handleConnectionFailed("Internal error: Background task executor not available.");
            return;
        }

        this.lastAttemptedNickname = nickname.trim();
        clientViewModel.handleConnectionInitiated(); // Update UI immediately to show connecting status
        
        // Perform connection and login in background thread
        backgroundTaskExecutor.submit(() -> {
            try {
                LOGGER.info("Attempting to connect to " + host + ":" + port + " on background thread");
                networkManager.connect(host, port); // Potentially blocking operation now on background thread
                
                if (networkManager.isConnected()) {
                    LOGGER.info("Successfully connected to " + host + ":" + port + ". Sending login request.");
                    
                    // Update UI to show logged in status - using UI thread handler
                    uiThreadHandler.runOnUIThread(() -> clientViewModel.handleConnected());
                    
                    ClientLoginRequest loginRequest = new ClientLoginRequest(this.lastAttemptedNickname);
                    boolean sent = networkManager.sendMessage(loginRequest);
                    
                    if (!sent) {
                        LOGGER.warning("Failed to send login request for nickname: " + this.lastAttemptedNickname);
                        uiThreadHandler.runOnUIThread(() -> 
                            clientViewModel.handleConnectionFailed("Could not send login request after connection."));
                    }
                } else {
                    LOGGER.warning("Connection attempt finished, but not connected.");
                    uiThreadHandler.runOnUIThread(() -> 
                        clientViewModel.handleConnectionFailed("Failed to establish connection (adapter reported not connected)."));
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Connection to " + host + ":" + port + " failed: " + e.getMessage(), e);
                uiThreadHandler.runOnUIThread(() -> 
                    clientViewModel.handleConnectionFailed("Connection error: " + e.getMessage()));
            } catch (Exception e) {
                // Catch any other unexpected exceptions during connection process
                LOGGER.log(Level.SEVERE, "Unexpected error during connection: " + e.getMessage(), e);
                uiThreadHandler.runOnUIThread(() -> 
                    clientViewModel.handleConnectionFailed("Unexpected error: " + e.getMessage()));
            }
        });
    }

    /**
     * Initiates the network connection and sends the login request with technology selection.
     * This is an overloaded version that includes the technology parameter.
     */
    public void initiateConnectionAndLogin(String host, int port, String nickname, String technology) {
        // Technology is handled by ClientApp before this method is called
        initiateConnectionAndLogin(host, port, nickname);
    }
}
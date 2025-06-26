package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * TUI view for server connection.
 * Migrated to new unified architecture.
 */
public class TuiConnectionView extends BaseUIView {

    private final Scanner scanner;

    public TuiConnectionView(TuiContext context) {
        this.scanner = context.getScanner();
        initialize(context);
    }
    
    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.CONNECTION;
    }
    
    @Override
    public String getTitle() {
        return "Server Connection";
    }
    
    private TuiConsole getConsole() {
        // Access TuiConsole through the UIContext - need to cast safely
        if (context instanceof it.polimi.ingsw.client.ui.tui.TuiContext tuiContext) {
            return tuiContext.getConsole();
        }
        throw new IllegalStateException("Expected TuiContext but got " + context.getClass());
    }
    
    @Override
    protected void onShow() {
        TuiConsole console = getConsole();
        console.clearScreen();
        console.printHeader();
        console.printSectionHeader("SERVER CONNECTION");
        console.printInfo("Please enter the server details to connect.");
        console.println("");
        promptForConnection();
    }
    
    @Override
    protected void onHide() {}
    
    @Override
    protected void onRefresh() {}
    
    private void promptForConnection() {
        TuiConsole console = getConsole();
        // The loop continues as long as this view is active and the client is not connected.
        while (isActive() && !context.getClientState().isConnected()) {
            try {
                // Get hostname
                System.out.print("Enter hostname (default: localhost): ");
                String hostname = scanner.nextLine().trim();
                if (hostname.isEmpty()) {
                    hostname = "localhost";
                }
                
                // Get protocol selection
                System.out.print("Select protocol - (1) Socket, (2) RMI (default: 1): ");
                String protocolInput = scanner.nextLine().trim();
                boolean useSocket = true;
                int defaultPort = 12345;
                
                if (protocolInput.equals("2") || protocolInput.toLowerCase().equals("rmi")) {
                    useSocket = false;
                    defaultPort = 1099;
                    console.printInfo("Selected protocol: RMI");
                } else {
                    console.printInfo("Selected protocol: Socket");
                }
                
                // Get port
                System.out.print("Enter port (default: " + defaultPort + "): ");
                String portInput = scanner.nextLine().trim();
                int port = defaultPort;
                if (!portInput.isEmpty()) {
                    try {
                        port = Integer.parseInt(portInput);
                    } catch (NumberFormatException e) {
                        console.printError("Invalid port number. Using default port " + defaultPort + ".");
                        port = defaultPort;
                    }
                }
                
                // Attempt connection by directly calling the controller
                String protocolName = useSocket ? "Socket" : "RMI";
                console.printLoading("Connecting to " + hostname + ":" + port + " via " + protocolName);
                
                // We use .get() here to block the TUI input loop until the connection attempt is complete.
                boolean success = context.getController().connect(hostname, port, useSocket).get();
                
                if (success) {
                    // On success, the controller changes the model's view state, which will
                    // cause the TuiManager to switch to the Login view, breaking this loop.
                    console.printSuccess("Connection successful!");
                } else {
                    console.printError("Connection failed. Please try again.");
                    promptRetry();
                }
                
            } catch (ExecutionException | InterruptedException e) {
                console.printError("Connection error: " + e.getMessage());
                promptRetry();
                // If the thread was interrupted, restore the interrupt status.
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break; // Exit the loop if interrupted
                }
            }
        }
    }
    
    private void promptRetry() {
        TuiConsole console = getConsole();
        console.println("");
        System.out.print("Try again? (y/n): ");
        String retry = scanner.nextLine().trim().toLowerCase();
        if (!retry.equals("y") && !retry.equals("yes")) {
            console.printInfo("Exiting application.");
            System.exit(0);
        }
        console.println("");
    }
}
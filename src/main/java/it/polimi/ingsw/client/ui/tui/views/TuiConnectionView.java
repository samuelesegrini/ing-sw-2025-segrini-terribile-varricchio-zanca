package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
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
    
    public TuiConnectionView() {
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.CONNECTION;
    }
    
    @Override
    public String getTitle() {
        return "Server Connection";
    }
    
    private TuiConsole getConsole() {
        return ((TuiContext) context).getConsole();
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
    protected void onHide() {
        // TUI views don't need special hiding logic
    }
    
    @Override
    protected void onRefresh() {
        // Called when model properties change, but not needed here as onShow handles the loop.
    }
    
    private void promptForConnection() {
        TuiConsole console = getConsole();
        // The loop continues as long as this view is active and the client is not connected.
        while (isActive() && !context.getModel().isConnected()) {
            try {
                // Get hostname
                System.out.print("Enter hostname (default: localhost): ");
                String hostname = scanner.nextLine().trim();
                if (hostname.isEmpty()) {
                    hostname = "localhost";
                }
                
                // Get port
                System.out.print("Enter port (default: 12345): ");
                String portInput = scanner.nextLine().trim();
                int port = 12345;
                if (!portInput.isEmpty()) {
                    try {
                        port = Integer.parseInt(portInput);
                    } catch (NumberFormatException e) {
                        console.printError("Invalid port number. Using default port 12345.");
                        port = 12345;
                    }
                }
                
                // Attempt connection by directly calling the controller
                console.printLoading("Connecting to " + hostname + ":" + port);
                
                // We use .get() here to block the TUI input loop until the connection attempt is complete.
                boolean success = context.getController().connect(hostname, port, true).get();
                
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
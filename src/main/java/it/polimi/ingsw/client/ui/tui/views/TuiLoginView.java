package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * TUI view for login.
 */
public class TuiLoginView extends BaseUIView {
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    private final TuiConsole console;
    private final Scanner scanner;
    
    public TuiLoginView(TuiContext context) {
        this.console = context.getConsole();
        this.scanner = new Scanner(System.in);
        initialize(context);
    }
    
    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.LOGIN;
    }
    
    @Override
    public String getTitle() {
        return "Login";
    }
    
    @Override
    protected void onShow() {
        console.printSectionHeader("LOGIN");
        console.printSuccess("Connected to server successfully!");
        console.println("Please choose a nickname to continue.");
        console.println("");
        console.println("Nickname requirements:");
        console.println("• 3-20 characters long");
        console.println("• Letters, numbers, underscore and hyphen only");
        console.println("");
        promptForLogin();
    }
    
    @Override
    protected void onHide() {}
    
    @Override
    protected void onRefresh() {
        if (context.getClientState().isLoggedIn()) {
            console.printSuccess("Logged in as: " + context.getClientState().getCurrentNickname());
        }
    }
    
    private void promptForLogin() {
        // Loop as long as this view is active and we are not logged in
        while (isActive() && !context.getClientState().isLoggedIn()) {
            System.out.print("Enter nickname: ");
            String nickname = scanner.nextLine().trim();

            if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
                console.printError("Invalid nickname format. Please try again.");
                continue;
            }

            console.printLoading("Logging in as " + nickname);

            try {
                boolean success = context.getController().login(nickname).get();

                if (!success) {
                    console.printError("Login failed. The nickname might already be taken or is invalid.");
                    // The loop will continue, re-prompting the user.
                }
                // On success, the controller changes the view state, which will make isActive() false
                // for the next iteration, breaking the loop.

            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                console.printError("Login error: " + e.getMessage());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
    }

    private void promptRetry() {
        console.println("");
        // The login loop will continue automatically
    }
}
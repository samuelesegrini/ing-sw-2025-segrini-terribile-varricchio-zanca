package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * TUI view for user login.
 * Migrated to new unified architecture.
 */
public class TuiLoginView extends BaseUIView {
    
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    
    private final Scanner scanner;
    
    public TuiLoginView(TuiContext context) {
        this.scanner = new Scanner(System.in);
        initialize(context);
    }
    
    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.LOGIN;
    }
    
    @Override
    public String getTitle() {
        return "Login";
    }
    
    private TuiConsole getConsole() {
        return ((TuiContext) context).getConsole();
    }
    
    @Override
    protected void onShow() {
        TuiConsole console = getConsole();
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
        if (context.getModel().isLoggedIn()) {
            getConsole().printSuccess("Logged in as: " + context.getModel().getCurrentNickname());
        }
    }
    
    private void promptForLogin() {
        TuiConsole console = getConsole();
        // Loop as long as this view is active and we are not logged in
        while (isActive() && !context.getModel().isLoggedIn()) {
            System.out.print("Enter nickname: ");
            String nickname = scanner.nextLine().trim();

            if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
                console.printError("Invalid nickname format. Please try again.");
                continue; // Ask for nickname again
            }

            console.printLoading("Logging in as " + nickname);

            try {
                // Directly call the controller and block for the result
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
        getConsole().println("");
        // The login loop will continue automatically
    }
}
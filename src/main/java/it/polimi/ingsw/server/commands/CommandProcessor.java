package it.polimi.ingsw.server.commands;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.polimi.ingsw.model.domain.general.GameModel;
// Import Stack if needed for history
// import java.util.Stack;

/**
 * Processes Commands serially to ensure thread-safe modifications to the GameModel.
 * Uses a single-threaded executor internally (as per ADR-0005).
 */
public class CommandProcessor {

    private static final Logger logger = Logger.getLogger(CommandProcessor.class.getName());

    private final GameModel gameModel; // The model instance this processor modifies
    private final ExecutorService commandExecutor; // Single-threaded executor for serial execution
    // private final Stack<Command> commandHistory; // Optional: for logging/debugging

    /**
     * Creates a CommandProcessor for a specific GameModel.
     *
     * @param gameModel The GameModel instance that commands will operate on.
     */
    public CommandProcessor(GameModel gameModel) {
        this.gameModel = gameModel;
        // Create a single-threaded executor to guarantee serial command execution
        this.commandExecutor = Executors.newSingleThreadExecutor();
        // this.commandHistory = new Stack<>();
        logger.info("CommandProcessor initialized.");
    }

    /**
     * Submits a command for execution on the internal serial executor.
     *
     * @param command The command to execute.
     * @return A CompletableFuture that will complete with the CommandResult
     *         once the command has finished executing serially.
     */
    public CompletableFuture<CommandResult> process(Command command) {
        // 1. Log the received command (optional).
        logger.log(Level.FINE, "Received command: {0}", command.getClass().getSimpleName());

        // 2. Submit the command's execute method to the single-threaded executor.
        CompletableFuture<CommandResult> futureResult = CompletableFuture.supplyAsync(() -> {
            try {
                // Execute the command, passing the game model
                // The execute method itself returns a CompletableFuture, but here we wait for its synchronous part if needed,
                // or handle its async result within this block. ADR implies execute performs the *core* mutation serially.
                // Simplest approach: Assume execute() performs mutation and returns result directly or its future completes here.
                // Let's assume execute() returns CompletableFuture<CommandResult> as per the interface.
                // We trigger it but need to get the result to complete *this* future.

                // If command.execute() truly returns a future, we might need to chain differently.
                // Assuming for now execute() primarily does its *synchronous* state change work here
                // and the returned future is for *downstream* async tasks (like notifications).

                // Revisit: If execute() is fully async, the serial guarantee needs care.
                // For now, assume execute blocks or completes its core mutation before returning.
                // A safer pattern might be `CompletableFuture.runAsync(command::executeCoreLogic, commandExecutor).then...`

                // Sticking to the interface: Execute and block for result *within the executor thread*.
                CommandResult result = command.execute(gameModel).join(); // Execute and get result

                // (Optional) Record command history if needed
                // if (result.isSuccess()) { commandHistory.push(command); }

                logger.log(Level.FINE, "Command {0} executed with result: {1}", new Object[]{command.getClass().getSimpleName(), result.isSuccess()});
                return result;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception during command execution: " + command.getClass().getSimpleName(), e);
                return CommandResult.failure("Internal server error during command execution: " + e.getMessage());
            }
        }, commandExecutor);

        // 3. Return the CompletableFuture to the caller.
        return futureResult;
    }

    /**
     * Shuts down the internal command executor.
     * Should be called when the game instance is terminated.
     */
    public void shutdown() {
        logger.info("Shutting down CommandProcessor executor.");
        commandExecutor.shutdown();
        // Add awaitTermination logic if needed
    }
}
package it.polimi.ingsw.server.commands;

import java.util.concurrent.CompletableFuture;

import it.polimi.ingsw.model.domain.general.GameModel;

/**
 * Interface representing a command that modifies the game state.
 * Commands are processed serially by the CommandProcessor to ensure state consistency.
 */
public interface Command {

    /**
     * Executes the command logic, potentially modifying the provided GameModel.
     * This method is executed serially by the CommandProcessor's dedicated executor.
     *
     * @param model The GameModel instance to potentially modify.
     * @return A CompletableFuture containing the result of the command execution (e.g., success/failure, data).
     *         The future should complete once the command logic is finished.
     */
    CompletableFuture<CommandResult> execute(GameModel model);

    /**
     * Checks if the command can be executed in the current game state (optional pre-check).
     * Can be used by the caller (e.g., GameInstanceController) before submitting to the processor.
     *
     * @param model The current GameModel state.
     * @return true if the command can likely be executed, false otherwise.
     */
    default boolean canExecute(GameModel model) {
        // Default implementation allows execution.
        // Specific commands can override this for pre-validation.
        return true;
    }
}
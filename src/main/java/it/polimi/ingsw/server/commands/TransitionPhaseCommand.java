package it.polimi.ingsw.server.commands;

import it.polimi.ingsw.model.domain.general.GameModel;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransitionPhaseCommand implements Command {

    private static final Logger logger = Logger.getLogger(TransitionPhaseCommand.class.getName());

    /**
     * Executes the command logic, potentially modifying the provided GameModel.
     * This method is executed serially by the CommandProcessor's dedicated executor.
     *
     * @param model The GameModel instance to potentially modify.
     * @return A CompletableFuture containing the result of the command execution (e.g., success/failure, data).
     * The future should complete once the command logic is finished.
     */
    @Override
    public CompletableFuture<CommandResult> execute(GameModel model) {

        try {
            // Check if game is in the correct phase to transition
            if (model.getCurrentPhase() == it.polimi.ingsw.model.enums.GamePhase.END) {
                String errorMsg = "Cannot transition phases, game is already in END phase.";
                logger.log(Level.WARNING, errorMsg);
                return CompletableFuture.completedFuture(CommandResult.failure(errorMsg));
            }

            // Transition to the next phase
            model.setCurrentPhase(model.getCurrentPhase().getNextPhase());
            logger.log(Level.INFO, "Transitioned to {0} phase.", model.getCurrentPhase());
            return CompletableFuture.completedFuture(CommandResult.success("Transitioned to next phase successfully.", model.getCurrentPhase()));

        } catch (IllegalStateException | IllegalArgumentException e) {
            // Handle exceptions from GameModel.setCurrentPhase
            logger.log(Level.WARNING, "Failed to execute TransitionPhaseCommand: " + e.getMessage());
            return CompletableFuture.completedFuture(CommandResult.failure(e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected errors
            logger.log(Level.SEVERE, "Unexpected error executing TransitionPhaseCommand " + e);
            return CompletableFuture.completedFuture(CommandResult.failure("Internal server error transitioning phases."));
        }
    }
}

package it.polimi.ingsw.server.commands;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.player.PlayerId;

/**
 * Command to add a new player to the game model.
 */
public class AddPlayerCommand implements Command {

    private static final Logger logger = Logger.getLogger(AddPlayerCommand.class.getName());

    // Store the PlayerId directly
    private final PlayerId playerId;

    /**
     * Constructor for AddPlayerCommand.
     * @param playerId The PlayerId object representing the player to add.
     */
    public AddPlayerCommand(PlayerId playerId) {
        this.playerId = playerId;
    }

    @Override
    public CompletableFuture<CommandResult> execute(GameModel model) {
        try {
            // PlayerId is now provided directly via the constructor

            // Check if game is in the correct phase to add players
            if (model.getCurrentPhase() != it.polimi.ingsw.model.enums.GamePhase.SETUP) {
                String errorMsg = "Cannot add players, game is not in SETUP phase.";
                logger.log(Level.WARNING, errorMsg);
                return CompletableFuture.completedFuture(CommandResult.failure(errorMsg));
            }

            // Add the player using the GameModel's method, providing both PlayerId and nickname
            model.addPlayer(playerId, playerId.getNickname());

            logger.log(Level.INFO, "Player \'{0}\' (ID: {1}) added to GameModel by command.", new Object[]{playerId.getNickname(), playerId});
            // Return success, including the PlayerId in the result data
            return CompletableFuture.completedFuture(CommandResult.success("Player added successfully.", playerId));

        } catch (IllegalStateException | IllegalArgumentException e) {
            // Catch exceptions from GameModel.addPlayer (e.g., max players, duplicate ID/nickname)
            logger.log(Level.WARNING, "Failed to execute AddPlayerCommand for Player ID " + playerId + ": " + e.getMessage());
            return CompletableFuture.completedFuture(CommandResult.failure(e.getMessage()));
        } catch (Exception e) {
            // Catch unexpected errors
            logger.log(Level.SEVERE, "Unexpected error executing AddPlayerCommand for Player ID " + playerId, e);
            return CompletableFuture.completedFuture(CommandResult.failure("Internal server error adding player."));
        }
    }

    // Optional: Override canExecute for pre-validation if needed
    // @Override
    // public boolean canExecute(GameModel model) {
    //     return model.getCurrentPhase() == it.polimi.ingsw.model.enums.GamePhase.SETUP &&
    //            model.getPlayerById(playerId) == null; // Check using the actual PlayerId object
    // }
} 
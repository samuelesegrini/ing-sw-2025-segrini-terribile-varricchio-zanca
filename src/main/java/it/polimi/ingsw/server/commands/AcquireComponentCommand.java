package it.polimi.ingsw.server.commands;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.GamePhase;

/**
 * Command to handle a player acquiring a component from the deck.
 */
public class AcquireComponentCommand implements Command {

    private static final Logger logger = Logger.getLogger(AcquireComponentCommand.class.getName());

    private final PlayerId playerId;
    // We might not need componentType if the model handles drawing randomly
    // private final String componentType; // Or ComponentType enum

    public AcquireComponentCommand(PlayerId playerId /*, String componentType */) {
        this.playerId = playerId;
        // this.componentType = componentType;
    }

    @Override
    public CompletableFuture<CommandResult> execute(GameModel model) {
        try {
            // 1. Validate Phase
            if (model.getCurrentPhase() != GamePhase.BUILDING) {
                return CompletableFuture.completedFuture(CommandResult.failure("Cannot acquire component: Not in BUILDING phase."));
            }

            // 2. Validate Player's Turn (if applicable - depends on game rules)
            // if (!model.getCurrentPlayer().getId().equals(playerId)) {
            //    return CompletableFuture.completedFuture(CommandResult.failure("Cannot acquire component: Not player's turn."));
            // }

            // 3. Draw component from the deck using GameModel method
            Optional<Component> drawnComponentOpt = model.drawComponent();

            if (drawnComponentOpt.isPresent()) {
                Component drawnComponent = drawnComponentOpt.get();
                logger.log(Level.INFO, "Player {0} acquired component {1}", new Object[]{playerId, drawnComponent.getClass().getSimpleName()});

                // TODO: Associate the drawn component with the player (e.g., add to player's hand/inventory)
                // This might require a method on the Player object within the GameModel.
                // model.getPlayerById(playerId).addComponentToHand(drawnComponent);

                // Return success, including the drawn component info (needs conversion to DTO eventually)
                // For now, just return success message.
                return CompletableFuture.completedFuture(CommandResult.success("Component acquired.", drawnComponent)); // Pass model Component for now
            } else {
                // Deck is empty
                logger.log(Level.WARNING, "Player {0} failed to acquire component: Deck is empty.", playerId);
                return CompletableFuture.completedFuture(CommandResult.failure("Component deck is empty."));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error executing AcquireComponentCommand for player " + playerId, e);
            return CompletableFuture.completedFuture(CommandResult.failure("Internal server error acquiring component."));
        }
    }
}
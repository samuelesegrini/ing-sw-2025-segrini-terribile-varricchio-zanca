package it.polimi.ingsw.server.commands;

import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.CargoHold;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AcquireComponentCommandTest {

    private GameModel gameModel;
    private PlayerId playerId;

    @BeforeEach
    void setUp() {
        // Initialize a GameModel for testing
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
        playerId = new PlayerId(UUID.randomUUID(), "Player1");

        gameModel.addPlayer(playerId, "Player1");

        // Create a ComponentDeck with a CargoHold component
        Map<Direction, ConnectorType> connectors = new HashMap<>();
        connectors.put(Direction.UP, ConnectorType.PLAIN);
        connectors.put(Direction.DOWN, ConnectorType.PLAIN);
        connectors.put(Direction.LEFT, ConnectorType.PLAIN);
        connectors.put(Direction.RIGHT, ConnectorType.PLAIN);

        CargoHold cargoHold = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        ComponentDeck componentDeck = new ComponentDeck(List.of(cargoHold));

        // Add the component deck to the game model
        gameModel.setComponentDeck(componentDeck);
    }

    @Test
    void testAcquireComponentSuccess() {
        // Set the correct phase
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Execute the command
        AcquireComponentCommand command = new AcquireComponentCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertTrue(result.isSuccess(), "The command should succeed");
        assertNotNull(result.getData(), "The acquired component should not be null");
    }

    @Test
    void testAcquireComponentInvalidPhase() {
        // Set an incorrect phase
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Execute the command
        AcquireComponentCommand command = new AcquireComponentCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Cannot acquire component: Not in BUILDING phase.", result.getMessage(), "The error message should be correct");
    }

    @Test
    void testAcquireComponentDeckEmpty() {
        //Set the correct phase
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Empty the component deck
        while (gameModel.getComponentDeck().draw().isPresent()) {
            // Continua a pescare finché il mazzo non è vuoto
        }

        // Execute the command
        AcquireComponentCommand command = new AcquireComponentCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Component deck is empty.", result.getMessage(), "The error message should be correct");
    }
}
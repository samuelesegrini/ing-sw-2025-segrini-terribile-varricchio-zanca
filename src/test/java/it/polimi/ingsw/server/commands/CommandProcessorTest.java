package it.polimi.ingsw.server.commands;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.GamePhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class CommandProcessorTest {

    private CommandProcessor commandProcessor;
    private GameModel gameModel;

    @BeforeEach
    void setUp() {
        // Initialize GameModel and CommandProcessor
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
        commandProcessor = new CommandProcessor(gameModel);
    }

    @AfterEach
    void tearDown() {
        // Shutdown the CommandProcessor
        commandProcessor.shutdown();
    }

    @Test
    void testProcessAddPlayerCommandSuccess() {
        // Set the game phase to SETUP
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Create an AddPlayerCommand
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "Player1");
        AddPlayerCommand command = new AddPlayerCommand(playerId);

        // Process the command
        CompletableFuture<CommandResult> futureResult = commandProcessor.process(command);
        CommandResult result = futureResult.join();

        // Verify the result
        assertTrue(result.isSuccess(), "The command should succeed");
        assertEquals("Player added successfully.", result.getMessage(), "The success message should be correct");
    }

    @Test
    void testProcessAddPlayerCommandInvalidPhase() {
        // Set the game phase to BUILDING
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Create an AddPlayerCommand
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "Player1");
        AddPlayerCommand command = new AddPlayerCommand(playerId);

        // Process the command
        CompletableFuture<CommandResult> futureResult = commandProcessor.process(command);
        CommandResult result = futureResult.join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Cannot add players, game is not in SETUP phase.", result.getMessage(), "The error message should be correct");
    }
}
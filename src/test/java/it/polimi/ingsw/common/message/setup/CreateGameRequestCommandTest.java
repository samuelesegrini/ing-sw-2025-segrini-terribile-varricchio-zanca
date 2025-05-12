package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateGameRequestCommandTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(CreateGameRequestCommand.class),
                "CreateGameRequestCommand should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(CreateGameRequestCommand.class),
                "CreateGameRequestCommand should implement Message interface");
    }

    @Test
    void constructorSetsSettings() {
        GameSettingsDTO settings = new GameSettingsDTO("My Game", 4, GameLevel.TEST_FLIGHT);
        CreateGameRequestCommand command = new CreateGameRequestCommand(settings);
        assertEquals(settings, command.getSettings());
    }
    
    @Test 
    void nullSettingsThrowsException() {
        assertThrows(NullPointerException.class, () -> new CreateGameRequestCommand(null));
    }

    @Test
    void fieldImmutability() {
        GameSettingsDTO settings1 = new GameSettingsDTO("Game 1", 2, GameLevel.TEST_FLIGHT);
        GameSettingsDTO settings2 = new GameSettingsDTO("Game 2", 3, GameLevel.LEVEL_II);
        
        CreateGameRequestCommand command1 = new CreateGameRequestCommand(settings1);
        CreateGameRequestCommand command2 = new CreateGameRequestCommand(settings2);
        
        assertEquals(settings1, command1.getSettings());
        assertEquals(settings2, command2.getSettings());
    }

    @Test
    void toStringContainsSettings() {
        GameSettingsDTO settings = new GameSettingsDTO("Multiplayer Game", 3, GameLevel.TEST_FLIGHT);
        CreateGameRequestCommand command = new CreateGameRequestCommand(settings);
        String toString = command.toString();
        
        assertTrue(toString.contains("settings"), "toString should contain settings");
        assertTrue(toString.contains("timestamp"), "toString should contain timestamp");
    }
} 
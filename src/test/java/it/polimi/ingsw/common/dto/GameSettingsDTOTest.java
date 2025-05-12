package it.polimi.ingsw.common.dto;

import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class GameSettingsDTOTest {

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(GameSettingsDTO.class),
                "GameSettingsDTO should implement Serializable");
    }

    @Test
    void constructorSetsAllFields() {
        GameSettingsDTO settings = new GameSettingsDTO("Test Game", 3, GameLevel.TEST_FLIGHT);
        
        assertEquals("Test Game", settings.getGameName());
        assertEquals(3, settings.getMaxPlayers());
        assertEquals(GameLevel.TEST_FLIGHT, settings.getGameLevel());
    }
    
    @Test
    void fieldAccessorsReturnCorrectValues() {
        GameSettingsDTO settings = new GameSettingsDTO("My Game", 4, GameLevel.LEVEL_II);
        
        assertEquals("My Game", settings.getGameName());
        assertEquals(4, settings.getMaxPlayers());
        assertEquals(GameLevel.LEVEL_II, settings.getGameLevel());
    }
    
    @Test
    void toStringContainsAllFields() {
        GameSettingsDTO settings = new GameSettingsDTO("Game Name", 2, GameLevel.TEST_FLIGHT);
        String toString = settings.toString();
        
        assertTrue(toString.contains("Game Name"), "toString should include game name");
        assertTrue(toString.contains("2"), "toString should include max players");
        assertTrue(toString.contains("TEST_FLIGHT"), "toString should include game level");
    }
    
    @Test
    void differentInstancesHaveIndependentValues() {
        GameSettingsDTO settings1 = new GameSettingsDTO("Game 1", 2, GameLevel.TEST_FLIGHT);
        GameSettingsDTO settings2 = new GameSettingsDTO("Game 2", 4, GameLevel.LEVEL_II);
        
        assertEquals("Game 1", settings1.getGameName());
        assertEquals(2, settings1.getMaxPlayers());
        assertEquals(GameLevel.TEST_FLIGHT, settings1.getGameLevel());
        
        assertEquals("Game 2", settings2.getGameName());
        assertEquals(4, settings2.getMaxPlayers());
        assertEquals(GameLevel.LEVEL_II, settings2.getGameLevel());
    }
} 
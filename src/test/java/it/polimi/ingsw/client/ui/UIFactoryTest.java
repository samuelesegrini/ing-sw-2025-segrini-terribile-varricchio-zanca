package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.ui.gui.JavaFXThreadHandler;
import it.polimi.ingsw.client.ui.tui.DirectThreadHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the UIFactory class which creates appropriate UI components
 * based on the selected UI mode (GUI or TUI).
 */
class UIFactoryTest {

    @Test
    void testCreateThreadHandlerGUI() {
        // Create thread handler in GUI mode
        UIThreadHandler handler = UIFactory.createThreadHandler(GameClientController.UIMode.GUI);
        
        // Verify the correct implementation was created
        assertNotNull(handler);
        assertTrue(handler instanceof JavaFXThreadHandler, "Expected JavaFX thread handler in GUI mode");
    }
    
    @Test
    void testCreateThreadHandlerTUI() {
        // Create thread handler in TUI mode
        UIThreadHandler handler = UIFactory.createThreadHandler(GameClientController.UIMode.TUI);
        
        // Verify the correct implementation was created
        assertNotNull(handler);
        assertTrue(handler instanceof DirectThreadHandler, "Expected direct thread handler in TUI mode");
    }
    
    @Test
    void testCreateThreadHandlerWithNullThrowsException() {
        // Passing null should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> UIFactory.createThreadHandler(null));
    }
    
    /**
     * Note: We can't fully test the createUserInterface method as it requires a real JavaFX Stage,
     * which is difficult to mock in a pure JUnit test without running in a JavaFX application thread.
     * Instead, we focus on testing the thread handlers which don't have this requirement.
     */
} 
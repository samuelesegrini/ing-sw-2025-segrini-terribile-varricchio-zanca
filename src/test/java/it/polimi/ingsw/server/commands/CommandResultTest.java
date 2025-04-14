package it.polimi.ingsw.server.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandResultTest {

    @Test
    void testSuccessWithoutMessage() {
        CommandResult result = CommandResult.success();
        assertTrue(result.isSuccess(), "The result should indicate success");
        assertEquals("Command executed successfully.", result.getMessage(), "The default success message should be correct");
        assertNull(result.getData(), "The data should be null for a default success result");
    }

    @Test
    void testSuccessWithMessage() {
        String customMessage = "Custom success message";
        CommandResult result = CommandResult.success(customMessage);
        assertTrue(result.isSuccess(), "The result should indicate success");
        assertEquals(customMessage, result.getMessage(), "The custom success message should be correct");
        assertNull(result.getData(), "The data should be null for a success result with a custom message");
    }

    @Test
    void testFailure() {
        String errorMessage = "An error occurred";
        CommandResult result = CommandResult.failure(errorMessage);
        assertFalse(result.isSuccess(), "The result should indicate failure");
        assertEquals(errorMessage, result.getMessage(), "The failure message should be correct");
        assertNull(result.getData(), "The data should be null for a failure result");
    }
}
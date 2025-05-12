package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMessageTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(ErrorMessage.class),
                "ErrorMessage should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(ErrorMessage.class),
                "ErrorMessage should implement Message interface");
    }

    @Test
    void constructorWithReasonOnly() {
        ErrorMessage message = new ErrorMessage("Test error reason");
        assertEquals("Test error reason", message.getReason());
        assertEquals(ErrorMessage.ErrorType.GENERAL, message.getErrorType());
    }

    @Test
    void constructorWithReasonAndType() {
        ErrorMessage message = new ErrorMessage("Auth failed", ErrorMessage.ErrorType.AUTHENTICATION_FAILURE);
        assertEquals("Auth failed", message.getReason());
        assertEquals(ErrorMessage.ErrorType.AUTHENTICATION_FAILURE, message.getErrorType());
    }

    @ParameterizedTest
    @EnumSource(ErrorMessage.ErrorType.class)
    void supportsAllErrorTypes(ErrorMessage.ErrorType errorType) {
        ErrorMessage message = new ErrorMessage("Error with type", errorType);
        assertEquals(errorType, message.getErrorType());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void throwsExceptionForInvalidReasons(String invalidReason) {
        assertThrows(IllegalArgumentException.class, () -> new ErrorMessage(invalidReason),
                "Constructor should throw IllegalArgumentException for null or empty reasons");
    }

    @Test
    void constructorWithNullErrorTypeUsesGeneralDefault() {
        ErrorMessage message = new ErrorMessage("Error with null type", null);
        assertEquals(ErrorMessage.ErrorType.GENERAL, message.getErrorType(), 
                "Null error type should default to GENERAL");
    }

    @Test
    void toStringContainsAllFields() {
        ErrorMessage message = new ErrorMessage("Error message content", ErrorMessage.ErrorType.NETWORK_ISSUE);
        String toString = message.toString();
        
        assertTrue(toString.contains("Error message content"), "toString should contain reason");
        assertTrue(toString.contains("NETWORK_ISSUE"), "toString should contain error type");
        assertTrue(toString.contains("timestamp"), "toString should contain timestamp");
    }
} 
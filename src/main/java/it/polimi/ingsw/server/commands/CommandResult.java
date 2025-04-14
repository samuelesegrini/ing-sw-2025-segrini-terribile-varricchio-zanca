package it.polimi.ingsw.server.commands;

import java.io.Serializable;

/**
 * Represents the result of executing a Command.
 * Indicates success or failure and can optionally carry data.
 */
public class CommandResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Object data; // Optional data payload (e.g., acquired component, updated state info)

    private CommandResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // --- Factory Methods ---

    public static CommandResult success() {
        return new CommandResult(true, "Command executed successfully.", null);
    }

    public static CommandResult success(String message) {
        return new CommandResult(true, message, null);
    }

    public static CommandResult success(String message, Object data) {
        return new CommandResult(true, message, data);
    }

    public static CommandResult failure(String reason) {
        return new CommandResult(false, reason, null);
    }

    // --- Getters ---

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked") // Caller is responsible for knowing the type
    public <T> T getData() {
        return (T) data;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
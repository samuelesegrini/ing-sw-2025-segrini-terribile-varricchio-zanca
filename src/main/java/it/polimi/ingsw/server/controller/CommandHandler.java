package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.server.controller.CommandContext;

/**
 * Functional interface for handling a specific type of command.
 *
 * @param <C> The specific type of CommandContext, which holds the command and other execution details.
 */
@FunctionalInterface
public interface CommandHandler<C extends CommandContext<?>> { // Generic on CommandContext
    /**
     * Handles the command encapsulated within the given context.
     *
     * @param context The context containing the command to handle and other necessary server resources.
     * @throws Exception if an error occurs during command processing.
     *                   The CommandDispatcher will typically catch this and notify the client.
     */
    void handle(C context) throws Exception;
}
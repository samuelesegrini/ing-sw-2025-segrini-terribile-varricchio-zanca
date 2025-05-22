package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.message.Message;

/**
 * Internal server event indicating an operation requested by a client could not be
 * completed due to an error, typically a validation or state issue found
 * within an ActionController before or after interacting with the domain model.
 * This is used to signal the NotificationController to send an ErrorMessage
 * back to the specific requesting client.
 */
public record InternalOperationErrorEvent(
        String requestingNetworkClientId,
        String reason,
        boolean isPotentiallyFatalToServerLogic // Indicates if this error might require server cleanup for this client
) implements Message {
    private static final long serialVersionUID = 1L;
}

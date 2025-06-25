package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.EventPublisherImpl;
import it.polimi.ingsw.common.message.request.Request;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.request.RequestContextImpl;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command dispatcher that handles self-executing requests.
 */
public class CommandDispatcher {
    private static final Logger LOGGER = Logger.getLogger(CommandDispatcher.class.getName());

    private final GameSessionManager sessionManager;
    private final PlayerSessionRegistry playerRegistry;
    private final ServerNetworkManager networkManager;
    private final EventPublisher eventPublisher;
    private final ExecutorService executorService;
    private final Map<String, String> networkClientToGamePlayerMap;

    public CommandDispatcher(GameSessionManager sessionManager,
                                     PlayerSessionRegistry playerRegistry,
                                     ServerNetworkManager networkManager,
                                     Map<String, String> networkClientToGamePlayerMap) {
        this.sessionManager = sessionManager;
        this.playerRegistry = playerRegistry;
        this.networkManager = networkManager;
        this.networkClientToGamePlayerMap = networkClientToGamePlayerMap;
        this.eventPublisher = new EventPublisherImpl(networkManager, playerRegistry, sessionManager);
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("request-processor-" + t.threadId());
                    return t;
                }
        );
    }

    /**
     * Dispatches a request for processing.
     */
    public void dispatch(Request request, String networkClientId) {
        LOGGER.fine("Dispatching request: " + request.getClass().getSimpleName() +
                " from client: " + networkClientId);

        // Process asynchronously
        executorService.submit(() -> processRequest(request, networkClientId));
    }

    /**
     * Processes a request and sends the response.
     */
    private void processRequest(Request request, String networkClientId) {
        RequestContext context = createContext(networkClientId);

        try {
            // Log request
            LOGGER.info("Processing " + request.getClass().getSimpleName() +
                    " from " + networkClientId);

            // Execute the request - it handles itself!
            Response response = request.execute(context);

            // Send response back to client
            if (response != null) {
                LOGGER.info("Attempting to send response: " + response.getClass().getSimpleName() + 
                           " to client: " + networkClientId);
                boolean sent = networkManager.sendMessageToClient(networkClientId, response);
                LOGGER.info("Response send result: " + sent + " for " + response.getClass().getSimpleName());
            } else {
                LOGGER.warning("No response generated for " + request.getClass().getSimpleName() + 
                              " from " + networkClientId);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing request", e);

            // Send error response
            ErrorResponse errorResponse = new ErrorResponse(
                    request.getCorrelationId(),
                    "Internal server error: " + e.getMessage(),
                    ErrorResponse.INTERNAL_ERROR
            );
            networkManager.sendMessageToClient(networkClientId, errorResponse);
        }
    }

    /**
     * Creates a request context for the given client.
     */
    private RequestContext createContext(String networkClientId) {
        return new RequestContextImpl(
                networkClientId,
                sessionManager,
                playerRegistry,
                eventPublisher,
                networkManager,
                networkClientToGamePlayerMap
        );
    }

    /**
     * Shuts down the dispatcher.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public EventPublisher getEventPublisher() {
        return  eventPublisher;
    }
}
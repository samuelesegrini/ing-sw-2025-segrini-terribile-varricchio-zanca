package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.message.Command;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.server.core.ConnectionMonitorService;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.common.event.EventBus;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandDispatcher {
    private static final Logger LOGGER = Logger.getLogger(CommandDispatcher.class.getName());

    // Maps command class to its specific handler
    @SuppressWarnings("rawtypes") // Suppress warning for CommandHandler without specific CommandContext type argument here
    private final Map<Class<? extends Command>, CommandHandler> handlers = new ConcurrentHashMap<>();

    // Core server components to be passed in CommandContext
    private final GameSessionManager sessionManager;
    private final ServerNetworkManager networkManager;
    private final EventBus serverEventBus;
    private final PlayerSessionRegistry playerSessionRegistry;
    private final Map<String, String> networkClientToGamePlayerMap;
    private final Map<String, String> activePlayersByIdMap;
    private final ExecutorService gameLogicExecutor;
    private final ConnectionMonitorService connectionMonitorService; // New field

    public CommandDispatcher(GameSessionManager sessionManager, ServerNetworkManager networkManager,
                             EventBus serverEventBus, PlayerSessionRegistry playerSessionRegistry,
                             Map<String, String> networkClientToGamePlayerMap,
                             Map<String, String> activePlayersByIdMap,
                             ExecutorService gameLogicExecutor,
                             ConnectionMonitorService connectionMonitorService) { // New parameter
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.networkManager = Objects.requireNonNull(networkManager);
        this.serverEventBus = Objects.requireNonNull(serverEventBus);
        this.playerSessionRegistry = Objects.requireNonNull(playerSessionRegistry);
        this.networkClientToGamePlayerMap = Objects.requireNonNull(networkClientToGamePlayerMap);
        this.activePlayersByIdMap = Objects.requireNonNull(activePlayersByIdMap);
        this.gameLogicExecutor = Objects.requireNonNull(gameLogicExecutor);
        this.connectionMonitorService = Objects.requireNonNull(connectionMonitorService); // Initialize
    }

    /**
     * Registers a handler for a specific command type.
     *
     * @param commandType The class of the command.
     * @param handler     The handler for this command type.
     * @param <T>         The command type.
     * @param <C>         The CommandContext type for this command.
     */
    public <T extends Command, C extends CommandContext<T>> void registerHandler(
            Class<T> commandType,
            CommandHandler<C> handler) {
        Objects.requireNonNull(commandType, "commandType cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        if (handlers.containsKey(commandType)) {
            LOGGER.warning("Handler for command type " + commandType.getSimpleName() + " is being overwritten.");
        }
        handlers.put(commandType, handler);
        LOGGER.info("Registered handler for command type: " + commandType.getSimpleName());
    }

    /**
     * Dispatches a command to its registered handler.
     *
     * @param command         The command to dispatch.
     * @param networkClientId The ID of the client who sent the command.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void dispatch(Command command, String networkClientId) {
        Objects.requireNonNull(command, "command cannot be null");
        Objects.requireNonNull(networkClientId, "networkClientId cannot be null");

        LOGGER.finer("Dispatching command: " + command.getClass().getSimpleName() + " from client: " + networkClientId);

        CommandHandler handler = handlers.get(command.getClass());

        if (handler != null) {
            CommandContext context = new CommandContext<>( // Pass connectionMonitorService here
                    command, networkClientId,
                    this.sessionManager, this.networkManager, this.serverEventBus,
                    this.playerSessionRegistry, this.networkClientToGamePlayerMap,
                    this.activePlayersByIdMap, this.gameLogicExecutor,
                    this.connectionMonitorService // Add new service to context
            );
            try {
                handler.handle(context);
            } catch (Exception e) { // Catch all exceptions from handler logic
                LOGGER.log(Level.SEVERE, "Error executing command " + command.getClass().getSimpleName() +
                        " for client " + networkClientId, e);
                // Send a generic error message back to the client
                networkManager.sendMessageToClient(networkClientId,
                        new ErrorMessage("An error occurred while processing your request: " + e.getMessage(),
                                ErrorMessage.ErrorType.SERVER_INTERNAL));
            }
        } else {
            LOGGER.warning("No handler registered for command type: " + command.getClass().getSimpleName());
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Unknown or unhandled command: " + command.getClass().getSimpleName(),
                            ErrorMessage.ErrorType.ILLEGAL_ACTION));
        }
    }
}
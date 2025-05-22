package it.polimi.ingsw.server.controller.action;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.message.setup.RequestGameListCommand;
import it.polimi.ingsw.common.message.setup.GameListResponseEvent;
import it.polimi.ingsw.server.controller.CommandContext;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.List;
import java.util.logging.Logger;

/**
 * Handles client requests related to browsing available games.
 */
public class GameBrowserActionController {
    private static final Logger LOGGER = Logger.getLogger(GameBrowserActionController.class.getName());

    public GameBrowserActionController() {
        // Constructor can be used for dependency injection if not using CommandContext for everything
        LOGGER.info("GameBrowserActionController initialized.");
    }

    /**
     * Handles a client's request for the list of available and running games.
     */
    public void handleRequestGameList(CommandContext<RequestGameListCommand> context) {
        String networkClientId = context.networkClientId();
        GameSessionManager sessionManager = context.sessionManager();
        ServerNetworkManager networkManager = context.networkManager();

        LOGGER.info("Handling RequestGameListCommand from NetID: " + networkClientId);

        // Ensure the client is actually logged in before serving game lists
        // This map comes from the CommandContext, populated by ServerApp from a central source
        if (!context.networkClientToGamePlayerMap().containsKey(networkClientId)) {
            LOGGER.warning("RequestGameList from non-logged-in NetID: " + networkClientId + ". Ignoring.");
            // Optionally send an error, though usually clients only request list after login
            // networkManager.sendMessageToClient(networkClientId,
            //    new ErrorMessage("You must be logged in to view game lists.", ErrorMessage.ErrorType.AUTHENTICATION_FAILURE));
            return;
        }

        List<GameLobbyInfoDTO> joinableGames = sessionManager.getJoinableGames();
        List<GameLobbyInfoDTO> runningGames = sessionManager.getRunningGames();

        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        networkManager.sendMessageToClient(networkClientId, response);

        LOGGER.fine("Sent GameListResponseEvent to NetID: " + networkClientId +
                " with " + joinableGames.size() + " joinable and " + runningGames.size() + " running games.");
    }
}
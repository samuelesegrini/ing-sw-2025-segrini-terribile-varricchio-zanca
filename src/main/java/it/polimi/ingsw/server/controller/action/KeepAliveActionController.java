package it.polimi.ingsw.server.controller.action;

import it.polimi.ingsw.common.message.system.PingMessage;
import it.polimi.ingsw.common.message.system.PongMessage;
import it.polimi.ingsw.server.controller.CommandContext;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.logging.Logger;

public class KeepAliveActionController {
    private static final Logger LOGGER = Logger.getLogger(KeepAliveActionController.class.getName());

    public KeepAliveActionController() {
        LOGGER.info("KeepAliveActionController initialized.");
    }

    /**
     * Handles an incoming PingMessage from a client. If it's a request, sends a PongMessage back.
     */
    public void handlePing(CommandContext<PingMessage> context) {
        PingMessage ping = context.command();
        String networkClientId = context.networkClientId();
        ServerNetworkManager networkManager = context.networkManager();

        LOGGER.finer("Received PingMessage(isRequest=" + ping.isRequest() + ") from NetID: " + networkClientId);

        if (ping.isRequest()) {
            // Client sent a Ping expecting a Pong
            PongMessage pong = new PongMessage(ping.getTimestamp());
            networkManager.sendMessageToClient(networkClientId, pong);
            LOGGER.finer("Sent PongMessage back to NetID: " + networkClientId);
        } else {
            // Client just sent a Ping as part of its own keep-alive, no response needed from server side command handler.
        }
    }

    /**
     * Handles an incoming PongMessage from a client (likely a response to a server-initiated Ping).
     */
    public void handlePong(CommandContext<PongMessage> context) {
        PongMessage pong = context.command();
        String networkClientId = context.networkClientId();
        LOGGER.finer("Received PongMessage from NetID: " + networkClientId + " (RTT: " + (System.currentTimeMillis() - pong.getOriginalPingTimestamp()) + "ms)");
        // Delegate to ConnectionMonitorService to update state
        if(context.connectionMonitorService() != null) {
            context.connectionMonitorService().recordPong(networkClientId, pong.getOriginalPingTimestamp());
        }
    }
}
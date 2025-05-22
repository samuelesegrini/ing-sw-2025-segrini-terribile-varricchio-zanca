package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Command;
import it.polimi.ingsw.server.core.ConnectionMonitorService;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Holds the context for command execution, including the command itself,
 * the client ID, and references to necessary server components.
 *
 * @param <T> The specific type of the Command being processed.
 */
public record CommandContext<T extends Command>(
        T command,
        String networkClientId,
        GameSessionManager sessionManager,
        ServerNetworkManager networkManager,
        EventBus serverEventBus,
        PlayerSessionRegistry playerSessionRegistry,
        Map<String, String> networkClientToGamePlayerMap, // networkClientId -> gamePlayerId
        Map<String, String> activePlayersByIdMap,         // gamePlayerId -> nickname
        ExecutorService gameLogicExecutor,                 // For offloading long tasks
        ConnectionMonitorService connectionMonitorService
) { }
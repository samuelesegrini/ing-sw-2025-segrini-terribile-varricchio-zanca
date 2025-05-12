package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server in response to a RequestGameListCommand,
 * containing lists of joinable and running games.
 */
public class GameListResponseEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final List<GameLobbyInfoDTO> joinableGames;
    private final List<GameLobbyInfoDTO> runningGames;

    public GameListResponseEvent(List<GameLobbyInfoDTO> joinableGames, List<GameLobbyInfoDTO> runningGames) {
        super();
        this.joinableGames = new ArrayList<>(Objects.requireNonNull(joinableGames, "joinableGames cannot be null"));
        this.runningGames = new ArrayList<>(Objects.requireNonNull(runningGames, "runningGames cannot be null"));
    }

    public List<GameLobbyInfoDTO> getJoinableGames() {
        return new ArrayList<>(joinableGames);
    }

    public List<GameLobbyInfoDTO> getRunningGames() {
        return new ArrayList<>(runningGames);
    }

    @Override
    public String toString() {
        return "GameListResponseEvent{" +
                "joinableGamesCount=" + joinableGames.size() +
                ", runningGamesCount=" + runningGames.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
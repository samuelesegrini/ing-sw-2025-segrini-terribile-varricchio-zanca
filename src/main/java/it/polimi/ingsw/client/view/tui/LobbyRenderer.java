package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.protocol.view.GameSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * The front desk, in text.
 *
 * <p>A pure function of what the client has been told, like every other renderer here. It shows
 * the games waiting for players and what is missing before this client can join one, because
 * the two questions a person has in a lobby are "what is there" and "what do I type next".
 */
public final class LobbyRenderer {

    private LobbyRenderer() {
    }

    /**
     * Lists the games waiting for players.
     *
     * @param games the open games, as of the last listing
     * @return the lines to print
     */
    public static List<String> render(List<GameSummary> games) {
        if (games.isEmpty()) {
            return List.of("No games are waiting for players. Type 'new 4' to open one.");
        }
        List<String> lines = new ArrayList<>();
        lines.add("Games waiting for players");
        for (GameSummary game : games) {
            lines.add("  " + String.format("%-10s", game.gameId())
                    + String.format("%-12s", levelOf(game))
                    + game.players().size() + "/" + game.seats() + "  "
                    + String.join(", ", game.players()));
        }
        lines.add("");
        lines.add("Type 'join <game>' to take a seat.");
        return List.copyOf(lines);
    }

    /**
     * Says what this client should do next.
     *
     * <p>Not a decoration. A person who has just been refused a nickname, or who is sitting at
     * a table waiting for two more people, wants to be told which of those it is.
     *
     * @param state everything the client knows
     * @return one line
     */
    public static String prompt(ClientState state) {
        if (!state.isConnected()) {
            return "the connection has gone — " + state.lostConnection().orElse("no reason given");
        }
        if (state.nickname().isEmpty()) {
            return "type 'name <nickname>' to begin";
        }
        if (state.gameId().isEmpty()) {
            return state.nickname().orElseThrow() + " — 'games' to look, 'new 4' to open one";
        }
        if (state.game().isEmpty()) {
            return "waiting at " + state.gameId().orElseThrow() + " for the others";
        }
        return state.nickname().orElseThrow() + " ("
                + state.colour().map(String::valueOf).orElse("?") + ")";
    }

    private static String levelOf(GameSummary game) {
        return game.level().name().toLowerCase().replace('_', ' ');
    }
}

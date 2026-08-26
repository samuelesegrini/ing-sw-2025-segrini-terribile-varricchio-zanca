package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.GameLevel;

/**
 * Getting to a table: claiming a name, and finding or making a game.
 *
 * <p>These are the only commands legal before a game starts, and none of them is legal
 * afterwards — leaving a game in progress is
 * {@link FlightCommand.GiveUp}, which has rules attached to it.
 */
public sealed interface LobbyCommand extends Command {

    /**
     * Claims a nickname for this connection.
     *
     * <p>The first message on any connection. Nicknames are unique among connected players
     * (requirement S2), so this can be refused.
     *
     * <p>It is also how a player comes back. If the name belongs to somebody who dropped
     * out of a game still in progress, the server rebinds the session to that seat and
     * answers with the game rather than the lobby, so there is no separate reconnect
     * command to get wrong (requirement AF4).
     *
     * @param nickname what to be called
     */
    record Login(String nickname) implements LobbyCommand {

        /**
         * Validates the name.
         *
         * @throws IllegalArgumentException if it is blank
         */
        public Login {
            if (nickname == null || nickname.isBlank()) {
                throw new IllegalArgumentException("a player needs a nickname");
            }
        }
    }

    /**
     * Asks what games are waiting for players.
     *
     * <p>Games in progress are not listed. There is nothing useful to do with one.
     */
    record ListGames() implements LobbyCommand {
    }

    /**
     * Opens a new game and takes the first seat.
     *
     * <p>The player who creates the game chooses how many are playing and which flight it
     * will be, which is the requirement's rule for the first player to connect.
     *
     * @param level which flight to play
     * @param seats how many players, two to four
     */
    record CreateGame(GameLevel level, int seats) implements LobbyCommand {

        /**
         * Validates the choice.
         *
         * @throws NullPointerException     if the level is {@code null}
         * @throws IllegalArgumentException if the game would seat fewer than two or more than four
         */
        public CreateGame {
            if (level == null) {
                throw new NullPointerException("a game needs a level");
            }
            if (seats < 2 || seats > 4) {
                throw new IllegalArgumentException("a game seats two to four players, not " + seats);
            }
        }
    }

    /**
     * Takes a seat at a game that is still filling up.
     *
     * <p>The game starts by itself when the last seat is taken; nobody declares it started.
     *
     * @param gameId which game, as named by {@link ListGames}
     */
    record JoinGame(String gameId) implements LobbyCommand {

        /**
         * Validates the choice.
         *
         * @throws IllegalArgumentException if the id is blank
         */
        public JoinGame {
            if (gameId == null || gameId.isBlank()) {
                throw new IllegalArgumentException("joining needs a game to join");
            }
        }
    }

    /**
     * Gives up a seat before the game starts.
     *
     * <p>Legal only while the game is still filling. Once it has started, leaving is giving
     * up, and that costs a player their cargo and their place in the standings (p.20).
     */
    record LeaveGame() implements LobbyCommand {
    }
}

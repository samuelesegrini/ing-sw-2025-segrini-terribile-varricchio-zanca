package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.GameSummary;

import java.util.List;

/**
 * What happens before a game starts.
 */
public sealed interface LobbyEvent extends Event {

    /**
     * The nickname is this client's.
     *
     * @param nickname the name as the server recorded it
     */
    record LoggedIn(String nickname) implements LobbyEvent {

        /**
         * Validates the name.
         *
         * @throws IllegalArgumentException if it is blank
         */
        public LoggedIn {
            if (nickname == null || nickname.isBlank()) {
                throw new IllegalArgumentException("a login needs a nickname");
            }
        }
    }

    /**
     * The games somebody could join.
     *
     * @param games the open games, oldest first
     */
    record GamesListed(List<GameSummary> games) implements LobbyEvent {

        /**
         * Takes a defensive copy of the list.
         */
        public GamesListed {
            games = List.copyOf(games);
        }
    }

    /**
     * This client now has a seat.
     *
     * <p>The colour is assigned by the server, not chosen. Everything else about the game
     * arrives in the {@link GameEvent.StateChanged} that follows.
     *
     * @param gameId which game
     * @param colour which set of markers is now this player's
     */
    record JoinedGame(String gameId, PlayerColor colour) implements LobbyEvent {

        /**
         * Validates the seat.
         *
         * @throws NullPointerException if the id or the colour is {@code null}
         */
        public JoinedGame {
            if (gameId == null || colour == null) {
                throw new NullPointerException("a seat needs a game and a colour");
            }
        }
    }

    /**
     * Somebody else took a seat at the same game.
     *
     * @param nickname who arrived
     * @param colour   their markers
     */
    record PlayerEntered(String nickname, PlayerColor colour) implements LobbyEvent {

        /**
         * Validates the arrival.
         *
         * @throws NullPointerException if the nickname or the colour is {@code null}
         */
        public PlayerEntered {
            if (nickname == null || colour == null) {
                throw new NullPointerException("an arrival needs a nickname and a colour");
            }
        }
    }

    /**
     * Somebody gave up their seat before the game started.
     *
     * @param nickname who left
     */
    record PlayerLeft(String nickname) implements LobbyEvent {

        /**
         * Validates the departure.
         *
         * @throws IllegalArgumentException if the nickname is blank
         */
        public PlayerLeft {
            if (nickname == null || nickname.isBlank()) {
                throw new IllegalArgumentException("a departure needs a nickname");
            }
        }
    }
}

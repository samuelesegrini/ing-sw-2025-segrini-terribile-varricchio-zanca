package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.PlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The route: who is where, and who plays first.
 *
 * <p>Both, because they are not the same question and a player needs the answer to each. The
 * order of play is read off the route, so it changes when anybody moves; and a ship a whole lap
 * behind the leader is out of the flight, which is invisible if positions are shown modulo the
 * length of the board.
 *
 * <p>Positions are therefore printed as they are — absolute, and going up for ever — rather than
 * wrapped. A player on 27 of a 24-space route has been round once, and the number says so.
 */
public final class RouteRenderer {

    private RouteRenderer() {
    }

    /**
     * Draws the route and the card on the table.
     *
     * @param flight  where everybody is
     * @param players who they are, for their names
     * @return the lines to print
     */
    public static List<String> render(FlightView flight, List<PlayerView> players) {
        List<String> lines = new ArrayList<>();
        lines.add("Route  " + flight.routeLength() + " spaces    "
                + flight.cardsLeft() + " cards left"
                + flight.cardIfAny()
                        .map(card -> "    on the table: " + words(card.type().name()))
                        .orElse(""));
        Map<PlayerColor, Integer> positions = flight.positions();
        for (PlayerColor player : flight.order()) {
            lines.add("  " + standing(player, positions, players, flight));
        }
        players.stream()
                .filter(PlayerView::retired)
                .forEach(player -> lines.add("  " + String.format("%-10s", player.nickname())
                        + "out of the flight"));
        return List.copyOf(lines);
    }

    private static String standing(PlayerColor player, Map<PlayerColor, Integer> positions,
                                   List<PlayerView> players, FlightView flight) {
        String name = players.stream()
                .filter(view -> view.colour() == player)
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(player.name().toLowerCase());
        int at = positions.getOrDefault(player, 0);
        int leader = flight.order().isEmpty() ? at
                : positions.getOrDefault(flight.order().get(0), at);
        String behind = at == leader ? "leading" : (leader - at) + " behind";
        return String.format("%-10s", name) + "(" + player + ")  space " + at + "   " + behind;
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

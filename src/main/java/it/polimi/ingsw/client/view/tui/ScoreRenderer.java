package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.protocol.view.PlayerView;

import java.util.ArrayList;
import java.util.List;

/**
 * The ledger, with every line the manual scores separately shown separately.
 *
 * <p>A total on its own is an argument waiting to happen. Galaxy Trucker is scored from five
 * different things — where you finished, whether your ship was the prettiest, what you sold,
 * what you earned on the way, and what you broke — and two of them are negative. A player who
 * lost wants to see which of the five it was, and a player who won usually wants to point at
 * one.
 *
 * <p>Losses are shown as a subtraction rather than folded into the total, for the same reason.
 */
public final class ScoreRenderer {

    private ScoreRenderer() {
    }

    /**
     * Draws the final ledger.
     *
     * @param sheets  one per player, richest first
     * @param players who they are, for their names
     * @return the lines to print
     */
    public static List<String> render(List<ScoreSheet> sheets, List<PlayerView> players) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("Final ledger");
        lines.add("  " + header());
        for (int place = 0; place < sheets.size(); place++) {
            lines.add("  " + row(place, sheets.get(place), players));
        }
        lines.add("");
        lines.add("  " + winner(sheets, players));
        return List.copyOf(lines);
    }

    private static String header() {
        return String.format("%-3s%-12s%8s%8s%8s%8s%8s%9s",
                "", "player", "finish", "prettiest", "goods", "earned", "lost", "total");
    }

    private static String row(int place, ScoreSheet sheet, List<PlayerView> players) {
        String name = players.stream()
                .filter(view -> view.colour() == sheet.player())
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(sheet.player().name().toLowerCase());
        return String.format("%-3s%-12s%8s%8s%8s%8s%8s%9d",
                (place + 1) + ".",
                name,
                sheet.finishedTheFlight() ? String.valueOf(sheet.finishReward()) : "—",
                sheet.prettiestShip() == 0 ? "—" : String.valueOf(sheet.prettiestShip()),
                sheet.goodsSold(),
                sheet.creditsEarned(),
                sheet.lostComponents() == 0 ? "—" : "-" + sheet.lostComponents(),
                sheet.total());
    }

    /**
     * Says who won, and whether they made anything doing it.
     *
     * <p>Finishing in credit is not the same as finishing first, and the manual is quite happy
     * to let somebody win a flight they lost money on.
     */
    private static String winner(List<ScoreSheet> sheets, List<PlayerView> players) {
        if (sheets.isEmpty()) {
            return "nobody scored anything";
        }
        ScoreSheet best = sheets.get(0);
        String name = players.stream()
                .filter(view -> view.colour() == best.player())
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(best.player().name().toLowerCase());
        return name + " wins with " + best.total() + " credits"
                + (best.isProfitable() ? "." : ", having lost money on the trip.");
    }
}

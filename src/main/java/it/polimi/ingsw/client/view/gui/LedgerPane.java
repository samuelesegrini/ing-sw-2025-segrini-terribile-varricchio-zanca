package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.List;

/**
 * The ledger, with every line the manual scores separately shown separately.
 *
 * <p>A total on its own is an argument waiting to happen. The game is scored from five things
 * and two of them are negative; a player who lost wants to see which of the five it was.
 *
 * <p>Losses are shown as a subtraction rather than folded in, and a bonus that does not apply
 * shows a dash rather than a nought — "not applicable" and "you earned nothing" are different
 * statements about a flight.
 */
final class LedgerPane extends VBox {

    private static final String[] COLUMNS =
            {"", "player", "finish", "prettiest", "goods", "earned", "lost", "total"};

    private final ClientState state;
    private final GridPane table = new GridPane();
    private final Label winner = new Label();
    private final Label waiting = new Label("The scores are being worked out.");

    LedgerPane(ClientState state) {
        this.state = state;

        table.setHgap(18);
        table.setVgap(6);
        winner.setFont(Font.font(16));

        setSpacing(16);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);
        Label heading = new Label("Final ledger");
        heading.setFont(Font.font(22));
        getChildren().addAll(heading, waiting, table, winner);
    }

    /**
     * Redraws from the state.
     *
     * @see ShipyardPane#redraw()
     */
    void redraw() {
        GameView game = state.game().orElse(null);
        if (game == null) {
            return;
        }
        List<ScoreSheet> sheets = game.scoresIfAny().orElse(List.of());
        waiting.setVisible(sheets.isEmpty());
        waiting.setManaged(sheets.isEmpty());

        table.getChildren().clear();
        if (sheets.isEmpty()) {
            winner.setText("");
            return;
        }
        for (int column = 0; column < COLUMNS.length; column++) {
            Label header = new Label(COLUMNS[column]);
            header.setFont(Font.font(13));
            table.add(header, column, 0);
        }
        for (int place = 0; place < sheets.size(); place++) {
            ScoreSheet sheet = sheets.get(place);
            String[] cells = {
                    (place + 1) + ".",
                    nameOf(game, sheet),
                    sheet.finishedTheFlight() ? String.valueOf(sheet.finishReward()) : "—",
                    sheet.prettiestShip() == 0 ? "—" : String.valueOf(sheet.prettiestShip()),
                    String.valueOf(sheet.goodsSold()),
                    String.valueOf(sheet.creditsEarned()),
                    sheet.lostComponents() == 0 ? "—" : "-" + sheet.lostComponents(),
                    String.valueOf(sheet.total())
            };
            for (int column = 0; column < cells.length; column++) {
                table.add(new Label(cells[column]), column, place + 1);
            }
        }
        ScoreSheet best = sheets.get(0);
        winner.setText(nameOf(game, best) + " wins with " + best.total() + " credits"
                + (best.isProfitable() ? "." : ", having lost money on the trip."));
    }

    private static String nameOf(GameView game, ScoreSheet sheet) {
        return game.players().stream()
                .filter(player -> player.colour() == sheet.player())
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(sheet.player().name().toLowerCase());
    }
}

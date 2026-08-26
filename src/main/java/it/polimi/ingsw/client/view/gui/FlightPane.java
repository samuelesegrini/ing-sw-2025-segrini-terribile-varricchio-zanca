package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The flight: the card on the table, where everybody is, and whatever is being asked.
 *
 * <p>The buttons come from {@link PromptChoices}, which is a pure function of the question — so
 * a button that is not there is a choice the server would refuse, and there is no way for this
 * pane to invent one.
 *
 * <p>Positions are shown as the numbers they are rather than as markers pushed round a printed
 * track. A ship a whole lap behind the leader is out of the flight, and that is a difference of
 * twenty-four on the number and no difference at all on the picture.
 */
final class FlightPane extends BorderPane {

    private static final double CARD_WIDTH = 300;

    private final ClientState state;
    private final Consumer<Command> outbox;
    private final TileImages images;

    private final Pane card = new Pane();
    private final Label cardName = new Label();
    private final VBox route = new VBox(6);
    private final Label question = new Label();
    private final FlowPane buttons = new FlowPane(8, 8);
    private final Label damage = new Label();
    private final VBox chosen = new VBox(6);

    /** Squares a player has picked for a question that takes several. */
    private final List<Position> picked = new ArrayList<>();

    FlightPane(ClientState state, Consumer<Command> outbox, TileImages images) {
        this.state = state;
        this.outbox = outbox;
        this.images = images;

        question.setWrapText(true);
        question.setMaxWidth(560);
        question.setFont(Font.font(15));
        damage.setWrapText(true);
        damage.setMaxWidth(560);

        setPadding(new Insets(16));
        setLeft(cardSide());
        setCenter(asking());
        setRight(routeSide());
    }

    private Pane cardSide() {
        VBox box = new VBox(8, cardName, card);
        box.setPrefWidth(CARD_WIDTH + 20);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    private Pane asking() {
        VBox box = new VBox(12, question, buttons, chosen, damage);
        box.setPadding(new Insets(0, 20, 0, 20));
        return box;
    }

    private Pane routeSide() {
        Button giveUp = new Button("Leave the route");
        giveUp.setOnAction(clicked -> outbox.accept(new FlightCommand.GiveUp()));
        VBox box = new VBox(10, new Label("Route"), route, giveUp);
        box.setPrefWidth(280);
        return box;
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
        game.flightIfAny().ifPresent(flight -> {
            drawCard(flight);
            drawRoute(game, flight);
        });
        drawQuestion(game);
        damage.setText(state.lastRefusal().orElse(""));
    }

    private void drawCard(FlightView flight) {
        card.getChildren().clear();
        flight.cardIfAny().ifPresentOrElse(identity -> {
            cardName.setText(words(identity.type().name()));
            images.ofCard(identity.id()).ifPresent(picture -> {
                ImageView view = new ImageView(picture);
                view.setFitWidth(CARD_WIDTH);
                view.setPreserveRatio(true);
                card.getChildren().add(view);
            });
        }, () -> cardName.setText("between cards"));
    }

    /**
     * Where everybody is, as numbers.
     *
     * <p>Absolute and going up for ever, because a ship a lap behind the leader is out of the
     * flight and positions taken modulo the board would show that player in front.
     */
    private void drawRoute(GameView game, FlightView flight) {
        route.getChildren().clear();
        route.getChildren().add(new Label(flight.routeLength() + " spaces, "
                + flight.cardsLeft() + " cards left"));
        int leader = flight.order().isEmpty() ? 0
                : flight.positions().getOrDefault(flight.order().get(0), 0);
        for (PlayerColor player : flight.order()) {
            int at = flight.positions().getOrDefault(player, 0);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle marker = new Circle(7, colourOf(player));
            marker.setStroke(Color.BLACK);
            row.getChildren().addAll(marker,
                    new Label(nameOf(game, player) + "   space " + at
                            + (at == leader ? "   leading" : "   " + (leader - at) + " behind")));
            route.getChildren().add(row);
        }
        game.players().stream()
                .filter(PlayerView::retired)
                .forEach(player -> route.getChildren().add(
                        new Label(player.nickname() + " — out of the flight")));
    }

    private void drawQuestion(GameView game) {
        buttons.getChildren().clear();
        chosen.getChildren().clear();
        Optional<PlayerPrompt> pending = game.pendingIfAny();
        if (pending.isEmpty()) {
            question.setText("");
            return;
        }
        PlayerPrompt prompt = pending.orElseThrow();
        if (prompt.player() != game.you()) {
            question.setText("Waiting for " + nameOf(game, prompt.player()) + ".");
            picked.clear();
            return;
        }
        ShipView ship = shipOf(game, game.you());
        question.setText(PromptChoices.question(prompt, ship));

        for (PromptChoices.Option option : PromptChoices.of(prompt, ship, game.you())) {
            Button press = new Button(option.label());
            press.setOnAction(clicked -> pressed(option, prompt, game.you()));
            buttons.getChildren().add(press);
        }
        if (!picked.isEmpty()) {
            chosen.getChildren().add(new Label("chosen: " + picked.size()));
        }
    }

    /**
     * Acts on a button.
     *
     * <p>Most answer outright. The ones that point at a square are collected until there are as
     * many as the question wants, because giving up crew takes as many as it takes and sending
     * them one at a time would be a different conversation.
     */
    private void pressed(PromptChoices.Option option, PlayerPrompt prompt, PlayerColor you) {
        if (option.answersOutright()) {
            picked.clear();
            outbox.accept(new FlightCommand.Answer(option.choice()));
            return;
        }
        picked.add(option.cell());
        if (prompt instanceof PlayerPrompt.GiveUpCrew crew && picked.size() >= crew.count()) {
            List<Position> giving = List.copyOf(picked);
            picked.clear();
            outbox.accept(new FlightCommand.Answer(new PlayerChoice.CrewGiven(you, giving)));
            return;
        }
        if (prompt instanceof PlayerPrompt.DeclarePower) {
            // Powering is cumulative: each press adds a component, and declaring sends them all.
            outbox.accept(new FlightCommand.Answer(new PlayerChoice.Declaration(you,
                    new it.polimi.ingsw.common.game.BatteryPlan(
                            java.util.Set.copyOf(picked)))));
            picked.clear();
            return;
        }
        redraw();
    }

    private static ShipView shipOf(GameView game, PlayerColor whose) {
        return game.players().stream()
                .filter(player -> player.colour() == whose)
                .map(PlayerView::ship)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no seat for " + whose));
    }

    private static String nameOf(GameView game, PlayerColor whose) {
        return game.players().stream()
                .filter(player -> player.colour() == whose)
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(whose.name().toLowerCase());
    }

    private static Color colourOf(PlayerColor player) {
        return switch (player) {
            case RED -> Color.CRIMSON;
            case BLUE -> Color.ROYALBLUE;
            case GREEN -> Color.SEAGREEN;
            case YELLOW -> Color.GOLD;
        };
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Building a ship by clicking at it.
 *
 * <p>The board is the printed one, photographed, and the tiles are the printed ones. Where a
 * square is on that photograph is {@link BoardGeometry}'s business and was measured from the
 * artwork rather than guessed, so a tile dropped on a square lands where the player aimed.
 *
 * <p>Nothing here knows a rule. A square that is not part of the ship is refused before the
 * command goes out, because the client can see the outline in the projection and there is no
 * sense making the server say so; everything else — connectors, whether a tile may sit next to
 * another, whether the shipyard is even open — is the server's answer, shown as it comes back.
 */
final class ShipyardPane extends BorderPane {

    private static final double BOARD_WIDTH = 760;
    private static final double BOARD_HEIGHT = 551;

    private final ClientState state;
    private final java.util.function.Consumer<it.polimi.ingsw.common.protocol.Command> outbox;
    private final TileImages images;

    private final Pane board = new Pane();
    private final Label heading = new Label();
    private final Label trouble = new Label();
    private final Label hourglass = new Label();
    private final Label finished = new Label();
    private final VBox hand = new VBox(8);
    private final FlowPane pile = new FlowPane(6, 6);
    private final FlowPane reserved = new FlowPane(6, 6);
    private final HBox otherShips = new HBox(8);

    /** Whose ship is on screen. Yours until you ask to see somebody else's — requirement G6. */
    private PlayerColor looking;

    /** How far the tile in hand has been turned, before it is put down. */
    private Rotation turned = Rotation.NONE;

    /**
     * Builds the shipyard.
     *
     * @param state  what the client knows
     * @param outbox where commands go — a consumer rather than the connection itself, because
     *               that is all this needs and because a test can then watch what it sends
     * @param images the pictures
     */
    ShipyardPane(ClientState state,
                 java.util.function.Consumer<it.polimi.ingsw.common.protocol.Command> outbox,
                 TileImages images) {
        this.state = state;
        this.outbox = outbox;
        this.images = images;

        board.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        board.setOnMouseClicked(click -> clickedTheBoard(click.getX(), click.getY()));

        trouble.setWrapText(true);
        trouble.setMaxWidth(300);
        hand.setAlignment(Pos.CENTER);

        setPadding(new Insets(16));
        setTop(header());
        setCenter(board);
        setRight(sidebar());
        setBottom(otherShips);
    }

    // ------------------------------------------------------------------ the furniture

    private Pane header() {
        HBox row = new HBox(16, heading, hourglass, finished);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 12, 0));
        return row;
    }

    private Pane sidebar() {
        Button draw = new Button("Draw from the heap");
        draw.setOnAction(clicked -> send(new BuildingCommand.DrawFromPool()));

        Button turn = new Button("Turn it");
        turn.setOnAction(clicked -> {
            turned = Rotation.values()[(turned.ordinal() + 1) % Rotation.values().length];
            redraw();
        });

        Button weld = new Button("Weld it down");
        weld.setOnAction(clicked -> send(new BuildingCommand.Weld()));

        Button putBack = new Button("Put it back");
        putBack.setOnAction(clicked -> send(new BuildingCommand.ReturnToPool()));

        Button setAside = new Button("Set aside");
        setAside.setOnAction(clicked -> send(new BuildingCommand.Reserve()));

        Button flip = new Button("Flip the hourglass");
        flip.setOnAction(clicked -> send(new BuildingCommand.FlipTimer()));

        Button peek = new Button("Peek at a pile");
        peek.setOnAction(clicked -> send(new BuildingCommand.ScoutPile(0)));

        Button done = new Button("Finished building");
        done.setOnAction(clicked -> send(new BuildingCommand.FinishBuilding(null)));

        VBox box = new VBox(10,
                new Label("In hand"), hand,
                draw, turn, weld, putBack, setAside,
                new Label("Set aside"), reserved,
                new Label("Face up — anybody may take these"), pile,
                flip, peek, done, trouble);
        box.setPadding(new Insets(0, 0, 0, 16));
        box.setPrefWidth(320);
        return box;
    }

    // ------------------------------------------------------------------ what a click means

    /**
     * Puts the tile in hand on the square that was clicked.
     *
     * <p>A square that is not part of the ship is refused here rather than sent: the outline is
     * in the projection, so the client can see it, and a refusal that arrives instantly reads
     * as the board saying no rather than the server disagreeing.
     *
     * <p>Package-private so a test can call it with a point rather than manufacture a mouse
     * event. Firing one at the pane does not reach this: the handler is on the board inside it,
     * and a test that fired at the wrong node would pass while clicking nothing.
     *
     * @param x across, from the left of the board
     * @param y down, from the top
     */
    void clickedTheBoard(double x, double y) {
        Optional<ShipView> mine = shipOf(state.game().orElse(null), me());
        if (mine.isEmpty()) {
            return;
        }
        ShipView ship = mine.orElseThrow();
        Optional<Position> cell = geometry(ship).cellAt(x, y);
        if (cell.isEmpty()) {
            trouble.setText("that is not a square on the ship");
            return;
        }
        Position square = cell.orElseThrow();
        if (!ship.outline().contains(square)) {
            trouble.setText("the ship does not reach that far");
            return;
        }
        trouble.setText("");
        send(ship.cells().containsKey(square)
                ? new BuildingCommand.AdjustPlacement(square, turned)
                : new BuildingCommand.PlaceInHand(square, turned));
    }

    private void send(it.polimi.ingsw.common.protocol.Command command) {
        outbox.accept(command);
    }

    // ------------------------------------------------------------------ drawing it

    /**
     * Redraws everything from the state.
     *
     * <p>Called on every change, so it rebuilds the board rather than tracking what moved.
     * Thirty-five tiles is nothing to lay out, and a screen that redraws from the truth cannot
     * drift away from it.
     */
    void redraw() {
        GameView game = state.game().orElse(null);
        if (game == null) {
            return;
        }
        PlayerColor whose = looking == null ? game.you() : looking;
        ShipView ship = shipOf(game, whose).orElse(null);
        if (ship == null) {
            return;
        }

        heading.setText(whose == game.you()
                ? "Your ship"
                : nameOf(game, whose) + "'s ship — 'Your ship' to go back");
        drawBoard(ship);
        game.buildingIfAny().ifPresent(this::drawTheYard);
        drawReserved(ship);
        drawOtherShips(game);
        trouble.setText(state.lastRefusal().orElse(trouble.getText()));
    }

    private void drawBoard(ShipView ship) {
        board.getChildren().clear();
        Image backdrop = images.ofPath(Artwork.bundled().ofShipBoard(
                state.game().orElseThrow().level()));
        ImageView cardboard = new ImageView(backdrop);
        cardboard.setFitWidth(BOARD_WIDTH);
        cardboard.setFitHeight(BOARD_HEIGHT);
        board.getChildren().add(cardboard);

        BoardGeometry grid = geometry(ship);
        for (Map.Entry<Position, CellView> welded : ship.cells().entrySet()) {
            double[] at = grid.boundsOf(welded.getKey());
            images.ofTile(welded.getValue().tile().tileId()).ifPresent(picture -> {
                ImageView view = new ImageView(picture);
                view.setFitWidth(at[2]);
                view.setFitHeight(at[3]);
                view.setX(at[0]);
                view.setY(at[1]);
                view.setRotate(degrees(welded.getValue().tile().rotation()));
                board.getChildren().add(view);
            });
        }
        state.game().flatMap(GameView::buildingIfAny)
                .flatMap(BuildingView::unweldedIfAny)
                .ifPresent(loose -> {
            double[] at = grid.boundsOf(loose);
            Rectangle marker = new Rectangle(at[0], at[1], at[2], at[3]);
            marker.setFill(Color.TRANSPARENT);
            marker.setStroke(Color.GOLD);
            marker.setStrokeWidth(3);
            board.getChildren().add(marker);
        });
    }

    private void drawTheYard(BuildingView yard) {
        hand.getChildren().clear();
        yard.handIfAny().ifPresent(tile -> hand.getChildren().add(pictureOf(tile, turned)));
        if (yard.handIfAny().isEmpty()) {
            turned = Rotation.NONE;
            hand.getChildren().add(new Label("empty — draw one"));
        }

        pile.getChildren().clear();
        yard.faceUpPile().forEach(tile -> {
            Group picture = pictureOf(tile, Rotation.NONE);
            picture.setOnMouseClicked(clicked ->
                    send(new BuildingCommand.TakeFaceUp(tile.tileId())));
            pile.getChildren().add(picture);
        });

        hourglass.setText(yard.hourglassSpaces() == 0
                ? "no hourglass"
                : "hourglass " + (yard.hourglassSpace() == null || yard.hourglassSpace() < 0
                        ? "not started"
                        : (yard.hourglassSpace() + 1) + "/" + yard.hourglassSpaces())
                        + (yard.secondsRemaining() > 0 ? "  " + yard.secondsRemaining() + "s" : ""));
        finished.setText(yard.finished().isEmpty()
                ? "nobody has finished"
                : "finished: " + yard.finished().stream().map(Enum::name).sorted()
                        .reduce((left, right) -> left + " " + right).orElse(""));
    }

    private void drawReserved(ShipView ship) {
        reserved.getChildren().clear();
        ship.reserved().forEach(tile -> reserved.getChildren().add(
                pictureOf(tile, Rotation.NONE)));
        if (ship.reserved().isEmpty()) {
            reserved.getChildren().add(new Label("nothing"));
        }
    }

    /**
     * A row of buttons for looking at everybody else's ship.
     *
     * <p>Requirement G6. Somebody deciding whether to stop building wants to know how far along
     * the others are, and that is not a number — it is what their ship looks like.
     */
    private void drawOtherShips(GameView game) {
        otherShips.getChildren().clear();
        otherShips.setPadding(new Insets(12, 0, 0, 0));
        otherShips.setAlignment(Pos.CENTER);
        Button mine = new Button("Your ship");
        mine.setOnAction(clicked -> {
            looking = null;
            redraw();
        });
        otherShips.getChildren().add(mine);
        game.players().stream()
                .filter(player -> player.colour() != game.you())
                .forEach(player -> {
                    Button look = new Button(player.nickname());
                    look.setOnAction(clicked -> {
                        looking = player.colour();
                        redraw();
                    });
                    otherShips.getChildren().add(look);
                });
    }

    // ------------------------------------------------------------------ bits and pieces

    private Group pictureOf(TileView tile, Rotation rotation) {
        Group group = new Group();
        images.ofTile(tile.tileId()).ifPresent(picture -> {
            ImageView view = new ImageView(picture);
            view.setFitWidth(90);
            view.setFitHeight(90);
            view.setRotate(degrees(rotation));
            group.getChildren().add(view);
        });
        return group;
    }

    private BoardGeometry geometry(ShipView ship) {
        return new BoardGeometry(BOARD_WIDTH, BOARD_HEIGHT, ship.rows(), ship.columns());
    }

    private PlayerColor me() {
        return state.game().map(GameView::you).orElse(null);
    }

    private static Optional<ShipView> shipOf(GameView game, PlayerColor whose) {
        if (game == null || whose == null) {
            return Optional.empty();
        }
        return game.players().stream()
                .filter(player -> player.colour() == whose)
                .map(PlayerView::ship)
                .findFirst();
    }

    private static String nameOf(GameView game, PlayerColor whose) {
        return game.players().stream()
                .filter(player -> player.colour() == whose)
                .map(PlayerView::nickname)
                .findFirst()
                .orElse(whose.name().toLowerCase());
    }

    private static double degrees(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 90;
            case CLOCKWISE_180 -> 180;
            case CLOCKWISE_270 -> 270;
        };
    }
}

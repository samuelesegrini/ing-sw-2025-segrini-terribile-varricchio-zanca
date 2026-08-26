package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.DamageReport;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.game.ShipViolation;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.game.ViolationKind;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.GameSummary;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One instance of every message the protocol defines.
 *
 * <p>Written out by hand rather than generated, for two reasons. It is the fixture
 * {@link ProtocolContractTest} sends down a wire to prove the messages survive the trip,
 * and it is a worked example of what each message actually looks like — which is worth
 * more to somebody implementing a client than the record declaration is.
 *
 * <p>Every message belongs here. The contract test compares this list against the sealed
 * hierarchies and fails if one is missing, so a message added without a sample is a build
 * failure rather than a message nobody ever tried to send.
 */
public final class Messages {

    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);

    private Messages() {
    }

    /**
     * Returns one of each command.
     *
     * @return every command the protocol defines, one instance apiece
     */
    public static List<Command> commands() {
        return List.of(
                new LobbyCommand.Login("samuele"),
                new LobbyCommand.ListGames(),
                new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 4),
                new LobbyCommand.JoinGame("game-1"),
                new LobbyCommand.LeaveGame(),

                new BuildingCommand.DrawFromPool(),
                new BuildingCommand.TakeFaceUp("cabin_UUUU"),
                new BuildingCommand.TakeReserved("battery_UD-SD"),
                new BuildingCommand.ReturnToPool(),
                new BuildingCommand.Reserve(),
                new BuildingCommand.PlaceInHand(CABIN, Rotation.CLOCKWISE_90),
                new BuildingCommand.AdjustPlacement(CABIN, Rotation.CLOCKWISE_180),
                new BuildingCommand.Weld(),
                new BuildingCommand.ScoutPile(1),
                new BuildingCommand.PutPileBack(),
                new BuildingCommand.FlipTimer(),
                new BuildingCommand.FinishBuilding(3),

                new PreparationCommand.RemoveComponent(HOLD),
                new PreparationCommand.KeepPiece(Set.of(CABIN, HOLD)),
                new PreparationCommand.BoardCrew(CABIN, AlienColor.PURPLE),
                new PreparationCommand.FinishPreparation(),

                new FlightCommand.Answer(new PlayerChoice.Declaration(
                        PlayerColor.RED, BatteryPlan.powering(HOLD))),
                new FlightCommand.GiveUp());
    }

    /**
     * Returns one of each event.
     *
     * @return every event the protocol defines, one instance apiece
     */
    public static List<Event> events() {
        return List.of(
                new LobbyEvent.LoggedIn("samuele"),
                new LobbyEvent.GamesListed(List.of(summary())),
                new LobbyEvent.JoinedGame("game-1", PlayerColor.RED),
                new LobbyEvent.PlayerEntered("chiara", PlayerColor.BLUE),
                new LobbyEvent.PlayerLeft("chiara"),

                new GameEvent.StateChanged(state()),
                new GameEvent.PhaseBegan(GamePhase.FLIGHT),
                new GameEvent.Rejected("Weld", "there is no tile in your hand"),
                new GameEvent.ConnectionChanged(PlayerColor.BLUE, false),
                new GameEvent.GameEnded(),

                new FlightEvent.CardRevealed(card()),
                new FlightEvent.Awaiting(prompt()),
                new FlightEvent.DiceRolled(7),
                new FlightEvent.ThreatResolved(PlayerColor.RED, hit(), damage()),
                new FlightEvent.ShipMoved(PlayerColor.RED, 6, 9),
                new FlightEvent.ShipRetired(PlayerColor.BLUE, "a whole lap behind the leader"),
                new FlightEvent.CardResolved());
    }

    /**
     * Returns every choice a player can make, for the one command that carries them.
     *
     * <p>{@link FlightCommand.Answer} is the only message whose payload is itself a sealed
     * set, so covering the command once would leave seven shapes untested.
     *
     * @return one instance of each {@link PlayerChoice}
     */
    public static List<PlayerChoice> choices() {
        return List.of(
                new PlayerChoice.Take(PlayerColor.RED),
                new PlayerChoice.Leave(PlayerColor.RED),
                new PlayerChoice.Declaration(PlayerColor.RED, BatteryPlan.powering(HOLD)),
                new PlayerChoice.Done(PlayerColor.RED),
                PlayerChoice.CargoStowed.load(PlayerColor.RED, HOLD, GoodColor.BLUE),
                PlayerChoice.CargoStowed.move(PlayerColor.RED, HOLD, CABIN, GoodColor.BLUE),
                PlayerChoice.CargoStowed.jettison(PlayerColor.RED, HOLD, GoodColor.RED),
                new PlayerChoice.CrewGiven(PlayerColor.RED, List.of(CABIN)),
                PlayerChoice.DefenceChosen.none(PlayerColor.RED),
                PlayerChoice.DefenceChosen.using(PlayerColor.RED, HOLD),
                new PlayerChoice.FragmentKept(PlayerColor.RED, Set.of(CABIN)),
                new PlayerChoice.PlanetChosen(PlayerColor.RED, 2));
    }

    /**
     * Returns every question a card can ask, for the one event that carries them.
     *
     * @return one instance of each {@link PlayerPrompt}
     */
    public static List<PlayerPrompt> prompts() {
        return List.of(
                new PlayerPrompt.TakeOrLeave(PlayerColor.RED, "4 credits for the salvage", 1),
                new PlayerPrompt.DeclarePower(
                        PlayerColor.RED, ShipAttribute.FIREPOWER, Set.of(HOLD), 2),
                new PlayerPrompt.ArrangeCargo(
                        PlayerColor.RED, Map.of(GoodColor.RED, 1, GoodColor.BLUE, 2), Set.of(HOLD)),
                new PlayerPrompt.GiveUpCrew(PlayerColor.RED, 2, Set.of(CABIN)),
                new PlayerPrompt.ChooseDefence(PlayerColor.RED, hit(), CABIN, Set.of(HOLD)),
                new PlayerPrompt.ChooseDefence(PlayerColor.RED, hit(), null, Set.of()),
                new PlayerPrompt.ChooseFragment(PlayerColor.RED, List.of(Set.of(CABIN), Set.of(HOLD))),
                new PlayerPrompt.ChoosePlanet(
                        PlayerColor.RED, Map.of(0, Map.of(GoodColor.GREEN, 2)), 2));
    }

    /**
     * Returns a filled-in picture of a game in flight.
     *
     * <p>The heaviest message the protocol has, and the one worth exercising: it nests
     * every view, and a client that can read one can read anything.
     *
     * @return a game view with a ship, a shipyard and a route in it
     */
    public static GameView state() {
        return new GameView("game-1", GameLevel.LEVEL_II, GamePhase.FLIGHT, PlayerColor.RED,
                List.of(player(PlayerColor.RED, "samuele"), player(PlayerColor.BLUE, "chiara")),
                shipyard(), route(), prompt(), List.of(scoreSheet()));
    }

    private static GameSummary summary() {
        return new GameSummary("game-1", GameLevel.LEVEL_II, 4, List.of("samuele", "chiara"));
    }

    private static PlayerView player(PlayerColor colour, String nickname) {
        return new PlayerView(nickname, colour, true, false, 12, ship());
    }

    private static ShipView ship() {
        return new ShipView(5, 7, 5, 4, Set.of(CABIN, HOLD),
                Map.of(CABIN, new CellView(tile(ComponentKind.CABIN), 0, List.of(), 2, null),
                        HOLD, new CellView(tile(ComponentKind.CARGO_HOLD), 0,
                                List.of(GoodColor.BLUE), 0, null)),
                List.of(tile(ComponentKind.STRUCTURAL_MODULE)), 1,
                new ShipAttributes(5, 3, 4),
                new ValidationReport(List.of(new ShipViolation(
                        ViolationKind.BLOCKED_ENGINE_EXHAUST, Set.of(HOLD),
                        "something is welded behind this engine"))));
    }

    private static TileView tile(ComponentKind kind) {
        return new TileView(kind.name().toLowerCase() + "_UUUU", kind, Rotation.NONE,
                Map.of(Direction.NORTH, Connector.UNIVERSAL,
                        Direction.EAST, Connector.SINGLE,
                        Direction.SOUTH, Connector.DOUBLE,
                        Direction.WEST, Connector.PLAIN));
    }

    private static BuildingView shipyard() {
        return new BuildingView(96, List.of(tile(ComponentKind.SHIELD)),
                tile(ComponentKind.BATTERY), HOLD, List.of(card()), 1, 3, 42,
                Set.of(PlayerColor.BLUE), List.of(2, 4));
    }

    private static FlightView route() {
        return new FlightView(24, Map.of(PlayerColor.RED, 9, PlayerColor.BLUE, 6),
                List.of(PlayerColor.RED, PlayerColor.BLUE), card(), 7);
    }

    private static AdventureCardIdentity card() {
        return new AdventureCardIdentity("pirates_lvl2", AdventureCardType.PIRATES,
                CardLevel.LEVEL_II, false);
    }

    private static PlayerPrompt prompt() {
        return new PlayerPrompt.ChooseDefence(PlayerColor.RED, hit(), CABIN, Set.of(HOLD));
    }

    private static Hit hit() {
        return new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7);
    }

    private static DamageReport damage() {
        return new DamageReport(CABIN, DamageReport.Outcome.DESTROYED, CABIN,
                List.of(Set.of(HOLD)));
    }

    private static ScoreSheet scoreSheet() {
        return new ScoreSheet(PlayerColor.RED, true, 4, 4, 9, 12, 1);
    }
}

package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;

import java.util.Optional;

/**
 * What happened, in a sentence.
 *
 * <p>Half of what makes this game worth playing is watching a seven come up on somebody else's
 * meteor, and a board state cannot tell you that. So the narration is printed as it arrives.
 *
 * <p>It is only ever printed. Nothing here is read back, nothing is counted, and a client that
 * threw all of it away would still draw the same board — which is the property
 * {@code ClientStateTest} checks and the reason this can be a plain {@code switch} returning
 * strings.
 */
public final class NarrationRenderer {

    private NarrationRenderer() {
    }

    /**
     * Turns one event into a line.
     *
     * @param event what arrived
     * @return the line to print, or empty for something not worth saying out loud
     */
    public static Optional<String> render(Event event) {
        return Optional.ofNullable(switch (event) {
            case LobbyEvent.LoggedIn loggedIn -> "  you are " + loggedIn.nickname();
            case LobbyEvent.JoinedGame joined ->
                    "  you have a seat at " + joined.gameId() + " as " + joined.colour();
            case LobbyEvent.PlayerEntered entered ->
                    "  " + entered.nickname() + " sat down (" + entered.colour() + ")";
            case LobbyEvent.PlayerLeft left -> "  " + left.nickname() + " left";
            case GameEvent.PhaseBegan phase ->
                    "── " + name(phase.phase().name()) + " — " + purposeOf(phase.phase());
            case GameEvent.ConnectionChanged changed ->
                    // Neutral on purpose. The same event announces a player arriving for the
                    // first time and one returning after their laptop closed, and "is back"
                    // reads oddly for somebody who has never been away.
                    "  " + changed.player() + (changed.connected() ? " is connected" : " has dropped");
            case GameEvent.TurnSkipped skipped ->
                    "  " + skipped.player() + " is away — " + skipped.what() + " for them";
            case GameEvent.GameSuspended waiting ->
                    "── waiting for somebody to come back; " + waiting.secondsRemaining()
                            + "s before the last player standing takes it";
            case GameEvent.GameResumed ignored -> "── somebody is back; carrying on";
            case GameEvent.GameEnded ignored -> "── the game is over";
            case GameEvent.Rejected refused -> "  ✗ " + refused.reason();
            case FlightEvent.CardRevealed revealed ->
                    "  a card is turned over: " + name(revealed.card().type().name());
            case FlightEvent.Awaiting awaiting ->
                    "  waiting for " + awaiting.prompt().player();
            case FlightEvent.DiceRolled rolled -> "  the dice come up " + rolled.total();
            case FlightEvent.ThreatResolved threat -> threat(threat);
            case FlightEvent.ShipMoved moved ->
                    "  " + moved.player() + " moves from " + moved.from() + " to " + moved.to();
            case FlightEvent.ShipRetired retired ->
                    "  " + retired.player() + " is out of the flight: " + retired.reason();
            default -> null;
        });
    }

    /**
     * Says a game has begun, and who is in it.
     *
     * <p>Printed when the client first has a {@link GameView} where it had none, rather than
     * off the first {@code PhaseBegan}. That is not a preference: there is no {@code PhaseBegan}
     * at the start of a game at all. {@code GameController} sets its {@code lastAnnounced} to
     * the phase the game opens in, so the opening phase is never announced — a line triggered
     * off it would print for every phase except the one this exists for.
     *
     * <p>Taking the transition rather than the event also settles the rejoin case on its own:
     * a client has no game and then has one exactly once, whether it is sitting down at a new
     * table or coming back to a flight already in progress.
     *
     * @param game what the player can see
     * @return the line naming the game, its rules and the table
     */
    public static String gameBegan(GameView game) {
        String table = game.players().stream()
                .map(player -> player.nickname() + " (" + player.colour()
                        + (player.colour() == game.you() ? ", you" : "") + ")")
                .collect(java.util.stream.Collectors.joining(", "));
        return "── " + game.gameId() + " — " + name(game.level().name()) + " — " + table;
    }

    /**
     * Says what a phase is for, in one clause.
     *
     * <p>What the player can now do, not which commands do it — {@code help} lists those, and
     * restating them here would be the same list in two places drifting apart.
     *
     * <p>Exhaustive over {@link GamePhase} with no {@code default}, so a phase added later
     * stops the build here rather than printing its own name back at a player as though that
     * explained anything.
     *
     * @param phase where the game has got to
     * @return what it is for
     */
    public static String purposeOf(GamePhase phase) {
        return switch (phase) {
            case LOBBY -> "waiting for the table to fill";
            case BUILDING -> "build a ship out of what is on the table";
            case VALIDATION -> "put right whatever will not fly";
            case CREW_PLACEMENT -> "put people and aliens in the cabins";
            case FLIGHT -> "the cards are turned over one at a time";
            case SCORING -> "the ledger is settled";
            case FINISHED -> "nothing left but the final board";
        };
    }

    private static String threat(FlightEvent.ThreatResolved threat) {
        String opening = "  " + name(threat.hit().kind().name()) + " from the "
                + threat.hit().from().name().toLowerCase() + " at " + threat.player() + ": ";
        return opening + switch (threat.damage().outcome()) {
            case MISSED -> "it misses";
            case BOUNCED -> "it glances off";
            case DEFENDED -> "it is stopped";
            case DESTROYED -> "a component is destroyed";
        };
    }

    private static String name(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}

package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyEvent;

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
                    "── " + phase.phase().name().toLowerCase().replace('_', ' ');
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

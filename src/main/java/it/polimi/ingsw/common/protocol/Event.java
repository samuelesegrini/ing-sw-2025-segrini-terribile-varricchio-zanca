package it.polimi.ingsw.common.protocol;

import java.io.Serializable;

/**
 * Everything the server tells a client.
 *
 * <p>Server to client is a closed set of records: facts about what happened, never
 * instructions about what to draw. What a view makes of them is the view's business, which
 * is what lets a text interface and a graphical one read the same stream.
 *
 * <p><b>Facts, then truth.</b> Events come in batches, and every batch ends with a
 * {@link GameEvent.StateChanged} carrying the whole picture. The events before it say what
 * happened — the dice came up seven, the red player lost a cabin — and exist so a view can
 * narrate. A client that ignores all of them and reads only the state is still correct.
 *
 * <p>That rule is worth more than it looks. It means there is no way for a client to drift
 * out of step with the server by missing a message, because nothing is ever expressed only
 * as a delta. It also means reconnecting is the same operation as joining: send the state
 * (requirement AF4). No replay log, no sequence numbers, no resynchronisation protocol —
 * all of which are things that go wrong.
 */
public sealed interface Event extends Serializable
        permits LobbyEvent, GameEvent, FlightEvent {
}

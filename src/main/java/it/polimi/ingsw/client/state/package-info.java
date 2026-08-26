/**
 * Everything a client knows, which is the last thing it was told.
 *
 * <p>There is no game state here — there is a copy of what the server said was true a moment
 * ago, and nothing computed from it that could later disagree. Every question a screen has is
 * either answered by that view or is a question for the server.
 *
 * <p>Which is the protocol's promise taken literally rather than trusted. If a client can be
 * written this way then <em>"a client that ignores every narration event and reads only the
 * state is still correct"</em> is true; and if it could not, the protocol would be wrong, and
 * it is better to find that out here than in a demonstration.
 */
package it.polimi.ingsw.client.state;

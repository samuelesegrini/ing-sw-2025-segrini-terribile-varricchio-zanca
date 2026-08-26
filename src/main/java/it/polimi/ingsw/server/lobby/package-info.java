/**
 * The front desk: names, tables, and getting people to the right one.
 *
 * <p>Everything here happens on one thread, for the same reason a game does. Nicknames are
 * unique, seats are finite, and a table fills at the moment somebody takes the last one — all
 * three are what two connections arriving together get wrong, and a queue settles them by
 * arrival order rather than by whoever won a race.
 *
 * <p>The lobby stops being involved the moment a game starts. A player's commands then go
 * straight to that game's own queue, and the lobby keeps only enough to answer the one question
 * it is still asked: where somebody belongs when they come back.
 *
 * <p>Which is also why there is no reconnect command. Logging in with a name that belongs to a
 * seat nobody is attached to <em>is</em> reconnecting. A separate code path would be a second
 * way of doing the same thing, and the two would eventually disagree.
 */
package it.polimi.ingsw.server.lobby;

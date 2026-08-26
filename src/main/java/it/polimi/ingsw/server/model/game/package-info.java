/**
 * The game aggregate and the phases it moves through.
 *
 * <p>{@link it.polimi.ingsw.server.model.game.Game} owns the ships, the pool, the hourglass,
 * the deck and the route, and is the only thing that knows all of them at once. It decides
 * nothing about <em>when</em> things may happen: that belongs to the current phase, which is
 * what keeps the {@code if} chains a project like this usually accumulates from ever being
 * written.
 *
 * <p>A phase decides three things. What it is called, so a client can be told. What to do with
 * a command. And whether it is over — which it answers rather than announces, so that no phase
 * has to know what follows it.
 *
 * <p>Nothing here is thread-safe, on purpose. Commands are applied one at a time on one
 * thread, which the controller arranges (architecture § 4). That is what lets the whole model
 * beneath be written without a lock, and why two players reaching for the same face-up tile is
 * settled by which command arrived first rather than by which thread happened to run.
 */
package it.polimi.ingsw.server.model.game;

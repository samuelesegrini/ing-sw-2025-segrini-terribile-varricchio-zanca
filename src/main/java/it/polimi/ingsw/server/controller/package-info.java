/**
 * One game, one thread, one command at a time.
 *
 * <p>Every command a game will ever see goes on a queue and is applied on a single thread.
 * That one decision is why nothing in the model has a lock in it, and why two players reaching
 * for the same face-up tile is settled by which command arrived first rather than by which
 * thread happened to run (architecture § 4). It is also not something a reader can verify by
 * looking: {@code GameControllerTest} sends thirty-two simultaneous draws for one player and
 * expects exactly one of them to succeed, which is a test that fails the moment the queue does.
 *
 * <p>Nothing about a transport reaches this far. A command is a command whether it came off a
 * socket or out of an RMI call, which is what requirement S5 needs and what stops the phases
 * from ever growing a special case.
 *
 * <p>{@link it.polimi.ingsw.server.controller.PlayerSession} is the other half: the model knows
 * that the red player exists, and a session knows whether anybody is presently attached to
 * being the red player. Keeping those apart is what makes a drop a small event rather than a
 * crisis — the channel is cleared, the game carries on, and a reconnection is the same
 * operation as arriving for the first time.
 */
package it.polimi.ingsw.server.controller;

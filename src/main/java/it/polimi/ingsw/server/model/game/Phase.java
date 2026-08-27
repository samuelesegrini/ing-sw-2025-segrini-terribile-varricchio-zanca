package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;

import java.util.List;
import java.util.Optional;

/**
 * One stage of a game, and the only thing that decides what may happen during it.
 *
 * <p>The alternative — a phase enum and {@code if} chains in the controller — is where a
 * project like this collects its bugs, because every new command is a new chance to forget a
 * check. A phase object cannot forget to reject a command belonging to another phase: the
 * command never reaches it, and {@link Game} refuses it by default.
 *
 * <p>A phase decides three things and nothing else. What it is called, so a client can be
 * told. What to do with a command. And whether it is over, which it answers rather than
 * announces — the aggregate asks after every command, so a phase never has to know what
 * follows it.
 */
interface Phase {

    /**
     * Returns what this phase is called on the wire.
     *
     * @return the phase
     */
    GamePhase name();

    /**
     * Applies a command, or explains why it cannot be.
     *
     * @param player  who sent it, already checked against the connection it arrived on
     * @param command what they want
     * @return what happened, or why nothing did
     */
    Reaction apply(PlayerColor player, Command command);

    /**
     * Says whether this phase has finished, and what comes next.
     *
     * <p>Asked after every accepted command. Answering rather than announcing keeps a phase
     * from having to know what follows it, which is what lets the test flight and level II
     * differ by a table rather than by a branch.
     *
     * @return the next phase, or empty while this one is still running
     */
    Optional<Phase> next();

    /**
     * Finishes this phase for a player who is not there to finish it themselves.
     *
     * <p>Asked on every tick for everybody away, so that a shipyard does not stay open on a
     * laptop that has closed. Most phases have nothing this could mean — a flight cannot be
     * flown on somebody's behalf, and a score cannot be settled early — so the default is to
     * refuse, and a phase that <em>can</em> be finished says so by overriding.
     *
     * <p>Refusing is the ordinary answer, not an error: it is also what a phase says when
     * the player had already finished before they left.
     *
     * <p>The narration belongs to the phase rather than to the caller, because the phase is
     * the only thing that knows what it just did on that player's behalf.
     *
     * @param player who is away
     * @return what was done for them, or why nothing was
     */
    default Reaction finishFor(PlayerColor player) {
        return new Reaction.Refused("nothing here can be finished on somebody's behalf");
    }

    /**
     * Says what happened on the way in.
     *
     * <p>Most phases begin by waiting, and have nothing to report. The flight does not: it
     * turns a card over the moment it starts, and that has to reach the players who were
     * watching the fleet launch rather than being discovered in the next state.
     *
     * @return what to narrate, empty for a phase that starts quietly
     */
    default List<Event> onEntry() {
        return List.of();
    }

    /**
     * Returns the decision the game is waiting for.
     *
     * @return the outstanding prompt, or empty when the game is waiting for nobody
     */
    default Optional<PlayerPrompt> pending() {
        return Optional.empty();
    }

    /**
     * Returns the card being resolved.
     *
     * @return the card on the table, or empty when there is not one
     */
    default Optional<AdventureCardIdentity> cardOnTheTable() {
        return Optional.empty();
    }

    /**
     * Returns how many cards are still to come.
     *
     * @return what is left of the deck, zero before the flight starts
     */
    default int cardsLeft() {
        return 0;
    }
}

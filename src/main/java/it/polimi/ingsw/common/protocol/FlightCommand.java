package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.PlayerChoice;

/**
 * The flight, which is two commands.
 *
 * <p>Every card, from Stardust to the Combat Zone, is a sequence of questions and the
 * answers to them. The questions are {@link it.polimi.ingsw.common.game.PlayerPrompt}s and
 * the answers are {@link PlayerChoice}s, and there is nothing else a flight consists of —
 * so there is nothing else to put here except giving up.
 */
public sealed interface FlightCommand extends Command {

    /**
     * Answers the question the game is waiting for.
     *
     * <p>The one command that carries a payload the model already understands. The
     * resolution framework written for the cards is the wire format, which is why there is
     * no {@code TakeReward} command, no {@code DeclareFirepower} command, and no switch
     * anywhere that has to grow when a card is added.
     *
     * <p><b>The payload names a player, and that name is not evidence.</b> A
     * {@link PlayerChoice} carries the player it came from because the model needed that
     * before there was a network; over a socket it is a claim anybody can make. The server
     * checks it against the session the command arrived on and refuses a mismatch, or one
     * player could throw another's crew out of the airlock.
     *
     * @param choice the answer
     */
    record Answer(PlayerChoice choice) implements FlightCommand {

        /**
         * Validates the answer.
         *
         * @throws NullPointerException if the choice is {@code null}
         */
        public Answer {
            if (choice == null) {
                throw new NullPointerException("an answer needs a choice");
            }
        }
    }

    /**
     * Gives up the flight.
     *
     * <p>The ship leaves the route, keeps whatever credits it has earned, and loses its
     * cargo and its share of the finishing order (p.20). Legal at any point during the
     * flight; the manual does not make a player wait for their turn to quit.
     */
    record GiveUp() implements FlightCommand {
    }
}

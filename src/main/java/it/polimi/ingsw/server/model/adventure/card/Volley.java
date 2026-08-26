package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Defence;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.List;
import java.util.Optional;

/**
 * One ship being fired on, shot by shot.
 *
 * <p>Three cards send things at a ship — the Pirates, a meteor swarm and the last line of
 * a combat zone — and all three walk the same path: offer each shot in turn, let the
 * player put a shield or a cannon in front of it if anything would work, apply it, and
 * stop to ask which piece to keep if the ship comes apart.
 *
 * <p>The defences offered are already filtered to what would actually work. Offering a
 * player a shield that faces the wrong way, or a cannon that cannot reach, would be
 * offering them a choice the rules do not give.
 */
final class Volley {

    private final Ship ship;
    private final List<Hit> shots;

    private int next;
    private boolean awaitingFragmentChoice;

    /**
     * Sets up a volley against one ship.
     *
     * @param ship  the ship being fired on
     * @param shots what is coming, in order
     */
    Volley(Ship ship, List<Hit> shots) {
        this.ship = ship;
        this.shots = List.copyOf(shots);
    }

    /**
     * Returns the next question for the player under fire.
     *
     * @param player whose ship it is
     * @return the outstanding question, or empty when the volley is over
     */
    Optional<PlayerPrompt> nextQuestion(PlayerColor player) {
        if (awaitingFragmentChoice) {
            return Optional.of(new PlayerPrompt.ChooseFragment(player, ship.pieces()));
        }
        if (next >= shots.size()) {
            return Optional.empty();
        }
        Hit shot = shots.get(next);
        return Optional.of(new PlayerPrompt.ChooseDefence(
                player, shot, ship.targetOf(shot), ship.defencesAgainst(shot)));
    }

    /**
     * Applies the player's answer to the outstanding question.
     *
     * @param choice what they answered
     * @throws IllegalArgumentException if the answer does not fit the question
     */
    void apply(PlayerChoice choice) {
        if (awaitingFragmentChoice) {
            if (!(choice instanceof PlayerChoice.FragmentKept kept)) {
                throw new IllegalArgumentException("this ship is in pieces and one has to be chosen");
            }
            ship.keepFragment(kept.piece());
            awaitingFragmentChoice = false;
            return;
        }
        if (!(choice instanceof PlayerChoice.DefenceChosen defence)) {
            throw new IllegalArgumentException("a shot is incoming and is waiting to be answered");
        }
        Hit shot = shots.get(next++);
        Defence answer = defence.component().map(Defence::using).orElseGet(Defence::none);
        awaitingFragmentChoice = ship.applyHit(shot, answer).brokeUp();
    }

    /**
     * Tells whether every shot has landed and the ship is whole again.
     *
     * @return {@code true} when nothing is left to ask
     */
    boolean isSpent() {
        return !awaitingFragmentChoice && next >= shots.size();
    }
}

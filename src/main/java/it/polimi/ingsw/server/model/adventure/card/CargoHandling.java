package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.Map;
import java.util.Set;

/**
 * Stowing what a card is offering, which is several decisions rather than one.
 *
 * <p>Three cards hand out goods — Planets, Smugglers, Abandoned Station — and all three want
 * the same conversation: here is what is on offer and here are your holds, tell me what you are
 * doing, and tell me again until you say you have finished. Written three times that is three
 * chances to forget to ask again, which is how the goods came to be offered and never taken.
 *
 * <p>A player takes a cube, decides the red one will not fit anywhere useful, moves a blue one
 * to free the special hold, and only then says they are done. Every step is a separate answer
 * because every step can be refused on its own — a red cube in an ordinary hold, a hold that is
 * already full, a cube the card never offered.
 */
final class CargoHandling {

    private CargoHandling() {
    }

    /**
     * Applies one stowing move and works out what to ask next.
     *
     * @param ship    whose hold
     * @param stowing what the player is doing
     * @return the same question again, since arranging cargo is not finished until they say so
     * @throws IllegalArgumentException if the move is not one the ship will accept
     */
    static PlayerPrompt apply(Ship ship, PlayerChoice.CargoStowed stowing) {
        switch (stowing.what()) {
            case LOAD -> {
                if (!ship.load(stowing.hold(), stowing.colour())) {
                    throw new IllegalArgumentException("that cube will not go in there");
                }
            }
            case MOVE -> ship.moveCargo(stowing.hold(),
                    stowing.destination().orElseThrow(), stowing.colour());
            case JETTISON -> ship.jettison(stowing.hold(), stowing.colour());
        }
        return offer(ship, stowing.player());
    }

    /**
     * Asks what to do with what is left on offer.
     *
     * @param ship   whose holds
     * @param player who is stowing
     * @return the question, carrying what is still available and where it could go
     */
    static PlayerPrompt offer(Ship ship, it.polimi.ingsw.common.game.PlayerColor player) {
        Map<GoodColor, Integer> left = ship.cargoWithinReach();
        Set<Position> holds = ship.cargo().keySet();
        return new PlayerPrompt.ArrangeCargo(player, left, holds);
    }
}

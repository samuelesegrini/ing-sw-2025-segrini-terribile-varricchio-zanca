package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.PlayerColor;

import java.io.Serializable;

/**
 * One player, their ship and where they stand.
 *
 * <p>Whether somebody is connected is part of what everyone else can see, because a game
 * carries on without a player who has dropped and the others need to know why nobody is
 * taking their turn (requirement AF4).
 *
 * @param nickname      what they are called
 * @param colour        which set of markers is theirs
 * @param connected     whether their client is currently attached
 * @param retired       whether they have given up or been forced out of the flight
 * @param credits       cosmic credits earned so far during the flight
 * @param ship          their ship, which everybody can see
 */
public record PlayerView(String nickname, PlayerColor colour, boolean connected,
                         boolean retired, int credits, ShipView ship) implements Serializable {

    /**
     * Validates the player.
     *
     * @throws NullPointerException if any part is {@code null}
     */
    public PlayerView {
        if (nickname == null || colour == null || ship == null) {
            throw new NullPointerException("a player view needs a nickname, a colour and a ship");
        }
    }
}

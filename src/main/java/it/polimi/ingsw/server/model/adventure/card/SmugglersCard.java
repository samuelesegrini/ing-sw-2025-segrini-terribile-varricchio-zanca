package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumMap;
import java.util.Map;

/**
 * Smugglers: the enemy that trades in cargo rather than cash.
 *
 * <p>Beat them and their goods are yours, at the cost of flight days. Lose and they take
 * your most valuable cubes — and if you have run out of cargo they take batteries instead,
 * and if you have neither, there is nothing more they can do to you (manual p.11, p.12).
 *
 * <p>The reward being cargo rather than credits is what makes this enemy awkward to win
 * against. Credits arrive whatever shape a ship is in; goods need a hold with room, and a
 * red cube needs a reinforced one. A player can beat the smugglers and still come away
 * with less than the card promised.
 *
 * @param identity     what deck building knows about this card
 * @param firepower    the strength printed beside their cannon
 * @param goods        what beating them is worth
 * @param goodsPenalty how many valuables losing costs
 * @param flightDays   what claiming the goods costs
 */
public record SmugglersCard(AdventureCardIdentity identity, int firepower,
                            Map<GoodColor, Integer> goods, int goodsPenalty,
                            int flightDays) implements AdventureCard {

    /**
     * Validates the printed values and takes a defensive copy of the goods.
     *
     * @throws IllegalArgumentException if a value is negative, the haul is empty, or losing
     *                                  costs nothing
     */
    public SmugglersCard {
        if (firepower < 0 || flightDays < 0) {
            throw new IllegalArgumentException("a smuggler card cannot carry negative values");
        }
        if (goodsPenalty < 1) {
            throw new IllegalArgumentException("smugglers who take nothing are not smugglers");
        }
        Map<GoodColor, Integer> haul = new EnumMap<>(GoodColor.class);
        goods.forEach((color, count) -> {
            if (count < 0) {
                throw new IllegalArgumentException("a haul cannot hold " + count + " " + color + " cubes");
            }
            if (count > 0) {
                haul.put(color, count);
            }
        });
        if (haul.isEmpty()) {
            throw new IllegalArgumentException("smugglers carrying nothing are not worth fighting");
        }
        goods = Map.copyOf(haul);
    }

    /**
     * Returns how many cubes beating them is worth.
     *
     * @return the size of the haul
     */
    public int haulSize() {
        return goods.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Fights down the route, handing the winner a hold full of contraband.
     */
    private static final class Resolution extends EnemyResolution {

        private final SmugglersCard card;

        Resolution(Flight flight, SmugglersCard card) {
            super(flight, card.firepower(), card.flightDays());
            this.card = card;
        }

        @Override
        protected String rewardDescription() {
            return card.haulSize() + " cubes of contraband";
        }

        @Override
        protected boolean claimReward(PlayerColor player) {
            Ship ship = flight().shipOf(player);
            ship.beginCargoOperations(card.goods());
            askAgain(new PlayerPrompt.ArrangeCargo(player, card.goods(), ship.cargo().keySet()));
            // The days are not paid until the player is finished stowing.
            return false;
        }

        @Override
        protected void punish(PlayerColor player) {
            // Most valuable first, then batteries, then nothing: a ship with neither
            // cannot be robbed further.
            flight().shipOf(player).surrender(card.goodsPenalty());
        }

        @Override
        protected boolean applyExtra(PlayerChoice choice) {
            if (!(choice instanceof PlayerChoice.Done done)) {
                return super.applyExtra(choice);
            }
            flight().shipOf(done.player()).endCargoOperations();
            payForReward(done.player());
            return true;
        }
    }
}

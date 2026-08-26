package it.polimi.ingsw.server.model.adventure.card;

import java.util.List;

/**
 * What the weakest ship on a line of a combat zone suffers.
 *
 * <p>Four kinds, and the level I and level II cards pick different ones for different
 * attributes (manual p.13, and the level II card's own artwork). Sealed so that a card
 * carrying a penalty nobody has written the handling for cannot compile.
 */
public sealed interface CombatPenalty {

    /**
     * Falling back along the route.
     *
     * @param days how many flight days are lost
     */
    record LoseFlightDays(int days) implements CombatPenalty {

        /**
         * Validates the penalty.
         *
         * @throws IllegalArgumentException if the loss is not positive
         */
        public LoseFlightDays {
            if (days < 1) {
                throw new IllegalArgumentException("a penalty of " + days + " flight days is no penalty");
            }
        }
    }

    /**
     * Giving up crew, the player choosing which cabins.
     *
     * @param count how many crew are lost
     */
    record LoseCrew(int count) implements CombatPenalty {

        /**
         * Validates the penalty.
         *
         * @throws IllegalArgumentException if the loss is not positive
         */
        public LoseCrew {
            if (count < 1) {
                throw new IllegalArgumentException("a penalty of " + count + " crew is no penalty");
            }
        }
    }

    /**
     * Handing over valuables, most valuable first and batteries once the holds are empty.
     *
     * @param count how many valuables are lost
     */
    record LoseGoods(int count) implements CombatPenalty {

        /**
         * Validates the penalty.
         *
         * @throws IllegalArgumentException if the loss is not positive
         */
        public LoseGoods {
            if (count < 1) {
                throw new IllegalArgumentException("a penalty of " + count + " goods is no penalty");
            }
        }
    }

    /**
     * Being shot at.
     *
     * @param shots what is coming, in order
     */
    record TakeFire(List<ThreatPattern> shots) implements CombatPenalty {

        /**
         * Validates the penalty and takes a defensive copy.
         *
         * @throws IllegalArgumentException if nothing is fired, or a meteor is
         */
        public TakeFire {
            shots = List.copyOf(shots);
            if (shots.isEmpty()) {
                throw new IllegalArgumentException("a volley of nothing is no penalty");
            }
            shots.forEach(shot -> {
                if (shot.isMeteor()) {
                    throw new IllegalArgumentException("a combat zone fires cannons, not " + shot.kind());
                }
            });
        }
    }
}

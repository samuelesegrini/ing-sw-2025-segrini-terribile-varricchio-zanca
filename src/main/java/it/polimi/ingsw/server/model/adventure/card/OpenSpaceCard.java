package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Open Space: the stretch of motorway where engines finally earn their keep.
 *
 * <p>Each player in turn declares engine power and immediately travels that many empty
 * spaces, jumping anybody in the way for free (manual p.13). It is the card that reorders
 * a flight, and the only one where a ship can gain ground rather than merely lose less
 * of it.
 *
 * <p>It is also the one card that can put a player out for a reason that has nothing to
 * do with damage: a ship declaring no engine power at all is left behind, whatever else
 * is still working.
 *
 * <p>Carries no printed values, so it needs nothing beyond its identity.
 *
 * @param identity what deck building knows about this card
 */
public record OpenSpaceCard(AdventureCardIdentity identity) implements AdventureCard {

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight);
    }

    /**
     * Asks each player for engine power, moves them, and clears out whoever declared none.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final List<PlayerColor> stranded = new ArrayList<>();

        Resolution(Flight flight) {
            super(flight.stillFlying());
            this.flight = flight;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            Ship ship = flight.shipOf(player);
            return Optional.of(new PlayerPrompt.DeclarePower(
                    player, ShipAttribute.ENGINE_POWER,
                    ship.componentsOfKind(ComponentKind.DOUBLE_ENGINE), ship.availableCharges()));
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            if (!(choice instanceof PlayerChoice.Declaration declaration)) {
                throw new IllegalArgumentException(
                        "Open Space is waiting for a declared engine power, not " + choice);
            }
            PlayerColor player = declaration.player();
            Ship ship = flight.shipOf(player);
            BatteryPlan plan = declaration.plan();

            // Charges are spent on declaring, whether or not the move achieves anything.
            int power = ship.attributes(plan).enginePower();
            ship.spend(plan);

            if (power == 0) {
                stranded.add(player);
            } else {
                flight.route().advance(player, power);
            }
            return false;
        }

        /**
         * Puts out the ships that declared nothing.
         *
         * <p>Held back until every declaration is in, because a forced give-up is checked
         * once the card is fully resolved (manual p.20). A ship with no engines still
         * watches everybody else move before it leaves.
         */
        @Override
        protected void afterEveryone() {
            stranded.forEach(flight::giveUp);
        }
    }
}

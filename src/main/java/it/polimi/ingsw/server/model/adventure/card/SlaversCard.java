package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.List;

/**
 * Slavers: beat them for cash, lose to them and they take your people.
 *
 * <p>The enemy whose penalty a player has some say in. Losing costs a fixed number of
 * crew, but which crew is the player's choice — humans or aliens, out of whichever cabins
 * they pick (manual p.19). A ship that hands over its last human is out of the race once
 * the card is done, so the choice can matter more than the count.
 *
 * @param identity   what deck building knows about this card
 * @param firepower  the strength printed beside their cannon
 * @param credits    what beating them pays
 * @param crewPenalty how many crew losing costs
 * @param flightDays what claiming the reward costs
 */
public record SlaversCard(AdventureCardIdentity identity, int firepower, int credits,
                          int crewPenalty, int flightDays) implements AdventureCard {

    /**
     * Validates the printed values.
     *
     * @throws IllegalArgumentException if any value is negative, or losing costs no crew
     */
    public SlaversCard {
        if (firepower < 0 || credits < 0 || flightDays < 0) {
            throw new IllegalArgumentException("a slaver card cannot carry negative values");
        }
        if (crewPenalty < 1) {
            throw new IllegalArgumentException("slavers who take nobody are not slavers");
        }
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Fights down the route, paying winners in cash and taking crew from losers.
     */
    private static final class Resolution extends EnemyResolution {

        private final SlaversCard card;

        Resolution(Flight flight, SlaversCard card) {
            super(flight, card.firepower(), card.flightDays());
            this.card = card;
        }

        @Override
        protected String rewardDescription() {
            return card.credits() + " credits for beating the slavers";
        }

        @Override
        protected boolean claimReward(PlayerColor player) {
            flight().awardCredits(player, card.credits());
            payForReward(player);
            return true;
        }

        @Override
        protected void punish(PlayerColor player) {
            askAgain(new PlayerPrompt.GiveUpCrew(player, card.crewPenalty(),
                    flight().shipOf(player).occupiedCabins()));
        }

        @Override
        protected boolean applyExtra(PlayerChoice choice) {
            if (!(choice instanceof PlayerChoice.CrewGiven given)) {
                return super.applyExtra(choice);
            }
            handOverCrew(given.player(), given.cabins());
            // The enemy is not beaten, so it moves on to the next ship.
            return false;
        }

        private void handOverCrew(PlayerColor player, List<Position> cabins) {
            Ship ship = flight().shipOf(player);
            int owed = Math.min(card.crewPenalty(), ship.crewCount());
            if (cabins.size() != owed) {
                throw new IllegalArgumentException(
                        "the slavers are owed " + owed + " crew, not " + cabins.size());
            }
            cabins.forEach(ship::loseOneCrewFrom);
        }
    }
}

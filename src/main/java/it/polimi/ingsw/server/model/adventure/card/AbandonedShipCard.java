package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.List;
import java.util.Optional;

/**
 * Abandoned Ship: sell it to your own crew, and lose them with it.
 *
 * <p>The harsher of the two abandoned sites. It pays cash rather than cargo, and the price
 * is crew — the manual's joke is that they are tired of flying with you and will pay for a
 * ship of their own (p.12). A player without enough people to give away is not offered it.
 *
 * <p>Which crew leave is the player's decision, and a real one: an alien is one crew member
 * rather than two, so giving one up costs less headcount and more capability, and a ship
 * down to its last human has to think hard.
 *
 * @param identity   what deck building knows about this card
 * @param crewCost   how many crew must be given up
 * @param credits    what the crew pay for the ship
 * @param flightDays what the deal costs in flight days
 */
public record AbandonedShipCard(AdventureCardIdentity identity, int crewCost, int credits,
                                int flightDays) implements AdventureCard {

    /**
     * Validates the printed values.
     *
     * @throws IllegalArgumentException if the crew cost is not positive, or a value is negative
     */
    public AbandonedShipCard {
        if (crewCost < 1) {
            throw new IllegalArgumentException("an abandoned ship costing " + crewCost + " crew is free");
        }
        if (credits < 0 || flightDays < 0) {
            throw new IllegalArgumentException("an abandoned ship cannot pay in days or charge in credits");
        }
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Offers the wreck down the route until somebody takes it, then collects their crew.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final AbandonedShipCard card;

        Resolution(Flight flight, AbandonedShipCard card) {
            super(flight.stillFlying());
            this.flight = flight;
            this.card = card;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            if (flight.shipOf(player).crewCount() < card.crewCost()) {
                // Not a refusal: a ship with nobody to spare is never offered the deal.
                return Optional.empty();
            }
            return Optional.of(new PlayerPrompt.TakeOrLeave(player,
                    "an abandoned ship worth " + card.credits() + " credits, for "
                            + card.crewCost() + " crew", card.flightDays()));
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            return switch (choice) {
                case PlayerChoice.Leave ignored -> false;
                case PlayerChoice.Take take -> {
                    askAgain(new PlayerPrompt.GiveUpCrew(take.player(), card.crewCost(),
                            flight.shipOf(take.player()).occupiedCabins()));
                    // Nobody behind is asked; this player still owes the crew.
                    yield true;
                }
                case PlayerChoice.CrewGiven given -> {
                    handOverCrew(given.player(), given.cabins());
                    yield true;
                }
                default -> throw new IllegalArgumentException(
                        "an abandoned ship is waiting to be taken, left or paid for, not " + choice);
            };
        }

        private void handOverCrew(PlayerColor player, List<Position> cabins) {
            if (cabins.size() != card.crewCost()) {
                throw new IllegalArgumentException(
                        "the ship costs " + card.crewCost() + " crew, not " + cabins.size());
            }
            Ship ship = flight.shipOf(player);
            cabins.forEach(ship::loseOneCrewFrom);
            flight.awardCredits(player, card.credits());
            flight.route().fallBack(player, card.flightDays());
        }
    }
}

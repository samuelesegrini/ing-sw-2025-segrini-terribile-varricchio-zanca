package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Hit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Meteor Swarm: the same rocks, thrown at everybody at once.
 *
 * <p>The leader rolls once for each meteor and that roll applies to every ship (manual
 * p.13). What happens next is entirely down to how each ship was built, which is what
 * makes this the card that settles arguments about tidy construction:
 *
 * <ul>
 *   <li>a <b>small</b> meteor bounces off a smooth side for nothing, and destroys an
 *       exposed connector unless a shield covering that side is paid for;</li>
 *   <li>a <b>big</b> one ignores smooth sides and shields alike. The only answer is a
 *       cannon that can reach it — and one at the bow can only be shot down its own
 *       column (p.19).</li>
 * </ul>
 *
 * @param identity what deck building knows about this card
 * @param meteors  the meteors, in the order they arrive
 */
public record MeteorSwarmCard(AdventureCardIdentity identity, List<ThreatPattern> meteors)
        implements AdventureCard {

    /**
     * Validates the printed meteors and takes a defensive copy.
     *
     * @throws IllegalArgumentException if the swarm is empty or something in it is not a meteor
     */
    public MeteorSwarmCard {
        meteors = List.copyOf(meteors);
        if (meteors.isEmpty()) {
            throw new IllegalArgumentException("a swarm with no meteors in it is not a swarm");
        }
        meteors.forEach(meteor -> {
            if (!meteor.isMeteor()) {
                throw new IllegalArgumentException("a meteor swarm throws rocks, not " + meteor.kind());
            }
        });
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Rolls the swarm once, then walks each ship through it.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final List<Hit> swarm;
        private final Map<PlayerColor, Volley> volleys = new HashMap<>();

        Resolution(Flight flight, MeteorSwarmCard card) {
            super(flight.stillFlying());
            this.flight = flight;
            // Rolled once, here, for everybody. Rolling per ship would turn one swarm into
            // several unrelated ones and quietly halve how dangerous the card is.
            this.swarm = card.meteors().stream()
                    .map(meteor -> new Hit(meteor.kind(), meteor.from(), flight.dice().roll()))
                    .toList();
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            return volleyFor(player).nextQuestion(player);
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            Volley volley = volleyFor(choice.player());
            volley.apply(choice);
            volley.nextQuestion(choice.player()).ifPresent(this::askAgain);
            return false;
        }

        private Volley volleyFor(PlayerColor player) {
            return volleys.computeIfAbsent(player, who -> new Volley(flight.shipOf(who), swarm));
        }
    }
}

package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.server.model.flight.Dice;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Hit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pirates: beat them for cash, lose to them and they shoot.
 *
 * <p>The enemy that fires back, and the one rule worth being careful with is <em>when</em>
 * they fire. They work down the whole route first, and only then do the dice come out:
 * one roll per shot, made by the first player they beat, and that same roll applies to
 * every ship they beat (manual p.19).
 *
 * <p>Rolling separately for each victim would be a much gentler card. Sharing the rolls
 * means a bad seven wrecks the same column on every ship that lost, which is exactly the
 * kind of shared disaster the card is for.
 *
 * @param identity   what deck building knows about this card
 * @param firepower  the strength printed beside their cannon
 * @param credits    what beating them pays
 * @param shots      what they fire at everyone they beat
 * @param flightDays what claiming the reward costs
 */
public record PiratesCard(AdventureCardIdentity identity, int firepower, int credits,
                          List<ThreatPattern> shots, int flightDays) implements AdventureCard {

    /**
     * Validates the printed values and takes a defensive copy of the shots.
     *
     * @throws IllegalArgumentException if a value is negative, they fire nothing, or one of
     *                                  their shots is a meteor
     */
    public PiratesCard {
        if (firepower < 0 || credits < 0 || flightDays < 0) {
            throw new IllegalArgumentException("a pirate card cannot carry negative values");
        }
        shots = List.copyOf(shots);
        if (shots.isEmpty()) {
            throw new IllegalArgumentException("pirates who do not shoot are not pirates");
        }
        shots.forEach(shot -> {
            if (shot.isMeteor()) {
                throw new IllegalArgumentException("pirates fire cannons, not " + shot.kind());
            }
        });
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this, flight.dice());
    }

    /**
     * Fights down the route, then fires one shared volley at everybody it beat.
     */
    private static final class Resolution extends EnemyResolution {

        private final PiratesCard card;
        private final Dice dice;
        private final List<PlayerColor> beaten = new ArrayList<>();
        private final Map<PlayerColor, Volley> volleys = new HashMap<>();

        private boolean firing;

        Resolution(Flight flight, PiratesCard card, Dice dice) {
            super(flight, card.firepower(), card.flightDays());
            this.card = card;
            this.dice = dice;
        }

        @Override
        protected String rewardDescription() {
            return card.credits() + " credits for driving off the pirates";
        }

        @Override
        protected boolean claimReward(PlayerColor player) {
            flight().awardCredits(player, card.credits());
            payForReward(player);
            return true;
        }

        @Override
        protected void punish(PlayerColor player) {
            // Nothing happens yet. The pirates finish working down the route before the
            // dice come out, so that every ship they beat takes the same volley.
            beaten.add(player);
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            return firing ? volleyFor(player).nextQuestion(player) : super.promptFor(player);
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            if (!firing) {
                return super.apply(choice);
            }
            Volley volley = volleyFor(choice.player());
            volley.apply(choice);
            volley.nextQuestion(choice.player()).ifPresent(this::askAgain);
            return false;
        }

        /**
         * Rolls once for each shot, then sends everybody the pirates beat through it.
         */
        @Override
        protected void afterEveryone() {
            if (firing || beaten.isEmpty()) {
                return;
            }
            firing = true;
            List<Hit> volley = card.shots().stream()
                    .map(shot -> new Hit(shot.kind(), shot.from(), dice.roll()))
                    .toList();

            Map<PlayerColor, Volley> ordered = new LinkedHashMap<>();
            for (PlayerColor player : flight().stillFlying()) {
                if (beaten.contains(player)) {
                    ordered.put(player, new Volley(flight().shipOf(player), volley));
                }
            }
            volleys.putAll(ordered);
            queueAgain(List.copyOf(ordered.keySet()));
        }

        private Volley volleyFor(PlayerColor player) {
            Volley volley = volleys.get(player);
            if (volley == null) {
                throw new IllegalArgumentException("the pirates did not beat the " + player + " player");
            }
            return volley;
        }
    }
}

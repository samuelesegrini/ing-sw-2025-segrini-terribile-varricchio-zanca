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
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Combat Zone: three lines, each punishing whoever is weakest at something.
 *
 * <p>The card that inverts the usual advantage. Everywhere else being in front is good;
 * here a tie is broken against the player furthest ahead, so the leader pays for being
 * the leader (manual p.13).
 *
 * <p>Each line is its own pass, and that matters. Losing flight days on one line can
 * reorder the route before the next is evaluated, so the order of play is read again for
 * every line rather than once for the card. The manual notes this explicitly, which is the
 * clue that a single reading would be wrong.
 *
 * <p>The whole card is skipped when everybody else has given up. It penalises whoever is
 * weakest at something, and with nobody to compare against that is meaningless (p.20).
 *
 * @param identity what deck building knows about this card
 * @param lines    the lines, evaluated top to bottom
 */
public record CombatZoneCard(AdventureCardIdentity identity, List<CombatLine> lines)
        implements AdventureCard {

    /**
     * Validates the printed lines and takes a defensive copy.
     *
     * @throws IllegalArgumentException if the card has no lines
     */
    public CombatZoneCard {
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("a combat zone with no lines is not a combat zone");
        }
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /** Which half of a line the resolution is in. */
    private enum Phase {

        /** Working down the route collecting values. */
        COMPARING,

        /** Making the weakest ship pay. */
        PUNISHING
    }

    /**
     * Runs the lines one after another, comparing then punishing.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final CombatZoneCard card;
        private final Map<PlayerColor, Integer> declared = new LinkedHashMap<>();

        private int line;
        private Phase phase = Phase.COMPARING;
        private PlayerColor weakest;
        private Volley volley;

        Resolution(Flight flight, CombatZoneCard card) {
            // With only one ship left there is nobody to be weakest against, so the card
            // never starts (manual p.20).
            super(flight.isSolo() ? List.of() : flight.stillFlying());
            this.flight = flight;
            this.card = card;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            return phase == Phase.COMPARING ? comparisonPrompt(player) : penaltyPrompt(player);
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            if (phase == Phase.COMPARING) {
                compare(choice);
            } else {
                punish(choice);
            }
            return false;
        }

        /**
         * Ends a phase and starts the next one.
         *
         * <p>Called each time a pass runs out. Comparing is followed by punishing whoever
         * came last; punishing is followed by the next line, with the route order read
         * again because the last penalty may have changed it.
         */
        @Override
        protected void afterEveryone() {
            if (line >= card.lines().size()) {
                return;
            }
            if (phase == Phase.COMPARING) {
                settleLine();
            } else {
                phase = Phase.COMPARING;
                line++;
                declared.clear();
                weakest = null;
                volley = null;
                if (line < card.lines().size()) {
                    // Read again: a penalty on the last line may have reordered the route.
                    queueAgain(flight.stillFlying());
                }
            }
        }

        // ---------------------------------------------------------------- comparing

        private Optional<PlayerPrompt> comparisonPrompt(PlayerColor player) {
            CombatLine current = card.lines().get(line);
            if (!current.needsDeclaring()) {
                // Crew is counted, not declared: there is nothing optional about it.
                declared.put(player, flight.shipOf(player).crewCount());
                return Optional.empty();
            }
            Ship ship = flight.shipOf(player);
            ComponentKind doubles = current.attribute() == ShipAttribute.FIREPOWER
                    ? ComponentKind.DOUBLE_CANNON
                    : ComponentKind.DOUBLE_ENGINE;
            return Optional.of(new PlayerPrompt.DeclarePower(
                    player, current.attribute(), ship.componentsOfKind(doubles), ship.availableCharges()));
        }

        private void compare(PlayerChoice choice) {
            if (!(choice instanceof PlayerChoice.Declaration declaration)) {
                throw new IllegalArgumentException("this line is waiting for a declaration, not " + choice);
            }
            Ship ship = flight.shipOf(declaration.player());
            BatteryPlan plan = declaration.plan();
            declared.put(declaration.player(), valueOf(ship, plan));
            ship.spend(plan);
        }

        private int valueOf(Ship ship, BatteryPlan plan) {
            return switch (card.lines().get(line).attribute()) {
                // Firepower stays in halves so that 5½ keeps beating 5 here too.
                case FIREPOWER -> ship.attributes(plan).firepowerHalves();
                case ENGINE_POWER -> ship.attributes(plan).enginePower();
                case CREW -> ship.crewCount();
            };
        }

        // ---------------------------------------------------------------- punishing

        /**
         * Finds the weakest ship and starts making it pay.
         *
         * <p>Ties go against the player furthest ahead, which is why the search runs down
         * the route order rather than over an unordered map: the first player found holding
         * the lowest value is by definition the leading one among those tied.
         */
        private void settleLine() {
            weakest = flight.stillFlying().stream()
                    .filter(declared::containsKey)
                    .min(java.util.Comparator.comparingInt(declared::get))
                    .orElse(null);
            if (weakest == null) {
                line = card.lines().size();
                return;
            }
            phase = Phase.PUNISHING;
            queueAgain(List.of(weakest));
        }

        private Optional<PlayerPrompt> penaltyPrompt(PlayerColor player) {
            Ship ship = flight.shipOf(player);
            return switch (card.lines().get(line).penalty()) {
                case CombatPenalty.LoseFlightDays days -> {
                    flight.route().fallBack(player, days.days());
                    yield Optional.empty();
                }
                case CombatPenalty.LoseGoods goods -> {
                    ship.surrender(goods.count());
                    yield Optional.empty();
                }
                case CombatPenalty.LoseCrew crew -> Optional.of(new PlayerPrompt.GiveUpCrew(
                        player, Math.min(crew.count(), ship.crewCount()), ship.occupiedCabins()));
                case CombatPenalty.TakeFire fire -> {
                    volley = new Volley(ship, fire.shots().stream()
                            .map(shot -> new Hit(shot.kind(), shot.from(), flight.dice().roll()))
                            .toList());
                    yield volley.nextQuestion(player);
                }
            };
        }

        private void punish(PlayerChoice choice) {
            if (volley != null) {
                volley.apply(choice);
                volley.nextQuestion(choice.player()).ifPresent(this::askAgain);
                return;
            }
            if (!(choice instanceof PlayerChoice.CrewGiven given)) {
                throw new IllegalArgumentException("this penalty is waiting for crew, not " + choice);
            }
            handOverCrew(given.player(), given.cabins());
        }

        private void handOverCrew(PlayerColor player, List<Position> cabins) {
            Ship ship = flight.shipOf(player);
            CombatPenalty.LoseCrew penalty = (CombatPenalty.LoseCrew) card.lines().get(line).penalty();
            int owed = Math.min(penalty.count(), ship.crewCount());
            if (cabins.size() != owed) {
                throw new IllegalArgumentException("this line costs " + owed + " crew, not " + cabins.size());
            }
            cabins.forEach(ship::loseOneCrewFrom);
        }
    }
}

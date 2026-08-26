package it.polimi.ingsw.common.game;



import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A decision an adventure card is waiting on.
 *
 * <p>Sealed, so that a client rendering prompts cannot quietly forget one when a new card
 * introduces a kind of decision. Every variant names the player being asked, because a
 * prompt addressed to nobody in particular is not a prompt.
 *
 * <p>Variants arrive with the cards that need them rather than being guessed at up
 * front — a prompt nobody sends is a prompt nobody has thought through.
 */
public sealed interface PlayerPrompt extends Serializable {

    /**
     * Returns who is being asked.
     *
     * @return the player whose answer the card is waiting for
     */
    PlayerColor player();

    /**
     * An offer the player may take or leave.
     *
     * <p>Shared by every card that puts something on the table for one player at a time:
     * an abandoned ship, an abandoned station, the reward for beating an enemy. All of
     * them cost flight days, and all of them may simply be declined (manual p.12, p.19).
     *
     * @param player      who is being offered it
     * @param description what is on offer, in the language of the card
     * @param flightDays  what taking it costs
     */
    record TakeOrLeave(PlayerColor player, String description, int flightDays) implements PlayerPrompt {


        /**
         * Validates the offer.
         *
         * @throws IllegalArgumentException if the cost is negative
         * @throws NullPointerException     if the player or description is {@code null}
         */
        public TakeOrLeave {
            if (player == null || description == null) {
                throw new NullPointerException("an offer needs a player and a description");
            }
            if (flightDays < 0) {
                throw new IllegalArgumentException("an offer cannot pay flight days, got " + flightDays);
            }
        }
    }

    /**
     * A call to declare engine power or firepower.
     *
     * <p>The declaration is where a player spends batteries, and it is the same decision
     * whichever attribute is being asked for: which doubles to run, knowing the charges
     * are gone either way (manual p.11). Single engines, single cannons and aliens are not
     * in the offer because they are never optional — a player may not declare less than
     * they have (p.19).
     *
     * @param player           who is being asked
     * @param attribute        which attribute the card wants
     * @param activatable      the doubles that could be run, each costing one charge
     * @param chargesAvailable how many charges the ship still holds
     */
    record DeclarePower(PlayerColor player, ShipAttribute attribute,
                        Set<Position> activatable, int chargesAvailable) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws IllegalArgumentException if the charge count is negative
         * @throws NullPointerException     if the player or attribute is {@code null}
         */
        public DeclarePower {
            if (player == null || attribute == null) {
                throw new NullPointerException("a declaration needs a player and an attribute");
            }
            if (chargesAvailable < 0) {
                throw new IllegalArgumentException("a ship cannot hold " + chargesAvailable + " charges");
            }
            activatable = Set.copyOf(activatable);
        }
    }

    /**
     * A call to stow what a card has just put on the table.
     *
     * <p>The one moment cargo can be moved at all: cubes may be loaded, shuffled between
     * holds, or thrown overboard to make room (quick reference). Anything left on the
     * table when the player is done stays there.
     *
     * <p>The offer is a bound, not an instruction. A player is free to take less of it,
     * and often has to — red cubes need a reinforced hold, and holds run out.
     *
     * @param player  who is stowing
     * @param offered how many cubes of each colour are on the table
     * @param holds   the cells that could take something
     */
    record ArrangeCargo(PlayerColor player, Map<GoodColor, Integer> offered,
                        Set<Position> holds) implements PlayerPrompt {

        /**
         * Validates the call and takes defensive copies.
         *
         * @throws NullPointerException if the player is {@code null}
         */
        public ArrangeCargo {
            if (player == null) {
                throw new NullPointerException("a cargo call needs a player");
            }
            offered = Map.copyOf(offered);
            holds = Set.copyOf(holds);
        }
    }

    /**
     * A call to give up crew.
     *
     * <p>The player chooses which cabins the losses come out of, and that is a real choice:
     * an alien is worth two humans of space but only one crew member, and giving up the
     * wrong one can cost a ship its firepower bonus or its last human (manual p.11, p.18).
     *
     * @param player who is losing crew
     * @param count  how many must go
     * @param cabins the cabins with somebody in them
     */
    record GiveUpCrew(PlayerColor player, int count, Set<Position> cabins) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws IllegalArgumentException if the count is not positive
         * @throws NullPointerException     if the player is {@code null}
         */
        public GiveUpCrew {
            if (player == null) {
                throw new NullPointerException("a crew call needs a player");
            }
            if (count < 1) {
                throw new IllegalArgumentException("a card taking " + count + " crew is taking nothing");
            }
            cabins = Set.copyOf(cabins);
        }
    }

    /**
     * A shot or a meteor on its way in, and what could be put in front of it.
     *
     * <p>The options are already filtered to what would actually work: a shield covering
     * the right side, or a cannon that can reach a big meteor from where it sits. An empty
     * set means nothing can be done, which is the normal answer to heavy fire.
     *
     * <p>The target is named so a player can see what they are about to lose. It is
     * {@code null} when the roll named a line with nothing on it, and the whole thing is a
     * formality; read it through {@link #targetIfAny()}.
     *
     * @param player  whose ship is in the way
     * @param hit     what is coming
     * @param target  the component it would strike, {@code null} when it misses
     * @param options the components that could stop it, each costing a charge to use
     */
    record ChooseDefence(PlayerColor player, Hit hit, Position target,
                         Set<Position> options) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws NullPointerException if any part is {@code null}
         */
        public ChooseDefence {
            if (player == null || hit == null) {
                throw new NullPointerException("an incoming shot needs a player and a hit");
            }
            options = Set.copyOf(options);
        }

        /**
         * Returns the component the shot would strike.
         *
         * @return the target, or empty when the roll named a line with nothing on it
         */
        public Optional<Position> targetIfAny() {
            return Optional.ofNullable(target);
        }
    }

    /**
     * A call to choose which piece of a broken ship to carry on with.
     *
     * <p>Everything outside the chosen piece flies away and is lost along the route
     * (manual p.10). The manual offers the choice however lopsided the pieces are.
     *
     * @param player whose ship came apart
     * @param pieces the pieces it is in, largest first
     */
    record ChooseFragment(PlayerColor player, List<Set<Position>> pieces) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws IllegalArgumentException if there is nothing to choose between
         * @throws NullPointerException     if the player is {@code null}
         */
        public ChooseFragment {
            if (player == null) {
                throw new NullPointerException("a fragment call needs a player");
            }
            pieces = pieces.stream().map(Set::copyOf).toList();
            if (pieces.size() < 2) {
                throw new IllegalArgumentException("a ship in one piece needs no choosing");
            }
        }
    }

    /**
     * A call to pick a planet to land on, or none.
     *
     * <p>Only free planets are offered: one ship per planet, and once a rocket marker is on
     * one nobody else may land there (manual p.12). Landing is never compulsory, and
     * landing purely to deny somebody else a good planet is a legitimate move — which is
     * why the goods on each one are shown rather than just their number.
     *
     * @param player     who is choosing
     * @param planets    the free planets, by their printed number, and what is on each
     * @param flightDays what landing costs
     */
    record ChoosePlanet(PlayerColor player, Map<Integer, Map<GoodColor, Integer>> planets,
                        int flightDays) implements PlayerPrompt {

        /**
         * Validates the call and takes a defensive copy.
         *
         * @throws IllegalArgumentException if nothing is on offer or the cost is negative
         * @throws NullPointerException     if the player is {@code null}
         */
        public ChoosePlanet {
            if (player == null) {
                throw new NullPointerException("a planet call needs a player");
            }
            if (flightDays < 0) {
                throw new IllegalArgumentException("landing cannot pay flight days");
            }
            planets = planets.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
            if (planets.isEmpty()) {
                throw new IllegalArgumentException("a call with no free planets should not be made");
            }
        }
    }
}
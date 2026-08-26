package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.BatteryComponent;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.server.model.component.CannonComponent;
import it.polimi.ingsw.server.model.component.EngineComponent;
import it.polimi.ingsw.server.model.component.CargoHoldComponent;
import it.polimi.ingsw.server.model.component.ShieldComponent;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.StartingCabinTile;
import it.polimi.ingsw.server.model.component.Tile;
import it.polimi.ingsw.server.model.crew.AlienColor;
import it.polimi.ingsw.server.model.goods.Forfeit;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.goods.GoodsBank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * One player's ship: what is welded where, and everything the rules ask about it.
 *
 * <p>This is the deep module of the model. The grid, the connector matrix and the
 * connectivity search are all private; callers ask questions in the language of the
 * rules — is this legal, what is your firepower, how many exposed connectors — and
 * never iterate cells. That is deliberate: the previous implementation exposed its
 * internals, and every caller grew its own version of the rules until two validators
 * were disagreeing about the same ship.
 */
public final class Ship {

    /** Firepower is counted in halves, so a whole point is worth two. */
    private static final int HALVES_PER_POINT = 2;

    /** An alien of the right colour is worth two points, when the attribute is already above zero. */
    private static final int ALIEN_BONUS = 2;

    private final ShipGrid grid;
    private final ShipValidator validator;
    private final GoodsBank bank;

    private boolean cargoOperationsOpen;
    private int lostComponents;

    /**
     * Builds a ship around its starting cabin.
     *
     * <p>The bank is a collaborator rather than a caller's responsibility, so that cubes
     * are conserved by construction: everything a ship loads comes out of it, and
     * everything it sells, jettisons or has taken from it goes straight back. Leaving that
     * to callers means one of them eventually forgets, and cubes quietly appear from
     * nowhere.
     *
     * @param spec          the board this ship is built on
     * @param startingCabin the cabin the player was given
     * @param bank          the goods every ship in the game draws from
     * @throws NullPointerException if any argument is {@code null}
     */
    public Ship(ShipBoardSpec spec, StartingCabinTile startingCabin, GoodsBank bank) {
        this.grid = new ShipGrid(spec);
        this.validator = new ShipValidator(grid);
        this.bank = bank;
        grid.put(spec.startingCabin(), ShipComponent.place(startingCabin, Rotation.NONE));
    }

    // ---------------------------------------------------------------- shape

    /**
     * Returns the board this ship is built on.
     *
     * @return the board specification
     */
    public ShipBoardSpec board() {
        return grid.spec();
    }

    /**
     * Returns what is welded where.
     *
     * @return an unmodifiable view of the occupied cells
     */
    public Map<Position, ShipComponent> components() {
        return grid.occupied();
    }

    /**
     * Returns the component in a cell.
     *
     * @param cell the cell to look at
     * @return what is there, or empty when the cell is free
     */
    public Optional<ShipComponent> componentAt(Position cell) {
        return grid.at(cell);
    }

    /**
     * Tells whether a component may be welded into a cell right now.
     *
     * <p>This is the check the manual expects to happen as a tile is put down: the cell
     * has to be inside the outline, free, and touching the ship. Whether the joint is a
     * legal one is settled when building ends, the way the physical game settles it by
     * eye (§ D4 of the rules specification).
     *
     * @param cell the cell to test
     * @return {@code true} when a component could go there
     */
    public boolean canPlaceAt(Position cell) {
        return grid.isUsable(cell) && !grid.isOccupied(cell) && grid.touchesShip(cell);
    }

    /**
     * Welds a piece into a cell.
     *
     * @param cell     where it goes
     * @param tile     the printed piece
     * @param rotation how far it is turned
     * @return the component that was created
     * @throws IllegalArgumentException if the cell cannot take a component
     */
    public ShipComponent place(Position cell, Tile tile, Rotation rotation) {
        if (!canPlaceAt(cell)) {
            throw new IllegalArgumentException(
                    tile.id() + " cannot be welded at " + cell + ": the cell is taken, outside the ship, "
                            + "or touching nothing");
        }
        ShipComponent component = ShipComponent.place(tile, rotation);
        grid.put(cell, component);
        return component;
    }

    /**
     * Lifts a component off without counting it as lost.
     *
     * <p>For one purpose only: sliding the tile a player has just put down, before it is
     * welded. Nothing is lost when a piece is moved from one cell to another, and counting
     * it would charge the player a credit for changing their mind.
     *
     * @param cell the cell to clear
     * @return what was lifted, or empty when the cell was already free
     */
    public Optional<ShipComponent> lift(Position cell) {
        return grid.remove(cell);
    }

    /**
     * Tears a component off and writes it off.
     *
     * <p>For fixing an illegal ship: components removed to make a ship legal go to the
     * discard pile and count as lost along the route, a credit each at journey's end
     * (manual p.8, p.17). Whatever the removal strands is left to the caller, because the
     * rules differ — correcting a ship lets the player choose what else to sacrifice,
     * while a hit simply drops everything that comes loose.
     *
     * @param cell the cell to clear
     * @return what was discarded, or empty when the cell was already free
     */
    public Optional<ShipComponent> discard(Position cell) {
        Optional<ShipComponent> removed = grid.remove(cell);
        removed.ifPresent(component -> {
            returnTokensToBank(component);
            lostComponents++;
        });
        return removed;
    }

    /**
     * Returns how many components this ship has lost along the route.
     *
     * <p>Everything in the discard pile: shot off, torn away when the ship broke up,
     * removed to make an illegal ship legal, and reserved but never attached. Each one
     * costs a credit at journey's end (manual p.10).
     *
     * @return the number of components lost
     */
    public int lostComponentCount() {
        return lostComponents;
    }

    /**
     * Writes off components that never made it onto the ship.
     *
     * <p>The tiles left in the reservation corner when building ends. They were never
     * welded on, so the ship never held them, but the manual counts them as lost along
     * the route all the same (p.17).
     *
     * @param count how many were abandoned
     * @throws IllegalArgumentException if the count is negative
     */
    public void writeOffAbandoned(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("cannot write off " + count + " components");
        }
        lostComponents += count;
    }

    // ---------------------------------------------------------------- legality

    /**
     * Checks the ship against the assembly rules.
     *
     * @return the violations found, empty when the ship may fly as it stands
     */
    public ValidationReport validate() {
        return validator.validate();
    }

    /**
     * Tells whether the ship is a single welded piece.
     *
     * @return {@code true} when nothing has come loose
     */
    public boolean isWhole() {
        return grid.isWhole();
    }

    /**
     * Returns the separate pieces the ship is currently in, largest first.
     *
     * <p>When a hit breaks a ship up the player keeps one piece and loses the rest
     * (manual p.10), so this is what that choice is offered from.
     *
     * @return one set of cells per piece
     */
    public List<Set<Position>> pieces() {
        return grid.pieces();
    }

    /**
     * Counts the connectors facing open space.
     *
     * <p>One per exposed side, whatever its pipe count, and never a smooth side. This is
     * what Stardust charges a flight day for and what the prettiest ship reward measures
     * (manual p.8).
     *
     * @return the number of exposed connectors
     */
    public int exposedConnectors() {
        return grid.exposedConnectors();
    }


    // ---------------------------------------------------------------- damage

    /**
     * Returns the component a threat would strike.
     *
     * <p>The dice name a line, the direction says which end it comes from, and the threat
     * meets the first component it finds. A roll that names no line on this board, or a
     * line with nothing on it, misses the ship entirely — a real and reasonably common
     * outcome, not an error.
     *
     * @param hit the incoming threat
     * @return the cell it would strike, or empty when it misses
     */
    public Optional<Position> targetOf(Hit hit) {
        OptionalInt line = hit.from().addressesColumn()
                ? board().columnForDiceSum(hit.diceSum())
                : board().rowForDiceSum(hit.diceSum());
        return line.isEmpty() ? Optional.empty() : grid.firstInLine(hit.from(), line.getAsInt());
    }

    /**
     * Returns the components that could stop a threat.
     *
     * <p>Shields turn aside small meteors and light fire, and only from the sides they
     * cover. Cannons shoot big meteors, and only from the right place: one coming at the
     * bow can be hit only by a forward cannon in its own column, while one coming from a
     * side or the stern can be hit by a cannon pointing at it in the same row or column or
     * either neighbouring one (manual p.19).
     *
     * <p>Heavy fire returns nothing, because nothing stops it.
     *
     * @param hit the incoming threat
     * @return the cells whose components could be used against it
     */
    public Set<Position> defencesAgainst(Hit hit) {
        if (hit.kind().stoppableByShield()) {
            return components().entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof ShieldComponent shield
                            && shield.covers(hit.from()))
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        if (hit.kind().stoppableByCannon()) {
            return targetOf(hit)
                    .map(target -> cannonsBearingOn(hit, target))
                    .orElseGet(Set::of);
        }
        return Set.of();
    }

    private Set<Position> cannonsBearingOn(Hit hit, Position target) {
        int line = hit.from().addressesColumn() ? target.column() : target.row();
        boolean fromTheBow = hit.from() == Direction.NORTH;
        return components().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CannonComponent cannon
                        && cannon.muzzleDirection() == hit.from()
                        && bears(entry.getKey(), hit, line, fromTheBow))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private boolean bears(Position cannon, Hit hit, int line, boolean fromTheBow) {
        int cannonLine = hit.from().addressesColumn() ? cannon.column() : cannon.row();
        // A meteor at the bow can only be shot down the column it is coming along; from
        // anywhere else, a neighbouring line will do.
        return fromTheBow ? cannonLine == line : Math.abs(cannonLine - line) <= 1;
    }

    /**
     * Resolves one threat against this ship.
     *
     * <p>Order matters. A small meteor that meets a smooth side bounces for nothing, so
     * that is checked before any defence is spent — a player who offers a shield against a
     * meteor that was going to bounce keeps their battery.
     *
     * <p>If the threat gets through, the component is destroyed, whatever it was carrying
     * goes back to the bank, and any alien left without its life support leaves with it
     * (manual p.10, p.18). The ship may end up in several pieces, in which case the report
     * says so and {@link #keepFragment} settles which one flies on.
     *
     * @param hit     the incoming threat
     * @param defence what the player is putting in its way, if anything
     * @return what happened
     * @throws IllegalArgumentException if the defence is not one this ship can mount
     * @throws IllegalStateException    if the ship is still in pieces from an earlier hit,
     *                                  with the choice of which to keep unmade
     */
    public DamageReport applyHit(Hit hit, Defence defence) {
        requireNoPendingChoice();
        Optional<Position> target = targetOf(hit);
        if (target.isEmpty()) {
            return report(target, DamageReport.Outcome.MISSED, Optional.empty());
        }

        ShipComponent struck = grid.at(target.get()).orElseThrow();
        if (hit.kind().bouncesOffSmoothSides() && !struck.connectorFacing(hit.from()).isConnector()) {
            return report(target, DamageReport.Outcome.BOUNCED, Optional.empty());
        }

        if (defence.component().isPresent()) {
            spendOnDefence(hit, defence.component().get());
            return report(target, DamageReport.Outcome.DEFENDED, Optional.empty());
        }

        destroy(target.get());
        return report(target, DamageReport.Outcome.DESTROYED, target);
    }

    private void spendOnDefence(Hit hit, Position component) {
        if (!defencesAgainst(hit).contains(component)) {
            throw new IllegalArgumentException(
                    componentAt(component).map(ShipComponent::id).orElse("nothing at " + component)
                            + " cannot stop a " + hit.kind() + " arriving from the " + hit.from());
        }
        boolean costsACharge = grid.at(component)
                .map(used -> used.kind().consumesCharge())
                .orElse(false);
        if (costsACharge) {
            spend(BatteryPlan.powering(component));
        }
    }

    private DamageReport report(Optional<Position> target, DamageReport.Outcome outcome,
                                Optional<Position> destroyed) {
        return new DamageReport(target, outcome, destroyed, pieces());
    }

    /**
     * Keeps one piece of a broken ship and lets the rest fly away.
     *
     * <p>The choice the manual gives a player whose ship has come apart (p.10). Everything
     * outside the chosen piece is lost along the route, and whatever those components were
     * carrying goes straight back to the bank.
     *
     * @param fragment the piece to carry on with
     * @return the cells that flew away
     * @throws IllegalArgumentException if that is not one of the pieces the ship is in
     */
    public Set<Position> keepFragment(Set<Position> fragment) {
        if (!pieces().contains(fragment)) {
            throw new IllegalArgumentException(fragment + " is not one of this ship's pieces");
        }
        Set<Position> lost = new HashSet<>(components().keySet());
        lost.removeAll(fragment);
        lost.forEach(this::destroy);
        return Set.copyOf(lost);
    }

    private void requireNoPendingChoice() {
        if (!isWhole()) {
            throw new IllegalStateException("this ship is in pieces: choose one before anything else happens");
        }
    }

    private void destroy(Position cell) {
        grid.remove(cell).ifPresent(component -> {
            returnTokensToBank(component);
            lostComponents++;
        });
        removeStrandedAliens();
    }

    private void returnTokensToBank(ShipComponent component) {
        switch (component) {
            case BatteryComponent battery -> battery.drain();
            case CargoHoldComponent hold -> {
                bank.giveBackAll(hold.contents());
                hold.jettisonAll();
            }
            case CabinComponent cabin -> cabin.evacuate();
            default -> {
                // Nothing else carries anything the bank wants back.
            }
        }
    }

    // ---------------------------------------------------------------- life support

    /**
     * Tells whether a cabin is joined to a life support module of the right colour.
     *
     * <p>Joined, not merely next door: the two have to be welded together for the module
     * to keep anything alive (manual p.18).
     *
     * @param cabin the cabin to check
     * @param color the species that would live there
     * @return {@code true} when a matching module is welded to that cabin
     */
    public boolean isLifeSupported(Position cabin, AlienColor color) {
        return grid.jointNeighbours(cabin).stream()
                .map(grid::at)
                .flatMap(Optional::stream)
                .anyMatch(neighbour -> neighbour.kind() == color.lifeSupport());
    }

    /**
     * Sends home any alien whose life support has been destroyed.
     *
     * <p>Losing the module loses the alien with it — it leaves in an escape pod, the
     * manual says (p.18).
     */
    private void removeStrandedAliens() {
        List<Position> stranded = new ArrayList<>();
        components().forEach((cell, component) -> {
            if (component instanceof CabinComponent cabin) {
                cabin.alien()
                        .filter(color -> !isLifeSupported(cell, color))
                        .ifPresent(color -> stranded.add(cell));
            }
        });
        stranded.forEach(cell -> ((CabinComponent) grid.at(cell).orElseThrow()).evacuate());
    }

    // ---------------------------------------------------------------- crew

    /**
     * Counts everyone aboard, humans and aliens alike.
     *
     * <p>Aliens are crew for every purpose the rules compare — combat zones, abandoned
     * stations, slavers (manual p.18) — and an alien counts as one, not as the two humans
     * whose space it took.
     *
     * @return the number of crew members
     */
    public int crewCount() {
        return cabins().mapToInt(CabinComponent::crewCount).sum();
    }

    /**
     * Counts the humans aboard.
     *
     * <p>Tracked apart from the total because losing the last human forces a player out
     * of the race: aliens cannot fly the ship on their own (manual p.20).
     *
     * @return the number of humans
     */
    public int humanCount() {
        return cabins().mapToInt(CabinComponent::humans).sum();
    }

    /**
     * Returns the aliens aboard.
     *
     * @return the colours of the aliens carried, at most one of each
     */
    public Set<AlienColor> aliens() {
        return cabins()
                .map(CabinComponent::alien)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }



    // ---------------------------------------------------------------- cargo

    /**
     * Returns what is in each hold.
     *
     * @return an immutable view, hold by hold, each list most valuable first
     */
    public Map<Position, List<GoodColor>> cargo() {
        Map<Position, List<GoodColor>> manifest = new java.util.LinkedHashMap<>();
        components().forEach((cell, component) -> {
            if (component instanceof CargoHoldComponent hold) {
                manifest.put(cell, hold.contents());
            }
        });
        return java.util.Collections.unmodifiableMap(manifest);
    }

    /**
     * Returns every cube aboard, most valuable first.
     *
     * <p>The order cards take them in, and the order they are sold in at journey's end.
     *
     * @return the whole manifest
     */
    public List<GoodColor> manifest() {
        return holds()
                .flatMap(hold -> hold.contents().stream())
                .sorted()
                .toList();
    }

    /**
     * Returns how many cubes are aboard.
     *
     * @return the total cargo
     */
    public int cargoCount() {
        return holds().mapToInt(CargoHoldComponent::load).sum();
    }

    /**
     * Returns the holds that could take a cube of the given colour right now.
     *
     * <p>Red goods are hazardous and only a reinforced hold will carry them (manual p.7),
     * so this is often shorter than a player expects.
     *
     * @param color the colour to place
     * @return the cells whose holds have room and the right rating
     */
    public Set<Position> holdsAccepting(GoodColor color) {
        return components().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CargoHoldComponent hold && hold.accepts(color))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Opens the one window in which cubes may be moved.
     *
     * <p>Loading is the only moment cargo can be redistributed or thrown overboard
     * (quick reference). Outside it a ship's holds are sealed, which is what stops a
     * player shuffling their cargo the instant before a smuggler takes the most valuable
     * cube.
     */
    public void beginCargoOperations() {
        cargoOperationsOpen = true;
    }

    /**
     * Closes the window.
     */
    public void endCargoOperations() {
        cargoOperationsOpen = false;
    }

    /**
     * Tells whether cubes may be moved at the moment.
     *
     * @return {@code true} while a card is letting this ship load
     */
    public boolean cargoOperationsOpen() {
        return cargoOperationsOpen;
    }

    /**
     * Takes a cube from the bank and puts it in a hold.
     *
     * <p>Returns whether it worked rather than failing when the bank is empty. Running
     * out is a rule, not an error: a player who finds nothing left to load still pays the
     * flight days (manual p.19).
     *
     * @param hold  the hold to fill
     * @param color the colour wanted
     * @return {@code true} when a cube was actually loaded
     * @throws IllegalStateException    if no card is letting this ship load
     * @throws IllegalArgumentException if there is no hold there
     */
    public boolean load(Position hold, GoodColor color) {
        requireCargoOperationsOpen();
        CargoHoldComponent target = holdAt(hold);
        if (!target.accepts(color)) {
            throw new IllegalArgumentException(target.id() + " will not take a " + color + " cube");
        }
        if (!bank.take(color)) {
            return false;
        }
        target.store(color);
        return true;
    }

    /**
     * Throws a cube overboard, back into the bank.
     *
     * <p>Which is how it becomes available to a ship further back in the route order
     * (manual p.19) — jettisoning is a decision that affects other people.
     *
     * @param hold  the hold to empty
     * @param color the colour to discard
     * @throws IllegalStateException if no card is letting this ship load
     */
    public void jettison(Position hold, GoodColor color) {
        requireCargoOperationsOpen();
        holdAt(hold).remove(color);
        bank.giveBack(color);
    }

    /**
     * Moves a cube from one hold to another.
     *
     * @param from  the hold to take it out of
     * @param to    the hold to put it into
     * @param color the colour to move
     * @throws IllegalStateException    if no card is letting this ship load
     * @throws IllegalArgumentException if the destination will not take it
     */
    public void moveCargo(Position from, Position to, GoodColor color) {
        requireCargoOperationsOpen();
        CargoHoldComponent source = holdAt(from);
        CargoHoldComponent destination = holdAt(to);
        if (!destination.accepts(color)) {
            throw new IllegalArgumentException(destination.id() + " will not take a " + color + " cube");
        }
        source.remove(color);
        destination.store(color);
    }

    /**
     * Hands over valuables when a card demands them.
     *
     * <p>The manual's cascade (p.11): the most valuable goods first, then battery charges
     * once the holds are empty, and then nothing — a ship with neither cannot be taken
     * from. No cargo window is needed, because this is not a choice the player is making.
     *
     * @param count how much is demanded
     * @return what was actually given up
     * @throws IllegalArgumentException if the demand is negative
     */
    public Forfeit surrender(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("a card cannot demand " + count + " goods");
        }
        List<GoodColor> given = new ArrayList<>();
        int remaining = count;
        while (remaining > 0) {
            Optional<CargoHoldComponent> richest = holds()
                    .filter(hold -> hold.mostValuable().isPresent())
                    .min(java.util.Comparator.comparing(hold -> hold.mostValuable().orElseThrow()));
            if (richest.isEmpty()) {
                break;
            }
            GoodColor taken = richest.get().mostValuable().orElseThrow();
            richest.get().remove(taken);
            bank.giveBack(taken);
            given.add(taken);
            remaining--;
        }
        int charges = drawCharges(remaining);
        return new Forfeit(given, charges, remaining - charges);
    }

    /**
     * Sells everything aboard and hands the cubes back.
     *
     * <p>Journey's end: the whole manifest goes back to the bank and the player is paid
     * for it (manual p.15).
     *
     * @return what was sold, most valuable first
     */
    public List<GoodColor> sellAllCargo() {
        List<GoodColor> sold = manifest();
        holds().forEach(CargoHoldComponent::jettisonAll);
        bank.giveBackAll(sold);
        return sold;
    }

    private void requireCargoOperationsOpen() {
        if (!cargoOperationsOpen) {
            throw new IllegalStateException("cargo can only be moved while a card is letting this ship load");
        }
    }

    private CargoHoldComponent holdAt(Position cell) {
        return grid.at(cell)
                .filter(CargoHoldComponent.class::isInstance)
                .map(CargoHoldComponent.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("there is no cargo hold at " + cell));
    }

    private java.util.stream.Stream<CargoHoldComponent> holds() {
        return components().values().stream()
                .filter(CargoHoldComponent.class::isInstance)
                .map(CargoHoldComponent.class::cast);
    }

    // ---------------------------------------------------------------- crew placement

    /**
     * Returns the alien species this cabin could take right now.
     *
     * <p>Three things have to line up. The cabin must be able to host an alien at all —
     * the starting cabin never can. A life support module of that colour must be welded
     * to it, not merely sitting next door. And the ship must not already be carrying an
     * alien of that colour, since one of each is the limit (manual p.18).
     *
     * <p>A cabin joined to both colours of module offers a genuine choice between them,
     * which is the one place the rule gets interesting.
     *
     * @param cabin the cabin to ask about
     * @return the species that could move in, possibly none
     * @throws IllegalArgumentException if there is no cabin there
     */
    public Set<AlienColor> aliensAllowedIn(Position cabin) {
        CabinComponent component = cabinAt(cabin);
        if (!component.canHostAlien()) {
            return Set.of();
        }
        Set<AlienColor> aboard = aliens();
        return java.util.Arrays.stream(AlienColor.values())
                .filter(color -> !aboard.contains(color))
                .filter(color -> isLifeSupported(cabin, color))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Moves two humans into a cabin.
     *
     * <p>Always available: a human in a space suit can live anywhere, including a cabin
     * fitted out for aliens (manual p.18).
     *
     * @param cabin the cabin to fill
     * @throws IllegalArgumentException if there is no cabin there
     * @throws IllegalStateException    if it already has crew aboard
     */
    public void boardHumansIn(Position cabin) {
        cabinAt(cabin).boardHumans();
    }

    /**
     * Moves an alien into a cabin, in place of the two humans it displaces.
     *
     * @param cabin the cabin to fill
     * @param color the species moving in
     * @throws IllegalArgumentException if there is no cabin there, or that species cannot live there
     * @throws IllegalStateException    if the cabin already has crew aboard
     */
    public void boardAlienIn(Position cabin, AlienColor color) {
        CabinComponent component = cabinAt(cabin);
        if (!aliensAllowedIn(cabin).contains(color)) {
            throw new IllegalArgumentException(
                    "a " + color + " alien cannot live in " + component.id()
                            + ": " + whyNot(cabin, color));
        }
        component.boardAlien(color);
    }

    private String whyNot(Position cabin, AlienColor color) {
        if (!cabinAt(cabin).canHostAlien()) {
            return "the starting cabin never takes an alien";
        }
        if (aliens().contains(color)) {
            return "the ship already carries a " + color + " alien";
        }
        return "no " + color + " life support module is welded to it";
    }

    /**
     * Fills every empty cabin with humans.
     *
     * <p>The rest of launch preparation, once a player has decided where their aliens go.
     * Every cabin ends up occupied, because the manual leaves no room for an empty one
     * (p.18).
     *
     * @return how many cabins were filled
     */
    public int fillRemainingCabinsWithHumans() {
        List<CabinComponent> empty = cabins().filter(CabinComponent::isEmpty).toList();
        empty.forEach(CabinComponent::boardHumans);
        return empty.size();
    }

    /**
     * Tells whether every cabin has somebody in it.
     *
     * @return {@code true} when no cabin is empty
     */
    public boolean crewIsAboard() {
        return cabins().noneMatch(CabinComponent::isEmpty);
    }

    /**
     * Returns the cabins that could still take an alien.
     *
     * <p>What a client offers the player during launch preparation.
     *
     * @return the empty cabins with at least one species available, and which species
     */
    public Map<Position, Set<AlienColor>> alienBerths() {
        Map<Position, Set<AlienColor>> berths = new java.util.LinkedHashMap<>();
        components().forEach((cell, component) -> {
            if (component instanceof CabinComponent cabin && cabin.isEmpty()) {
                Set<AlienColor> allowed = aliensAllowedIn(cell);
                if (!allowed.isEmpty()) {
                    berths.put(cell, allowed);
                }
            }
        });
        return java.util.Collections.unmodifiableMap(berths);
    }

    /**
     * Returns the cells welded directly to this one.
     *
     * <p>Welded, not merely touching. Two components sitting side by side with smooth
     * sides facing each other are neighbours and are not joined, and several rules turn on
     * exactly that difference — whether a life support module keeps an alien alive,
     * whether a ship holds together, and how far an epidemic spreads.
     *
     * @param cell the cell to look around
     * @return its interconnected neighbours
     */
    public Set<Position> joinedTo(Position cell) {
        return grid.jointNeighbours(cell);
    }

    /**
     * Returns the cabins with somebody aboard.
     *
     * @return the occupied cabin cells, in placement order
     */
    public Set<Position> occupiedCabins() {
        return components().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CabinComponent cabin && !cabin.isEmpty())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    /**
     * Takes one crew member out of a cabin.
     *
     * <p>No choice is involved: a cabin holds either humans or a single alien, so which
     * one leaves is decided by who is in there. Where the player does get a choice — the
     * Slavers taking crew, for instance — the choice is which cabin, not which occupant.
     *
     * @param cabin the cabin to empty a bunk in
     * @throws IllegalArgumentException if there is no cabin there
     * @throws IllegalStateException    if the cabin is already empty
     */
    public void loseOneCrewFrom(Position cabin) {
        CabinComponent component = cabinAt(cabin);
        component.removeOne(component.alien().isPresent());
    }

    private CabinComponent cabinAt(Position cell) {
        return grid.at(cell)
                .filter(CabinComponent.class::isInstance)
                .map(CabinComponent.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("there is no cabin at " + cell));
    }

    // ---------------------------------------------------------------- attributes

    /**
     * Returns how many battery charges are left across the whole ship.
     *
     * @return the charges available to spend
     */
    public int availableCharges() {
        return batteries().mapToInt(BatteryComponent::charges).sum();
    }

    /**
     * Fills every battery compartment to its printed capacity.
     *
     * <p>Done once, during launch preparation (manual p.9). There is no recharging
     * afterwards.
     */
    public void chargeBatteries() {
        batteries().forEach(BatteryComponent::fill);
    }

    /**
     * Works out what the ship is worth if the given doubles are powered.
     *
     * <p>Pure: it spends nothing. Declaring an attribute for real also costs the
     * charges, which is {@link #spend} — keeping the two apart is what lets a client
     * show a player what each choice would buy before they commit to it.
     *
     * @param plan which doubles to run
     * @return firepower in halves, engine power, and crew
     * @throws IllegalArgumentException if the plan names a component that cannot be
     *                                  powered, or costs more charges than remain
     */
    public ShipAttributes attributes(BatteryPlan plan) {
        checkPlan(plan);
        return new ShipAttributes(firepowerHalves(plan), enginePower(plan), crewCount());
    }

    /**
     * Spends the charges a plan costs.
     *
     * <p>Charges come off whichever compartments still have them: the manual never ties
     * a battery to the component it powers, and says as much (p.7).
     *
     * @param plan the plan being carried out
     * @throws IllegalArgumentException if the plan is not one this ship can carry out
     */
    public void spend(BatteryPlan plan) {
        checkPlan(plan);
        drawCharges(plan.cost());
    }

    /**
     * Takes charges off whichever compartments still have them.
     *
     * @param wanted how many to draw
     * @return how many were actually available
     */
    private int drawCharges(int wanted) {
        int drawn = 0;
        for (BatteryComponent battery : batteries().toList()) {
            while (drawn < wanted && !battery.isEmpty()) {
                battery.spend();
                drawn++;
            }
        }
        return drawn;
    }

    /**
     * Returns firepower in halves, aliens included.
     *
     * <p>A purple alien is worth two points, but only if the cannons alone already come
     * to something: the manual is clear that it will not fight bare-tentacled (p.18).
     *
     * @param plan which double cannons are powered
     * @return firepower counted in halves
     */
    private int firepowerHalves(BatteryPlan plan) {
        int halves = components().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof CannonComponent)
                .mapToInt(entry -> ((CannonComponent) entry.getValue())
                        .firepowerHalves(plan.powers(entry.getKey())))
                .sum();
        if (halves > 0 && aliens().contains(AlienColor.PURPLE)) {
            halves += ALIEN_BONUS * HALVES_PER_POINT;
        }
        return halves;
    }

    /**
     * Returns engine power, aliens included.
     *
     * <p>A brown alien is worth two points, but only if the engines alone already come to
     * something: it will not get out and push (manual p.18).
     *
     * @param plan which double engines are powered
     * @return engine power
     */
    private int enginePower(BatteryPlan plan) {
        int power = components().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof EngineComponent)
                .mapToInt(entry -> ((EngineComponent) entry.getValue()).power(plan.powers(entry.getKey())))
                .sum();
        if (power > 0 && aliens().contains(AlienColor.BROWN)) {
            power += ALIEN_BONUS;
        }
        return power;
    }

    private void checkPlan(BatteryPlan plan) {
        for (Position cell : plan.powered()) {
            ShipComponent component = grid.at(cell).orElseThrow(() ->
                    new IllegalArgumentException("nothing to power at " + cell));
            if (!component.kind().consumesCharge()) {
                throw new IllegalArgumentException(component.id() + " at " + cell + " runs without a charge");
            }
        }
        if (plan.cost() > availableCharges()) {
            throw new IllegalArgumentException(
                    "the plan costs " + plan.cost() + " charges but only " + availableCharges() + " remain");
        }
    }

    private java.util.stream.Stream<CabinComponent> cabins() {
        return components().values().stream()
                .filter(CabinComponent.class::isInstance)
                .map(CabinComponent.class::cast);
    }

    private java.util.stream.Stream<BatteryComponent> batteries() {
        return components().values().stream()
                .filter(BatteryComponent.class::isInstance)
                .map(BatteryComponent.class::cast);
    }
}

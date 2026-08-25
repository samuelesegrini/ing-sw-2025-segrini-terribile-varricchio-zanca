package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.BatteryComponent;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.server.model.component.CannonComponent;
import it.polimi.ingsw.server.model.component.EngineComponent;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.StartingCabinTile;
import it.polimi.ingsw.server.model.component.Tile;
import it.polimi.ingsw.server.model.crew.AlienColor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Builds a ship around its starting cabin.
     *
     * @param spec          the board this ship is built on
     * @param startingCabin the cabin the player was given
     * @throws NullPointerException if either argument is {@code null}
     */
    public Ship(ShipBoardSpec spec, StartingCabinTile startingCabin) {
        this.grid = new ShipGrid(spec);
        this.validator = new ShipValidator(grid);
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
     * Tears a component off the ship.
     *
     * <p>Used to fix an illegal ship and to resolve damage. Whatever the removal strands
     * is not dealt with here — the caller decides, because the rules differ: correcting a
     * ship lets the player choose what else to sacrifice, while a hit simply drops
     * everything that comes loose.
     *
     * @param cell the cell to clear
     * @return what was removed, or empty when the cell was already free
     */
    public Optional<ShipComponent> remove(Position cell) {
        return grid.remove(cell);
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
        int remaining = plan.cost();
        for (BatteryComponent battery : batteries().toList()) {
            while (remaining > 0 && !battery.isEmpty()) {
                battery.spend();
                remaining--;
            }
        }
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

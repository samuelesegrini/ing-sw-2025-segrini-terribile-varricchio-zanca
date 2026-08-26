package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.ShipAttribute;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.Optional;

/**
 * The fight every enemy card puts up, minus what it wants and what it pays.
 *
 * <p>Smugglers, Slavers and Pirates differ only in their reward and their penalty. The
 * fight itself is one procedure (manual p.12): the enemy works down the route, each
 * player declares firepower, and three things can happen.
 *
 * <ul>
 *   <li><b>Beaten</b> — the player may take the reward at the cost of flight days, or
 *       decline and keep both. Either way the enemy leaves and <em>nobody behind is
 *       attacked</em>.</li>
 *   <li><b>Matched</b> — nothing happens to this player, and the enemy is not beaten. It
 *       moves on. This is the outcome people forget: a draw is not a win.</li>
 *   <li><b>Beaten by</b> — the player takes the penalty, and the enemy moves on.</li>
 * </ul>
 *
 * <p>Firepower is compared in halves, so a ship on 5½ against an enemy of 5 wins and one
 * on 5 draws. Rounding either way here would decide fights the manual says go the other
 * way.
 */
abstract class EnemyResolution extends TurnByTurnResolution {

    /** Firepower is carried in halves, so an enemy's printed strength doubles. */
    private static final int HALVES_PER_POINT = 2;

    private final Flight flight;
    private final int enemyFirepowerHalves;
    private final int flightDays;

    /**
     * Sets up a fight down the route.
     *
     * @param flight         the flight the enemy turned up in
     * @param enemyFirepower the strength printed beside the enemy's cannon
     * @param flightDays     what claiming the reward costs
     */
    EnemyResolution(Flight flight, int enemyFirepower, int flightDays) {
        super(flight.stillFlying());
        this.flight = flight;
        this.enemyFirepowerHalves = enemyFirepower * HALVES_PER_POINT;
        this.flightDays = flightDays;
    }

    /**
     * Returns the flight this fight is happening in.
     *
     * @return the flight
     */
    protected final Flight flight() {
        return flight;
    }

    /**
     * Describes what beating this enemy is worth, for the offer put to the winner.
     *
     * @return a phrase in the language of the card
     */
    protected abstract String rewardDescription();

    /**
     * Hands the winner what they came for.
     *
     * <p>May ask a follow-up question — the Smugglers' goods have to be stowed — in which
     * case the flight days are paid when that is finished rather than here.
     *
     * @param player the winner
     * @return {@code true} when the reward is settled, {@code false} when a follow-up is
     *         still outstanding
     */
    protected abstract boolean claimReward(PlayerColor player);

    /**
     * Makes a loser pay.
     *
     * <p>May ask a follow-up question — the Slavers let a player choose which crew go — and
     * may do nothing at all now, as the Pirates do when they are waiting to fire on
     * everybody at once.
     *
     * @param player the loser
     */
    protected abstract void punish(PlayerColor player);

    /**
     * Handles an answer none of the shared steps recognises.
     *
     * @param choice the answer
     * @return {@code true} to end the card here
     */
    protected boolean applyExtra(PlayerChoice choice) {
        throw new IllegalArgumentException("this enemy was not expecting " + choice);
    }

    /**
     * Pays the flight days a reward costs.
     *
     * <p>Called by a subclass once its reward is settled.
     *
     * @param player the winner
     */
    protected final void payForReward(PlayerColor player) {
        flight.route().fallBack(player, flightDays);
    }

    /**
     * Asks a player to declare firepower.
     *
     * <p>Overridable because a card may have a second phase with questions of its own —
     * the Pirates fire on everybody they beat once the fight is over.
     *
     * @param player whose turn it is
     * @return the call to declare
     */
    @Override
    protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
        Ship ship = flight.shipOf(player);
        return Optional.of(new PlayerPrompt.DeclarePower(
                player, ShipAttribute.FIREPOWER,
                ship.componentsOfKind(ComponentKind.DOUBLE_CANNON), ship.availableCharges()));
    }

    /**
     * Applies an answer to the fight.
     *
     * <p>Overridable for the same reason as {@link #promptFor}; a subclass with a second
     * phase should delegate here while the fight is still going on.
     *
     * @param choice the answer
     * @return {@code true} to end the card here
     */
    @Override
    protected boolean apply(PlayerChoice choice) {
        return switch (choice) {
            case PlayerChoice.Declaration declaration -> fight(declaration);
            case PlayerChoice.Take take -> claimReward(take.player());
            case PlayerChoice.Leave ignored ->
                // Declining costs nothing at all: no reward, and no flight days either.
                    true;
            default -> applyExtra(choice);
        };
    }

    private boolean fight(PlayerChoice.Declaration declaration) {
        PlayerColor player = declaration.player();
        Ship ship = flight.shipOf(player);

        int halves = ship.attributes(declaration.plan()).firepowerHalves();
        ship.spend(declaration.plan());

        if (halves > enemyFirepowerHalves) {
            askAgain(new PlayerPrompt.TakeOrLeave(player, rewardDescription(), flightDays));
            // Nobody behind is attacked, but the winner still has an offer to answer.
            return true;
        }
        if (halves < enemyFirepowerHalves) {
            punish(player);
        }
        // A draw leaves the player untouched and the enemy undefeated, so it carries on.
        return false;
    }
}

package it.polimi.ingsw.common.game;

/**
 * What happens when the game asks somebody who is not there.
 *
 * <p>A dropped connection must not stop three other people playing, so the server answers on
 * behalf of whoever is away. That makes this a rule of the game rather than a convenience: the
 * answer has to be the same every time, has to be legal, and has to be one nobody would call
 * unfair on the absent player's behalf.
 *
 * <p>So it is always the passive answer — the one that takes nothing, spends nothing and risks
 * nothing:
 *
 * <ul>
 *   <li><b>An offer</b> is declined. Taking salvage costs flight days, and spending somebody
 *       else's days while they are away is not a decision to make for them.</li>
 *   <li><b>A declaration</b> is made as the ship stands, with no batteries spent. This is the
 *       Open Space case too: an absent player's engines are whatever they built, and burning
 *       their last charge to move further is a choice, not a default.</li>
 *   <li><b>Goods</b> are left on the ground. Loading a cube means choosing a hold.</li>
 *   <li><b>Crew</b> is given up from the first cabins that have anybody in them, because this
 *       is the one question with no passive answer — somebody is going whatever happens.</li>
 *   <li><b>An incoming shot</b> is taken rather than blocked, because blocking costs a
 *       battery.</li>
 *   <li><b>A broken ship</b> keeps its largest piece, which is the least destructive reading
 *       of a question that has no free answer.</li>
 *   <li><b>A planet</b> is flown past. This is the one place where the passive answer differs
 *       from the obvious one: landing pays goods, but it also costs flight days and takes a
 *       planet somebody present might want.</li>
 * </ul>
 *
 * <p>The switch is exhaustive over {@link PlayerPrompt}, so a new kind of question cannot be
 * added without somebody deciding what happens when nobody answers it.
 */
public final class SkippedTurn {

    private SkippedTurn() {
    }

    /**
     * Returns the answer the server gives on behalf of an absent player.
     *
     * @param prompt what is being asked
     * @return an answer that takes nothing, spends nothing and risks nothing
     */
    public static PlayerChoice answerFor(PlayerPrompt prompt) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave offer -> new PlayerChoice.Leave(offer.player());
            case PlayerPrompt.DeclarePower declare ->
                    new PlayerChoice.Declaration(declare.player(), BatteryPlan.none());
            case PlayerPrompt.ArrangeCargo cargo -> new PlayerChoice.Done(cargo.player());
            case PlayerPrompt.GiveUpCrew crew -> new PlayerChoice.CrewGiven(crew.player(),
                    crew.cabins().stream().limit(crew.count()).toList());
            case PlayerPrompt.ChooseDefence defence ->
                    PlayerChoice.DefenceChosen.none(defence.player());
            case PlayerPrompt.ChooseFragment fragment ->
                    new PlayerChoice.FragmentKept(fragment.player(), largestOf(fragment));
            // Fly on. Landing pays goods, but it costs flight days and takes a planet somebody
            // who is actually here might want.
            case PlayerPrompt.ChoosePlanet planet -> new PlayerChoice.Leave(planet.player());
        };
    }

    /**
     * Keeps as much of a broken ship as possible.
     *
     * <p>There is no free answer to this one — every piece but the chosen one is lost — so the
     * least destructive reading is the biggest.
     */
    private static java.util.Set<Position> largestOf(PlayerPrompt.ChooseFragment fragment) {
        return fragment.pieces().stream()
                .max(java.util.Comparator.comparingInt(java.util.Set::size))
                .orElseThrow(() -> new IllegalStateException(
                        "a ship in no pieces at all cannot be asked which to keep"));
    }
}

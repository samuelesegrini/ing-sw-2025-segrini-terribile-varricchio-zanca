package it.polimi.ingsw.common.game;

/**
 * The least interesting answer to any question a card can ask.
 *
 * <p>Take nothing, power nothing, keep the first piece. For tests whose point is to reach the
 * end of a flight rather than to play one well — what the cards do when answered properly was
 * settled card by card in M3, and a test that also tried to play cleverly would be testing two
 * things and telling you neither.
 *
 * <p>The switch is exhaustive over {@link PlayerPrompt}, so a new kind of question cannot be
 * added without somebody deciding what the boring answer to it is.
 */
public final class Answers {

    private Answers() {
    }

    /**
     * Returns the simplest legal answer to a question.
     *
     * @param prompt what is being asked
     * @return an answer that does as little as possible
     */
    public static PlayerChoice simplestTo(PlayerPrompt prompt) {
        return switch (prompt) {
            case PlayerPrompt.TakeOrLeave leave -> new PlayerChoice.Leave(leave.player());
            case PlayerPrompt.DeclarePower declare ->
                    new PlayerChoice.Declaration(declare.player(), BatteryPlan.none());
            case PlayerPrompt.ArrangeCargo cargo -> new PlayerChoice.Done(cargo.player());
            case PlayerPrompt.GiveUpCrew crew -> new PlayerChoice.CrewGiven(crew.player(),
                    crew.cabins().stream().limit(crew.count()).toList());
            case PlayerPrompt.ChooseDefence defence ->
                    PlayerChoice.DefenceChosen.none(defence.player());
            case PlayerPrompt.ChooseFragment fragment ->
                    new PlayerChoice.FragmentKept(fragment.player(), fragment.pieces().get(0));
            case PlayerPrompt.ChoosePlanet planet ->
                    new PlayerChoice.PlanetChosen(planet.player(),
                            planet.planets().keySet().iterator().next());
        };
    }
}

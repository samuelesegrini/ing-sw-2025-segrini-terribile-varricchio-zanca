package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.view.ShipView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a question offers exactly the answers it allows, and no others.
 *
 * <p>A button the server would refuse is worse than a missing one: it teaches a player a rule
 * the game does not have, and they find out by being told no in the middle of a fight. Every
 * prompt already carries its own options, so this is about not inventing any.
 *
 * <p>No screen: it is a function from a question to a list of things to press.
 */
class PromptChoicesTest {

    private static final PlayerColor ME = PlayerColor.RED;
    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);

    private static final ShipView SHIP = new ShipView(5, 7, 5, 4,
            Set.of(CABIN, HOLD), Map.of(), List.of(), List.of(), 0,
            new ShipAttributes(0, 0, 0), ValidationReport.legal());

    private static List<String> labels(PlayerPrompt prompt) {
        return PromptChoices.of(prompt, SHIP, ME).stream()
                .map(PromptChoices.Option::label)
                .toList();
    }

    @Test
    @DisplayName("an offer is taken or left, and nothing else")
    void anOffer() {
        List<String> buttons = labels(new PlayerPrompt.TakeOrLeave(ME, "4 credits", 1));

        assertEquals(List.of("Take it", "Leave it"), buttons);
    }

    @Test
    @DisplayName("a shot nothing can stop offers only taking it")
    void anUnstoppableShot() {
        PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(ME,
                new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7), CABIN, Set.of());

        assertEquals(List.of("Take the hit"), labels(prompt),
                "offering a shield that cannot help teaches a rule the game does not have");
    }

    @Test
    @DisplayName("a shot something can stop offers that thing, named by its printed square")
    void aStoppableShot() {
        PlayerPrompt prompt = new PlayerPrompt.ChooseDefence(ME,
                new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 7), CABIN, Set.of(HOLD));

        List<String> buttons = labels(prompt);

        assertEquals(2, buttons.size());
        assertTrue(buttons.contains("Stop it with 7,8"), buttons.toString());
    }

    @Test
    @DisplayName("declaring offers going as you are, plus each thing a charge would power")
    void declaring() {
        PlayerPrompt prompt = new PlayerPrompt.DeclarePower(
                ME, ShipAttribute.FIREPOWER, Set.of(HOLD, CABIN), 2);

        List<String> buttons = labels(prompt);

        assertEquals("Declare as you are", buttons.get(0));
        assertTrue(buttons.contains("Power 7,7"));
        assertTrue(buttons.contains("Power 7,8"));
    }

    @Test
    @DisplayName("crew is counted rather than declared, so there is one button and it says so")
    void declaringCrew() {
        PlayerPrompt prompt = new PlayerPrompt.DeclarePower(ME, ShipAttribute.CREW, Set.of(), 2);

        assertEquals(List.of("Go on"), labels(prompt));
        assertTrue(PromptChoices.question(prompt, SHIP).contains("counted, not declared"));
    }

    @Test
    @DisplayName("only the planets still free are offered, and flying on is always one of them")
    void planets() {
        PlayerPrompt prompt = new PlayerPrompt.ChoosePlanet(ME,
                Map.of(1, Map.of(GoodColor.BLUE, 2), 3, Map.of(GoodColor.RED, 1)), 1);

        List<String> buttons = labels(prompt);

        assertEquals(3, buttons.size(), buttons.toString());
        assertTrue(buttons.stream().anyMatch(text -> text.startsWith("Land on 1")));
        assertTrue(buttons.stream().anyMatch(text -> text.startsWith("Land on 3")));
        assertFalse(buttons.stream().anyMatch(text -> text.startsWith("Land on 2")),
                "planet two has somebody on it and is not in the offer");
        assertTrue(buttons.contains("Fly on"));
    }

    @Test
    @DisplayName("the buttons do not move about between one question and the next")
    void aStableOrder() {
        // The prompts carry sets, and a set has no order. Without sorting, the same question
        // asked twice could put 'Power 7,8' where 'Power 7,7' was a moment ago — which is how
        // somebody spends a battery they meant to keep.
        PlayerPrompt prompt = new PlayerPrompt.DeclarePower(
                ME, ShipAttribute.FIREPOWER, Set.of(HOLD, CABIN, new Position(1, 2)), 3);

        assertEquals(labels(prompt), labels(prompt));
        assertEquals(List.of("Declare as you are", "Power 6,6", "Power 7,7", "Power 7,8"),
                labels(prompt), "along a row, then down, which is how a player reads a board");
    }

    @Test
    @DisplayName("every kind of question offers something to press")
    void nothingIsADeadEnd() {
        List<PlayerPrompt> everything = List.of(
                new PlayerPrompt.TakeOrLeave(ME, "salvage", 1),
                new PlayerPrompt.DeclarePower(ME, ShipAttribute.ENGINE_POWER, Set.of(HOLD), 1),
                new PlayerPrompt.ArrangeCargo(ME, Map.of(GoodColor.BLUE, 1), Set.of(HOLD)),
                new PlayerPrompt.GiveUpCrew(ME, 1, Set.of(CABIN)),
                new PlayerPrompt.ChooseDefence(ME,
                        new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 7), CABIN, Set.of(HOLD)),
                new PlayerPrompt.ChooseFragment(ME, List.of(Set.of(CABIN), Set.of(HOLD))),
                new PlayerPrompt.ChoosePlanet(ME, Map.of(1, Map.of()), 1));

        for (PlayerPrompt prompt : everything) {
            assertFalse(PromptChoices.of(prompt, SHIP, ME).isEmpty(),
                    prompt.getClass().getSimpleName() + " offers nothing to press");
            assertNotNull(PromptChoices.question(prompt, SHIP));
            assertFalse(PromptChoices.question(prompt, SHIP).isBlank(),
                    prompt.getClass().getSimpleName() + " does not say what it is asking");
        }
    }

    @Test
    @DisplayName("stowing offers a hold for each cube on offer, and a way to stop")
    void stowing() {
        PlayerPrompt prompt = new PlayerPrompt.ArrangeCargo(
                ME, Map.of(GoodColor.BLUE, 2), Set.of(HOLD));

        List<String> buttons = labels(prompt);

        assertEquals("Finished stowing", buttons.get(0));
        assertTrue(buttons.contains("Put blue in 7,8"), buttons.toString());
    }

    @Test
    @DisplayName("a piece of a broken ship is offered by how much of the ship it is")
    void pieces() {
        PlayerPrompt prompt = new PlayerPrompt.ChooseFragment(
                ME, List.of(Set.of(CABIN), Set.of(HOLD, new Position(1, 2))));

        List<String> buttons = labels(prompt);

        assertTrue(buttons.contains("Keep the piece with 1 component"), buttons.toString());
        assertTrue(buttons.contains("Keep the piece with 2 components"), buttons.toString());
    }

    @Test
    @DisplayName("a button that answers outright says so, and one that points at a square does not")
    void whatAButtonDoes() {
        PromptChoices.Option answering = PromptChoices.of(
                new PlayerPrompt.TakeOrLeave(ME, "salvage", 1), SHIP, ME).get(0);
        PromptChoices.Option pointing = PromptChoices.of(
                new PlayerPrompt.GiveUpCrew(ME, 1, Set.of(CABIN)), SHIP, ME).get(0);

        assertTrue(answering.answersOutright());
        assertEquals(new PlayerChoice.Take(ME), answering.choice());
        assertFalse(pointing.answersOutright(), "giving up crew takes as many as it takes");
        assertEquals(CABIN, pointing.cell());
    }
}

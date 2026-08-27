package it.polimi.ingsw.common.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a question knows about its own answers.
 *
 * <p>Three facts about every prompt used to live somewhere other than the prompt: the answer
 * given when nobody is there to give one lived in a switch in {@code SkippedTurn}, the words
 * that announced it lived in a second switch in {@code Game}, and which answers the question
 * even admits was written out once per card in eight {@code default} arms.
 *
 * <p>Nothing held those three in agreement. This test is what replaces that: the passive
 * answer to a question has to be an answer that question accepts, checked for every variant,
 * so a prompt added later cannot ship with a default its own card would throw out.
 */
class PlayerPromptTest {

    private static final PlayerColor WHO = PlayerColor.RED;
    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);

    /** One of every variant, so that adding a prompt without listing it fails the count. */
    private static List<PlayerPrompt> everyKind() {
        return List.of(
                new PlayerPrompt.TakeOrLeave(WHO, "4 credits for the salvage", 1),
                new PlayerPrompt.DeclarePower(WHO, ShipAttribute.FIREPOWER, Set.of(HOLD), 2),
                new PlayerPrompt.ArrangeCargo(WHO, Map.of(GoodColor.RED, 1), Set.of(HOLD)),
                new PlayerPrompt.GiveUpCrew(WHO, 1, Set.of(CABIN)),
                new PlayerPrompt.ChooseDefence(WHO, new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7),
                        CABIN, Set.of(HOLD)),
                new PlayerPrompt.ChooseFragment(WHO, List.of(Set.of(CABIN), Set.of(HOLD))),
                new PlayerPrompt.ChoosePlanet(WHO, Map.of(1, Map.of(GoodColor.GREEN, 2)), 2));
    }

    @Nested
    @DisplayName("the answer given when nobody is there")
    class PassiveAnswers {

        @ParameterizedTest
        @MethodSource("it.polimi.ingsw.common.game.PlayerPromptTest#everyKind")
        @DisplayName("is an answer the question itself admits")
        void passiveAnswerIsAccepted(PlayerPrompt prompt) {
            // The invariant that had nowhere to live while these two facts were in different
            // files. A prompt whose default answer its own card would refuse would freeze the
            // table it was supposed to unfreeze — which is the whole reason the default exists.
            assertTrue(prompt.accepts(prompt.passiveAnswer()),
                    prompt.getClass().getSimpleName() + " gives a default answer it refuses");
        }

        @ParameterizedTest
        @MethodSource("it.polimi.ingsw.common.game.PlayerPromptTest#everyKind")
        @DisplayName("is addressed by the player who was asked")
        void passiveAnswerIsSignedByTheRightPlayer(PlayerPrompt prompt) {
            assertEquals(prompt.player(), prompt.passiveAnswer().player());
        }

        @ParameterizedTest
        @MethodSource("it.polimi.ingsw.common.game.PlayerPromptTest#everyKind")
        @DisplayName("is announced in words somebody can read")
        void passingIsDescribed(PlayerPrompt prompt) {
            assertFalse(prompt.describePassing().isBlank());
        }

        @Test
        @DisplayName("takes nothing on offer, rather than accepting on an absent player's behalf")
        void offersAreDeclined() {
            PlayerPrompt offer = new PlayerPrompt.TakeOrLeave(WHO, "a salvage claim", 3);

            // Taking costs flight days. Spending somebody else's days while they are away is
            // not a decision to make for them (manual p.12).
            assertInstanceOf(PlayerChoice.Leave.class, offer.passiveAnswer());
        }

        @Test
        @DisplayName("spends no batteries, because a charge is a choice")
        void declarationsSpendNothing() {
            PlayerPrompt declare =
                    new PlayerPrompt.DeclarePower(WHO, ShipAttribute.FIREPOWER, Set.of(HOLD), 2);

            PlayerChoice.Declaration passive = (PlayerChoice.Declaration) declare.passiveAnswer();
            assertEquals(BatteryPlan.none(), passive.plan());
        }

        @Test
        @DisplayName("gives up exactly the crew demanded, no more")
        void crewGivenMatchesWhatWasAsked() {
            PlayerPrompt crew = new PlayerPrompt.GiveUpCrew(WHO, 1, Set.of(CABIN, HOLD));

            PlayerChoice.CrewGiven given = (PlayerChoice.CrewGiven) crew.passiveAnswer();
            assertEquals(1, given.cabins().size());
        }

        @Test
        @DisplayName("keeps the largest piece of a broken ship, the least destructive reading")
        void theBiggestFragmentIsKept() {
            Position third = new Position(3, 3);
            PlayerPrompt broken = new PlayerPrompt.ChooseFragment(WHO,
                    List.of(Set.of(CABIN), Set.of(HOLD, third)));

            PlayerChoice.FragmentKept kept = (PlayerChoice.FragmentKept) broken.passiveAnswer();
            assertEquals(Set.of(HOLD, third), kept.piece());
        }
    }

    @Nested
    @DisplayName("which answers a question admits")
    class Accepting {

        @Test
        @DisplayName("an offer takes yes or no, and nothing else")
        void anOfferTakesYesOrNo() {
            PlayerPrompt offer = new PlayerPrompt.TakeOrLeave(WHO, "a salvage claim", 3);

            assertTrue(offer.accepts(new PlayerChoice.Take(WHO)));
            assertTrue(offer.accepts(new PlayerChoice.Leave(WHO)));
            assertFalse(offer.accepts(new PlayerChoice.Done(WHO)));
        }

        @Test
        @DisplayName("stowing takes a move or a full stop, because it is several answers in a row")
        void stowingTakesAMoveOrAStop() {
            PlayerPrompt cargo = new PlayerPrompt.ArrangeCargo(WHO, Map.of(GoodColor.RED, 1),
                    Set.of(HOLD));

            assertTrue(cargo.accepts(new PlayerChoice.Done(WHO)));
            assertFalse(cargo.accepts(new PlayerChoice.Take(WHO)));
        }

        @Test
        @DisplayName("a planet call takes a landing or a refusal, but never a bare acceptance")
        void aPlanetCallTakesALandingOrARefusal() {
            PlayerPrompt planets =
                    new PlayerPrompt.ChoosePlanet(WHO, Map.of(1, Map.of(GoodColor.GREEN, 2)), 2);

            assertTrue(planets.accepts(new PlayerChoice.PlanetChosen(WHO, 1)));
            assertTrue(planets.accepts(new PlayerChoice.Leave(WHO)));
            // Take is what answers an offer. A planets card has no case for it and would
            // previously have thrown from inside the card rather than being refused here.
            assertFalse(planets.accepts(new PlayerChoice.Take(WHO)));
        }
    }
}

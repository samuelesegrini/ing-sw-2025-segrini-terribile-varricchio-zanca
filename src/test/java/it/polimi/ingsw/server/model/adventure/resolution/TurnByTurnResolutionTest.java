package it.polimi.ingsw.server.model.adventure.resolution;

import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the skeleton the adventure cards share, without any card's rules in the way.
 *
 * <p>Three shapes from the manual come out of the same loop, and the only thing that
 * separates them is whether {@link TurnByTurnResolution#apply} says to stop: everyone
 * acts (Planets, Open Space), the first taker ends it (an abandoned ship), or the first
 * winner ends it (an enemy). Testing that here means each card can then be tested on its
 * own rules rather than on the walk.
 *
 * <p>The other thing worth pinning is that a card cannot be answered by the wrong person,
 * or answered twice. A resolution that trusted its input would let a client hand a
 * combat zone the wrong player's declaration and never notice.
 *
 * <p>Components involved: {@link TurnByTurnResolution}, {@link PlayerPrompt},
 * {@link PlayerChoice}.
 */
class TurnByTurnResolutionTest {

    private static final List<PlayerColor> ORDER =
            List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN);

    /**
     * A card that records who answered what, and stops when told to.
     *
     * <p>Stands in for a real card so that the walk can be tested without any rules.
     */
    private static final class Walk extends TurnByTurnResolution {

        private final Set<PlayerColor> skipped;
        private final Set<PlayerColor> stopsOnAcceptanceBy;
        private final List<String> log = new ArrayList<>();
        private int finishedCount;

        Walk(List<PlayerColor> order, Set<PlayerColor> skipped, Set<PlayerColor> stopsOnAcceptanceBy) {
            super(order);
            this.skipped = skipped;
            this.stopsOnAcceptanceBy = stopsOnAcceptanceBy;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            if (skipped.contains(player)) {
                return Optional.empty();
            }
            return Optional.of(new PlayerPrompt.TakeOrLeave(player, "a salvage claim", 2));
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            boolean took = choice instanceof PlayerChoice.Take;
            log.add(choice.player() + (took ? " took" : " left"));
            return took && stopsOnAcceptanceBy.contains(choice.player());
        }

        @Override
        protected void afterEveryone() {
            finishedCount++;
        }
    }

    private static Walk everyoneActs() {
        return new Walk(ORDER, Set.of(), Set.of());
    }

    private static Walk firstTakerEndsIt() {
        return new Walk(ORDER, Set.of(), Set.copyOf(ORDER));
    }

    private static PlayerColor asked(TurnByTurnResolution resolution) {
        return resolution.pending().orElseThrow().player();
    }

    @Nested
    @DisplayName("walking the queue")
    class WalkingTheQueue {

        @Test
        @DisplayName("players are asked in the order given, leader first")
        void playersAreAskedInOrder() {
            Walk walk = everyoneActs();

            assertEquals(PlayerColor.RED, asked(walk));
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));
            assertEquals(PlayerColor.BLUE, asked(walk));
            walk.submit(new PlayerChoice.Leave(PlayerColor.BLUE));
            assertEquals(PlayerColor.GREEN, asked(walk));
        }

        @Test
        @DisplayName("a card with nobody to ask is finished before it starts")
        void aCardWithNobodyToAskIsFinishedAtOnce() {
            Walk walk = new Walk(ORDER, Set.copyOf(ORDER), Set.of());

            assertTrue(walk.isComplete());
            assertEquals(1, walk.finishedCount);
        }

        @Test
        @DisplayName("a player with nothing to answer is passed over rather than refused")
        void aPlayerWithNothingToAnswerIsPassedOver() {
            Walk walk = new Walk(ORDER, Set.of(PlayerColor.BLUE), Set.of());

            assertEquals(PlayerColor.RED, asked(walk));
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertEquals(PlayerColor.GREEN, asked(walk), "blue had nothing to answer");
        }

        @Test
        @DisplayName("the card is finished once everybody has answered")
        void theCardFinishesWhenEverybodyHasAnswered() {
            Walk walk = everyoneActs();
            ORDER.forEach(player -> walk.submit(new PlayerChoice.Leave(player)));

            assertTrue(walk.isComplete());
            assertEquals(List.of("RED left", "BLUE left", "GREEN left"), walk.log);
        }
    }

    @Nested
    @DisplayName("stopping early")
    class StoppingEarly {

        @Test
        @DisplayName("the first taker ends the card, and nobody behind is asked")
        void theFirstTakerEndsTheCard() {
            Walk walk = firstTakerEndsIt();
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));
            walk.submit(new PlayerChoice.Take(PlayerColor.BLUE));

            assertTrue(walk.isComplete());
            assertEquals(List.of("RED left", "BLUE took"), walk.log, "green was never asked");
        }

        @Test
        @DisplayName("declining does not end the card, so the offer passes down the route")
        void decliningPassesTheOfferOn() {
            Walk walk = firstTakerEndsIt();
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertFalse(walk.isComplete());
            assertEquals(PlayerColor.BLUE, asked(walk));
        }

        @Test
        @DisplayName("the last player taking it ends the card the same way")
        void theLastPlayerTakingItEndsItToo() {
            Walk walk = firstTakerEndsIt();
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));
            walk.submit(new PlayerChoice.Leave(PlayerColor.BLUE));
            walk.submit(new PlayerChoice.Take(PlayerColor.GREEN));

            assertTrue(walk.isComplete());
        }
    }

    @Nested
    @DisplayName("what happens once")
    class WhatHappensOnce {

        @Test
        @DisplayName("the closing step runs exactly once, however the card ended")
        void theClosingStepRunsExactlyOnce() {
            Walk everyone = everyoneActs();
            ORDER.forEach(player -> everyone.submit(new PlayerChoice.Leave(player)));
            everyone.pending();
            everyone.pending();

            assertEquals(1, everyone.finishedCount);

            Walk stopped = firstTakerEndsIt();
            stopped.submit(new PlayerChoice.Take(PlayerColor.RED));
            stopped.pending();

            assertEquals(1, stopped.finishedCount);
        }
    }

    @Nested
    @DisplayName("refusing what it did not ask for")
    class RefusingBadInput {

        @Test
        @DisplayName("an answer from the wrong player is refused, not applied to whoever was asked")
        void anAnswerFromTheWrongPlayerIsRefused() {
            Walk walk = everyoneActs();

            assertThrows(IllegalArgumentException.class,
                    () -> walk.submit(new PlayerChoice.Take(PlayerColor.GREEN)));
            assertEquals(PlayerColor.RED, asked(walk), "the card is still waiting on red");
            assertEquals(List.of(), walk.log);
        }

        @Test
        @DisplayName("answering a finished card is refused rather than silently ignored")
        void answeringAFinishedCardIsRefused() {
            Walk walk = everyoneActs();
            ORDER.forEach(player -> walk.submit(new PlayerChoice.Leave(player)));

            assertThrows(IllegalStateException.class,
                    () -> walk.submit(new PlayerChoice.Leave(PlayerColor.RED)));
        }

        @Test
        @DisplayName("answering twice is refused, because the second answer belongs to the next player")
        void answeringTwiceIsRefused() {
            Walk walk = everyoneActs();
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertThrows(IllegalArgumentException.class,
                    () -> walk.submit(new PlayerChoice.Leave(PlayerColor.RED)));
        }

        @Test
        @DisplayName("an answer of the wrong kind is refused here, before the card sees it")
        void anAnswerOfTheWrongKindIsRefused() {
            Walk walk = everyoneActs();

            // An offer takes yes or no. Done answers a cargo call, and this card never makes
            // one — which every card used to establish for itself in the default arm of its
            // own switch, eight times over.
            assertThrows(IllegalArgumentException.class,
                    () -> walk.submit(new PlayerChoice.Done(PlayerColor.RED)));
            assertEquals(List.of(), walk.log, "the card was never handed it");
        }

        @Test
        @DisplayName("and the refusal names both the question and the answer")
        void theRefusalNamesBoth() {
            Walk walk = everyoneActs();

            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> walk.submit(new PlayerChoice.Done(PlayerColor.RED)));

            assertTrue(refused.getMessage().contains("TakeOrLeave"));
            assertTrue(refused.getMessage().contains("Done"));
        }

        @Test
        @DisplayName("a refused answer leaves the question standing, so the queue does not walk past")
        void aRefusedAnswerLeavesTheQuestionStanding() {
            Walk walk = everyoneActs();

            assertThrows(IllegalArgumentException.class,
                    () -> walk.submit(new PlayerChoice.Done(PlayerColor.RED)));

            assertEquals(PlayerColor.RED, asked(walk), "the card is still waiting on red");
            walk.submit(new PlayerChoice.Leave(PlayerColor.RED));
            assertEquals(PlayerColor.BLUE, asked(walk));
        }
    }

    @Nested
    @DisplayName("the offer itself")
    class TheOffer {

        @Test
        @DisplayName("an offer names its player, what is on the table and what it costs")
        void anOfferCarriesWhatAClientNeeds() {
            PlayerPrompt.TakeOrLeave offer =
                    new PlayerPrompt.TakeOrLeave(PlayerColor.RED, "a salvage claim", 3);

            assertEquals(PlayerColor.RED, offer.player());
            assertEquals("a salvage claim", offer.description());
            assertEquals(3, offer.flightDays());
        }

        @Test
        @DisplayName("an offer that pays flight days is a programming error, not a bargain")
        void anOfferCannotPayFlightDays() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PlayerPrompt.TakeOrLeave(PlayerColor.RED, "a bargain", -1));
        }
    }
}

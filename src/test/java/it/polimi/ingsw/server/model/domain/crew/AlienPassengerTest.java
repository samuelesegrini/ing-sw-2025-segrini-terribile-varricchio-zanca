package it.polimi.ingsw.server.model.domain.crew;

import it.polimi.ingsw.server.model.enums.crew.AlienColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AlienPassenger with >95% coverage.
 * Tests all methods, constructors, edge cases, and Galaxy Trucker game rules.
 */
class AlienPassengerTest {

    private AlienPassenger purpleAlien;
    private AlienPassenger brownAlien;

    @BeforeEach
    void setUp() {
        purpleAlien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
        brownAlien = new AlienPassenger(AlienColor.ALIEN_BROWN, "Klick");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create purple alien with correct properties")
        void shouldCreatePurpleAlienCorrectly() {
            AlienPassenger alien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "TestAlien");

            assertEquals(AlienColor.ALIEN_PURPLE, alien.getColor());
            assertEquals("TestAlien", alien.getName());
            assertTrue(alien.isActive(), "New alien should be active by default");
        }

        @Test
        @DisplayName("Should create brown alien with correct properties")
        void shouldCreateBrownAlienCorrectly() {
            AlienPassenger alien = new AlienPassenger(AlienColor.ALIEN_BROWN, "TestBrown");

            assertEquals(AlienColor.ALIEN_BROWN, alien.getColor());
            assertEquals("TestBrown", alien.getName());
            assertTrue(alien.isActive(), "New alien should be active by default");
        }

        @Test
        @DisplayName("Should create alien with empty name")
        void shouldCreateAlienWithEmptyName() {
            AlienPassenger alien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "");

            assertEquals("", alien.getName());
            assertEquals(AlienColor.ALIEN_PURPLE, alien.getColor());
            assertTrue(alien.isActive());
        }

        @Test
        @DisplayName("Should create alien with null name")
        void shouldCreateAlienWithNullName() {
            AlienPassenger alien = new AlienPassenger(AlienColor.ALIEN_BROWN, null);

            assertNull(alien.getName());
            assertEquals(AlienColor.ALIEN_BROWN, alien.getColor());
            assertTrue(alien.isActive());
        }

        @Test
        @DisplayName("Should create alien with very long name")
        void shouldCreateAlienWithLongName() {
            String longName = "A".repeat(1000);
            AlienPassenger alien = new AlienPassenger(AlienColor.ALIEN_PURPLE, longName);

            assertEquals(longName, alien.getName());
            assertEquals(AlienColor.ALIEN_PURPLE, alien.getColor());
            assertTrue(alien.isActive());
        }
    }

    @Nested
    @DisplayName("Basic Getter Tests")
    class BasicGetterTests {

        @Test
        @DisplayName("Should return correct color for purple alien")
        void shouldReturnCorrectColorForPurple() {
            assertEquals(AlienColor.ALIEN_PURPLE, purpleAlien.getColor());
        }

        @Test
        @DisplayName("Should return correct color for brown alien")
        void shouldReturnCorrectColorForBrown() {
            assertEquals(AlienColor.ALIEN_BROWN, brownAlien.getColor());
        }

        @Test
        @DisplayName("Should return correct name for purple alien")
        void shouldReturnCorrectNameForPurple() {
            assertEquals("Zorg", purpleAlien.getName());
        }

        @Test
        @DisplayName("Should return correct name for brown alien")
        void shouldReturnCorrectNameForBrown() {
            assertEquals("Klick", brownAlien.getName());
        }

        @Test
        @DisplayName("Should return active status correctly")
        void shouldReturnActiveStatusCorrectly() {
            assertTrue(purpleAlien.isActive());
            assertTrue(brownAlien.isActive());
        }
    }

    @Nested
    @DisplayName("Active Status Management Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should set alien to inactive")
        void shouldSetAlienToInactive() {
            purpleAlien.setActive(false);

            assertFalse(purpleAlien.isActive());
            assertEquals(AlienColor.ALIEN_PURPLE, purpleAlien.getColor());
            assertEquals("Zorg", purpleAlien.getName());
        }

        @Test
        @DisplayName("Should set alien to active")
        void shouldSetAlienToActive() {
            purpleAlien.setActive(false);
            purpleAlien.setActive(true);

            assertTrue(purpleAlien.isActive());
        }

        @Test
        @DisplayName("Should maintain active status when set to true multiple times")
        void shouldMaintainActiveStatusWhenSetMultipleTimes() {
            purpleAlien.setActive(true);
            purpleAlien.setActive(true);
            purpleAlien.setActive(true);

            assertTrue(purpleAlien.isActive());
        }

        @Test
        @DisplayName("Should maintain inactive status when set to false multiple times")
        void shouldMaintainInactiveStatusWhenSetMultipleTimes() {
            purpleAlien.setActive(false);
            purpleAlien.setActive(false);
            purpleAlien.setActive(false);

            assertFalse(purpleAlien.isActive());
        }

        @Test
        @DisplayName("Should toggle active status correctly")
        void shouldToggleActiveStatusCorrectly() {
            // Initially active
            assertTrue(purpleAlien.isActive());

            // Set to inactive
            purpleAlien.setActive(false);
            assertFalse(purpleAlien.isActive());

            // Set back to active
            purpleAlien.setActive(true);
            assertTrue(purpleAlien.isActive());

            // Set to inactive again
            purpleAlien.setActive(false);
            assertFalse(purpleAlien.isActive());
        }
    }

    @Nested
    @DisplayName("Combat Bonus Tests - Galaxy Trucker Rules")
    class CombatBonusTests {

        @Test
        @DisplayName("Active purple alien should provide +2 combat bonus")
        void activePurpleAlienShouldProvideCorrectCombatBonus() {
            assertEquals(2, purpleAlien.getCombatBonus());
        }

        @Test
        @DisplayName("Active brown alien should provide 0 combat bonus")
        void activeBrownAlienShouldProvideZeroCombatBonus() {
            assertEquals(0, brownAlien.getCombatBonus());
        }

        @Test
        @DisplayName("Inactive purple alien should provide 0 combat bonus")
        void inactivePurpleAlienShouldProvideZeroCombatBonus() {
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getCombatBonus());
        }

        @Test
        @DisplayName("Inactive brown alien should provide 0 combat bonus")
        void inactiveBrownAlienShouldProvideZeroCombatBonus() {
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getCombatBonus());
        }

        @Test
        @DisplayName("Combat bonus should change with active status")
        void combatBonusShouldChangeWithActiveStatus() {
            // Initially active purple alien
            assertEquals(2, purpleAlien.getCombatBonus());

            // Set to inactive
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getCombatBonus());

            // Set back to active
            purpleAlien.setActive(true);
            assertEquals(2, purpleAlien.getCombatBonus());
        }
    }

    @Nested
    @DisplayName("Engine Bonus Tests - Galaxy Trucker Rules")
    class EngineBonusTests {

        @Test
        @DisplayName("Active brown alien should provide +2 engine bonus")
        void activeBrownAlienShouldProvideCorrectEngineBonus() {
            assertEquals(2, brownAlien.getEngineBonus());
        }

        @Test
        @DisplayName("Active purple alien should provide 0 engine bonus")
        void activePurpleAlienShouldProvideZeroEngineBonus() {
            assertEquals(0, purpleAlien.getEngineBonus());
        }

        @Test
        @DisplayName("Inactive brown alien should provide 0 engine bonus")
        void inactiveBrownAlienShouldProvideZeroEngineBonus() {
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getEngineBonus());
        }

        @Test
        @DisplayName("Inactive purple alien should provide 0 engine bonus")
        void inactivePurpleAlienShouldProvideZeroEngineBonus() {
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getEngineBonus());
        }

        @Test
        @DisplayName("Engine bonus should change with active status")
        void engineBonusShouldChangeWithActiveStatus() {
            // Initially active brown alien
            assertEquals(2, brownAlien.getEngineBonus());

            // Set to inactive
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getEngineBonus());

            // Set back to active
            brownAlien.setActive(true);
            assertEquals(2, brownAlien.getEngineBonus());
        }
    }

    @Nested
    @DisplayName("Life Support Bonus Tests")
    class LifeSupportBonusTests {

        @Test
        @DisplayName("Active brown alien should provide +1 life support bonus")
        void activeBrownAlienShouldProvideLifeSupportBonus() {
            assertEquals(1, brownAlien.getLifeSupportBonus());
        }

        @Test
        @DisplayName("Active purple alien should provide 0 life support bonus")
        void activePurpleAlienShouldProvideZeroLifeSupportBonus() {
            assertEquals(0, purpleAlien.getLifeSupportBonus());
        }

        @Test
        @DisplayName("Inactive brown alien should provide 0 life support bonus")
        void inactiveBrownAlienShouldProvideZeroLifeSupportBonus() {
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getLifeSupportBonus());
        }

        @Test
        @DisplayName("Inactive purple alien should provide 0 life support bonus")
        void inactivePurpleAlienShouldProvideZeroLifeSupportBonus() {
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getLifeSupportBonus());
        }

        @Test
        @DisplayName("Life support bonus should change with active status")
        void lifeSupportBonusShouldChangeWithActiveStatus() {
            // Initially active brown alien
            assertEquals(1, brownAlien.getLifeSupportBonus());

            // Set to inactive
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getLifeSupportBonus());

            // Set back to active
            brownAlien.setActive(true);
            assertEquals(1, brownAlien.getLifeSupportBonus());
        }
    }

    @Nested
    @DisplayName("End Game Bonus Tests")
    class EndGameBonusTests {

        @Test
        @DisplayName("Active purple alien should provide +3 end game bonus")
        void activePurpleAlienShouldProvideCorrectEndGameBonus() {
            assertEquals(3, purpleAlien.getEndGameBonus());
        }

        @Test
        @DisplayName("Active brown alien should provide +2 end game bonus")
        void activeBrownAlienShouldProvideCorrectEndGameBonus() {
            assertEquals(2, brownAlien.getEndGameBonus());
        }

        @Test
        @DisplayName("Inactive purple alien should provide 0 end game bonus")
        void inactivePurpleAlienShouldProvideZeroEndGameBonus() {
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getEndGameBonus());
        }

        @Test
        @DisplayName("Inactive brown alien should provide 0 end game bonus")
        void inactiveBrownAlienShouldProvideZeroEndGameBonus() {
            brownAlien.setActive(false);
            assertEquals(0, brownAlien.getEndGameBonus());
        }

        @Test
        @DisplayName("End game bonus should change with active status")
        void endGameBonusShouldChangeWithActiveStatus() {
            // Initially active purple alien
            assertEquals(3, purpleAlien.getEndGameBonus());

            // Set to inactive
            purpleAlien.setActive(false);
            assertEquals(0, purpleAlien.getEndGameBonus());

            // Set back to active
            purpleAlien.setActive(true);
            assertEquals(3, purpleAlien.getEndGameBonus());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return correct string representation for active purple alien")
        void shouldReturnCorrectStringForActivePurpleAlien() {
            String expected = "AlienPassenger{color=ALIEN_PURPLE, name='Zorg', active=true}";
            assertEquals(expected, purpleAlien.toString());
        }

        @Test
        @DisplayName("Should return correct string representation for active brown alien")
        void shouldReturnCorrectStringForActiveBrownAlien() {
            String expected = "AlienPassenger{color=ALIEN_BROWN, name='Klick', active=true}";
            assertEquals(expected, brownAlien.toString());
        }

        @Test
        @DisplayName("Should return correct string representation for inactive alien")
        void shouldReturnCorrectStringForInactiveAlien() {
            purpleAlien.setActive(false);
            String expected = "AlienPassenger{color=ALIEN_PURPLE, name='Zorg', active=false}";
            assertEquals(expected, purpleAlien.toString());
        }

        @Test
        @DisplayName("Should return correct string representation for alien with null name")
        void shouldReturnCorrectStringForAlienWithNullName() {
            AlienPassenger alienWithNullName = new AlienPassenger(AlienColor.ALIEN_PURPLE, null);
            String expected = "AlienPassenger{color=ALIEN_PURPLE, name='null', active=true}";
            assertEquals(expected, alienWithNullName.toString());
        }

        @Test
        @DisplayName("Should return correct string representation for alien with empty name")
        void shouldReturnCorrectStringForAlienWithEmptyName() {
            AlienPassenger alienWithEmptyName = new AlienPassenger(AlienColor.ALIEN_BROWN, "");
            String expected = "AlienPassenger{color=ALIEN_BROWN, name='', active=true}";
            assertEquals(expected, alienWithEmptyName.toString());
        }

        @Test
        @DisplayName("Should return correct string representation for alien with special characters in name")
        void shouldReturnCorrectStringForAlienWithSpecialCharacters() {
            AlienPassenger alienWithSpecialName = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zö'rg-123!@#");
            String expected = "AlienPassenger{color=ALIEN_PURPLE, name='Zö'rg-123!@#', active=true}";
            assertEquals(expected, alienWithSpecialName.toString());
        }
    }

    @Nested
    @DisplayName("Equals Tests")
    class EqualsTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(purpleAlien, purpleAlien);
        }

        @Test
        @DisplayName("Should be equal to alien with same color and name")
        void shouldBeEqualToAlienWithSameColorAndName() {
            AlienPassenger otherAlien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
            assertEquals(purpleAlien, otherAlien);
        }

        @Test
        @DisplayName("Should not be equal to alien with different color")
        void shouldNotBeEqualToAlienWithDifferentColor() {
            AlienPassenger otherAlien = new AlienPassenger(AlienColor.ALIEN_BROWN, "Zorg");
            assertNotEquals(purpleAlien, otherAlien);
        }

        @Test
        @DisplayName("Should not be equal to alien with different name")
        void shouldNotBeEqualToAlienWithDifferentName() {
            AlienPassenger otherAlien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Different");
            assertNotEquals(purpleAlien, otherAlien);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertNotEquals(null, purpleAlien);
        }

        @Test
        @DisplayName("Should not be equal to different class object")
        void shouldNotBeEqualToDifferentClassObject() {
            assertNotEquals("not an alien", purpleAlien);
        }

        @Test
        @DisplayName("Should be equal regardless of active status")
        void shouldBeEqualRegardlessOfActiveStatus() {
            AlienPassenger otherAlien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
            otherAlien.setActive(false);

            assertEquals(purpleAlien, otherAlien);
        }

        @Test
        @DisplayName("Should handle null names in equality")
        void shouldHandleNullNamesInEquality() {
            AlienPassenger alien1 = new AlienPassenger(AlienColor.ALIEN_PURPLE, null);
            AlienPassenger alien2 = new AlienPassenger(AlienColor.ALIEN_PURPLE, null);

            assertEquals(alien1, alien2);
        }

        @Test
        @DisplayName("Should not be equal when one name is null and other is not")
        void shouldNotBeEqualWhenOneNameIsNullAndOtherIsNot() {
            AlienPassenger alienWithNullName = new AlienPassenger(AlienColor.ALIEN_PURPLE, null);
            assertNotEquals(purpleAlien, alienWithNullName);
        }
    }

    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("Should have same hash code for equal objects")
        void shouldHaveSameHashCodeForEqualObjects() {
            AlienPassenger otherAlien = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
            assertEquals(purpleAlien.hashCode(), otherAlien.hashCode());
        }

        @Test
        @DisplayName("Should be consistent across multiple calls")
        void shouldBeConsistentAcrossMultipleCalls() {
            int hash1 = purpleAlien.hashCode();
            int hash2 = purpleAlien.hashCode();
            int hash3 = purpleAlien.hashCode();

            assertEquals(hash1, hash2);
            assertEquals(hash2, hash3);
        }

        @Test
        @DisplayName("Should handle null names in hash code")
        void shouldHandleNullNamesInHashCode() {
            AlienPassenger alienWithNullName = new AlienPassenger(AlienColor.ALIEN_PURPLE, null);

            // Should not throw exception
            assertDoesNotThrow(() -> alienWithNullName.hashCode());
        }

        @Test
        @DisplayName("Should maintain hash code contract with equals")
        void shouldMaintainHashCodeContractWithEquals() {
            AlienPassenger alien1 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
            AlienPassenger alien2 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");

            if (alien1.equals(alien2)) {
                assertEquals(alien1.hashCode(), alien2.hashCode());
            }
        }

        @Test
        @DisplayName("Should not be affected by active status change")
        void shouldNotBeAffectedByActiveStatusChange() {
            int hashBeforeChange = purpleAlien.hashCode();
            purpleAlien.setActive(false);
            int hashAfterChange = purpleAlien.hashCode();

            assertEquals(hashBeforeChange, hashAfterChange);
        }
    }

    @Nested
    @DisplayName("Galaxy Trucker Rules Integration Tests")
    class GalaxyTruckerRulesTests {

        @Test
        @DisplayName("Should correctly implement purple alien combat bonus rule")
        void shouldCorrectlyImplementPurpleAlienCombatBonusRule() {
            // Purple aliens provide +2 combat strength according to Galaxy Trucker rules
            AlienPassenger purple = new AlienPassenger(AlienColor.ALIEN_PURPLE, "CombatAlien");

            assertEquals(2, purple.getCombatBonus());
            assertEquals(0, purple.getEngineBonus());
            assertEquals(0, purple.getLifeSupportBonus());
            assertEquals(3, purple.getEndGameBonus());
        }

        @Test
        @DisplayName("Should correctly implement brown alien engine bonus rule")
        void shouldCorrectlyImplementBrownAlienEngineBonusRule() {
            // Brown aliens provide +2 engine strength according to Galaxy Trucker rules
            AlienPassenger brown = new AlienPassenger(AlienColor.ALIEN_BROWN, "EngineAlien");

            assertEquals(0, brown.getCombatBonus());
            assertEquals(2, brown.getEngineBonus());
            assertEquals(1, brown.getLifeSupportBonus());
            assertEquals(2, brown.getEndGameBonus());
        }

        @Test
        @DisplayName("Should maintain alien bonuses rules consistency")
        void shouldMaintainAlienBonusesRulesConsistency() {
            // Test that purple and brown aliens have distinct, non-overlapping bonuses

            // Purple alien should only have combat and end-game bonuses
            assertTrue(purpleAlien.getCombatBonus() > 0);
            assertEquals(0, purpleAlien.getEngineBonus());
            assertEquals(0, purpleAlien.getLifeSupportBonus());
            assertTrue(purpleAlien.getEndGameBonus() > 0);

            // Brown alien should only have engine, life support, and end-game bonuses
            assertEquals(0, brownAlien.getCombatBonus());
            assertTrue(brownAlien.getEngineBonus() > 0);
            assertTrue(brownAlien.getLifeSupportBonus() > 0);
            assertTrue(brownAlien.getEndGameBonus() > 0);
        }

        @Test
        @DisplayName("Should handle inactive aliens according to rules")
        void shouldHandleInactiveAliensAccordingToRules() {
            // When aliens are inactive (e.g., lost in combat), they provide no bonuses
            purpleAlien.setActive(false);
            brownAlien.setActive(false);

            assertEquals(0, purpleAlien.getCombatBonus());
            assertEquals(0, purpleAlien.getEngineBonus());
            assertEquals(0, purpleAlien.getLifeSupportBonus());
            assertEquals(0, purpleAlien.getEndGameBonus());

            assertEquals(0, brownAlien.getCombatBonus());
            assertEquals(0, brownAlien.getEngineBonus());
            assertEquals(0, brownAlien.getLifeSupportBonus());
            assertEquals(0, brownAlien.getEndGameBonus());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle rapid status changes correctly")
        void shouldHandleRapidStatusChangesCorrectly() {
            // Test rapid toggling of active status
            for (int i = 0; i < 100; i++) {
                purpleAlien.setActive(i % 2 == 0);

                if (i % 2 == 0) {
                    assertEquals(2, purpleAlien.getCombatBonus());
                } else {
                    assertEquals(0, purpleAlien.getCombatBonus());
                }
            }
        }

        @Test
        @DisplayName("Should maintain immutability of color and name")
        void shouldMaintainImmutabilityOfColorAndName() {
            AlienColor originalColor = purpleAlien.getColor();
            String originalName = purpleAlien.getName();

            // These values should never change
            purpleAlien.setActive(false);
            purpleAlien.setActive(true);

            assertEquals(originalColor, purpleAlien.getColor());
            assertEquals(originalName, purpleAlien.getName());
        }

        @Test
        @DisplayName("Should handle extreme name lengths")
        void shouldHandleExtremeNameLengths() {
            String veryLongName = "A".repeat(10000);
            AlienPassenger alienWithLongName = new AlienPassenger(AlienColor.ALIEN_PURPLE, veryLongName);

            assertEquals(veryLongName, alienWithLongName.getName());
            assertEquals(2, alienWithLongName.getCombatBonus());
        }

        @Test
        @DisplayName("Should handle name with unicode characters")
        void shouldHandleNameWithUnicodeCharacters() {
            String unicodeName = "👽🚀🌟";
            AlienPassenger alienWithUnicodeName = new AlienPassenger(AlienColor.ALIEN_BROWN, unicodeName);

            assertEquals(unicodeName, alienWithUnicodeName.getName());
            assertEquals(2, alienWithUnicodeName.getEngineBonus());
        }
    }

    @Nested
    @DisplayName("Multiple Aliens Interaction Tests")
    class MultipleAliensTests {

        @Test
        @DisplayName("Should handle multiple aliens with same properties")
        void shouldHandleMultipleAliensWithSameProperties() {
            AlienPassenger purple1 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");
            AlienPassenger purple2 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Zorg");

            assertEquals(purple1, purple2);
            assertEquals(purple1.hashCode(), purple2.hashCode());
            assertEquals(purple1.toString(), purple2.toString());
        }

        @Test
        @DisplayName("Should maintain independence between alien instances")
        void shouldMaintainIndependenceBetweenAlienInstances() {
            AlienPassenger purple1 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Alien1");
            AlienPassenger purple2 = new AlienPassenger(AlienColor.ALIEN_PURPLE, "Alien2");

            // Change status of one alien
            purple1.setActive(false);

            // Other alien should not be affected
            assertTrue(purple2.isActive());
            assertEquals(0, purple1.getCombatBonus());
            assertEquals(2, purple2.getCombatBonus());
        }
    }
}
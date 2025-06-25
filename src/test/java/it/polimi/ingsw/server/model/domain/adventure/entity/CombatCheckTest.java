//package it.polimi.ingsw.server.model.domain.adventure.entity;
//
//import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
//import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
//import it.polimi.ingsw.server.model.domain.player.Player;
//import it.polimi.ingsw.server.model.domain.player.PlayerId;
//import it.polimi.ingsw.server.model.domain.ship.Ship;
//import it.polimi.ingsw.server.model.enums.GameLevel;
//import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
//import it.polimi.ingsw.server.model.enums.adventure.PenaltyType;
//import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
//import it.polimi.ingsw.server.model.enums.ship.Direction;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import org.junit.jupiter.api.BeforeEach;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//class CombatCheckTest {
//    private CombatCheck combatCheckCrew;
//    private CombatCheck combatCheckCannons;
//    private CombatCheck combatCheckEngines;
//    private Player player1, player2, player3;
//    private List<CannonFire> cannons;
//
//    @BeforeEach
//    void setUp() {
//
//        //Inizializzo i giocatori
//        PlayerId playerId1 = new PlayerId(UUID.randomUUID(), "Samuele");
//        PlayerId playerId2 = new PlayerId(UUID.randomUUID(), "Diego");
//        PlayerId playerId3 = new PlayerId(UUID.randomUUID(), "Manuela");
//
//        player1 = new Player(playerId1);
//        player2 = new Player (playerId2);
//        player3 = new Player(playerId3);
//
////        player1.setShip(new Ship(player1, GameLevel.TEST_FLIGHT));
////        player2.setShip(new Ship(player2, GameLevel.TEST_FLIGHT));
////        player3.setShip(new Ship(player3, GameLevel.TEST_FLIGHT));
//
//        player1.getShip().setCrew(1); // Caso limite: valore minimo
//        player2.getShip().setCrew(3);
//        player3.getShip().setCrew(3);
//
//        player1.getShip().setEngines(5.5);
//        player2.getShip().setEngines(3.5);
//        player3.getShip().setEngines(0); // Caso limite: nessun motore
//
//        player1.getShip().setCannons(10);
//        player2.getShip().setCannons(10); // Caso limite: pareggio
//        player3.getShip().setCannons(60);
//
//        //Inizializzo le cannonate
//        cannons = new ArrayList<>();
//        CannonFire cannonFire1 = new CannonFire(Direction.UP,ShotIntensity.LIGHT);
//        CannonFire cannonFire2 = new CannonFire(Direction.LEFT,ShotIntensity.HEAVY);
//        cannons.add(cannonFire1);
//        cannons.add(cannonFire2);
//
//        // Inizializzo i CombatCheck con diversi attributi
//        combatCheckCrew = new CombatCheck(CombatAttributeType.CREW_COUNT, PenaltyType.FLIGHT_DAYS_LOSS, 3);
//        combatCheckEngines = new CombatCheck(CombatAttributeType.ENGINE_POWER, PenaltyType.CREW_LOSS, 2);
//        combatCheckCannons = new CombatCheck(CombatAttributeType.CANNON_STRENGTH, cannons);
//
//    }
//
//    @Test
//    void testCombatLoser_CrewCount() {
//        Player loser = combatCheckCrew.getCombatLoser(List.of(player1, player2, player3));
//        assertEquals(player1, loser, "Il giocatore con meno membri equipaggio dovrebbe essere Samuele");
//    }
//
//    @Test
//    void testCombatLoser_CannonStrength() {
//        Player loser = combatCheckCannons.getCombatLoser(List.of(player1, player2, player3));
//        assertEquals(player1, loser, "Il giocatore con meno cannoni dovrebbe essere Samuele (pareggio con Diego, primo in lista)");
//    }
//
//    @Test
//    void testCombatLoser_EnginePower() {
//        Player loser = combatCheckEngines.getCombatLoser(List.of(player1, player2, player3));
//        assertEquals(player3, loser, "Il giocatore con meno motori dovrebbe essere Manuela (0 motori)");
//    }
//
//    @Test
//    void testCombatLoser_AllSameValues() {
//
//        player1.getShip().setCrew(5);
//        player2.getShip().setCrew(5);
//        player3.getShip().setCrew(5);
//
//        Player loser = combatCheckCrew.getCombatLoser(List.of(player1, player2, player3));
//        assertEquals(player1, loser, "In caso di tutti i valori uguali, il primo nella lista dovrebbe perdere");
//    }
//
//    @Test
//    void getAttributeTest(){
//
//        assertEquals(CombatAttributeType.CREW_COUNT, combatCheckCrew.getAttribute());
//
//    }
//
//    @Test
//    void getPenaltyTypeTest(){
//
//        assertEquals(PenaltyType.FLIGHT_DAYS_LOSS, combatCheckCrew.getPenaltyType());
//
//    }
//
//    @Test
//    void getPenaltyValueTest(){
//
//        assertEquals(3, combatCheckCrew.getPenaltyValue());
//
//    }
//
//    @Test
//    void getCannonFiresTest(){
//
//        assertEquals(cannons, combatCheckCannons.getCannonFires());
//
//    }
//
//
//}









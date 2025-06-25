package it.polimi.ingsw.server.model.domain.player;

import it.polimi.ingsw.server.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test

    void test(){

        PlayerId playerId1, playerId2;
        playerId1 = new PlayerId(new UUID(1,4), "gigio");
        playerId2 = new PlayerId(new UUID(1,4), "topolino");

        Player player1;
        player1 = new Player(playerId1);

        Player player2;
        player2 = new Player(playerId2, PlayerColor.RED);

        assertThrows(IllegalArgumentException.class, () -> { new Player(playerId1, null); });

        assertThrows(IllegalArgumentException.class, () -> { new Player(null); });

        assertEquals(player1.getId(), playerId1);
        assertEquals(player2.getId(), playerId2);

        assertEquals(player2.getColor(), PlayerColor.RED);

        assertFalse(player1.getFlightData().equals( player2.getFlightData()));

        assertThrows(IllegalArgumentException.class, () -> { player1.setColor(null); });
        assertThrows(IllegalArgumentException.class, () -> { player2.setColor(null); });

        player1.setColor(PlayerColor.BLUE);
        assertEquals(player1.getColor(), PlayerColor.BLUE);

        assertEquals(0, player1.getCredits());
        assertEquals(0, player2.getCredits());

        assertThrows(IllegalArgumentException.class, () -> {player1.addCredits(-2);});
        assertThrows(IllegalArgumentException.class, () -> {player2.addCredits(-5);});


        player1.addCredits(5);

        assertEquals(player1.getCredits(), 5);

        assertThrows(IllegalArgumentException.class, () -> {player1.subtractCredits(-2);});
        assertThrows(IllegalArgumentException.class, () -> {player2.subtractCredits(-5);});

        assertThrows(IllegalArgumentException.class, () -> {player2.subtractCredits(2);});

        player1.subtractCredits(5);
        assertEquals(player1.getCredits(), 0);

        assertEquals(player2.getFinalScore(), 0);

        player1.setFinalScore(5);
        assertEquals(player1.getFinalScore(), 5);


        assertFalse(player1.equals(player2));

        assertFalse(player1.hashCode() == player2.hashCode());

        assertEquals(player1.toString(), "Player{" +
                "id=" + player1.getId() +
                ", color=" + player1.getColor() +
                ", credits=" + player1.getCredits() +
                ", crewMembers=0}");

    }





}





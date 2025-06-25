package it.polimi.ingsw.server.model.domain.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerIdTest {

    @Test

    void test(){

        PlayerId playerId;
        playerId = new PlayerId(new UUID(1,4), "nickname");

        assertEquals("nickname", playerId.getNickname());


        PlayerId playerId2;
        playerId2 = null;
        playerId2 = playerId2.fromString ("nick");

        assertEquals("nick", playerId2.toString());



        assertThrows(IllegalArgumentException.class, () -> {PlayerId playerId3 = new PlayerId(new UUID(1,4), "nick"); playerId3.fromString("");});



        assertTrue(playerId.equals(playerId));

        assertFalse(playerId.equals(playerId2));

        assertFalse(playerId.equals(null));




        assertEquals(playerId2.hashCode(), playerId2.getNickname().hashCode());


    }

}






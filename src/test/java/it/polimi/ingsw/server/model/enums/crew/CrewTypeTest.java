package it.polimi.ingsw.server.model.enums.crew;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrewTypeTest {

    @Test

    void test(){

        CrewType member;

        member = CrewType.ALIEN_BROWN;
        assertEquals(CrewType.ALIEN_BROWN, member);

        assertTrue(member.isAlien());

        assertEquals(member.getAlienColor(), CrewType.ALIEN_BROWN.getAlienColor());


        assertEquals(member.getMaxPerCabin(), 1);



    }

}
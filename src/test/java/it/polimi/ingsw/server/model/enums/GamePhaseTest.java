package it.polimi.ingsw.server.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;



class GamePhaseTest {


    @Test

    void test(){

        GamePhase gamePhase = GamePhase.SETUP;

        assertEquals(GamePhase.SETUP, gamePhase);

        assertEquals(GamePhase.BUILDING, gamePhase.getNextPhase());

        gamePhase = gamePhase.getNextPhase();

        gamePhase = gamePhase.getNextPhase();

        assertEquals(GamePhase.FLIGHT, gamePhase);

        assertEquals(GamePhase.END, gamePhase.getNextPhase());

        gamePhase = gamePhase.getNextPhase();

        assertEquals(GamePhase.END, gamePhase.getNextPhase());




    }

}
















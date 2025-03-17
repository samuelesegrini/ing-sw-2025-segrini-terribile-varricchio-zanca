package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.card.*;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.domain.flight.FlightBoard;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.ship.Ship;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor interface for processing different types of adventure cards.
 * <p>
 * This interface implements the Visitor design pattern to allow operations 
 * on adventure cards without modifying their classes. Each method handles 
 * a specific card type and can return a generic result.
 * </p>
 * 
 * @param <T> The return type of the visitor operations
 */
public interface AdventureCardVisitor<T> {

    /**
     * Visits an AbandonedShipCard.
     *
     * @param card The abandoned ship card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitAbandonedShipCard(AbandonedShipCard card, GameState state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getPlayerOrderByPosition();

        for(Player player : playersOrdered ) {
            if ((!card.isVisited()) && (player.getShip().getCrewNumber() >= card.getCrewLost())) {
                flightBoard.movePlayer(player, AbandonedShipCard.lostDays(), false);
                card.visit();
                System.out.println(player.getId().getNickname() + " ha saccheggiato la nave ");
            }
            break;  // passa al giocatore successivo
        }
    }

    /**
     * Visits a MeteorSwarmCard.
     *
     * @param card The meteor swarm card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitMeteorSwarmCard(MeteorSwarmCard card, GameState state);
    
    /**
     * Visits a PiratesCard.
     *
     * @param card The pirates card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitPiratesCard(PiratesCard card, GameState state);
    
    /**
     * Visits a PlanetsCard.
     *
     * @param card The planets card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitPlanetsCard(PlanetsCard card, GameState state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getPlayerOrderByPosition();

        System.out.println("Resolving planet: " + card.getType());

        for(Player player : playersOrdered ){
            for(Planet planet : card.getPlanets()){
                //the planet must be unvisited and the player must have enough space to gather resources
                if((!planet.isVisited()) && (player.getShip().addResources(planet.getGoodQuantities()))){
                    flightBoard.movePlayer(player, card.getLostDays(), false);
                    planet.setVisited();
                    System.out.println(player.getId().getNickname() + " è atterrato su " + planet.getName());
                    break;  // passa al giocatore successivo
                }
            }
        }

    }
    
    /**
     * Visits an OpenSpaceCard.
     *
     * @param card The open space card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitOpenSpaceCard(OpenSpaceCard card, GameState state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered;
        for(Player player : playersOrdered){
            flightBoard.movePlayer(player, player.getShip().getEngineStrength(), true);
        }
    }
    
    /**
     * Visits a StardustCard.
     *
     * @param card The stardust card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitStardustCard(StardustCard card, GameState state);
    
    /**
     * Visits a SlaversCard.
     *
     * @param card The slavers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitSlaversCard(SlaversCard card, GameState state);
    
    /**
     * Visits a SmugglersCard.
     *
     * @param card The smugglers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitSmugglersCard(SmugglersCard card, GameState state);
    
    /**
     * Visits a CombatZoneCard.
     *
     * @param card The combat zone card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitCombatZoneCard(CombatZoneCard card, GameState state){

    }

    /**
     * Visits an EpidemicCard.
     *
     * @param card The epidemic card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitEpidemicCard(EpidemicCard card, GameState state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getPlayerOrderByPosition();

        for(Player player : playersOrdered ) {
            //perde un membro dell'equipaggio per ogni cabina collegata ad un'altra
            break;  // passa al giocatore successivo
        }
    }
    
    /**
     * Visits an AbandonedStationCard.
     *
     * @param card The abandoned station card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitAbandonedStationCard(AbandonedStationCard card, GameState state){
            FlightBoard flightBoard = state.getFlightBoard();
            List<Player> playersOrdered = flightBoard.getPlayerOrderByPosition();

            for (Player player : playersOrdered) {
                if ((!card.isVisited()) && (player.getShip().getCrewNumber() >= card.getMinCrewRequired())) {
                    flightBoard.movePlayer(player, AbandonedShipCard.lostDays(), false);
                    card.visit();
                    player.getShip().addResources(card.getGoodQuantities());
                    System.out.println(player.getId().getNickname() + " ha saccheggiato la stazione");
                    break;  // passa al giocatore successivo
                }
            }
        }
    /**
     * Visits a CosmicDustCard.
     *
     * @param card The cosmic dust card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    T visitCosmicDustCard(StardustCard card, GameState state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getPlayerOrderByPosition();
        for (Player player : playersOrdered){
            flightBoard.movePlayer(player, player.getShip().getExposedComponents() , false);
        }
    }
}
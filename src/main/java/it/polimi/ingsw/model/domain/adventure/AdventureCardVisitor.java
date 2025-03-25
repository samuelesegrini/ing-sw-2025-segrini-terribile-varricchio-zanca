package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.adventure.card.*;
import it.polimi.ingsw.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.domain.flight.FlightBoard;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.*;

import static it.polimi.ingsw.model.enums.ship.ComponentType.CARGO_HOLD_SPECIAL;

/**
 * Visitor interface for processing different types of adventure cards.
 * <p>
 * This interface implements the Visitor design pattern to allow operations 
 * on adventure cards without modifying their classes. Each method handles 
 * a specific card type and can return a generic result.
 * </p>
 *
 */
public class AdventureCardVisitor {

    /**
     * Visits an AbandonedShipCard.
     *
     * @param card The abandoned ship card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedShipCard(AbandonedShipCard card, GameModel state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        System.out.println("Resolving "+card.getType());

        for(Player player : playersOrdered ) {
            if ((!card.isVisited()) && (player.getShip().getCrewNumber() >= card.getCrewLost())) {
                flightBoard.movePlayer(player, card.getLostDays(), false);
                player.addCredits(card.getCreditsGained());
                player.updateCrewMember(card.getCrewLost(), true);

                card.setVisited();
                System.out.println(player.getId().getNickname() + " has repaired the ship and sold it to part of their crew. ");
            }
        }
        return card.isVisited();
    }

    /**
     * Visits a MeteorSwarmCard.
     *
     * @param card The meteor swarm card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitMeteorSwarmCard(MeteorSwarmCard card, GameModel state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Meteor meteor: card.getMeteorPattern()){
            Random dice = new Random();
            int index = dice.nextInt(6) + 1;

            for(Player player : playersOrdered){
                Position impactPosition = player.getShip().getGrid().findFirstComponet(meteor.getApproach(), index);
                Component impactComponent = player.getShip().getGrid().get(impactPosition);

                if(meteor.getShotIntensity() == ShotIntensity.LIGHT){
                    if(impactComponent.getConnectorAt(meteor.getApproach())== ConnectorType.PLAIN){
                        System.out.println(player.getId().getNickname()+ " has deflected meteor number"
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                    else if(player.getShip().getGrid().protectedByShield(meteor.getApproach())){
                        System.out.println(player.getId().getNickname()+ " has activated a shield against meteor number"
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                }

                else {
                    if(player.getShip().getGrid().protectedByCannon(meteor.getApproach(), index)){
                        System.out.println(player.getId().getNickname()+ " has shot meteor number"
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                }

                System.out.println(player.getId().getNickname()+ " has no protection against meteor number"
                        +card.getMeteorPattern().indexOf(meteor));
                player.getShip().removeComponent(impactPosition);
            }
        }
        return true;
    }
    
    /**
     * Visits a PiratesCard.
     *
     * @param card The pirates card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitPiratesCard(PiratesCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered ) {
            if(player.getShip().getCannonStrength()>card.getPowerLevel()){
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
            }
            else if(player.getShip().getCannonStrength()==card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannonStrength()<card.getPowerLevel()){
                defeated.add(player);
            }
        }
        if(!defeated.isEmpty()){
            for(Player player : defeated) {
                for(CannonFire cannonFire : card.getAttackPattern()){

                    Random dice1 = new Random();
                    Random dice2 = new Random();
                    int index1 = dice1.nextInt(6) + 1;
                    int index2 = dice2.nextInt(6) + 1;
                    int index = index1 + index2;

                    Position impactPosition = player.getShip().getGrid().findFirstComponet(cannonFire.getApproach(), index);
                    Component impactComponent = player.getShip().getGrid().get(impactPosition);

                    if(cannonFire.isBlockable() && player.getShip().getGrid().protectedByShield(cannonFire.getApproach())){
                        System.out.println(player.getId().getNickname()+ " has activated a shield against cannon fire number "
                        +card.getAttackPattern().indexOf(cannonFire));
                    } else {
                        System.out.println(player.getId().getNickname()+ " has no protection against cannon fire number "
                                +card.getAttackPattern().indexOf(cannonFire));
                        player.getShip().removeComponent(impactPosition);
                    }
                }
            }
        }
        return true;
    }

    
    /**
     * Visits a PlanetsCard.
     *
     * @param card The planets card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitPlanetsCard(PlanetsCard card, GameModel state){
        System.out.println("Resolving planet: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

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
        return true;
    }
    
    /**
     * Visits an OpenSpaceCard.
     *
     * @param card The open space card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitOpenSpaceCard (OpenSpaceCard card, GameModel state){
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered){
            flightBoard.movePlayer(player, (int)player.getShip().getEngineStrength(), true);
            if(player.getShip().getEngineStrength() <= 0){
                defeated.add(player);
            }
        }
        if(!defeated.isEmpty()){
            for(Player player : defeated){
                flightBoard.abandonPlayer(player);
            }
        }
        return true;
    }
    
    /**
     * Visits a StardustCard.
     *
     * @param card The stardust card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitStardustCard(StardustCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        Collections.reverse(playersOrdered);

        for(Player player : playersOrdered){
            int exposedConnectors = player.getShip().getExposedConnectors();
            flightBoard.movePlayer(player, exposedConnectors, false);
        }
        return true;
    }
    
    /**
     * Visits a SlaversCard.
     *
     * @param card The slavers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSlaversCard(SlaversCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player: playersOrdered){
            if(player.getShip().getCannonStrength() == card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannonStrength() < card.getPowerLevel()){
                player.updateCrewMember(card.getCrewLossAmount(), true);
            }
            else if(player.getShip().getCannonStrength()>card.getPowerLevel()){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
        }
        if(card.isDefeated()){
            return true;
        }
        return false;
    }
    
    /**
     * Visits a SmugglersCard.
     *
     * @param card The smugglers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSmugglersCard(SmugglersCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        double powerLevel = card.getPowerLevel();
        for(Player player: playersOrdered){
            double cannonStrength = player.getShip().getCannonStrength();
            if(powerLevel == cannonStrength){
                continue;
            }
            else if(powerLevel < cannonStrength){
                if(!(player.getShip().removeValuableResources(card.getGoodsLostIfDefeated()))){
                    throw new IllegalArgumentException("Not enough resources available!");
                }
            }
            else if (powerLevel > cannonStrength){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.getShip().addResources(card.getAvailableGoods());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
        }
        if(card.isDefeated()){
            return true;
        }
        return false;
    }
    
    /**
     * Visits a CombatZoneCard.
     *
     * @param card The combat zone card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public T visitCombatZoneCard(CombatZoneCard card, GameModel state){
        //se crew persa tutta giocatore è costretto ad abbandonare
    }

    /**
     * Visits an EpidemicCard.
     *
     * @param card The epidemic card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public T visitEpidemicCard(EpidemicCard card, GameModel state){
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
    public T visitAbandonedStationCard(AbandonedStationCard card, GameModel state){
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
    public boolean visitCosmicDustCard(StardustCard card, GameModel state) {
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        for (Player player : playersOrdered) {
            flightBoard.movePlayer(player, player.getShip().getExposedComponents(), false);
        }
    }
}
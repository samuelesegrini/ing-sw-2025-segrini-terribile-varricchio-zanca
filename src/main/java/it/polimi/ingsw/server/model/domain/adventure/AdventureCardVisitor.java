package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.server.model.enums.adventure.PenaltyType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.*;

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
        System.out.println("Resolving "+card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player : playersOrdered ) {
            if ((!card.isVisited()) && (player.getShip().getCrew() > card.getCrewLost())) {
                flightBoard.movePlayer(player, card.getLostDays(), false);
                player.addCredits(card.getCreditsGained());
                player.getShip().setCrew(player.getShip().getCrew() - card.getCrewLost());

                card.setVisited();
                System.out.println(player.getId().getNickname() + " has repaired the ship and sold it to part of their crew. ");
            }
        }
        return true;
    }

    /**
     * Visits a MeteorSwarmCard.
     *
     * @param card The meteor swarm card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitMeteorSwarmCard(MeteorSwarmCard card, GameModel state){
        System.out.println("Resolving "+card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        if(flightBoard.getPlayerCount()==1){
            state.getAdventureDeck().drawNextCard();
            System.out.println("Skipping " +card.getType()+" card");
            return false;
        }

        for(Meteor meteor: card.getMeteorPattern()){
            Random dice = new Random();
            int index = dice.nextInt(6);
            System.out.println(index);
            System.out.println(meteor.getApproach());

            for(Player player : playersOrdered){
                Position impactPosition = player.getShip().findFirstComponent(meteor.getApproach(), index);
                System.out.println("impactPosition: " + impactPosition);

                if(impactPosition == null){
                    System.out.println("Meteor number " +card.getMeteorPattern().indexOf(meteor)+ " missed " +player.getId().getNickname()+ "'s ship");
                    break;
                }

                Component impactComponent = player.getShip().getBoard()[impactPosition.getRow()][impactPosition.getCol()];
                if(meteor.getShotIntensity() == ShotIntensity.LIGHT){
                    if(impactComponent.getConnectorAt(meteor.getApproach())== ConnectorType.PLAIN){
                        System.out.println(player.getId().getNickname()+ " has deflected meteor number "
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                    else if(player.getShip().protectedByShield(meteor.getApproach())){
                        System.out.println(player.getId().getNickname()+ " has activated a shield against meteor number "
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                }

                else {
                    if(player.getShip().protectedByCannon(meteor.getApproach(), index)){
                        System.out.println(player.getId().getNickname()+ " has shot meteor number "
                                +card.getMeteorPattern().indexOf(meteor));
                        break;
                    }
                }

                System.out.println(player.getId().getNickname()+ " has no protection against meteor number "
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
            if(player.getShip().getCannons()>card.getPowerLevel()){
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                System.out.println("Player " + player.getId().getNickname() +
                        " has defeated the pirates and received a reward of " + card.getCreditReward());
            }
            else if(player.getShip().getCannons()==card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannons()<card.getPowerLevel()){
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

                    Component[][] board = player.getShip().getBoard();
                    if ( cannonFire.getApproach() == Direction.UP || cannonFire.getApproach() == Direction.DOWN) {
                        // For UP and DOWN directions, fixedIndex represents a column
                        if (index < 0 || index >= board[0].length) {
                            System.out.println("Column index out of bounds: " + index);
                            continue;
                        }
                    } else {
                        // For LEFT and RIGHT directions, fixedIndex represents a row
                        if (index < 0 || index >= board.length) {
                            System.out.println("Row index out of bounds: " + index);
                            continue;
                        }
                    }

                    System.out.println("Direction: " + cannonFire.getApproach()+ " index: " + index);
                    Position impactPosition = player.getShip().findFirstComponent(cannonFire.getApproach(), index);
                    if(impactPosition==null){
                        System.out.println("Player " + player.getId().getNickname() + " has no component in the impact position");
                        continue;
                    }
                    Component impactComponent = player.getShip().getBoard()[impactPosition.getRow()][impactPosition.getCol()];

                    if(cannonFire.isBlockable() && player.getShip().protectedByShield(cannonFire.getApproach())){
                        System.out.println(player.getId().getNickname()+ " has activated a shield against cannon fire number "
                        +card.getAttackPattern().indexOf(cannonFire));
                    } else {
                        System.out.println(player.getId().getNickname()+ " has no protection against cannon fire number "
                                +card.getAttackPattern().indexOf(cannonFire));
                        player.getShip().removeComponent(impactPosition);
                        System.out.println("Player " + player.getId().getNickname() + " has lost the component in position ("
                                + impactPosition.getRow()+ "," + impactPosition.getCol() + ")");
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
     * @return Returns {code @true} f at least one player has landed on a planet, {code @false} otherwise.
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
                    System.out.println(player.getId().getNickname() + " è atterrato su " + planet.getNumber());
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
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered){
            flightBoard.movePlayer(player, (int)player.getShip().getEngines(), true);
            if(player.getShip().getEngines() <= 0){
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
            if(player.getShip().getCannons() == card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannons() < card.getPowerLevel()){
                player.getShip().setCrew(player.getShip().getCrew()- card.getCrewLossAmount());
            }
            else if(player.getShip().getCannons() > card.getPowerLevel()){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
        }
        return true;
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
            double cannonStrength = player.getShip().getCannons();
            if(powerLevel == cannonStrength){
                continue;
            }
            else if (powerLevel < cannonStrength){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.getShip().addResources(card.getAvailableGoods());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
            else if(powerLevel > cannonStrength){
                System.out.println("Power level: " + powerLevel+ " cannon strength: " + cannonStrength);
                if(!(player.getShip().removeValuableResources(card.getGoodsLostIfDefeated()))){
                    throw new IllegalArgumentException("Not enough resources available!");
                }
            }
        }
        return true;
    }
    
    /**
     * Visits a CombatZoneCard.
     *
     * @param card The combat zone card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitCombatZoneCard(CombatZoneCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        if(flightBoard.getPlayerCount()==1){
            state.getAdventureDeck().drawNextCard();
            System.out.println("Skipping " +card.getType()+" card");
            return false;
        }

        for(CombatCheck check : card.getCombatChecks()){
            CombatAttributeType attributeType = check.getAttribute();
            Player combatLoser = check.getCombatLoser(playersOrdered);
            System.out.println("Combat loser: " + combatLoser.getId().getNickname());

            PenaltyType penalty = check.getPenaltyType();
            switch (penalty){
                case CREW_LOSS:
                    combatLoser.getShip().setCrew(combatLoser.getShip().getCrew() - check.getPenaltyValue());
                    System.out.println(combatLoser.getId().getNickname() + " has lost " + check.getPenaltyValue() + " crew members");
                    if(combatLoser.getShip().getCrew() == 0){
                        flightBoard.abandonPlayer(combatLoser); //controllo che andrebbe fatto direttamente in updateCrew
                    }
                    break;
                case FLIGHT_DAYS_LOSS:
                    flightBoard.movePlayer(combatLoser, check.getPenaltyValue(), false);
                    System.out.println(combatLoser.getId().getNickname() + " has lost " + check.getPenaltyValue() + " flight days");
                    break;
                case CANNON_FIRE:
                    for(CannonFire cannonFire : check.getCannonFires()){
                        Random dice1 = new Random();
                        Random dice2 = new Random();
                        int index1 = dice1.nextInt(6);
                        int index2 = dice2.nextInt(6);
                        int index = index1 + index2;

                        Component[][] board = combatLoser.getShip().getBoard();
                        if ( cannonFire.getApproach() == Direction.UP || cannonFire.getApproach() == Direction.DOWN) {
                            // For UP and DOWN directions, fixedIndex represents a column
                            if (index < 0 || index >= board[0].length) {
                                System.out.println("Column index out of bounds: " + index);
                                continue;
                            }
                        } else {
                            // For LEFT and RIGHT directions, fixedIndex represents a row
                            if (index < 0 || index >= board.length) {
                                System.out.println("Row index out of bounds: " + index);
                                continue;
                            }
                        }
                        Position impactPosition = combatLoser.getShip().findFirstComponent(cannonFire.getApproach(), index);
                        if(impactPosition==null){
                            System.out.println("Player " + combatLoser.getId().getNickname() + " has no component in the impact position");
                            continue;
                        }
                        Component impactComponent = combatLoser.getShip().getBoard()[impactPosition.getRow()][impactPosition.getCol()];

                        if(cannonFire.isBlockable() && combatLoser.getShip().protectedByShield(cannonFire.getApproach())) {
                            System.out.println(combatLoser.getId().getNickname()+ " has activated a shield against cannon fire number "
                                    +check.getCannonFires().indexOf(cannonFire));
                        } else {
                            System.out.println(combatLoser.getId().getNickname()+ " has no protection against cannon fire number "
                                    +check.getCannonFires().indexOf(cannonFire));
                            combatLoser.getShip().removeComponent(impactPosition);
                        }
                    }
                    break;
            }
        }
        return true;
    }


    /**
     * Visits an EpidemicCard.
     *
     * @param card The epidemic card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitEpidemicCard(EpidemicCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player : playersOrdered ) {
            int connectedCabin = 0;
            Ship ship = player.getShip();
            Component[][] board = player.getShip().getBoard();
            connectedCabin = ship.countAllAdjacentCabins();
            int crewUpdated = ship.getCrew() - connectedCabin;
            ship.setCrew(crewUpdated);
        }
        return true;
    }
        
    
    /**
     * Visits an AbandonedStationCard.
     *
     * @param card The abandoned station card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedStationCard(AbandonedStationCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for (Player player : playersOrdered) {
            if ((!card.isVisited()) && (player.getShip().getCrew() >= card.getMinCrewRequired())) {
                //se il giocatore sceglie di prendere le risorse e perdere giorni di volo
                card.setVisited();
                flightBoard.movePlayer(player, card.getLostDays(), false);
                player.getShip().addResources(card.getGoodQuantities());
                System.out.println(player.getId().getNickname() + " looted the abandoned station.");
                }
            }
         return true;
    }
}
package it.polimi.ingsw.model.domain.ship;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.Direction;
import org.controlsfx.control.spreadsheet.Grid;

import static it.polimi.ingsw.model.enums.ship.ComponentType.BATTERY;

public class Ship {
    private Grid <T> grid;
    private ShipBoardLayout layout;
    private List<Component> components;
    private List<Component> reservedComponents;
    private Map<GoodType, Integer> resources;
    private int lostComponents;

    // Diego
    private int cannons;
    private int engines;
    private int batteries;
    private int crew;

    public Grid<T> getGrid() {return this.grid;}
    public ShipBoardLayout getLayout() {return this.layout;}
    public List<Component> getComponents() {return this.components;}
    public List<Component> getReservedComponents() {return this.reservedComponents;}
    public int getLostComponents() {return this.lostComponents;}
    public Map<GoodType, Integer> getResources() {return this.resources;}


    public void setCannons(int cannons) {this.cannons = cannons;}
    public int getCannons() {return this.cannons;}
    public void setEngines(int engines) {this.engines = engines;}
    public int getEngines() {return this.engines;}
    public void setBatteries(int batteries) {this.batteries = batteries;}
    public int getBatteries() {return this.batteries;}
    public void setCrew(int crew) {this.crew = crew;}
    public int getCrew() {return this.crew;}
    // Diego

    public Ship(ShipBoardLayout layout, Grid<T> grid) {
        this.grid = grid;
        this.layout = layout;
    }

    /**
     * Adds a component to the ship at the specified position on the grid.
     * @param component The component to add.
     * @param position  The position on the grid where the component should be placed.
     */
    public void addComponent(Component component, Position position) {
        this.components.add(component);
    }

    /**
     * Removes the component from the ship at the specified position on the grid.
     * @param position The position on the grid from which the component should be removed.
     */
    public void removeComponent(Position position) {
        this.components.remove(grid.getGrid().get(position));
    }

    /**
     * Reserves a component for future use, allowing it to be kept aside without attaching it to the ship.
     * If there are already 2 reserved components, the first one is removed to make room for the new component.
     *
     * @param component The component to reserve.
     */
    public void reserveComponent(Component component) {
        if (this.reservedComponents.size() >= 2) {
            this.reservedComponents.remove(0);
        }
        this.reservedComponents.add(component);
    }

    /**
     * Calculates and returns the total engine strength of the ship.
     * @return The engine strength.
     */
    public double getEngineStrength() {}

    /**
     * Calculates and returns the total cannon strength of the ship.
     * @return The cannon strength.
     */
    public double getCannonStrength() {}

    /**
     * Returns the number of crew members assigned to the ship.
     * @return The crew number.
     */
    public int getCrewNumber() {}

    /**
     * Calculates the total capacity of standard cargo holds (CARGO_HOLD).
     * @return The total capacity of all standard cargo holds in the ship.
     */
    public int getCargoHoldCapacity(){
        int capacity =0;
        for (Component component : this.components) {
            if(component.getType()==ComponentType.CARGO_HOLD){
                capacity += component.count();
            }
        }
        return capacity;
    }

    /**
     * Calculates the total capacity of special cargo holds (CARGO_HOLD_SPECIAL).
     * @return The total capacity of all special cargo holds in the ship.
     */
    public int getCargoHoldSpecialCapacity(){
        int capacity =0;
        for (Component component : this.components) {
            if(component.getType()==ComponentType.CARGO_HOLD_SPECIAL){
                capacity += component.count();
            }
        }
        return capacity;
    }

    /**
     * Adds resources to the ship's cargo holds, following these rules:
     * 1. If there is enough free space in the cargo holds, the resources are added.
     * 2. If the total cargo capacity is sufficient but some space is occupied,
     *    old resources are removed (as needed) to make space for the new ones.
     * 3. If there is not enough space, the addition fails.
     *
     * @param newResources A map containing the resources (GoodType) and their respective quantities to add.
     * @return {@code true} if resources were successfully added, {@code false} if there was insufficient space.
     */
    public boolean addResources(Map<GoodType, Integer> newResources) {
        int totalNormalCapacity = getCargoHoldCapacity();
        int totalSpecialCapacity = getCargoHoldSpecialCapacity();

        // Calculate the currently occupied space
        int occupiedNormal = 0;
        int occupiedSpecial = 0;

        for (Map.Entry<GoodType, Integer> entry : resources.entrySet()) {
            if (entry.getKey() == GoodType.RED) {
                occupiedSpecial += entry.getValue();
            } else {
                occupiedNormal += entry.getValue();
            }
        }

        // Available space in cargo holds
        int freeNormal = totalNormalCapacity - occupiedNormal;
        int freeSpecial = totalSpecialCapacity - occupiedSpecial;

        // Adding new resources
        for (Map.Entry<GoodType, Integer> entry : newResources.entrySet()) {
            GoodType type = entry.getKey();
            int amount = entry.getValue();

            if (type == GoodType.RED) {
                if (freeSpecial >= amount) {
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeSpecial -= amount;
                } else {
                    return false; // Not enough space for special goods
                }
            } else {
                if (freeNormal >= amount) {
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeNormal -= amount;
                } else if (totalSpecialCapacity - occupiedSpecial >= amount) {
                    // If no space in normal cargo, try special cargo
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeSpecial -= amount;
                } else {
                    return false; // Not enough space for normal goods
                }
            }
        }

        return true; // Resources added successfully
    }

    public boolean removeValuableResources(int deletingNumber) {
        int totalNormalCapacity = getCargoHoldCapacity();
        int totalSpecialCapacity = getCargoHoldSpecialCapacity();

        // Calculate the currently occupied space
        int occupiedNormal = 0;
        int occupiedSpecial = 0;

        for (Map.Entry<GoodType, Integer> entry : resources.entrySet()) {
            if (entry.getKey() == GoodType.RED) {
                occupiedSpecial += entry.getValue();
            } else {
                occupiedNormal += entry.getValue();
            }
        }

        //elimino prima tutte le merci rosse
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.RED)) {
                while (occupiedSpecial > 0 && deletingNumber > 0 && resources.get(GoodType.RED) > 0) {
                    this.resources.put(GoodType.RED, resources.get(GoodType.RED) - 1);
                    deletingNumber--;
                    occupiedNormal--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.RED) == 0) {
                        this.resources.remove(GoodType.RED);
                    }
                }
            }
        }
        //Elimino altre merci in ordine decrescente di valore (BLUE, GREEN, YELLOW)
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.BLUE)) {
                while (occupiedNormal > 0 && deletingNumber > 0 && resources.get(GoodType.BLUE) > 0) {
                    this.resources.put(GoodType.BLUE, resources.get(GoodType.BLUE) - 1);
                    deletingNumber--;
                    occupiedNormal--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.BLUE) == 0) {
                        this.resources.remove(GoodType.BLUE);
                    }
                }
            }
        }

        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.GREEN)) {
                while (occupiedNormal > 0 && deletingNumber > 0 && resources.get(GoodType.GREEN) > 0) {
                    this.resources.put(GoodType.GREEN, resources.get(GoodType.GREEN) - 1);
                    deletingNumber--;
                    occupiedNormal--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.GREEN) == 0) {
                        this.resources.remove(GoodType.GREEN);
                    }
                }
            }
        }

        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.YELLOW)) {
                while (occupiedNormal > 0 && deletingNumber > 0 && resources.get(GoodType.YELLOW) > 0) {
                    this.resources.put(GoodType.YELLOW, resources.get(GoodType.YELLOW) - 1);
                    deletingNumber--;
                    occupiedNormal--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.YELLOW) == 0) {
                        this.resources.remove(GoodType.YELLOW);
                    }
                }
            }
        }

        if (deletingNumber > 0) {
            for(Component component : this.components){
                if(component.getType()==BATTERY){
                    while(component.getCurrentBatteries()>0 && deletingNumber>0){
                        component.setCurrentBatteries(component.getCurrentBatteries()-1);
                        deletingNumber--;
                    }
                }
            }
        }
        if (deletingNumber > 0){
            return false;
        }
        return true;
    }
}

package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.controller.ClientController;

/**
 * Ship building service simplified for Simple Direct Model Architecture.
 * NOTE: Most functionality moved to direct UI-UIContext interaction.
 */
public class ShipBuildingService {
    private final ClientController controller;
    
    public ShipBuildingService(ClientController controller) {
        this.controller = controller;
    }
    
    // NOTE: This class is largely deprecated in favor of direct UIContext usage.
    // Kept for backward compatibility during migration.
}
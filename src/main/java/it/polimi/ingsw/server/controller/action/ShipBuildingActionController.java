// File: it.polimi.ingsw.server.controller.action.ShipBuildingActionController.java
// VERSION: More complete implementation using assumed GameModel methods and result objects
package it.polimi.ingsw.server.controller.action;

import it.polimi.ingsw.common.dto.*; // All DTOs
import it.polimi.ingsw.common.message.building.*;
import it.polimi.ingsw.server.controller.CommandContext;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.event.*;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.Optional;
import java.util.logging.Logger;

public class ShipBuildingActionController {
    private static final Logger LOGGER = Logger.getLogger(ShipBuildingActionController.class.getName());

    public ShipBuildingActionController() {
        LOGGER.info("ShipBuildingActionController initialized.");
    }

}
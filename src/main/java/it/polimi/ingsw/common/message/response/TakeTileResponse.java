package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.common.ComponentData;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import java.util.UUID;

/**
 * Response to a TakeTileRequest, containing the complete information about the drawn tile.
 */
public class TakeTileResponse extends AbstractResponse {
    private final ComponentData componentData;

    public TakeTileResponse(UUID correlationId, ComponentData componentData) {
        super(correlationId);
        this.componentData = componentData;
    }


    public ComponentData getComponentData() {
        return componentData;
    }


    @Override
    public void handleOnClient(ClientContext context) {
        // Create ComponentInstance from complete server data
        ComponentInstance componentInstance;
        if (componentData.getConnectors() != null) {
            // Full component data with connectors
            componentInstance = new ComponentInstance(
                componentData.getId(), 
                componentData.getType(), 
                componentData.getConnectors()
            );
            componentInstance.setDirection(componentData.getDefaultDirection());

            LocalGameState.getInstance().addHeldTile(componentInstance);
        }
        //TODO: check better handling because to remove error i moved LocalGameState.getistance ... into the if body
        // But it is temporary
        context.getModel().firePropertyChange("heldTiles", null, null);
        context.showNotification("Tile Drawn", "You drew a " + componentData.getType().name(),
                it.polimi.ingsw.client.ui.NotificationType.INFO);
    }
}
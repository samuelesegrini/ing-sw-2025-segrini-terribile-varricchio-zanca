package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import java.util.UUID;

/**
 * Response to a TakeTileRequest, containing the information about the drawn tile.
 */
public class TakeTileResponse extends AbstractResponse {
    private final String tileId;
    private final ComponentType tileType;

    public TakeTileResponse(UUID correlationId, String tileId, ComponentType tileType) {
        super(correlationId);
        this.tileId = tileId;
        this.tileType = tileType;
    }

    public String getTileId() {
        return tileId;
    }

    public ComponentType getTileType() {
        return tileType;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        LocalGameState.getInstance().addHeldTile(tileType);
        context.getModel().firePropertyChange("heldTiles", null, null);
        context.showNotification("Tile Drawn", "You drew a " + tileType.name(),
                it.polimi.ingsw.client.ui.NotificationType.INFO);
    }
}
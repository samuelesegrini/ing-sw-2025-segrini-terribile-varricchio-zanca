package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.UIContext;
import it.polimi.ingsw.client.core.UIRefreshable;

import java.beans.PropertyChangeEvent;

/**
 * Simplified TUI Ship Building view for Simple Direct Model Architecture.
 * NOTE: This is a simplified stub - full TUI implementation pending.
 */
public class TuiShipBuildingView extends BaseUIView implements UIRefreshable {
    private ClientController controller;
    private UIContext uiContext;

    public TuiShipBuildingView(ClientController controller, UIContext uiContext) {
        this.controller = controller;
        this.uiContext = uiContext;
    }

    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Ship Building (TUI)";
    }

    @Override
    protected void onShow() {
        System.out.println("=== SHIP BUILDING PHASE (TUI) ===");
        System.out.println("NOTE: Full TUI implementation pending.");
        System.out.println("Use GUI version for complete functionality.");
        refresh();
    }

    @Override
    protected void onHide() {
        // TUI cleanup
    }

    @Override
    protected void onRefresh() {
        refresh();
    }

    @Override
    public void refresh() {
        if (uiContext != null && uiContext.getClientState() != null && uiContext.getClientState().isInGame()) {
            System.out.println("[TUI] Refreshing ship building display...");
            displayBasicInfo();
        }
    }

    private void displayBasicInfo() {
        var ship = uiContext.getClientState().getLocalPlayerShip();
        var deck = uiContext.getClientState().getComponentDeck();
        
        if (ship != null) {
            System.out.println("Ship: " + ship.getRows() + "x" + ship.getCols() + " grid");
        }
        
        if (deck != null) {
            System.out.println("Available components: " + deck.getAvailableComponents().size());
        }
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        refresh();
    }
}
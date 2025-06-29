package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.core.UIView;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.server.model.enums.GamePhase;

public class TuiGameView implements UIView {

    private final ClientController controller;
    private final ClientState clientState;
    private final TuiContext context;

    private UIView currentPhaseView;

    public TuiGameView(ClientController controller, TuiContext context) {
        this.controller = controller;
        this.clientState = controller.getClientState();
        this.context = context;
    }

    @Override
    public void refresh() {
        GamePhase currentPhase = clientState.getCurrentPhase();
        UIView newPhaseView = switch (currentPhase) {
            case BUILDING -> new TuiShipBuildingView(controller, context);
            case FLIGHT -> new TuiFlightView(controller, context);
            default -> null;
        };

        if (currentPhaseView == null || !currentPhaseView.getClass().equals(newPhaseView.getClass())) {
            currentPhaseView = newPhaseView;
        }

        if (currentPhaseView != null) {
            currentPhaseView.refresh();
        }
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.BUILDING;
    }

    @Override
    public String getTitle() {
        return "Game View";
    }

    @Override
    public void initialize(UIContext context) {
        // No specific initialization needed here as sub-views are initialized on refresh
    }

    @Override
    public void show() {
        // Show the current phase view if it exists
        if (currentPhaseView != null) {
            currentPhaseView.show();
        }
    }

    @Override
    public void hide() {
        // Hide the current phase view if it exists
        if (currentPhaseView != null) {
            currentPhaseView.hide();
        }
    }

    @Override
    public boolean isActive() {
        // Active if any sub-view is active
        return currentPhaseView != null && currentPhaseView.isActive();
    }

    @Override
    public void dispose() {
        // Dispose the current phase view if it exists
        if (currentPhaseView != null) {
            currentPhaseView.dispose();
        }
    }
}
package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.AbstractUIContext;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.core.ViewNavigatorImpl;
import it.polimi.ingsw.client.ui.core.UIThreadService;
import javafx.stage.Stage;

/**
 * GUI-specific implementation of UIContext.
 * Provides JavaFX-specific services and components.
 */
public class GuiContext extends AbstractUIContext {
    
    private final Stage primaryStage;
    
    public GuiContext(ClientController controller, Stage primaryStage) {
        super(controller);
        this.primaryStage = primaryStage;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    @Override
    protected NotificationService createNotificationService() {
        return new NotificationManager();
    }
    
    @Override
    protected ViewNavigator createViewNavigator() {
        return new ViewNavigatorImpl(clientState);
    }
    
    @Override
    protected UIThreadService createThreadService() {
        return new GuiThreadService();
    }
}
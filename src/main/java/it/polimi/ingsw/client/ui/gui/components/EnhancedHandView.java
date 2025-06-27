package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import it.polimi.ingsw.client.ui.core.UIView;
import it.polimi.ingsw.client.core.ClientState;

/**
 * Hand UI component using Simple Direct Model Architecture.
 */
public class EnhancedHandView extends VBox implements UIView {
    
    public interface HandActionHandler {
        void onComponentSelected(Component component);
        void onRotateComponent();
        void onReturnComponent();
        void onReserveComponent();
        void onPlaceComponent();
    }
    
    private final HandActionHandler actionHandler;
    private UIContext uiContext; // Changed to non-final for initialize method
    
    // UI Components
    private Label handTitleLabel;
    private StackPane handSlotArea;
    private ComponentTileView currentComponentView;
    private HBox actionButtonsArea;
    private Label instructionsLabel;
    private VBox componentDetailsArea;
    
    // Action buttons
    private Button rotateButton;
    private Button returnButton;
    private Button reserveButton;
    private Button placeButton;
    
    // State
    private Component currentComponent;
    private boolean isEmpty = true;
    private boolean active = false; // Added for UIView
    
    // Animations
    private Timeline pulseAnimation;
    private FadeTransition componentFadeIn;

    public EnhancedHandView(UIContext uiContext, HandActionHandler actionHandler) {
        this.uiContext = uiContext;
        this.actionHandler = actionHandler;
        initializeView();
        setupHandSlot();
        setupActionButtons();
        setupComponentDetails();
        setupAnimations();
        updateEmptyState();
    }

    private void initializeView() {
        getStyleClass().add("enhanced-hand-view");
        setSpacing(10);
        setPadding(new Insets(15));
        setAlignment(Pos.CENTER);
        setPrefWidth(280);
        setMaxWidth(280);
        
        // Simple background
        setBackground(new Background(new BackgroundFill(
            Color.rgb(40, 40, 50, 0.9), 
            new CornerRadii(8), 
            null
        )));
        
        // Simple border
        setBorder(new Border(new BorderStroke(
            Color.rgb(80, 80, 100, 0.5),
            BorderStrokeStyle.SOLID,
            new CornerRadii(8),
            new BorderWidths(1)
        )));
    }

    private void setupHandSlot() {
        // Simple title
        handTitleLabel = new Label("Hand");
        handTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        // Component slot area
        handSlotArea = new StackPane();
        handSlotArea.setPrefSize(80, 80);
        handSlotArea.setMaxSize(80, 80);
        handSlotArea.setMinSize(80, 80);
        handSlotArea.getStyleClass().add("hand-slot-empty");
        
        // Simple slot background
        Rectangle slotBackground = new Rectangle(80, 80);
        slotBackground.setFill(Color.TRANSPARENT);
        slotBackground.setStroke(Color.rgb(120, 120, 140, 0.6));
        slotBackground.setStrokeWidth(1);
        slotBackground.setArcWidth(5);
        slotBackground.setArcHeight(5);
        handSlotArea.getChildren().add(slotBackground);
        
        // Simple empty slot label
        Label emptySlotLabel = new Label("Empty");
        emptySlotLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");
        handSlotArea.getChildren().add(emptySlotLabel);
        
        // Click handler for slot
        handSlotArea.setOnMouseClicked(e -> {
            if (currentComponent != null && actionHandler != null) {
                actionHandler.onComponentSelected(currentComponent);
            }
        });
        
        getChildren().addAll(handTitleLabel, handSlotArea);
    }

    private void setupActionButtons() {
        actionButtonsArea = new HBox(5);
        actionButtonsArea.setAlignment(Pos.CENTER);
        
        // Simple button style
        rotateButton = new Button("Rotate");
        styleActionButton(rotateButton);
        rotateButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onRotateComponent();
            }
        });
        
        returnButton = new Button("Return");
        styleActionButton(returnButton);
        returnButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onReturnComponent();
            }
        });
        
        reserveButton = new Button("Reserve");
        styleActionButton(reserveButton);
        reserveButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onReserveComponent();
            }
        });
        
        placeButton = new Button("Place");
        styleActionButton(placeButton);
        placeButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onPlaceComponent();
            }
        });
        
        actionButtonsArea.getChildren().addAll(rotateButton, returnButton, reserveButton, placeButton);
        getChildren().add(actionButtonsArea);
    }

    private void styleActionButton(Button button) {
        button.setPrefWidth(60);
        button.setPrefHeight(25);
        button.setStyle(
            "-fx-background-color: #555; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 9px; " +
            "-fx-background-radius: 3; " +
            "-fx-border-radius: 3;"
        );
    }

    private void setupComponentDetails() {
        componentDetailsArea = new VBox(5);
        componentDetailsArea.setAlignment(Pos.CENTER);
        componentDetailsArea.setPadding(new Insets(5));
        
        // Simple instructions
        instructionsLabel = new Label("Select component to start");
        instructionsLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 9px;");
        instructionsLabel.setWrapText(true);
        instructionsLabel.setMaxWidth(250);
        
        componentDetailsArea.getChildren().add(instructionsLabel);
        getChildren().add(componentDetailsArea);
    }

    private void setupAnimations() {
        // Simple fade in animation for components
        componentFadeIn = new FadeTransition(Duration.millis(200));
        componentFadeIn.setFromValue(0.0);
        componentFadeIn.setToValue(1.0);
    }

    public void updateComponent(Component component) {
        System.out.println(String.format("[DEBUG] EnhancedHandView.updateComponent: %s", 
            component != null ? component.getId() : "null"));
        
        this.currentComponent = component;
        this.isEmpty = (component == null);
        
        if (isEmpty) {
            System.out.println("[DEBUG] EnhancedHandView: Updating to empty state");
            updateEmptyState();
        } else {
            System.out.println(String.format("[DEBUG] EnhancedHandView: Updating to component state: %s", component.getId()));
            updateComponentState();
        }
    }

    private void updateEmptyState() {
        // Clear component view
        handSlotArea.getChildren().removeIf(node -> node instanceof ComponentTileView);
        
        // Update slot styling
        handSlotArea.getStyleClass().removeAll("hand-slot-filled");
        handSlotArea.getStyleClass().add("hand-slot-empty");
        
        // Show empty slot styling
        handSlotArea.getChildren().stream()
            .filter(node -> node instanceof Rectangle)
            .forEach(node -> {
                Rectangle rect = (Rectangle) node;
                rect.setVisible(true);
                rect.setStroke(Color.rgb(120, 120, 140, 0.6));
            });
        
        // Show empty label
        handSlotArea.getChildren().stream()
            .filter(node -> node instanceof Label)
            .forEach(node -> node.setVisible(true));
        
        // Update title
        handTitleLabel.setText("Hand (Empty)");
        handTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ccc;");
        
        // Disable action buttons
        rotateButton.setDisable(true);
        returnButton.setDisable(true);
        reserveButton.setDisable(true);
        placeButton.setDisable(true);
        
        // Update instructions
        instructionsLabel.setText("Select component to start");
    }

    private void updateComponentState() {
        // Update slot styling
        handSlotArea.getStyleClass().removeAll("hand-slot-empty");
        handSlotArea.getStyleClass().add("hand-slot-filled");
        
        // Hide empty slot elements
        handSlotArea.getChildren().stream()
            .filter(node -> node instanceof Rectangle || (node instanceof Label && !(node instanceof ComponentTileView)))
            .forEach(node -> node.setVisible(false));
        
        // Remove old component view
        handSlotArea.getChildren().removeIf(node -> node instanceof ComponentTileView);
        
        // Create new component view
        currentComponentView = new ComponentTileView(currentComponent);
        currentComponentView.setSelected(true);
        currentComponentView.getStyleClass().add("in-hand");
        
        handSlotArea.getChildren().add(currentComponentView);
        
        // Animate component appearance
        currentComponentView.setOpacity(0);
        componentFadeIn.setNode(currentComponentView);
        componentFadeIn.play();
        
        // Update title
        handTitleLabel.setText("Hand (Ready)");
        handTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9f9;");
        
        // Enable action buttons
        rotateButton.setDisable(false);
        returnButton.setDisable(false);
        reserveButton.setDisable(false);
        placeButton.setDisable(false);
        
        // Update instructions and details
        updateComponentDetails();
    }

    private void updateComponentDetails() {
        if (currentComponent == null) return;
        
        componentDetailsArea.getChildren().clear();
        
        // Simple component name
        Label nameLabel = new Label(currentComponent.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 10px;");
        
        // Simple instructions
        Label newInstructions = new Label("Click grid to place");
        newInstructions.setStyle("-fx-text-fill: #aaa; -fx-font-size: 9px;");
        newInstructions.setWrapText(true);
        newInstructions.setMaxWidth(250);
        
        componentDetailsArea.getChildren().addAll(nameLabel, newInstructions);
    }


    public Component getCurrentComponent() {
        return currentComponent;
    }

    public boolean isEmpty() {
        return isEmpty;
    }
    
    public void clearSelection() {
        if (currentComponentView != null) {
            currentComponentView.setSelected(false);
        }
    }
    
    public void setComponentSelected(boolean selected) {
        if (currentComponentView != null) {
            currentComponentView.setSelected(selected);
        }
    }
    
    @Override
    public void refresh() {
        if (uiContext.getClientState().isInGame()) {
            var localPlayer = uiContext.getClientState().getLocalPlayer();
            if (localPlayer != null) {
                var heldComponents = localPlayer.getHeldComponents();
                System.out.println("[TAKETILE DEBUG] EnhancedHandView.refresh() - Player has " + heldComponents.size() + " held components");
                
                updateComponent(heldComponents.isEmpty() ? null : heldComponents.get(0));
            } else {
                System.out.println("[TAKETILE DEBUG] EnhancedHandView.refresh() - No local player found");
            }
        } else {
            System.out.println("[TAKETILE DEBUG] EnhancedHandView.refresh() - Not in game");
        }
    }

    @Override
    public ClientState.ViewState getViewState() {
        // This view is part of the GAME view, so it returns GAME
        return ClientState.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Enhanced Hand View";
    }

    @Override
    public void initialize(UIContext context) {
        // This view is initialized via its constructor, but we can register it here
        // if it needs to be part of the refreshable views in ClientState.
        // If it's a sub-component managed by a parent UIView, it might not need to register itself.
        // For now, assuming it's managed by a parent view that calls its refresh().
        // If it needs to be directly refreshed by ClientState, uncomment the line below:
        // context.getClientState().registerRefreshableView(this);
        this.uiContext = context; // Ensure context is set if initialize is called later
    }

    @Override
    public void show() {
        this.setVisible(true);
        this.active = true;
        refresh(); // Refresh when shown
    }

    @Override
    public void hide() {
        this.setVisible(false);
        this.active = false;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void dispose() {
        // Clean up resources if any
    }
}

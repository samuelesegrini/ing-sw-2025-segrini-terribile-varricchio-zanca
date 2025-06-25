package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.client.core.UIRefreshable;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.client.ui.UIContext;
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

/**
 * Hand UI component using Simple Direct Model Architecture.
 */
public class EnhancedHandView extends VBox implements UIRefreshable {
    
    public interface HandActionHandler {
        void onComponentSelected(Component component);
        void onRotateComponent();
        void onReturnComponent();
        void onReserveComponent();
        void onPlaceComponent();
    }
    
    private final HandActionHandler actionHandler;
    private final UIContext uiContext;
    
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
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        setPrefWidth(350);
        setMaxWidth(350);
        
        // Modern gradient background
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, null,
            new Stop(0, Color.rgb(45, 45, 55, 0.95)),
            new Stop(1, Color.rgb(25, 25, 35, 0.95))
        );
        setBackground(new Background(new BackgroundFill(gradient, new CornerRadii(15), null)));
        
        // Subtle border
        setBorder(new Border(new BorderStroke(
            Color.rgb(100, 100, 120, 0.3),
            BorderStrokeStyle.SOLID,
            new CornerRadii(15),
            new BorderWidths(1)
        )));
        
        // Drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        shadow.setOffsetY(5);
        setEffect(shadow);
    }

    private void setupHandSlot() {
        // Title with modern styling
        handTitleLabel = new Label("🎯 Component Hand");
        handTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");
        
        // Component slot area
        handSlotArea = new StackPane();
        handSlotArea.setPrefSize(120, 120);
        handSlotArea.setMaxSize(120, 120);
        handSlotArea.setMinSize(120, 120);
        handSlotArea.getStyleClass().add("hand-slot-empty");
        
        // Slot background with dashed border when empty
        Rectangle slotBackground = new Rectangle(120, 120);
        slotBackground.setFill(Color.TRANSPARENT);
        slotBackground.setStroke(Color.rgb(150, 150, 180, 0.5));
        slotBackground.setStrokeWidth(2);
        slotBackground.getStrokeDashArray().addAll(10.0, 5.0);
        slotBackground.setArcWidth(10);
        slotBackground.setArcHeight(10);
        handSlotArea.getChildren().add(slotBackground);
        
        // Empty slot label
        Label emptySlotLabel = new Label("Drop Component Here");
        emptySlotLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-font-style: italic;");
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
        actionButtonsArea = new HBox(8);
        actionButtonsArea.setAlignment(Pos.CENTER);
        
        // Rotate button with icon
        rotateButton = new Button("🔄 Rotate");
        styleActionButton(rotateButton, "#4CAF50");
        rotateButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onRotateComponent();
                animateRotation();
            }
        });
        
        // Return button with icon
        returnButton = new Button("↩️ Return");
        styleActionButton(returnButton, "#FF9800");
        returnButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onReturnComponent();
                animateReturn();
            }
        });
        
        // Reserve button with icon
        reserveButton = new Button("📦 Reserve");
        styleActionButton(reserveButton, "#9C27B0");
        reserveButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onReserveComponent();
            }
        });
        
        // Place button with icon
        placeButton = new Button("🎯 Place");
        styleActionButton(placeButton, "#2196F3");
        placeButton.setOnAction(e -> {
            if (actionHandler != null) {
                actionHandler.onPlaceComponent();
            }
        });
        
        actionButtonsArea.getChildren().addAll(rotateButton, returnButton, reserveButton, placeButton);
        getChildren().add(actionButtonsArea);
    }

    private void styleActionButton(Button button, String baseColor) {
        button.getStyleClass().addAll("enhanced-hand-button");
        button.setPrefWidth(80);
        button.setPrefHeight(35);
        button.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 10px; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 2);",
            baseColor
        ));
        
        // Hover effect
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        // Add tooltip
        Tooltip tooltip = new Tooltip();
        switch (button.getText()) {
            case "🔄 Rotate" -> tooltip.setText("Rotate component clockwise");
            case "↩️ Return" -> tooltip.setText("Return component to warehouse");
            case "📦 Reserve" -> tooltip.setText("Reserve component for later use");
            case "🎯 Place" -> tooltip.setText("Click grid to place component");
        }
        button.setTooltip(tooltip);
    }

    private void setupComponentDetails() {
        componentDetailsArea = new VBox(8);
        componentDetailsArea.setAlignment(Pos.CENTER);
        componentDetailsArea.setPadding(new Insets(10));
        componentDetailsArea.setStyle(
            "-fx-background-color: rgba(255,255,255,0.1); " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: rgba(255,255,255,0.2); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 10;"
        );
        
        // Instructions
        instructionsLabel = new Label("Select a component from the warehouse to begin building your ship.");
        instructionsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 11px; -fx-text-alignment: center;");
        instructionsLabel.setWrapText(true);
        instructionsLabel.setMaxWidth(300);
        
        componentDetailsArea.getChildren().add(instructionsLabel);
        getChildren().add(componentDetailsArea);
    }

    private void setupAnimations() {
        // Pulse animation for empty slot
        pulseAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(handSlotArea.scaleXProperty(), 1.0)),
            new KeyFrame(Duration.seconds(1), new KeyValue(handSlotArea.scaleXProperty(), 1.05)),
            new KeyFrame(Duration.seconds(2), new KeyValue(handSlotArea.scaleXProperty(), 1.0))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        
        // Fade in animation for components
        componentFadeIn = new FadeTransition(Duration.millis(300));
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
                rect.setStroke(Color.rgb(150, 150, 180, 0.5));
            });
        
        // Show empty label
        handSlotArea.getChildren().stream()
            .filter(node -> node instanceof Label)
            .forEach(node -> node.setVisible(true));
        
        // Update title
        handTitleLabel.setText("🎯 Component Hand (Empty)");
        handTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #cccccc; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");
        
        // Disable action buttons
        rotateButton.setDisable(true);
        returnButton.setDisable(true);
        reserveButton.setDisable(true);
        placeButton.setDisable(true);
        
        // Update instructions
        instructionsLabel.setText("Select a component from the warehouse to begin building your ship.");
        
        // Start pulse animation
        pulseAnimation.play();
    }

    private void updateComponentState() {
        // Stop pulse animation
        pulseAnimation.stop();
        handSlotArea.setScaleX(1.0);
        handSlotArea.setScaleY(1.0);
        
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
        
        // Scale up the component for better visibility
        currentComponentView.setScaleX(1.3);
        currentComponentView.setScaleY(1.3);
        
        // Add hover effect
        currentComponentView.setOnMouseEntered(e -> {
            currentComponentView.setScaleX(1.4);
            currentComponentView.setScaleY(1.4);
        });
        currentComponentView.setOnMouseExited(e -> {
            currentComponentView.setScaleX(1.3);
            currentComponentView.setScaleY(1.3);
        });
        
        handSlotArea.getChildren().add(currentComponentView);
        
        // Animate component appearance
        currentComponentView.setOpacity(0);
        componentFadeIn.setNode(currentComponentView);
        componentFadeIn.play();
        
        // Update title
        handTitleLabel.setText("🎯 Component Hand (Ready)");
        handTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");
        
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
        
        // Component name
        Label nameLabel = new Label(currentComponent.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Component type and direction
        Label typeLabel = new Label(String.format("Type: %s | Direction: %s", 
            currentComponent.getType().name(), currentComponent.getCurrentDirection().name()));
        typeLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");
        
        // Instructions
        Label newInstructions = new Label("Click on the ship grid to place this component. Use the buttons below to rotate or return it.");
        newInstructions.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px; -fx-text-alignment: center;");
        newInstructions.setWrapText(true);
        newInstructions.setMaxWidth(300);
        
        componentDetailsArea.getChildren().addAll(nameLabel, typeLabel, newInstructions);
    }

    private void animateRotation() {
        if (currentComponentView != null) {
            RotateTransition rotation = new RotateTransition(Duration.millis(300), currentComponentView);
            rotation.setByAngle(90);
            rotation.setOnFinished(e -> {
                currentComponentView.setRotate(0); // Reset rotation for next animation
                currentComponentView.refreshAfterRotation();
                updateComponentDetails();
            });
            rotation.play();
        }
    }

    private void animateReturn() {
        if (currentComponentView != null) {
            // Animate component flying away
            TranslateTransition translate = new TranslateTransition(Duration.millis(400), currentComponentView);
            translate.setToX(-200);
            translate.setToY(-100);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), currentComponentView);
            fade.setToValue(0);
            
            ParallelTransition parallel = new ParallelTransition(translate, fade);
            parallel.setOnFinished(e -> updateComponent(null));
            parallel.play();
        }
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
                
                updateComponent(heldComponents.isEmpty() ? null : heldComponents.get(0));
            }
        }
    }
    
}
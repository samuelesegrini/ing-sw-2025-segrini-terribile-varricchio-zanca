package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.client.core.state.ComponentInstance;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Physical-like junkyard pile view inspired by UITest implementation.
 * Creates a messy, scattered pile with both face-up and face-down components
 * that mimics real physical game behavior.
 * NO ComponentType support - ComponentInstance only.
 */
public class ComponentJunkyardView extends VBox {
    
    public interface JunkyardClickHandler {
        void onFaceDownTileClicked();
        void onComponentClicked(ComponentInstance component);
    }
    
    private final JunkyardClickHandler clickHandler;
    private final Random random = new Random();
    
    // Main pile area
    private Label titleLabel;
    private ScatteredPileView pileView;
    private Label instructionLabel;
    
    // Component data
    private List<ComponentInstance> availableComponents = new ArrayList<>();
    private List<ComponentInstance> faceUpComponents = new ArrayList<>();
    private int faceDownCount = 0;
    private boolean populated = false;

    public ComponentJunkyardView(JunkyardClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        initializeView();
        setupPileSection();
    }

    private void initializeView() {
        getStyleClass().add("component-junkyard");
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);
    }

    private void setupPileSection() {
        // Title
        titleLabel = new Label("Component Warehouse");
        titleLabel.getStyleClass().add("junkyard-title");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Pile view - this is where the magic happens
        pileView = new ScatteredPileView();
        pileView.getStyleClass().add("scattered-pile");
        pileView.setPrefSize(400, 300);
        pileView.setMinSize(300, 200);
        pileView.setStyle("-fx-border-color: #666; -fx-border-width: 2; -fx-border-radius: 10; " +
                         "-fx-background-color: rgba(50, 50, 50, 0.8); -fx-background-radius: 10;");
        
        // Instructions
        instructionLabel = new Label("Click any component to take it");
        instructionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-font-style: italic;");
        
        getChildren().addAll(titleLabel, pileView, instructionLabel);
        VBox.setVgrow(pileView, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Scattered pile view that mimics physical component placement
     */
    private class ScatteredPileView extends Pane {
        
        public ScatteredPileView() {
            // Listen for size changes to repopulate when container is ready
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > ComponentTileView.TILE_SIZE) {
                    checkAndRepopulateIfNeeded();
                }
            });
            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > ComponentTileView.TILE_SIZE) {
                    checkAndRepopulateIfNeeded();
                }
            });
        }
        
        private void checkAndRepopulateIfNeeded() {
            Platform.runLater(() -> {
                if (getWidth() > ComponentTileView.TILE_SIZE && getHeight() > ComponentTileView.TILE_SIZE) {
                    if (!populated || (getChildren().isEmpty() && !availableComponents.isEmpty())) {
                        repopulatePile();
                        populated = true;
                    }
                }
            });
        }
        
        public void repopulatePile() {
            getChildren().clear();
            
            double areaWidth = getWidth();
            double areaHeight = getHeight();
            double paddingHorizontal = getPadding().getLeft() + getPadding().getRight();
            double paddingVertical = getPadding().getTop() + getPadding().getBottom();
            double contentWidth = areaWidth - paddingHorizontal - ComponentTileView.TILE_SIZE;
            double contentHeight = areaHeight - paddingVertical - ComponentTileView.TILE_SIZE;
            double offsetX = getPadding().getLeft();
            double offsetY = getPadding().getTop();
            
            if (contentWidth <= 0 || contentHeight <= 0) {
                return;
            }
            
            // Create shuffled list for scattered placement
            List<ComponentInstance> componentsToPlace = new ArrayList<>(availableComponents);
            Collections.shuffle(componentsToPlace, random);
            
            int placedCount = 0;
            for (ComponentInstance component : componentsToPlace) {
                ComponentTileView tileView = new ComponentTileView(component);
                
                // Randomly decide if component is face-up or face-down
                // Face-up components from the faceUpComponents list, others face-down
                boolean isFaceUp = faceUpComponents.contains(component);
                if (!isFaceUp && placedCount < faceDownCount) {
                    // This is a face-down component
                    tileView.setFaceDown(true);
                }
                
                // Random position within content area
                double x = offsetX + random.nextDouble() * contentWidth;
                double y = offsetY + random.nextDouble() * contentHeight;
                
                // Random rotation for physical appearance (-35 to +35 degrees)
                double angle = random.nextDouble() * 70 - 35;
                
                tileView.setLayoutX(x);
                tileView.setLayoutY(y);
                tileView.setRotate(angle);
                
                // Click handler
                tileView.setOnMouseClicked(event -> {
                    handleComponentClicked(component, tileView);
                    event.consume();
                });
                
                getChildren().add(tileView);
                placedCount++;
            }
            
            // Ensure some components are brought to front for layering effect
            if (getChildren().size() > 1) {
                // Randomly bring some tiles to front
                List<Node> children = new ArrayList<>(getChildren());
                Collections.shuffle(children, random);
                for (int i = 0; i < Math.min(3, children.size()); i++) {
                    children.get(i).toFront();
                }
            }
            
            if (!getChildren().isEmpty()) {
                populated = true;
            }
        }
        
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            // Trigger population when layout is finalized
            if (getWidth() > 0 && getHeight() > 0) {
                if (!populated && !availableComponents.isEmpty() && getChildren().isEmpty()) {
                    checkAndRepopulateIfNeeded();
                }
            }
        }
    }
    
    private void handleComponentClicked(ComponentInstance component, ComponentTileView tileView) {
        if (clickHandler != null) {
            if (tileView.isFaceDown()) {
                // Face-down tile clicked - treat as random draw
                clickHandler.onFaceDownTileClicked();
            } else {
                // Face-up component clicked - specific selection
                clickHandler.onComponentClicked(component);
            }
        }
    }
    
    /**
     * Update the complete warehouse state
     */
    public void updateWarehouse(List<ComponentInstance> allComponents, List<ComponentInstance> faceUpComponents, int faceDownCount) {
        this.availableComponents = new ArrayList<>(allComponents);
        this.faceUpComponents = new ArrayList<>(faceUpComponents);
        this.faceDownCount = faceDownCount;
        
        // Update title
        int totalCount = allComponents.size();
        titleLabel.setText(String.format("Component Warehouse (%d total, %d face-up)", totalCount, faceUpComponents.size()));
        
        // Update instruction
        if (totalCount > 0) {
            instructionLabel.setText("Click any component to take it");
            pileView.setDisable(false);
            pileView.setOpacity(1.0);
        } else {
            instructionLabel.setText("Warehouse is empty - waiting for server data");
            pileView.setDisable(true);
            pileView.setOpacity(0.5);
        }
        
        // Repopulate the visual pile
        populated = false;
        pileView.repopulatePile();
    }
    
    /**
     * Add a face-up component to the pile (when returned)
     */
    public void addComponentFaceUp(ComponentInstance component) {
        if (!availableComponents.contains(component)) {
            availableComponents.add(component);
        }
        if (!faceUpComponents.contains(component)) {
            faceUpComponents.add(component);
        }
        
        // Add visually with animation
        addComponentVisually(component, true);
        updateTitle();
    }
    
    /**
     * Remove a component from the pile
     */
    public void removeComponent(ComponentInstance component) {
        availableComponents.remove(component);
        faceUpComponents.remove(component);
        
        // Remove visually
        removeComponentVisually(component);
        updateTitle();
    }
    
    private void addComponentVisually(ComponentInstance component, boolean faceUp) {
        ComponentTileView tileView = new ComponentTileView(component);
        if (!faceUp) {
            tileView.setFaceDown(true);
        }
        
        double areaWidth = pileView.getWidth();
        double areaHeight = pileView.getHeight();
        double contentWidth = areaWidth - ComponentTileView.TILE_SIZE;
        double contentHeight = areaHeight - ComponentTileView.TILE_SIZE;
        
        if (contentWidth > 0 && contentHeight > 0) {
            double x = random.nextDouble() * contentWidth;
            double y = random.nextDouble() * contentHeight;
            double angle = random.nextDouble() * 40 - 20; // Smaller rotation for added tiles
            
            tileView.setLayoutX(x);
            tileView.setLayoutY(y);
            tileView.setRotate(angle);
            
            tileView.setOnMouseClicked(event -> {
                handleComponentClicked(component, tileView);
                event.consume();
            });
            
            pileView.getChildren().add(tileView);
            tileView.toFront(); // New tiles appear on top
        }
    }
    
    private void removeComponentVisually(ComponentInstance component) {
        pileView.getChildren().removeIf(node -> {
            if (node instanceof ComponentTileView) {
                ComponentTileView tileView = (ComponentTileView) node;
                return component.equals(tileView.getComponentInstance());
            }
            return false;
        });
        
        // Repopulate if visually empty but should have components
        if (pileView.getChildren().isEmpty() && !availableComponents.isEmpty()) {
            populated = false;
            pileView.checkAndRepopulateIfNeeded();
        }
    }
    
    private void updateTitle() {
        int totalCount = availableComponents.size();
        titleLabel.setText(String.format("Component Warehouse (%d total, %d face-up)", totalCount, faceUpComponents.size()));
    }
    
    /**
     * Get all available components
     */
    public List<ComponentInstance> getAvailableComponents() {
        return new ArrayList<>(availableComponents);
    }
    
    /**
     * Get face-up components
     */
    public List<ComponentInstance> getFaceUpComponents() {
        return new ArrayList<>(faceUpComponents);
    }
    
    /**
     * Get face-down count
     */
    public int getFaceDownCount() {
        return faceDownCount;
    }
    
    /**
     * Check if warehouse is empty
     */
    public boolean isEmpty() {
        return availableComponents.isEmpty();
    }
    
    /**
     * Force repopulation of the pile (useful after major state changes)
     */
    public void refresh() {
        populated = false;
        Platform.runLater(() -> pileView.repopulatePile());
    }
    
    /**
     * Set the size of the pile view
     */
    public void setPileSize(double width, double height) {
        pileView.setPrefSize(width, height);
        pileView.setMinSize(width * 0.7, height * 0.7);
    }
    
    @Override
    public String toString() {
        return "ComponentJunkyardView{" +
                "totalComponents=" + availableComponents.size() +
                ", faceUpComponents=" + faceUpComponents.size() +
                ", faceDownCount=" + faceDownCount +
                '}';
    }
}
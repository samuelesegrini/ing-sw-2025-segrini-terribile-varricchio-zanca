package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.client.core.state.ComponentInstance;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure ComponentInstance-based inventory view for held and available components.
 * Shows component details, connectors, and rotation state.
 * NO ComponentType support - ComponentInstance only.
 */
public class ComponentInventoryView extends VBox {
    
    public interface ComponentClickHandler {
        void onAvailableComponentClicked(ComponentInstance component);
        void onHeldComponentClicked(ComponentInstance component);
    }
    
    private final ComponentClickHandler clickHandler;
    private VBox heldComponentsSection;
    private VBox availableComponentsSection;
    private FlowPane heldComponentsGrid;
    private FlowPane availableComponentsGrid;
    private Label heldComponentsLabel;
    private Label availableComponentsLabel;
    
    private List<ComponentInstance> heldComponents = new ArrayList<>();
    private List<ComponentInstance> availableComponents = new ArrayList<>();
    private ComponentTileView selectedComponentView = null;

    public ComponentInventoryView(ComponentClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        initializeView();
        setupHeldComponentsSection();
        setupAvailableComponentsSection();
    }

    private void initializeView() {
        getStyleClass().add("component-inventory");
        setSpacing(15);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);
    }

    private void setupHeldComponentsSection() {
        heldComponentsSection = new VBox(8);
        heldComponentsSection.getStyleClass().add("held-components-section");
        
        heldComponentsLabel = new Label("Held Components (0/2)");
        heldComponentsLabel.getStyleClass().add("section-title");
        
        heldComponentsGrid = new FlowPane();
        heldComponentsGrid.getStyleClass().add("components-grid");
        heldComponentsGrid.setHgap(8);
        heldComponentsGrid.setVgap(8);
        heldComponentsGrid.setAlignment(Pos.CENTER);
        
        ScrollPane heldScrollPane = new ScrollPane(heldComponentsGrid);
        heldScrollPane.getStyleClass().add("components-scroll");
        heldScrollPane.setFitToWidth(true);
        heldScrollPane.setPrefHeight(100);
        heldScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        heldScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        heldComponentsSection.getChildren().addAll(heldComponentsLabel, heldScrollPane);
        getChildren().add(heldComponentsSection);
    }

    private void setupAvailableComponentsSection() {
        availableComponentsSection = new VBox(8);
        availableComponentsSection.getStyleClass().add("available-components-section");
        
        availableComponentsLabel = new Label("Available Components (0)");
        availableComponentsLabel.getStyleClass().add("section-title");
        
        availableComponentsGrid = new FlowPane();
        availableComponentsGrid.getStyleClass().add("components-grid");
        availableComponentsGrid.setHgap(8);
        availableComponentsGrid.setVgap(8);
        availableComponentsGrid.setAlignment(Pos.CENTER);
        
        ScrollPane availableScrollPane = new ScrollPane(availableComponentsGrid);
        availableScrollPane.getStyleClass().add("components-scroll");
        availableScrollPane.setFitToWidth(true);
        availableScrollPane.setPrefHeight(120);
        availableScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        availableScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        availableComponentsSection.getChildren().addAll(availableComponentsLabel, availableScrollPane);
        getChildren().add(availableComponentsSection);
    }

    /**
     * Update held components display - PURE ComponentInstance
     */
    public void updateHeldComponents(List<ComponentInstance> components) {
        this.heldComponents = new ArrayList<>(components);
        
        // Update label
        heldComponentsLabel.setText(String.format("Held Components (%d/2)", components.size()));
        
        // Clear current display
        heldComponentsGrid.getChildren().clear();
        
        // Add component views
        for (ComponentInstance component : components) {
            ComponentTileView tileView = new ComponentTileView(component);
            tileView.setOnMouseClicked(event -> {
                handleHeldComponentClicked(tileView);
            });
            
            heldComponentsGrid.getChildren().add(tileView);
        }
    }

    /**
     * Update available components display - PURE ComponentInstance
     */
    public void updateAvailableComponents(List<ComponentInstance> components) {
        this.availableComponents = new ArrayList<>(components);
        
        // Update label
        availableComponentsLabel.setText(String.format("Available Components (%d)", components.size()));
        
        // Clear current display
        availableComponentsGrid.getChildren().clear();
        
        // Add component views
        for (ComponentInstance component : components) {
            ComponentTileView tileView = new ComponentTileView(component);
            tileView.setOnMouseClicked(event -> {
                handleAvailableComponentClicked(tileView);
            });
            
            availableComponentsGrid.getChildren().add(tileView);
        }
    }

    private void handleHeldComponentClicked(ComponentTileView tileView) {
        // Clear previous selection
        clearSelection();
        
        // Set new selection
        selectedComponentView = tileView;
        tileView.setSelected(true);
        
        // Notify handler
        if (clickHandler != null) {
            clickHandler.onHeldComponentClicked(tileView.getComponentInstance());
        }
    }

    private void handleAvailableComponentClicked(ComponentTileView tileView) {
        // Clear previous selection
        clearSelection();
        
        // Set new selection
        selectedComponentView = tileView;
        tileView.setSelected(true);
        
        // Notify handler
        if (clickHandler != null) {
            clickHandler.onAvailableComponentClicked(tileView.getComponentInstance());
        }
    }

    /**
     * Clear all component selection
     */
    public void clearSelection() {
        if (selectedComponentView != null) {
            selectedComponentView.setSelected(false);
            selectedComponentView = null;
        }
        
        // Also clear selection from all tile views
        clearSelectionFromGrid(heldComponentsGrid);
        clearSelectionFromGrid(availableComponentsGrid);
    }

    private void clearSelectionFromGrid(FlowPane grid) {
        grid.getChildren().forEach(node -> {
            if (node instanceof ComponentTileView tileView) {
                tileView.setSelected(false);
            }
        });
    }

    /**
     * Get currently selected component
     */
    public ComponentInstance getSelectedComponent() {
        return selectedComponentView != null ? selectedComponentView.getComponentInstance() : null;
    }

    /**
     * Get all held components
     */
    public List<ComponentInstance> getHeldComponents() {
        return new ArrayList<>(heldComponents);
    }

    /**
     * Get all available components
     */
    public List<ComponentInstance> getAvailableComponents() {
        return new ArrayList<>(availableComponents);
    }

    /**
     * Remove a specific held component
     */
    public void removeHeldComponent(ComponentInstance component) {
        heldComponents.removeIf(c -> c.getId().equals(component.getId()));
        updateHeldComponents(heldComponents);
    }

    /**
     * Add a component to held components
     */
    public void addHeldComponent(ComponentInstance component) {
        if (heldComponents.size() < 2) {
            heldComponents.add(component);
            updateHeldComponents(heldComponents);
        }
    }

    /**
     * Remove a specific available component
     */
    public void removeAvailableComponent(ComponentInstance component) {
        availableComponents.removeIf(c -> c.getId().equals(component.getId()));
        updateAvailableComponents(availableComponents);
    }

    /**
     * Add a component to available components
     */
    public void addAvailableComponent(ComponentInstance component) {
        availableComponents.add(component);
        updateAvailableComponents(availableComponents);
    }

    /**
     * Check if inventory contains a specific component
     */
    public boolean containsComponent(ComponentInstance component) {
        return heldComponents.stream().anyMatch(c -> c.getId().equals(component.getId())) ||
               availableComponents.stream().anyMatch(c -> c.getId().equals(component.getId()));
    }

    /**
     * Refresh the display after component rotation
     */
    public void refreshAfterRotation() {
        // Refresh all component views
        refreshGrid(heldComponentsGrid);
        refreshGrid(availableComponentsGrid);
    }

    private void refreshGrid(FlowPane grid) {
        grid.getChildren().forEach(node -> {
            if (node instanceof ComponentTileView tileView) {
                tileView.refreshAfterRotation();
            }
        });
    }

    /**
     * Set visibility of the available components section
     */
    public void setAvailableComponentsVisible(boolean visible) {
        availableComponentsSection.setVisible(visible);
        availableComponentsSection.setManaged(visible);
    }

    /**
     * Set visibility of the held components section
     */
    public void setHeldComponentsVisible(boolean visible) {
        heldComponentsSection.setVisible(visible);
        heldComponentsSection.setManaged(visible);
    }

    @Override
    public String toString() {
        return "ComponentInventoryView{" +
                "heldComponents=" + heldComponents.size() +
                ", availableComponents=" + availableComponents.size() +
                ", selectedComponent=" + (selectedComponentView != null ? selectedComponentView.getComponentId() : "none") +
                '}';
    }
}
package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI component for displaying ship components using server Component model.
 * Shows component image, ID, connectors, and rotation state.
 */
public class ComponentTileView extends StackPane {
    
    public static final double TILE_SIZE = 65;
    public static final double CONNECTOR_SIZE = 8;
    
    // Static image cache to avoid reloading same images
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> loadAttempted = new ConcurrentHashMap<>();
    
    private final Component component;
    private boolean isPlaced = false;
    private boolean isFaceUp = true;
    private boolean isInJunkyard = false;
    private boolean isFaceDown = false;
    private boolean isSelected = false;
    
    private ImageView imageView;
    private Label componentLabel;
    private Label idLabel;
    private StackPane connectorOverlay;
    private StackPane rotationIndicator;
    
    /**
     * Constructor for Component
     */
    public ComponentTileView(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        
        this.component = component;
        
        initializeView();
        setupImage();
        setupConnectorOverlay();
        setupRotationIndicator();
        setupTooltip();
        updateAppearance();
    }
    
    /**
     * Constructor for face-down tiles (when no specific component is known)
     */
    public ComponentTileView(boolean isFaceDown) {
        if (!isFaceDown) {
            throw new IllegalArgumentException("Use Component constructor for face-up tiles");
        }
        
        this.component = null;
        this.isFaceDown = true;
        this.isFaceUp = false;
        
        initializeView();
        setupFaceDownView();
        updateAppearance();
    }
    
    private void initializeView() {
        getStyleClass().add("component-tile");
        setPrefSize(TILE_SIZE, TILE_SIZE);
        setMinSize(TILE_SIZE, TILE_SIZE);
        setMaxSize(TILE_SIZE, TILE_SIZE);
    }
    
    private void setupImage() {
        if (component == null) return;
        
        try {
            String imagePath = "/images/components/" + component.getType().name().toLowerCase() + ".png";
            if (imagePath.isEmpty()) {
                setupTextDisplay();
                return;
            }
            
            // Check cache first
            Image cachedImage = imageCache.get(imagePath);
            if (cachedImage != null) {
                createImageView(cachedImage);
                return;
            }
            
            // Check if we've already tried loading this image and failed
            if (loadAttempted.getOrDefault(imagePath, false)) {
                setupTextDisplay();
                return;
            }
            
            // Try to load the image
            try {
                Image image = new Image(getClass().getResourceAsStream(imagePath));
                if (image.isError()) {
                    loadAttempted.put(imagePath, true);
                    setupTextDisplay();
                } else {
                    imageCache.put(imagePath, image);
                    createImageView(image);
                }
            } catch (Exception e) {
                loadAttempted.put(imagePath, true);
                setupTextDisplay();
            }
            
        } catch (Exception e) {
            setupTextDisplay();
        }
    }
    
    private void createImageView(Image image) {
        imageView = new ImageView(image);
        imageView.setFitWidth(TILE_SIZE * 0.8);
        imageView.setFitHeight(TILE_SIZE * 0.8);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        getChildren().add(imageView);
    }
    
    private void setupTextDisplay() {
        if (component == null) return;
        
        VBox textContainer = new VBox();
        textContainer.setAlignment(Pos.CENTER);
        textContainer.setSpacing(2);
        
        // Component type label
        componentLabel = new Label(component.getType().name());
        componentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        componentLabel.setTextFill(Color.WHITE);
        componentLabel.getStyleClass().add("component-type-label");
        
        // ID label
        idLabel = new Label(component.getId());
        idLabel.setFont(Font.font("Arial", 8));
        idLabel.setTextFill(Color.LIGHTGRAY);
        idLabel.getStyleClass().add("component-id-label");
        
        textContainer.getChildren().addAll(componentLabel, idLabel);
        getChildren().add(textContainer);
    }
    
    private void setupFaceDownView() {
        Rectangle faceDownRect = new Rectangle(TILE_SIZE * 0.9, TILE_SIZE * 0.9);
        faceDownRect.setFill(Color.DARKGRAY);
        faceDownRect.setStroke(Color.GRAY);
        faceDownRect.setStrokeWidth(2);
        faceDownRect.setArcWidth(5);
        faceDownRect.setArcHeight(5);
        
        Label faceDownLabel = new Label("?");
        faceDownLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        faceDownLabel.setTextFill(Color.WHITE);
        
        getChildren().addAll(faceDownRect, faceDownLabel);
    }
    
    private void setupConnectorOverlay() {
        if (component == null) return;
        
        connectorOverlay = new StackPane();
        connectorOverlay.setPrefSize(TILE_SIZE, TILE_SIZE);
        connectorOverlay.setMouseTransparent(true);
        
        updateConnectorDisplay();
        getChildren().add(connectorOverlay);
    }
    
    private void updateConnectorDisplay() {
        if (connectorOverlay == null || component == null) return;
        
        connectorOverlay.getChildren().clear();
        
        Direction currentDirection = component.getCurrentDirection();
        Map<Direction, ConnectorType> connectors = component.getConnectors();
        
        // Calculate connector positions based on current rotation
        for (Map.Entry<Direction, ConnectorType> entry : connectors.entrySet()) {
            Direction originalDir = entry.getKey();
            ConnectorType connectorType = entry.getValue();
            
            // Rotate the direction based on current component direction
            Direction rotatedDirection = rotateDirection(originalDir, currentDirection);
        }
    }
    
    private void positionConnector(Circle connector, Direction direction) {
        double offset = TILE_SIZE / 2 - CONNECTOR_SIZE / 2;
        
        switch (direction) {
            case UP -> {
                connector.setTranslateX(0);
                connector.setTranslateY(-offset);
            }
            case DOWN -> {
                connector.setTranslateX(0);
                connector.setTranslateY(offset);
            }
            case LEFT -> {
                connector.setTranslateX(-offset);
                connector.setTranslateY(0);
            }
            case RIGHT -> {
                connector.setTranslateX(offset);
                connector.setTranslateY(0);
            }
        }
    }
    
    private Direction rotateDirection(Direction original, Direction componentDirection) {
        // Simple rotation logic - this might need adjustment based on your rotation system
        int rotations = componentDirection.ordinal();
        Direction[] directions = Direction.values();
        int newIndex = (original.ordinal() + rotations) % directions.length;
        return directions[newIndex];
    }
    
    private void setupRotationIndicator() {
        if (component == null) return;
        
        rotationIndicator = new StackPane();
        rotationIndicator.setPrefSize(15, 15);
        rotationIndicator.setMaxSize(15, 15);
        
        Circle indicator = new Circle(6);
        indicator.setFill(Color.WHITE);
        indicator.setStroke(Color.BLACK);
        indicator.setStrokeWidth(1);
        
        Direction rotatedDirection = component.getCurrentDirection();
        String directionText = switch (rotatedDirection) {
            case UP -> "↑";
            case DOWN -> "↓";
            case LEFT -> "←";
            case RIGHT -> "→";
        };
        
        Label dirLabel = new Label(directionText);
        dirLabel.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        dirLabel.setTextFill(Color.BLACK);
        
        rotationIndicator.getChildren().addAll(indicator, dirLabel);
        rotationIndicator.setTranslateX(TILE_SIZE / 2 - 10);
        rotationIndicator.setTranslateY(-TILE_SIZE / 2 + 10);
        
        getChildren().add(rotationIndicator);
    }
    
    private void setupTooltip() {
        if (component == null) return;
        
        String displayText = component.getType().name();
        String tooltipText = String.format(
            "%s\nID: %s\nDirection: %s",
            displayText,
            component.getId(),
            component.getCurrentDirection().name()
        );
        
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(this, tooltip);
    }
    
    private void updateAppearance() {
        getStyleClass().removeAll("component-placed", "component-selected", "component-junkyard", "component-face-down");
        
        if (isFaceDown) {
            getStyleClass().add("component-face-down");
        } else if (isSelected) {
            getStyleClass().add("component-selected");
        } else if (isInJunkyard) {
            getStyleClass().add("component-junkyard");
        } else if (isPlaced) {
            getStyleClass().add("component-placed");
        }
    }
    
    // Getters and setters
    public Component getComponent() {
        return component;
    }
    
    public String getComponentId() {
        return component != null ? component.getId() : "unknown";
    }
    
    public boolean isPlaced() {
        return isPlaced;
    }
    
    public void setPlaced(boolean placed) {
        this.isPlaced = placed;
        updateAppearance();
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateAppearance();
    }
    
    public boolean isInJunkyard() {
        return isInJunkyard;
    }
    
    public void setInJunkyard(boolean inJunkyard) {
        this.isInJunkyard = inJunkyard;
        updateAppearance();
    }
    
    public void refreshAfterRotation() {
        if (component != null) {
            updateConnectorDisplay();
            setupRotationIndicator();
        }
    }
    
    public boolean isFaceDown() {
        return isFaceDown;
    }
    
    public void setFaceDown(boolean faceDown) {
        this.isFaceDown = faceDown;
        updateAppearance();
    }
}
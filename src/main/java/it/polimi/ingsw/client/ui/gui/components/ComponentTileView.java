package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.client.core.state.ComponentInstance;
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
 * Pure ComponentInstance-based GUI component for displaying ship components.
 * Shows component image, ID, connectors, and rotation state.
 * NO ComponentType fallbacks - ComponentInstance only.
 */
public class ComponentTileView extends StackPane {
    
    public static final double TILE_SIZE = 65;
    public static final double CONNECTOR_SIZE = 8;
    
    // Static image cache to avoid reloading same images
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> loadAttempted = new ConcurrentHashMap<>();
    
    private final ComponentInstance componentInstance;
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
     * Constructor for ComponentInstance (ONLY constructor - no fallbacks)
     */
    public ComponentTileView(ComponentInstance componentInstance) {
        if (componentInstance == null) {
            throw new IllegalArgumentException("ComponentInstance cannot be null");
        }
        
        this.componentInstance = componentInstance;
        
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
            throw new IllegalArgumentException("Use ComponentInstance constructor for face-up tiles");
        }
        
        this.componentInstance = null;
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
        if (componentInstance == null) return;
        
        try {
            String imagePath = componentInstance.getImageResourcePath();
            if (imagePath == null || imagePath.isEmpty()) {
                setupTextDisplay();
                return;
            }
            
            // Check cache first
            Image cachedImage = imageCache.get(imagePath);
            if (cachedImage != null) {
                createImageView(cachedImage);
                return;
            }
            
            // Check if we've already tried to load this image and failed
            if (loadAttempted.containsKey(imagePath)) {
                setupTextDisplay();
                return;
            }
            
            // Mark as attempted and try to load
            loadAttempted.put(imagePath, true);
            
            java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                try {
                    Image image = new Image(stream);
                    
                    if (!image.isError()) {
                        // Cache successful load
                        imageCache.put(imagePath, image);
                        createImageView(image);
                        stream.close();
                        return; // Successfully loaded
                    }
                    stream.close();
                } catch (Exception e) {
                    stream.close();
                }
            }
            
            System.err.println("Failed to load image: " + imagePath);
            setupTextDisplay();
            
        } catch (Exception e) {
            System.err.println("Exception loading component image: " + componentInstance.getImagePath());
            setupTextDisplay();
        }
    }
    
    private void createImageView(Image image) {
        imageView = new ImageView(image);
        imageView.setFitWidth(TILE_SIZE - 16);
        imageView.setFitHeight(TILE_SIZE - 16);
        imageView.setPreserveRatio(true);
        getChildren().add(imageView);
        StackPane.setAlignment(imageView, Pos.CENTER);
    }
    
    private void setupTextDisplay() {
        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER);
        textContainer.setMaxWidth(TILE_SIZE - 8);
        
        // Component symbol/type
        componentLabel = new Label(componentInstance.getDisplaySymbol());
        componentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        componentLabel.setTextFill(Color.WHITE);
        componentLabel.setAlignment(Pos.CENTER);
        
        // Component ID (last 6 characters)
        String shortId = componentInstance.getId();
        if (shortId.length() > 6) {
            shortId = shortId.substring(shortId.length() - 6);
        }
        idLabel = new Label(shortId);
        idLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 8));
        idLabel.setTextFill(Color.LIGHTGRAY);
        idLabel.setAlignment(Pos.CENTER);
        
        textContainer.getChildren().addAll(componentLabel, idLabel);
        getChildren().add(textContainer);
        StackPane.setAlignment(textContainer, Pos.CENTER);
        
        // Set background color based on component type
        Color bgColor = getComponentColor();
        setStyle(String.format("-fx-background-color: rgb(%d,%d,%d); -fx-border-color: #333; -fx-border-width: 1; -fx-background-radius: 3;",
            (int)(bgColor.getRed() * 255),
            (int)(bgColor.getGreen() * 255),
            (int)(bgColor.getBlue() * 255)
        ));
    }
    
    private void setupConnectorOverlay() {
        if (componentInstance == null) return;
        
        connectorOverlay = new StackPane();
        connectorOverlay.setPrefSize(TILE_SIZE, TILE_SIZE);
        connectorOverlay.setMouseTransparent(true);
        
        // Add connector indicators at each direction
        for (Direction direction : Direction.values()) {
            ConnectorType connectorType = componentInstance.getConnectorAt(direction);
            if (connectorType != null && connectorType != ConnectorType.PLAIN) {
                Circle connector = createConnectorIndicator(connectorType);
                connectorOverlay.getChildren().add(connector);
                positionConnector(connector, direction);
            }
        }
        
        getChildren().add(connectorOverlay);
    }
    
    private Circle createConnectorIndicator(ConnectorType type) {
        Circle connector = new Circle(CONNECTOR_SIZE / 2);
        
        Color color = switch (type != null ? type : ConnectorType.PLAIN) {
            case UNIVERSAL -> Color.GOLD;
            case DOUBLE -> Color.BLUE;
            case SINGLE -> Color.GREEN;
            case PLAIN -> Color.TRANSPARENT;
        };
        
        connector.setFill(color);
        connector.setStroke(Color.BLACK);
        connector.setStrokeWidth(1);
        
        return connector;
    }
    
    private void positionConnector(Circle connector, Direction direction) {
        double offset = (TILE_SIZE / 2) - (CONNECTOR_SIZE / 2) - 2;
        
        switch (direction) {
            case UP -> {
                StackPane.setAlignment(connector, Pos.TOP_CENTER);
                connector.setTranslateY(2);
            }
            case DOWN -> {
                StackPane.setAlignment(connector, Pos.BOTTOM_CENTER);
                connector.setTranslateY(-2);
            }
            case LEFT -> {
                StackPane.setAlignment(connector, Pos.CENTER_LEFT);
                connector.setTranslateX(2);
            }
            case RIGHT -> {
                StackPane.setAlignment(connector, Pos.CENTER_RIGHT);
                connector.setTranslateX(-2);
            }
        }
    }
    
    private void setupRotationIndicator() {
        if (componentInstance == null) return;
        
        rotationIndicator = new StackPane();
        rotationIndicator.setPrefSize(16, 16);
        rotationIndicator.setMaxSize(16, 16);
        rotationIndicator.setMouseTransparent(true);
        
        // Small triangle indicating current direction
        Label directionLabel = new Label(getDirectionSymbol());
        directionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        directionLabel.setTextFill(Color.WHITE);
        directionLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 8; -fx-padding: 1;");
        
        rotationIndicator.getChildren().add(directionLabel);
        getChildren().add(rotationIndicator);
        StackPane.setAlignment(rotationIndicator, Pos.TOP_RIGHT);
        rotationIndicator.setTranslateX(-2);
        rotationIndicator.setTranslateY(2);
    }
    
    private String getDirectionSymbol() {
        if (componentInstance == null) return "";
        
        return switch (componentInstance.getCurrentDirection()) {
            case UP -> "↑";
            case DOWN -> "↓";
            case LEFT -> "←";
            case RIGHT -> "→";
        };
    }
    
    private void setupFaceDownView() {
        try {
            // Try to load the galaxy-sky.jpg image for face-down components
            java.io.InputStream stream = getClass().getResourceAsStream("/assets/tiles/galaxy-sky.jpg");
            if (stream != null) {
                Image backImage = new Image(stream);
                if (!backImage.isError()) {
                    imageView = new ImageView(backImage);
                    imageView.setFitWidth(TILE_SIZE - 4);
                    imageView.setFitHeight(TILE_SIZE - 4);
                    imageView.setPreserveRatio(true);
                    getChildren().add(imageView);
                    StackPane.setAlignment(imageView, Pos.CENTER);
                    stream.close();
                    return;
                }
                stream.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to load galaxy-sky.jpg for face-down component: " + e.getMessage());
        }
        
        // Fallback to original "?" display if image loading fails
        componentLabel = new Label("?");
        componentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        componentLabel.setTextFill(Color.WHITE);
        componentLabel.setAlignment(Pos.CENTER);
        
        getChildren().add(componentLabel);
        StackPane.setAlignment(componentLabel, Pos.CENTER);
        
        setStyle("-fx-background-color: #4a4a4a; -fx-border-color: #666666; -fx-border-width: 2; -fx-background-radius: 3;");
    }
    
    private void setupTooltip() {
        if (componentInstance == null) return;
        
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append("Component: ").append(componentInstance.getDisplayName()).append("\n");
        tooltipText.append("ID: ").append(componentInstance.getId()).append("\n");
        tooltipText.append("Direction: ").append(componentInstance.getCurrentDirection()).append("\n");
        tooltipText.append("Connectors:\n");
        
        for (Direction dir : Direction.values()) {
            ConnectorType connector = componentInstance.getConnectorAt(dir);
            if (connector != ConnectorType.PLAIN) {
                tooltipText.append("  ").append(dir.name()).append(": ").append(connector.name()).append("\n");
            }
        }
        
        Tooltip tooltip = new Tooltip(tooltipText.toString());
        Tooltip.install(this, tooltip);
    }
    
    private Color getComponentColor() {
        if (componentInstance == null) return Color.GRAY;
        
        return switch (componentInstance.getType()) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> Color.ORANGE;
            case CANNON_SINGLE, CANNON_DOUBLE -> Color.RED;
            case CABIN, CABIN_START -> Color.YELLOW;
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> Color.BROWN;
            case BATTERY -> Color.PURPLE;
            case SHIELD -> Color.CYAN;
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> Color.PINK;
            case STRUCTURAL -> Color.LIGHTGRAY;
        };
    }
    
    private void updateAppearance() {
        // Clear previous dynamic styles
        getStyleClass().removeAll("placed-tile", "face-down", "in-junkyard", "selected-tile");
        
        if (isPlaced) {
            getStyleClass().add("placed-tile");
        }
        
        if (isInJunkyard) {
            getStyleClass().add("in-junkyard");
        }
        
        if (isSelected) {
            getStyleClass().add("selected-tile");
            setStyle(getStyle() + "; -fx-effect: dropshadow(gaussian, gold, 10, 0.8, 0, 0);");
        }
        
        if (isFaceDown) {
            getStyleClass().add("face-down");
        }
        
        // Update rotation indicator if it exists
        if (rotationIndicator != null && componentInstance != null) {
            Label directionLabel = (Label) rotationIndicator.getChildren().get(0);
            directionLabel.setText(getDirectionSymbol());
        }
    }
    
    // Public API methods
    public ComponentInstance getComponentInstance() {
        return componentInstance;
    }
    
    public String getComponentId() {
        return componentInstance != null ? componentInstance.getId() : null;
    }
    
    public boolean isPlaced() {
        return isPlaced;
    }
    
    public void setPlaced(boolean placed) {
        if (this.isPlaced != placed) {
            this.isPlaced = placed;
            updateAppearance();
        }
    }
    
    public boolean isFaceUp() {
        return isFaceUp;
    }
    
    public void setFaceUp(boolean faceUp) {
        if (this.isFaceUp != faceUp) {
            this.isFaceUp = faceUp;
            this.isFaceDown = !faceUp;
            updateAppearance();
        }
    }
    
    public boolean isInJunkyard() {
        return isInJunkyard;
    }
    
    public void setInJunkyard(boolean inJunkyard) {
        if (this.isInJunkyard != inJunkyard) {
            this.isInJunkyard = inJunkyard;
            updateAppearance();
        }
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            updateAppearance();
        }
    }
    
    public boolean isFaceDown() {
        return isFaceDown;
    }
    
    public void setFaceDown(boolean faceDown) {
        if (this.isFaceDown != faceDown) {
            this.isFaceDown = faceDown;
            this.isFaceUp = !faceDown;
            updateAppearance();
        }
    }
    
    /**
     * Update the display after component rotation
     */
    public void refreshAfterRotation() {
        // Clear and rebuild connector overlay
        if (connectorOverlay != null) {
            getChildren().remove(connectorOverlay);
        }
        setupConnectorOverlay();
        updateAppearance();
    }
    
    /**
     * Create a copy of this component tile view with a new component instance
     */
    public ComponentTileView createCopy(ComponentInstance newInstance) {
        ComponentTileView copy = new ComponentTileView(newInstance);
        copy.setPlaced(this.isPlaced);
        copy.setFaceUp(this.isFaceUp);
        copy.setInJunkyard(this.isInJunkyard);
        copy.setSelected(this.isSelected);
        return copy;
    }
    
    @Override
    public String toString() {
        return "ComponentTileView{" +
                "componentInstance=" + componentInstance +
                ", isPlaced=" + isPlaced +
                ", isFaceUp=" + isFaceUp +
                ", isInJunkyard=" + isInJunkyard +
                ", isSelected=" + isSelected +
                '}';
    }
}
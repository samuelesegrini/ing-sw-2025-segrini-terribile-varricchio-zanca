package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * GUI component for displaying and interacting with the three-stage building timer.
 * Provides visual feedback for timer stages and allows players to flip the timer.
 */
public class BuildingTimerView extends VBox {

    private Label stageLabel;
    private ProgressBar timerProgressBar;
    private Label timeRemainingLabel;
    private Label flipCountLabel;
    private Button flipButton;
    private VBox stageIndicators;
    
    // Timer state
    private BuildingTimer.TimerStage currentStage;
    private long timeRemaining;
    private int totalFlips;
    
    // Animation
    private Timeline progressAnimation;
    
    public BuildingTimerView() {
        initializeComponents();
        setupLayout();
        setupStyling();
    }
    
    private void initializeComponents() {
        stageLabel = new Label("⏰ Ready to Start Building");
        stageLabel.getStyleClass().addAll("timer-stage-label", "timer-stage-idle");
        
        timerProgressBar = new ProgressBar(0.0);
        timerProgressBar.setPrefWidth(300);
        timerProgressBar.getStyleClass().add("timer-progress-bar");
        
        timeRemainingLabel = new Label("--:--");
        timeRemainingLabel.getStyleClass().add("timer-remaining-label");
        
        flipCountLabel = new Label("Flips: 0");
        flipCountLabel.getStyleClass().add("timer-flip-count");
        
        flipButton = new Button("Start Timer");
        flipButton.getStyleClass().addAll("timer-flip-button", "primary-button");
        flipButton.setOnAction(e -> handleFlipButtonClick());
        
        stageIndicators = new VBox(5);
        stageIndicators.setAlignment(Pos.CENTER);
        
        // Initialize with NOT_STARTED stage
        currentStage = BuildingTimer.TimerStage.NOT_STARTED;
        timeRemaining = -1;
        totalFlips = 0;
    }
    
    private void setupLayout() {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        
        // Create stage indicators
        updateStageIndicators(BuildingTimer.TimerStage.NOT_STARTED);
        
        // Add all components
        getChildren().addAll(
            stageLabel,
            stageIndicators,
            timerProgressBar,
            timeRemainingLabel,
            flipCountLabel,
            flipButton
        );
    }
    
    private void setupStyling() {
        getStyleClass().add("building-timer-view");
        setStyle("""
            -fx-background-color: rgba(0, 0, 0, 0.1);
            -fx-background-radius: 10;
            -fx-padding: 20;
            -fx-border-color: #333;
            -fx-border-radius: 10;
            -fx-border-width: 2;
        """);
    }
    
    /**
     * Updates the timer display with new stage information
     */
    public void updateStage(BuildingTimer.TimerStage stage, long timeRemaining, int totalFlips) {
        Platform.runLater(() -> {
            this.currentStage = stage;
            this.timeRemaining = timeRemaining;
            this.totalFlips = totalFlips;
            
            updateStageDisplay(stage);
            updateTimeDisplay(timeRemaining);
            updateFlipCounter(totalFlips);
            updateFlipButtonState(stage);
            updateStageIndicators(stage);
            playStageTransitionAnimation(stage);
            
            // Start progress animation for active timers
            startProgressAnimation(timeRemaining);
        });
    }
    
    private void updateStageDisplay(BuildingTimer.TimerStage stage) {
        stageLabel.setText(getStageDisplayText(stage));
        stageLabel.getStyleClass().clear();
        stageLabel.getStyleClass().addAll("timer-stage-label", getStageStyleClass(stage));
    }
    
    private void updateTimeDisplay(long timeRemaining) {
        if (timeRemaining > 0) {
            int seconds = (int) (timeRemaining / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            timeRemainingLabel.setText(timeStr);
            timeRemainingLabel.getStyleClass().clear();
            timeRemainingLabel.getStyleClass().addAll("timer-remaining-label", "timer-active");
            
            // Update progress bar
            double progress = 1.0 - ((double) timeRemaining / 90000); // Assuming 90s stages
            timerProgressBar.setProgress(Math.max(0, Math.min(1, progress)));
        } else if (timeRemaining == 0) {
            timeRemainingLabel.setText("EXPIRED");
            timeRemainingLabel.getStyleClass().clear();
            timeRemainingLabel.getStyleClass().addAll("timer-remaining-label", "timer-expired");
            timerProgressBar.setProgress(1.0);
        } else {
            timeRemainingLabel.setText("--:--");
            timeRemainingLabel.getStyleClass().clear();
            timeRemainingLabel.getStyleClass().addAll("timer-remaining-label", "timer-idle");
            timerProgressBar.setProgress(0.0);
        }
    }
    
    private void updateFlipCounter(int totalFlips) {
        flipCountLabel.setText("Flips: " + totalFlips);
    }
    
    private void updateStageIndicators(BuildingTimer.TimerStage stage) {
        stageIndicators.getChildren().clear();
        
        // Create visual indicators for each stage
        HBox indicators = new HBox(15);
        indicators.setAlignment(Pos.CENTER);
        
        // Stage 1 indicator
        VBox stage1Box = createStageIndicator(
            "First Timer", 
            stage.ordinal() >= 1, 
            stage == BuildingTimer.TimerStage.FIRST_TIMER,
            Color.GREEN
        );
        
        // Stage 2 indicator
        VBox stage2Box = createStageIndicator(
            "Second Timer", 
            stage.ordinal() >= 3, 
            stage == BuildingTimer.TimerStage.SECOND_TIMER,
            Color.ORANGE
        );
        
        // End indicator
        VBox endBox = createStageIndicator(
            "Building End", 
            stage == BuildingTimer.TimerStage.BUILDING_ENDED, 
            stage == BuildingTimer.TimerStage.BUILDING_ENDED,
            Color.RED
        );
        
        // Add arrows between stages
        Label arrow1 = new Label("→");
        arrow1.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        
        Label arrow2 = new Label("→");
        arrow2.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        
        indicators.getChildren().addAll(stage1Box, arrow1, stage2Box, arrow2, endBox);
        stageIndicators.getChildren().add(indicators);
    }
    
    private VBox createStageIndicator(String label, boolean completed, boolean active, Color color) {
        Circle circle = new Circle(12);
        circle.setFill(completed ? color : Color.LIGHTGRAY);
        circle.setStroke(Color.DARKGRAY);
        circle.setStrokeWidth(2);
        
        if (active) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.YELLOW);
            glow.setRadius(10);
            circle.setEffect(glow);
        }
        
        Label stageLabel = new Label(label);
        stageLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #333;");
        
        VBox box = new VBox(3, circle, stageLabel);
        box.setAlignment(Pos.CENTER);
        
        return box;
    }
    
    private void updateFlipButtonState(BuildingTimer.TimerStage stage) {
        switch (stage) {
            case NOT_STARTED -> {
                flipButton.setText("Start Timer");
                flipButton.setDisable(false);
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "primary-button");
            }
            case FIRST_TIMER -> {
                flipButton.setText("Flip When Expired");
                flipButton.setDisable(timeRemaining > 0); // Enable when expired
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "secondary-button");
            }
            case FIRST_EXPIRED -> {
                flipButton.setText("Flip to Second Timer");
                flipButton.setDisable(false);
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "warning-button");
            }
            case SECOND_TIMER -> {
                flipButton.setText("End Building (Finish Ship First)");
                flipButton.setDisable(timeRemaining > 0); // Enable when expired
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "secondary-button");
            }
            case SECOND_EXPIRED -> {
                flipButton.setText("End Building Phase");
                flipButton.setDisable(false); // For finished players only
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "danger-button");
            }
            case BUILDING_ENDED -> {
                flipButton.setText("Building Complete");
                flipButton.setDisable(true);
                flipButton.getStyleClass().clear();
                flipButton.getStyleClass().addAll("timer-flip-button", "disabled-button");
            }
        }
    }
    
    private void startProgressAnimation(long timeRemaining) {
        if (progressAnimation != null) {
            progressAnimation.stop();
        }
        
        if (timeRemaining > 0) {
            // Animate progress bar countdown
            progressAnimation = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    this.timeRemaining -= 1000;
                    if (this.timeRemaining <= 0) {
                        this.timeRemaining = 0;
                        updateTimeDisplay(0);
                        updateFlipButtonState(currentStage);
                    } else {
                        updateTimeDisplay(this.timeRemaining);
                    }
                })
            );
            progressAnimation.setCycleCount(Timeline.INDEFINITE);
            progressAnimation.play();
        }
    }
    
    private void playStageTransitionAnimation(BuildingTimer.TimerStage stage) {
        // Flash effect for stage transitions
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(stageLabel.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(200), new KeyValue(stageLabel.opacityProperty(), 0.3)),
            new KeyFrame(Duration.millis(400), new KeyValue(stageLabel.opacityProperty(), 1.0))
        );
        flash.setCycleCount(3);
        flash.play();
    }
    
    private String getStageDisplayText(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "⏰ Ready to Start Building";
            case FIRST_TIMER -> "⏰ First Timer Running";
            case FIRST_EXPIRED -> "⏰ First Timer Expired - Click to Continue";
            case SECOND_TIMER -> "⏰⏰ Second Timer Running";
            case SECOND_EXPIRED -> "⏰⏰ Second Timer Expired - Finished Players Can End";
            case BUILDING_ENDED -> "🏁 Building Phase Complete";
        };
    }
    
    private String getStageStyleClass(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "timer-stage-idle";
            case FIRST_TIMER -> "timer-stage-first";
            case FIRST_EXPIRED -> "timer-stage-expired";
            case SECOND_TIMER -> "timer-stage-second";
            case SECOND_EXPIRED -> "timer-stage-expired";
            case BUILDING_ENDED -> "timer-stage-ended";
        };
    }
    
    private void handleFlipButtonClick() {
        // This would be connected to the client controller to send FlipBuildingTimerRequest
        if (flipButtonClickHandler != null) {
            flipButtonClickHandler.run();
        }
    }
    
    // Handler for flip button clicks - to be set by the view controller
    private Runnable flipButtonClickHandler;
    
    public void setFlipButtonClickHandler(Runnable handler) {
        this.flipButtonClickHandler = handler;
    }
    
    public void cleanup() {
        if (progressAnimation != null) {
            progressAnimation.stop();
        }
    }
}
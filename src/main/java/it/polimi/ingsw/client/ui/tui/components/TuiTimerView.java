package it.polimi.ingsw.client.ui.tui.components;

import it.polimi.ingsw.client.ui.tui.TuiManager;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;

/**
 * TUI component for displaying the three-stage building timer with ASCII art.
 * Provides clear visual feedback for timer stages in text-based interface.
 */
public class TuiTimerView {
    
    private final TuiManager tuiManager;
    private BuildingTimer.TimerStage currentStage;
    private long timeRemaining;
    private int totalFlips;
    
    public TuiTimerView(TuiManager tuiManager) {
        this.tuiManager = tuiManager;
        this.currentStage = BuildingTimer.TimerStage.NOT_STARTED;
        this.timeRemaining = -1;
        this.totalFlips = 0;
    }
    
    /**
     * Updates the timer display with new stage information
     */
    public void updateStage(BuildingTimer.TimerStage stage, long timeRemaining, int totalFlips) {
        this.currentStage = stage;
        this.timeRemaining = timeRemaining;
        this.totalFlips = totalFlips;
        
        displayTimerStatus();
    }
    
    /**
     * Displays the complete timer status in the TUI
     */
    public void displayTimerStatus() {
        clearTimerArea();
        displayTimerHeader();
        displayStageProgress();
        displayTimeRemaining();
        displayInstructions();
        displaySeparator();
    }
    
    private void clearTimerArea() {
        // Clear the timer display area
        tuiManager.clearLines(8); // Approximate number of lines used by timer display
    }
    
    private void displayTimerHeader() {
        tuiManager.println("═══════════════════════════════════════");
        tuiManager.println("            BUILDING TIMER             ");
        tuiManager.println("               Flips: " + String.format("%02d", totalFlips) + "               ");
        tuiManager.println("═══════════════════════════════════════");
    }
    
    private void displayStageProgress() {
        // Visual representation of the three stages
        String stage1 = getStageIndicator(currentStage.ordinal() >= 1, 
                                         currentStage == BuildingTimer.TimerStage.FIRST_TIMER);
        String stage2 = getStageIndicator(currentStage.ordinal() >= 3, 
                                         currentStage == BuildingTimer.TimerStage.SECOND_TIMER);
        String end = getStageIndicator(currentStage == BuildingTimer.TimerStage.BUILDING_ENDED, 
                                      currentStage == BuildingTimer.TimerStage.BUILDING_ENDED);

        tuiManager.println("Progress: " + stage1 + " ──→ " + stage2 + " ──→ " + end);
        tuiManager.println("         First   Second    End");
        
        // Current stage description
        tuiManager.println("Stage: " + getStageDescription(currentStage));
    }
    
    private String getStageIndicator(boolean completed, boolean active) {
        if (active) {
            return "◉"; // Active stage - filled circle with dot
        } else if (completed) {
            return "●"; // Completed stage - filled circle
        } else {
            return "○"; // Not reached - empty circle
        }
    }
    
    private void displayTimeRemaining() {
        tuiManager.println("");
        
        if (timeRemaining > 0) {
            int seconds = (int) (timeRemaining / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            tuiManager.println("Time Remaining: " + timeStr);
            
            // ASCII progress bar
            displayProgressBar(timeRemaining);
        } else if (timeRemaining == 0) {
            tuiManager.println("Time: EXPIRED ⏰");
            displayProgressBar(0);
        } else {
            tuiManager.println("Timer: Not Active");
        }
    }
    
    private void displayProgressBar(long timeRemaining) {
        // Calculate progress (assuming 90s stages)
        double progress;
        if (timeRemaining <= 0) {
            progress = 1.0;
        } else {
            progress = 1.0 - ((double) timeRemaining / 90000);
        }
        
        int barLength = 30;
        int filled = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        String percentage = String.format("%.1f%%", progress * 100);
        tuiManager.println(bar.toString() + " " + percentage);
    }
    
    private void displayInstructions() {
        tuiManager.println("");
        
        String instruction = getInstructionText(currentStage);
        tuiManager.println("💡 " + instruction);
        
        // Add contextual hints
        switch (currentStage) {
            case FIRST_TIMER -> {
                if (timeRemaining > 30000) { // More than 30 seconds
                    tuiManager.println("   Keep building! Timer will run for " + (timeRemaining / 1000) + " more seconds.");
                } else if (timeRemaining > 0) {
                    tuiManager.println("   ⚠️  Timer running low! Get ready to flip.");
                }
            }
            case SECOND_TIMER -> {
                if (timeRemaining > 30000) {
                    tuiManager.println("   Finish your ship! Only finished players can end building.");
                } else if (timeRemaining > 0) {
                    tuiManager.println("   🚨 Second timer running low! Finish quickly!");
                }
            }
            case SECOND_EXPIRED -> {
                tuiManager.println("   ⏰ Building will end automatically in 10 seconds if no one flips!");
            }
        }
    }
    
    private String getInstructionText(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Type 'flip' to start the first timer";
            case FIRST_TIMER -> "Wait for timer to expire, then type 'flip'";
            case FIRST_EXPIRED -> "Type 'flip' to start the second timer";
            case SECOND_TIMER -> "Finish your ship, then type 'flip' when timer expires";
            case SECOND_EXPIRED -> "Type 'flip' to end building (ship must be finished)";
            case BUILDING_ENDED -> "Building phase complete! Preparing for flight...";
        };
    }
    
    private String getStageDescription(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Ready to Begin";
            case FIRST_TIMER -> "First Timer Active";
            case FIRST_EXPIRED -> "First Timer Expired";
            case SECOND_TIMER -> "Second Timer Active";
            case SECOND_EXPIRED -> "Second Timer Expired";
            case BUILDING_ENDED -> "Building Complete";
        };
    }
    
    private void displaySeparator() {
        tuiManager.println("═══════════════════════════════════════");
        tuiManager.println("");
    }
    
    /**
     * Displays a compact timer status (for use in game status line)
     */
    public void displayCompactStatus() {
        String stageIcon = getStageIcon(currentStage);
        String timeStr = "";
        
        if (timeRemaining > 0) {
            int seconds = (int) (timeRemaining / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timeStr = String.format(" %02d:%02d", minutes, seconds);
        } else if (timeRemaining == 0) {
            timeStr = " EXPIRED";
        }
        
        tuiManager.print(String.format("Timer: %s%s (Flips: %d) ", 
                        stageIcon, timeStr, totalFlips));
    }
    
    private String getStageIcon(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "⏸️";
            case FIRST_TIMER -> "⏰";
            case FIRST_EXPIRED -> "⏰💥";
            case SECOND_TIMER -> "⏰⏰";
            case SECOND_EXPIRED -> "⏰⏰💥";
            case BUILDING_ENDED -> "🏁";
        };
    }
    
    /**
     * Shows a brief timer notification
     */
    public void showTimerNotification(String message) {
        tuiManager.println("");
        tuiManager.println("╔═══ TIMER NOTIFICATION ═══╗");
        
        // Simple message display
        String paddedMessage = String.format("║ %-25s ║", message.length() > 25 ? message.substring(0, 25) : message);
        tuiManager.println(paddedMessage);
        
        tuiManager.println("╚═══════════════════════════╝");
        tuiManager.println("");
    }
}
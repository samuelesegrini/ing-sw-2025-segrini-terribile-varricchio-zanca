package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the building phase timer system for Level II games.
 * Implements the three-stage hourglass timer mechanics described in the game rules.
 */
public class BuildingTimer {
    private static final long TIMER_DURATION_MS = 90_000; // 1 minute 30 seconds
    
    public enum TimerState {
        IDLE,           // Timer ready but not started
        FIRST_STAGE,    // First 90-second timer running
        SECOND_STAGE,   // Second 90-second timer running (only players with completed ships can flip)
        FINISHED        // Building phase ended
    }
    
    public enum TimerEvent {
        TIMER_FLIPPED,
        TIMER_EXPIRED,
        BUILDING_ENDED
    }
    
    public interface TimerEventListener {
        void onTimerEvent(TimerEvent event, String playerId, long timeRemaining);
    }
    
    private final GameLevel gameLevel;
    private final ScheduledExecutorService executor;
    private TimerState currentState;
    private ScheduledFuture<?> currentTimer;
    private long timerStartTime;
    private long timerDuration;
    private TimerEventListener eventListener;
    private String currentFlipperPlayerId;
    
    public BuildingTimer(GameLevel gameLevel) {
        this.gameLevel = gameLevel;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.currentState = TimerState.IDLE;
        this.timerDuration = TIMER_DURATION_MS;
    }
    
    public void setEventListener(TimerEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Starts the building phase timer system.
     * For TEST_FLIGHT: No timer restrictions
     * For LEVEL_II: Initializes the three-stage hourglass system
     */
    public void startBuildingPhase() {
        if (gameLevel == GameLevel.TEST_FLIGHT) {
            // Test flight has no timer restrictions
            currentState = TimerState.IDLE;
            return;
        }
        
        // Level II starts in idle state, waiting for first player to flip
        currentState = TimerState.IDLE;
        notifyEvent(TimerEvent.TIMER_FLIPPED, null, TIMER_DURATION_MS);
    }
    
    /**
     * Attempts to flip the timer to the next stage.
     * @param playerId The player attempting to flip the timer
     * @param playerHasCompletedShip Whether the player has completed their ship assembly
     * @return true if timer was successfully flipped, false otherwise
     */
    public boolean flipTimer(String playerId, boolean playerHasCompletedShip) {
        if (gameLevel == GameLevel.TEST_FLIGHT) {
            // Test flight has no timer restrictions
            return false;
        }
        
        switch (currentState) {
            case IDLE -> {
                // Any player can flip from idle to first stage
                startFirstStage(playerId);
                return true;
            }
            case FIRST_STAGE -> {
                // Any player can flip from first to second stage
                startSecondStage(playerId);
                return true;
            }
            case SECOND_STAGE -> {
                // Only players with completed ships can flip to end building
                if (playerHasCompletedShip) {
                    endBuildingPhase(playerId);
                    return true;
                } else {
                    return false; // Player cannot flip - ship not completed
                }
            }
            case FINISHED -> {
                return false; // Building phase already ended
            }
        }
        
        return false;
    }
    
    /**
     * Gets the current timer state
     */
    public TimerState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the time remaining in the current timer stage
     * @return Time remaining in milliseconds, or -1 if no timer is active
     */
    public long getTimeRemaining() {
        if (currentTimer == null || currentTimer.isDone()) {
            return -1;
        }
        
        long elapsed = System.currentTimeMillis() - timerStartTime;
        return Math.max(0, timerDuration - elapsed);
    }
    
    /**
     * Gets the player ID who flipped the current timer stage
     */
    public String getCurrentFlipperPlayerId() {
        return currentFlipperPlayerId;
    }
    
    /**
     * Checks if the timer system is active for this game level
     */
    public boolean isTimerActive() {
        return gameLevel == GameLevel.LEVEL_II && currentState != TimerState.IDLE;
    }
    
    /**
     * Forces the building phase to end (e.g., when all players finish)
     */
    public void forceEndBuildingPhase() {
        if (currentTimer != null) {
            currentTimer.cancel(false);
        }
        currentState = TimerState.FINISHED;
        notifyEvent(TimerEvent.BUILDING_ENDED, null, 0);
    }
    
    /**
     * Shuts down the timer system
     */
    public void shutdown() {
        if (currentTimer != null) {
            currentTimer.cancel(false);
        }
        executor.shutdown();
    }
    
    private void startFirstStage(String playerId) {
        currentState = TimerState.FIRST_STAGE;
        currentFlipperPlayerId = playerId;
        startTimer();
        notifyEvent(TimerEvent.TIMER_FLIPPED, playerId, timerDuration);
    }
    
    private void startSecondStage(String playerId) {
        if (currentTimer != null) {
            currentTimer.cancel(false);
        }
        
        currentState = TimerState.SECOND_STAGE;
        currentFlipperPlayerId = playerId;
        startTimer();
        notifyEvent(TimerEvent.TIMER_FLIPPED, playerId, timerDuration);
    }
    
    private void endBuildingPhase(String playerId) {
        if (currentTimer != null) {
            currentTimer.cancel(false);
        }
        
        currentState = TimerState.FINISHED;
        currentFlipperPlayerId = playerId;
        notifyEvent(TimerEvent.BUILDING_ENDED, playerId, 0);
    }
    
    private void startTimer() {
        timerStartTime = System.currentTimeMillis();
        
        currentTimer = executor.schedule(() -> {
            // Timer expired
            if (currentState == TimerState.FIRST_STAGE) {
                // First timer expired - wait for someone to flip to second stage
                // Building continues indefinitely until flipped
                notifyEvent(TimerEvent.TIMER_EXPIRED, currentFlipperPlayerId, 0);
            } else if (currentState == TimerState.SECOND_STAGE) {
                // Second timer expired - building phase ends automatically
                currentState = TimerState.FINISHED;
                notifyEvent(TimerEvent.BUILDING_ENDED, currentFlipperPlayerId, 0);
            }
        }, timerDuration, TimeUnit.MILLISECONDS);
    }
    
    private void notifyEvent(TimerEvent event, String playerId, long timeRemaining) {
        if (eventListener != null) {
            eventListener.onTimerEvent(event, playerId, timeRemaining);
        }
    }
}
package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.enums.GameLevel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Manages the building phase timer system for Level II games.
 * Implements the three-stage hourglass timer mechanics described in the game rules.
 */
public class BuildingTimer implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final long TIMER_DURATION_MS = 90_000; // 1 minute 30 seconds
    
    public enum TimerStage {
        NOT_STARTED(0, "Building Not Started"),
        FIRST_TIMER(1, "First Timer Running"),
        FIRST_EXPIRED(2, "First Timer Expired - Waiting for Flip"),
        SECOND_TIMER(3, "Second Timer Running"),
        SECOND_EXPIRED(4, "Second Timer Expired - Waiting for Flip"),
        BUILDING_ENDED(5, "Building Phase Ended");

        private final int order;
        private final String description;

        TimerStage(int order, String description) {
            this.order = order;
            this.description = description;
        }

        public boolean canTransitionTo(TimerStage other) {
            return other.order == this.order + 1;
        }

        public String getDescription() {
            return description;
        }

        public int getOrder() {
            return order;
        }
    }
    
    public enum TimerState {
        IDLE,
        FIRST_TIMER_RUNNING,
        FIRST_TIMER_EXPIRED,
        SECOND_TIMER_RUNNING,
        SECOND_TIMER_EXPIRED,
        BUILDING_ENDED
    }
    
    public enum TimerEvent {
        FIRST_TIMER_STARTED,
        FIRST_TIMER_EXPIRED,
        SECOND_TIMER_STARTED,
        SECOND_TIMER_EXPIRED,
        BUILDING_ENDED,
        FLIP_ATTEMPT_FAILED
    }

    public enum FlipResult {
        SUCCESS,
        TIMER_NOT_EXPIRED,
        PLAYER_NOT_FINISHED,
        INVALID_STAGE,
        BUILDING_ALREADY_ENDED
    }
    
    public interface TimerEventListener {
        void onTimerEvent(TimerEvent event, String playerId, long timeRemaining);
    }
    
    private final GameLevel gameLevel;
    private transient ScheduledExecutorService executor;
    private TimerStage currentStage;
    private transient ScheduledFuture<?> currentTimer;
    private long stageStartTime;
    private final long STAGE_DURATION;
    private TimerEventListener eventListener;
    private String lastFlipperPlayerId;
    private int totalFlips;
    private final Map<String, Boolean> playerFinishStatus; // Track who has finished
    
    public BuildingTimer(GameLevel gameLevel) {
        this.gameLevel = gameLevel;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.currentStage = TimerStage.NOT_STARTED;
        this.STAGE_DURATION = gameLevel.getDuration() > 0 ? gameLevel.getDuration() : TIMER_DURATION_MS;
        this.totalFlips = 0;
        this.playerFinishStatus = new ConcurrentHashMap<>();
    }
    
    public void setEventListener(TimerEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * Starts the building phase timer system.
     * Initializes the three-stage hourglass system for supported levels.
     */
    public void startBuildingPhase() {
        currentStage = TimerStage.NOT_STARTED;
        totalFlips = 0;
        playerFinishStatus.clear();
    }
    
    /**
     * Attempts to flip the timer to the next stage following Galaxy Trucker rules.
     * @param playerId The player attempting to flip the timer
     * @param playerHasFinishedShip Whether the player has finished their ship assembly
     * @return FlipResult indicating success or reason for failure
     */
    public synchronized FlipResult flipTimer(String playerId, boolean playerHasFinishedShip) {
        // Update player finish status
        if (playerHasFinishedShip) {
            playerFinishStatus.put(playerId, true);
        }

        switch (currentStage) {
            case NOT_STARTED -> {
                // Anyone can start the first timer
                startFirstTimer(playerId);
                return FlipResult.SUCCESS;
            }

            case FIRST_TIMER -> {
                // Anyone can flip to second timer, but first must be expired
                if (!isCurrentStageExpired()) {
                    return FlipResult.TIMER_NOT_EXPIRED;
                }
                startSecondTimer(playerId);
                return FlipResult.SUCCESS;
            }

            case FIRST_EXPIRED -> {
                // Anyone can flip expired first timer to second timer
                startSecondTimer(playerId);
                return FlipResult.SUCCESS;
            }

            case SECOND_TIMER -> {
                // Only finished players can end building phase
                if (!isCurrentStageExpired()) {
                    return FlipResult.TIMER_NOT_EXPIRED;
                }
                if (!playerHasFinishedShip) {
                    return FlipResult.PLAYER_NOT_FINISHED;
                }
                endBuildingPhase(playerId);
                return FlipResult.SUCCESS;
            }

            case SECOND_EXPIRED -> {
                // Only finished players can end building phase
                if (!playerHasFinishedShip) {
                    return FlipResult.PLAYER_NOT_FINISHED;
                }
                endBuildingPhase(playerId);
                return FlipResult.SUCCESS;
            }

            case BUILDING_ENDED -> {
                return FlipResult.BUILDING_ALREADY_ENDED;
            }
        }

        return FlipResult.INVALID_STAGE;
    }
    
    /**
     * Gets the current timer stage
     */
    public TimerStage getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Gets the current timer state (compatible with existing code)
     */
    public TimerState getCurrentState() {
        return switch (currentStage) {
            case NOT_STARTED -> TimerState.IDLE;
            case FIRST_TIMER -> TimerState.FIRST_TIMER_RUNNING;
            case FIRST_EXPIRED -> TimerState.FIRST_TIMER_EXPIRED;
            case SECOND_TIMER -> TimerState.SECOND_TIMER_RUNNING;
            case SECOND_EXPIRED -> TimerState.SECOND_TIMER_EXPIRED;
            case BUILDING_ENDED -> TimerState.BUILDING_ENDED;
        };
    }
    
    /**
     * Gets the time remaining in the current timer stage
     * @return Time remaining in milliseconds, or -1 if no timer is active
     */
    public long getTimeRemaining() {
        if (currentStage == TimerStage.NOT_STARTED || currentStage == TimerStage.BUILDING_ENDED) {
            return -1;
        }
        if (currentStage == TimerStage.FIRST_EXPIRED || currentStage == TimerStage.SECOND_EXPIRED) {
            return 0; // Expired but waiting for flip
        }

        long elapsed = System.currentTimeMillis() - stageStartTime;
        return Math.max(0, STAGE_DURATION - elapsed);
    }
    
    /**
     * Gets the player ID who last flipped the timer
     */
    public String getLastFlipperPlayerId() {
        return lastFlipperPlayerId;
    }

    /**
     * Gets the total number of timer flips performed
     */
    public int getTotalFlips() {
        return totalFlips;
    }
    
    /**
     * Checks if the timer system is active
     */
    public boolean isTimerActive() {
        return currentStage != TimerStage.NOT_STARTED && currentStage != TimerStage.BUILDING_ENDED;
    }

    /**
     * Checks if the current stage has expired
     */
    public boolean isCurrentStageExpired() {
        if (currentStage == TimerStage.FIRST_EXPIRED || currentStage == TimerStage.SECOND_EXPIRED) {
            return true;
        }
        if (currentStage == TimerStage.FIRST_TIMER || currentStage == TimerStage.SECOND_TIMER) {
            long elapsed = System.currentTimeMillis() - stageStartTime;
            return elapsed >= STAGE_DURATION;
        }
        return false;
    }
    
    /**
     * Forces the building phase to end (e.g., when all players finish)
     */
    public void forceEndBuildingPhase() {
        cancelCurrentTimer();
        currentStage = TimerStage.BUILDING_ENDED;
        totalFlips++;
        notifyTimerEvent(TimerEvent.BUILDING_ENDED, "FORCED_END");
    }
    
    /**
     * Shuts down the timer system
     */
    public void shutdown() {
        cancelCurrentTimer();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    /**
     * Reinitializes transient fields after deserialization
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }
    
    private void startFirstTimer(String playerId) {
        currentStage = TimerStage.FIRST_TIMER;
        stageStartTime = System.currentTimeMillis();
        lastFlipperPlayerId = playerId;
        totalFlips++;

        scheduleStageExpiration();
        notifyTimerEvent(TimerEvent.FIRST_TIMER_STARTED, playerId);
    }

    private void startSecondTimer(String playerId) {
        cancelCurrentTimer();
        currentStage = TimerStage.SECOND_TIMER;
        stageStartTime = System.currentTimeMillis();
        lastFlipperPlayerId = playerId;
        totalFlips++;

        scheduleStageExpiration();
        notifyTimerEvent(TimerEvent.SECOND_TIMER_STARTED, playerId);
    }

    private void endBuildingPhase(String playerId) {
        cancelCurrentTimer();
        currentStage = TimerStage.BUILDING_ENDED;
        lastFlipperPlayerId = playerId;
        totalFlips++;

        notifyTimerEvent(TimerEvent.BUILDING_ENDED, playerId);
    }

    private void scheduleStageExpiration() {
        currentTimer = executor.schedule(() -> {
            synchronized(this) {
                if (currentStage == TimerStage.FIRST_TIMER) {
                    currentStage = TimerStage.FIRST_EXPIRED;
                    notifyTimerEvent(TimerEvent.FIRST_TIMER_EXPIRED, lastFlipperPlayerId);
                } else if (currentStage == TimerStage.SECOND_TIMER) {
                    currentStage = TimerStage.SECOND_EXPIRED;
                    notifyTimerEvent(TimerEvent.SECOND_TIMER_EXPIRED, lastFlipperPlayerId);

                    // Galaxy Trucker rule: If second timer expires, building ends automatically
                    // Give a 10-second grace period for finished players to flip
                    executor.schedule(() -> {
                        synchronized(this) {
                            if (currentStage == TimerStage.SECOND_EXPIRED) {
                                currentStage = TimerStage.BUILDING_ENDED;
                                notifyTimerEvent(TimerEvent.BUILDING_ENDED, "AUTO_TIMEOUT");
                            }
                        }
                    }, 10000, TimeUnit.MILLISECONDS);
                }
            }
        }, STAGE_DURATION, TimeUnit.MILLISECONDS);
    }

    private void cancelCurrentTimer() {
        if (currentTimer != null && !currentTimer.isDone()) {
            currentTimer.cancel(false);
        }
    }

    private void notifyTimerEvent(TimerEvent event, String playerId) {
        if (eventListener != null) {
            eventListener.onTimerEvent(event, playerId, getTimeRemaining());
        }
    }
}
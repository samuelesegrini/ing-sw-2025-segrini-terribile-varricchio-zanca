package it.polimi.ingsw.client.ui;

/**
 * Interface for handling UI thread operations in a platform-agnostic way.
 * Provides abstraction over UI thread execution, allowing code to schedule operations
 * on the appropriate thread without being tied to specific UI frameworks.
 * 
 * This is crucial for maintaining separation between the GUI (JavaFX) and TUI implementations,
 * as they have different threading models and requirements.
 */
public interface UIThreadHandler {
    /**
     * Executes the given runnable on the UI thread.
     * For JavaFX, this would use Platform.runLater(), while for TUI it might
     * execute directly as there's no separate UI thread.
     * 
     * @param runnable The operation to execute on the UI thread
     */
    void runOnUIThread(Runnable runnable);
    
    /**
     * Executes a task in the background thread and then updates the UI with the result.
     * This is a convenience method that creates a daemon thread for the background work,
     * captures the result, and ensures the UI update happens on the appropriate thread.
     * 
     * @param backgroundTask The task to run in the background thread
     * @param uiUpdateTask The task to run on the UI thread with the result of the background task
     * @param <T> The type of the result returned by the background task
     */
    default <T> void runInBackgroundThenUpdateUI(BackgroundTask<T> backgroundTask, UIUpdateTask<T> uiUpdateTask) {
        Thread backgroundThread = new Thread(() -> {
            try {
                T result = backgroundTask.execute();
                runOnUIThread(() -> uiUpdateTask.update(result));
            } catch (Exception e) {
                runOnUIThread(() -> uiUpdateTask.handleError(e));
            }
        });
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }
    
    /**
     * Functional interface for tasks that execute in the background thread.
     * 
     * @param <T> The type of result produced by the background task
     */
    @FunctionalInterface
    interface BackgroundTask<T> {
        /**
         * Executes the background operation.
         * 
         * @return The result of the background operation
         * @throws Exception If the background operation fails
         */
        T execute() throws Exception;
    }
    
    /**
     * Functional interface for tasks that update the UI with background task results.
     * 
     * @param <T> The type of result consumed by the UI update
     */
    interface UIUpdateTask<T> {
        /**
         * Updates the UI with the result from the background task.
         * 
         * @param result The result from the background task
         */
        void update(T result);
        
        /**
         * Handles errors that occurred during background task execution.
         * Default implementation prints the error to standard error.
         * 
         * @param e The exception that occurred during background execution
         */
        default void handleError(Exception e) {
            System.err.println("Error in background task: " + e.getMessage());
        }
    }
} 
package it.polimi.ingsw.client.ui.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Unified threading service for UI operations.
 * Abstracts away the differences between JavaFX Platform.runLater() and TUI threading.
 */
public interface UIThreadService {
    
    /**
     * Executes a task on the UI thread.
     * For JavaFX: uses Platform.runLater()
     * For TUI: executes immediately or on designated thread
     */
    void runOnUIThread(Runnable task);
    
    /**
     * Executes a task asynchronously and returns when complete.
     */
    CompletableFuture<Void> runAsync(Runnable task);
    
    /**
     * Executes a task with a result on the UI thread.
     */
    <T> CompletableFuture<T> supplyOnUIThread(java.util.function.Supplier<T> supplier);
    
    /**
     * Checks if the current thread is the UI thread.
     */
    boolean isUIThread();
    
    /**
     * Gets an executor for background tasks.
     */
    Executor getBackgroundExecutor();
    
    /**
     * Shuts down the threading service.
     */
    void shutdown();
}
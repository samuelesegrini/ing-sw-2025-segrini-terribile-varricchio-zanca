package it.polimi.ingsw.client.ui.gui;

import it.polimi.ingsw.client.ui.core.UIThreadService;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * JavaFX implementation of the UI thread service.
 * Uses Platform.runLater() for UI thread operations.
 */
public class GuiThreadService implements UIThreadService {
    
    private final Executor backgroundExecutor;
    
    public GuiThreadService() {
        this.backgroundExecutor = ForkJoinPool.commonPool();
    }
    
    @Override
    public void runOnUIThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
    
    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, backgroundExecutor);
    }
    
    @Override
    public <T> CompletableFuture<T> supplyOnUIThread(java.util.function.Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        } else {
            CompletableFuture<T> future = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    T result = supplier.get();
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }
    }
    
    @Override
    public boolean isUIThread() {
        return Platform.isFxApplicationThread();
    }
    
    @Override
    public Executor getBackgroundExecutor() {
        return backgroundExecutor;
    }
    
    @Override
    public void shutdown() {
        // JavaFX Platform doesn't need explicit shutdown
        // Background executor is the common pool, so no shutdown needed
    }
}
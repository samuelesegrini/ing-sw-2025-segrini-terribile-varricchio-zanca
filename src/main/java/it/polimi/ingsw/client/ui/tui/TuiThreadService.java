package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.ui.core.UIThreadService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TUI implementation of the UI thread service.
 * Since TUI doesn't have a specific UI thread, all operations are immediate.
 */
public class TuiThreadService implements UIThreadService {
    
    private final ExecutorService backgroundExecutor;
    private final Thread mainThread;
    
    public TuiThreadService() {
        this.backgroundExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "TUI-Background");
            t.setDaemon(true);
            return t;
        });
        this.mainThread = Thread.currentThread();
    }
    
    @Override
    public void runOnUIThread(Runnable task) {
        // In TUI, we don't have a specific UI thread requirement
        // Execute immediately on current thread
        task.run();
    }
    
    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, backgroundExecutor);
    }
    
    @Override
    public <T> CompletableFuture<T> supplyOnUIThread(java.util.function.Supplier<T> supplier) {
        // Since TUI doesn't require specific thread, execute immediately
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public boolean isUIThread() {
        // Consider the main thread as the "UI thread" for TUI
        return Thread.currentThread() == mainThread;
    }
    
    @Override
    public Executor getBackgroundExecutor() {
        return backgroundExecutor;
    }
    
    @Override
    public void shutdown() {
        backgroundExecutor.shutdown();
    }
}
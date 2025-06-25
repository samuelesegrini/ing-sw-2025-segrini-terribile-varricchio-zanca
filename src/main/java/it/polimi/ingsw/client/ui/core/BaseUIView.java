package it.polimi.ingsw.client.ui.core;

import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import it.polimi.ingsw.client.core.UIRefreshable;
import it.polimi.ingsw.client.ui.core.UIContext;

/**
 * Abstract base implementation for UI views with Simple Direct Model Architecture.
 */
public abstract class BaseUIView implements UIView, UIRefreshable {
    protected static final Logger LOGGER = Logger.getLogger(BaseUIView.class.getName());
    protected UIContext context;
    protected boolean active = false;
    protected boolean initialized = false;
    public void initialize(UIContext context) {
        if (this.initialized) return;
        this.context = context;
        this.initialized = true;
        
        // Register with ClientState for automatic refresh
        context.getClientState().registerRefreshableView(this);
        
        // Register for model changes for lobby/connection UI
        context.getClientState().addPropertyChangeListener(this);

        LOGGER.info("Initialized view: " + getTitle());
    }
    @Override
    public void show() {
        if (!initialized) {
            throw new IllegalStateException("View must be initialized before showing: " + getTitle());
        }
        active = true;
        onShow();
        LOGGER.info("Showing view: " + getTitle());
    }
    @Override
    public void hide() {
        if (active) {
            active = false;
            onHide();
            LOGGER.info("Hiding view: " + getTitle());
        }
    }
    @Override
    public boolean isActive() {
        return active;
    }
    @Override
    public void refresh() {
        if (active) {
            onRefresh();
        }
    }
    
    public void refreshFromServerModels() {
        if (active && context != null) {
            context.getThreadService().runOnUIThread(this::onRefresh);
        }
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (active) {
            context.getThreadService().runOnUIThread(() -> {
                onPropertyChange(evt);
                refresh();
            });
        }
    }
    @Override
    public void dispose() {
        if (context != null) {
            context.getClientState().unregisterRefreshableView(this);
            context.getClientState().removePropertyChangeListener(this);
        }
        active = false;
        initialized = false;

        onDispose();
        LOGGER.info("Disposed view: " + getTitle());
    }
    /**
     Called when the view is shown.
     Subclasses should override this to perform show-specific logic.
     */
    protected abstract void onShow();
    /**
     Called when the view is hidden.
     Subclasses should override this to perform hide-specific logic.
     */
    protected abstract void onHide();
    /**
     Called when the view needs to be refreshed.
     Subclasses should override this to update their display.
     */
    protected abstract void onRefresh();
    /**
     Called when a property change event occurs.
     Subclasses can override this to handle specific property changes.
     */
    protected void onPropertyChange(PropertyChangeEvent evt) {}
    /**
     Called when the view is being disposed.
     Subclasses should override this to clean up resources.
     */
    protected void onDispose() {
    }
}
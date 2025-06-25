package it.polimi.ingsw.client;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Client-side model that maintains only connection, authentication, and view state.
 * All game and lobby state is now managed by ClientState (see new architecture).
 */
public class ClientModel {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // Connection state
    private boolean connected = false;
    private String serverHost;
    private int serverPort;
    private BooleanProperty connectedProperty = new SimpleBooleanProperty(this, "connected", connected);

    // Authentication state
    private boolean authenticated = false;
    private String playerId;
    private String nickname;
    private BooleanProperty authenticatedProperty = new SimpleBooleanProperty(this, "authenticated", authenticated);

    // View state
    private ViewState currentView = ViewState.CONNECTION;

    public enum ViewState {
        CONNECTION,
        LOGIN,
        LOBBY,
        GAME_LOBBY,
        GAME
    }

    // Property change support
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // Connection state
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        boolean old = this.connected;
        this.connected = connected;
        this.connectedProperty.set(connected);
        pcs.firePropertyChange("connected", old, connected);
    }

    public BooleanProperty connectedProperty() {
        return connectedProperty;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        String old = this.serverHost;
        this.serverHost = serverHost;
        pcs.firePropertyChange("serverHost", old, serverHost);
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        int old = this.serverPort;
        this.serverPort = serverPort;
        pcs.firePropertyChange("serverPort", old, serverPort);
    }

    // Authentication state
    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        boolean old = this.authenticated;
        this.authenticated = authenticated;
        this.authenticatedProperty.set(authenticated);
        pcs.firePropertyChange("authenticated", old, authenticated);
    }

    public BooleanProperty authenticatedProperty() {
        return authenticatedProperty;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        String old = this.playerId;
        this.playerId = playerId;
        pcs.firePropertyChange("playerId", old, playerId);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        String old = this.nickname;
        this.nickname = nickname;
        pcs.firePropertyChange("nickname", old, nickname);
    }

    // View state
    public ViewState getCurrentView() {
        return currentView;
    }

    public void setCurrentView(ViewState view) {
        ViewState old = this.currentView;
        this.currentView = view;
        pcs.firePropertyChange("currentView", old, view);
    }

    // Utility methods
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    public boolean isLoggedIn() {
        return isAuthenticated();
    }

    public String getCurrentNickname() {
        return getNickname();
    }
}

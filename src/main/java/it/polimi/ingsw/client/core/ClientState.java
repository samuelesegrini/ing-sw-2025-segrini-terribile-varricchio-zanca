package it.polimi.ingsw.client.core;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.GamePhase;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Direct Model Architecture client state.
 * Stores server models directly and refreshes only the current visible view.
 */
public class ClientState {

    public enum ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED
    }

    public enum ViewState {
        CONNECTION, LOGIN, LOBBY, GAME_LOBBY, GAME
    }

    // Connection & Authentication
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    private PlayerId playerId;
    private String playerNickname;

    // Lobby State
    private List<GameModel> availableGames;
    private List<Player> playersInLobby;
    private GameModel currentGameLobby;

    // Game State (null when not in game)
    private GameModel gameModel;

    // UI State
    private ViewState currentView = ViewState.CONNECTION;
    private UIRefreshable currentViewComponent;
    private final Set<UIRefreshable> registeredViews = ConcurrentHashMap.newKeySet();

    // Property change support for lobby/connection UI
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    // JavaFX properties for UI binding
    private final BooleanProperty connectedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty authenticatedProperty = new SimpleBooleanProperty(false);

    // === Connection Management ===
    public void setConnectionStatus(ConnectionStatus status) {
        ConnectionStatus old = this.connectionStatus;
        this.connectionStatus = status;
        connectedProperty.set(status == ConnectionStatus.CONNECTED);
        pcs.firePropertyChange("connectionStatus", old, status);
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setPlayerInfo(PlayerId playerId, String nickname) {
        this.playerId = playerId;
        this.playerNickname = nickname;
        authenticatedProperty.set(playerId != null);
        pcs.firePropertyChange("playerInfo", null, Map.of("id", playerId, "nickname", nickname));
    }

    public String getPlayerId() {
        return playerId != null ? playerId.toString() : null;
    }
    
    public PlayerId getPlayerIdObject() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }
    
    /**
     * Gets the player nickname (alias for getPlayerNickname)
     * @return The player nickname
     */
    public String getNickname() {
        return playerNickname;
    }

    // === Lobby Management ===
    public void setAvailableGames(List<GameModel> games) {
        this.availableGames = games;
        pcs.firePropertyChange("availableGames", null, games);
    }

    public List<GameModel> getAvailableGames() {
        return availableGames;
    }
    
    /**
     * Gets joinable games (alias for getAvailableGames)
     * @return List of joinable games
     */
    public List<GameModel> getJoinableGames() {
        return availableGames;
    }
    
    /**
     * Gets games in progress (stub - returns empty for now)
     * @return List of games in progress
     */
    public List<GameModel> getGamesInProgress() {
        // TODO: Implement filtering for games in progress
        return List.of();
    }

    public void setCurrentGameLobby(GameModel gameModel) {
        this.currentGameLobby = gameModel;
        pcs.firePropertyChange("currentGameLobby", null, gameModel);
    }

    public GameModel getCurrentGameLobby() {
        return currentGameLobby;
    }

    public void setPlayersInLobby(List<Player> players) {
        this.playersInLobby = players;
        pcs.firePropertyChange("playersInLobby", null, players);
    }

    public List<Player> getPlayersInLobby() {
        return playersInLobby;
    }

    // === Game State Management ===
    public void setGameModel(GameModel newGameModel) {
        this.gameModel = newGameModel;
        refreshCurrentView(); // Refresh game views
    }

    public GameModel getGameModel() {
        return gameModel;
    }
    
    /**
     * Gets the current game (alias for getGameModel for compatibility)
     * @return The current game model
     */
    public GameModel getCurrentGame() {
        return gameModel;
    }

    public void updatePlayer(Player updatedPlayer) {
        if (gameModel != null) {
            for (int i = 0; i < gameModel.getPlayers().size(); i++) {
                Player localPlayer = gameModel.getPlayers().get(i);
                if (localPlayer.getId().equals(updatedPlayer.getId())) {
                    gameModel.getPlayers().set(i, updatedPlayer);
                    break;
                }
            }
            refreshCurrentViewOnly();
        }
    }

    public void updateLocalPlayerShip(Ship updatedShip) {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null) {
            localPlayer.setShip(updatedShip);
            refreshCurrentViewOnly();
        }
    }

    public void updateComponentDeck(ComponentDeck updatedDeck) {
        if (gameModel != null) {
            gameModel.setComponentDeck(updatedDeck);
            refreshCurrentViewOnly();
        }
    }

    // === UI View Management ===
    public void setCurrentView(ViewState viewState, UIRefreshable viewComponent) {
        ViewState oldView = this.currentView;
        this.currentView = viewState;
        this.currentViewComponent = viewComponent;

        if (viewComponent != null) {
            registeredViews.add(viewComponent);
            Platform.runLater(() -> viewComponent.refresh());
        }

        pcs.firePropertyChange("currentView", oldView, viewState);
    }

    public ViewState getCurrentView() {
        return currentView;
    }

    public void setCurrentView(ViewState viewState) {
        ViewState oldView = this.currentView;
        this.currentView = viewState;
        pcs.firePropertyChange("currentView", oldView, viewState);
    }

    // === Direct Model Access (Game) ===
    public Ship getLocalPlayerShip() {
        Player localPlayer = getLocalPlayer();
        return localPlayer != null ? localPlayer.getShip() : null;
    }

    public ComponentDeck getComponentDeck() {
        return gameModel != null ? gameModel.getComponentDeck() : null;
    }

    public GamePhase getCurrentPhase() {
        return gameModel != null ? gameModel.getCurrentPhase() : GamePhase.SETUP;
    }

    //TODO: temporary fix converting PlayerId and string
    public Player getLocalPlayer() {
        if (gameModel != null && playerId != null) {
            // Assuming PlayerId can be constructed from a String
            return gameModel.getPlayerById(playerId);
        }
        return null;
    }

    // === Direct Model Access (Lobby) ===
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    public boolean isInGame() {
        return gameModel != null;
    }
    
    /**
     * Gets the JavaFX property for connection status binding
     * @return The connected property
     */
    public BooleanProperty connectedProperty() {
        return connectedProperty;
    }
    
    /**
     * Gets the JavaFX property for authentication status binding
     * @return The authenticated property
     */
    public BooleanProperty authenticatedProperty() {
        return authenticatedProperty;
    }
    
    public String getCurrentGameId() {
        return currentGameLobby != null ? currentGameLobby.getGameId() : null;
    }
    
    /**
     * Gets the current game lobby information
     * @return The current game lobby info, or null if not in a lobby
     */
    public GameModel getCurrentGameInfo() {
        return currentGameLobby;
    }
    
    /**
     * Checks if a player is ready (stub - needs proper implementation)
     * @param playerId The player ID to check
     * @return true if ready, false otherwise
     */
    public boolean isPlayerReady(String playerId) {
        // TODO: Implement proper ready state checking
        // This should query the game model or maintain ready state
        return false;
    }
    
    /**
     * Sets the authenticated status (based on having a playerId)
     * @param authenticated Whether the user is authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        if (!authenticated) {
            this.playerId = null;
            this.playerNickname = null;
        }
        // If setting to true, playerId should already be set via setPlayerInfo
    }
    
    /**
     * Checks if the user is authenticated
     * @return true if authenticated (has playerId)
     */
    public boolean isAuthenticated() {
        return playerId != null;
    }
    
    /**
     * Checks if the user is logged in (alias for isAuthenticated)
     * @return true if logged in
     */
    public boolean isLoggedIn() {
        return isAuthenticated();
    }
    
    /**
     * Gets the current nickname (alias for getNickname)
     * @return The current nickname
     */
    public String getCurrentNickname() {
        return getNickname();
    }

    // Simple UI Updates - Only Current View
    private void refreshCurrentViewOnly() {
        if (currentViewComponent != null) {
            Platform.runLater(() -> currentViewComponent.refresh());
        }
    }

    public void registerRefreshableView(UIRefreshable view) {
        if (view != null) {
            registeredViews.add(view);
        }
    }

    public void unregisterRefreshableView(UIRefreshable view) {
        registeredViews.remove(view);
        if (currentViewComponent == view) {
            currentViewComponent = null;
        }
    }

    private void refreshCurrentView() {
        refreshCurrentViewOnly();
    }

    // Property change listener support for lobby/connection UI
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }
    
    // === Property Change Methods ===
    
    /**
     * Fires a property change event
     * @param propertyName Property name
     * @param oldValue Old value
     * @param newValue New value
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
        refreshCurrentViewOnly();
    }
    
    // === Missing Methods for Response Handlers ===
    
    /**
     * Sets the ready status for a player in the lobby
     * @param playerId The player ID
     * @param ready The ready status
     */
    public void setPlayerReadyStatus(String playerId, boolean ready) {
        if (playersInLobby != null) {
            for (Player player : playersInLobby) {
                if (player.getId().equals(playerId)) {
                    player.setReady(ready);
                    pcs.firePropertyChange("playerReadyStatus", null, Map.of("playerId", playerId, "ready", ready));
                    refreshCurrentViewOnly();
                    break;
                }
            }
        }
    }
    
    /**
     * Sets ship validation result
     * @param isValid Whether the ship is valid
     * @param errors List of validation errors
     */
    public void setShipValidation(boolean isValid, java.util.List<String> errors) {
        pcs.firePropertyChange("shipValidation", null, java.util.Map.of("valid", isValid, "errors", errors));
        refreshCurrentViewOnly();
    }
    
    /**
     * Sets player ready status
     * @param playerId Player ID
     * @param ready Ready status
     */
    public void setPlayerReady(String playerId, boolean ready) {
        setPlayerReadyStatus(playerId, ready);
    }
    
    /**
     * Updates player ready status (alias for setPlayerReadyStatus)
     * @param playerId Player ID
     * @param ready Ready status
     */
    public void updatePlayerReadyStatus(String playerId, boolean ready) {
        setPlayerReadyStatus(playerId, ready);
    }
}
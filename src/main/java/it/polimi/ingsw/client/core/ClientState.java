package it.polimi.ingsw.client.core;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId; // Added import
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.GamePhase;
import javafx.application.Platform;

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
    private List<GameInfo> availableGames;
    private List<Player> playersInLobby;
    private GameInfo currentGameLobby;

    // Game State (null when not in game)
    private GameModel gameModel;

    // UI State
    private ViewState currentView = ViewState.CONNECTION;
    private UIRefreshable currentViewComponent;
    private final Set<UIRefreshable> registeredViews = ConcurrentHashMap.newKeySet();

    // Property change support for lobby/connection UI
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // === Connection Management ===
    public void setConnectionStatus(ConnectionStatus status) {
        ConnectionStatus old = this.connectionStatus;
        this.connectionStatus = status;
        pcs.firePropertyChange("connectionStatus", old, status);
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setPlayerInfo(PlayerId playerId, String nickname) {
        this.playerId = playerId;
        this.playerNickname = nickname;
        pcs.firePropertyChange("playerInfo", null, Map.of("id", playerId, "nickname", nickname));
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    // === Lobby Management ===
    public void setAvailableGames(List<GameInfo> games) {
        this.availableGames = games;
        pcs.firePropertyChange("availableGames", null, games);
    }

    public List<GameInfo> getAvailableGames() {
        return availableGames;
    }

    public void setCurrentGameLobby(GameInfo gameInfo) {
        this.currentGameLobby = gameInfo;
        pcs.firePropertyChange("currentGameLobby", null, gameInfo);
    }

    public GameInfo getCurrentGameLobby() {
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
}
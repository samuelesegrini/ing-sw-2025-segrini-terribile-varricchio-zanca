package it.polimi.ingsw.client;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.server.model.enums.GamePhase;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.*;
import java.beans.*;

/**
 * Client-side model that maintains the current state of the client.
 * Uses PropertyChangeSupport for observer pattern.
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

    // Game state
    private String currentGameId;
    private List<GameInfo> availableGames = new ArrayList<>();
    private GameInfo currentGame;
    private List<PlayerInfo> playersInLobby = new ArrayList<>();

    // View state
    private ViewState currentView = ViewState.CONNECTION;

    public String getCurrentGameId() {
        return currentGameId;
    }

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

    // Game state
    public List<GameInfo> getAvailableGames() {
        return new ArrayList<>(availableGames);
    }

    public void setAvailableGames(List<GameInfo> games) {
        List<GameInfo> old = this.availableGames;
        this.availableGames = new ArrayList<>(games);
        pcs.firePropertyChange("availableGames", old, availableGames);
    }

    public void addAvailableGame(GameInfo game) {
        List<GameInfo> old = new ArrayList<>(availableGames);
        availableGames.add(game);
        pcs.firePropertyChange("availableGames", old, availableGames);
    }

    public void removeAvailableGame(String gameId) {
        List<GameInfo> old = new ArrayList<>(availableGames);
        availableGames.removeIf(g -> g.gameId.equals(gameId));
        pcs.firePropertyChange("availableGames", old, availableGames);
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

    // Game management methods
    public void setCurrentGame(GameInfo game) {
        GameInfo old = this.currentGame;
        String oldGameId = this.currentGameId;
        this.currentGame = game;
        this.currentGameId = game != null ? game.getGameId() : null;
        pcs.firePropertyChange("currentGame", old, game);
        pcs.firePropertyChange("currentGameId", oldGameId, currentGameId);
    }

    public GameInfo getCurrentGame() {
        return currentGame;
    }

    public GameInfo getCurrentGameInfo() {
        return currentGame;
    }

    public void setCurrentGameInfo(GameInfo gameInfo) {
        setCurrentGame(gameInfo);
    }

    // Players in lobby management
    public void setPlayersInLobby(List<PlayerInfo> players) {
        List<PlayerInfo> old = this.playersInLobby;
        this.playersInLobby = new ArrayList<>(players);
        pcs.firePropertyChange("playersInLobby", old, this.playersInLobby);
    }

    public List<PlayerInfo> getPlayersInLobby() {
        return new ArrayList<>(playersInLobby);
    }

    public void setPlayerReadyStatus(String playerId, boolean ready) {
        List<PlayerInfo> old = new ArrayList<>(playersInLobby);
        List<PlayerInfo> updated = new ArrayList<>();

        for (PlayerInfo player : playersInLobby) {
            if (player.playerId.equals(playerId)) {
                updated.add(new PlayerInfo(player.playerId, player.nickname, ready));
            } else {
                updated.add(player);
            }
        }

        this.playersInLobby = updated;
        pcs.firePropertyChange("playersInLobby", old, playersInLobby);
        pcs.firePropertyChange("playerReady", Map.of("playerId", playerId, "ready", !ready), Map.of("playerId", playerId, "ready", ready));
    }

    public boolean isPlayerReady(String playerId) {
        return playersInLobby.stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .map(PlayerInfo::isReady)
                .orElse(false);
    }

    public void removePlayerFromLobby(String playerId) {
        List<PlayerInfo> old = new ArrayList<>(playersInLobby);
        playersInLobby.removeIf(player -> player.playerId.equals(playerId));
        pcs.firePropertyChange("playersInLobby", old, playersInLobby);
    }

    public void addPlayerToLobby(PlayerInfo player) {
        List<PlayerInfo> old = new ArrayList<>(playersInLobby);
        playersInLobby.add(player);
        pcs.firePropertyChange("playersInLobby", old, playersInLobby);
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

    // Ship building methods integration with LocalGameState
    public void startBuildingPhase() {
        LocalGameState.getInstance().setCurrentPhase(GamePhase.BUILDING);
        LocalGameState.getInstance().resetShipBuildingState();
        setCurrentView(ViewState.GAME);
        pcs.firePropertyChange("buildingPhaseStarted", false, true);
    }

    public void endBuildingPhase() {
        LocalGameState.getInstance().setCurrentPhase(GamePhase.FLIGHT);
        pcs.firePropertyChange("buildingPhaseEnded", false, true);
    }

    public void updatePlayerReadyStatus(String playerId, boolean ready) {
        LocalGameState.getInstance().setPlayerReady(playerId, ready);
        setPlayerReadyStatus(playerId, ready); // Update local lobby state as well
        pcs.firePropertyChange("playerReadyStatus", null, Map.of(playerId, ready));
    }

    // Ship building state getters for UI binding
    public boolean isBuildingPhaseActive() {
        return LocalGameState.getInstance().isBuildingPhaseActive();
    }

    public long getBuildingTimeRemaining() {
        return LocalGameState.getInstance().getBuildingTimeRemaining();
    }

    public boolean isShipValidated() {
        return LocalGameState.getInstance().isShipValidated();
    }

    public boolean isBuildingTimerFlipped() {
        return LocalGameState.getInstance().isBuildingTimerFlipped();
    }

    public Map<LocalGameState.ComponentStatType, Integer> getShipStats() {
        return LocalGameState.getInstance().getAllShipStats();
    }

    public List<String> getShipValidationErrors() {
        return LocalGameState.getInstance().getValidationErrors();
    }

    // Timer update method for real-time synchronization
    public void updateBuildingTimer(long timeRemaining) {
        LocalGameState.getInstance().updateBuildingTimer(timeRemaining);
        pcs.firePropertyChange("buildingTimeRemaining", null, timeRemaining);
    }

    public void setBuildingTimerFlipped(boolean flipped) {
        LocalGameState.getInstance().setBuildingTimerFlipped(flipped);
        pcs.firePropertyChange("buildingTimerFlipped", !flipped, flipped);
    }

    public void setShipValidation(boolean valid, List<String> errors) {
        LocalGameState.getInstance().setShipValidation(valid, errors);
        pcs.firePropertyChange("shipValidated", !valid, valid);
        pcs.firePropertyChange("shipValidationErrors", null, errors);
    }

    // Enhanced setters to update LocalGameState
    public void setPlayerIdWithState(String playerId) {
        setPlayerId(playerId);
        LocalGameState.getInstance().setLocalPlayerId(playerId);
    }

    public void setCurrentGameWithState(GameInfo game) {
        setCurrentGame(game);
        LocalGameState.getInstance().setCurrentGameId(game != null ? game.getGameId() : null);
    }
}

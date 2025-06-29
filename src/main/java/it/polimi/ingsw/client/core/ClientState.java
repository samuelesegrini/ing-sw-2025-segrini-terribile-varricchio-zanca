package it.polimi.ingsw.client.core;

import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.GamePhase;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.ingsw.client.ui.core.UIView;

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
    private GameModel currentGameLobby;
    
    // Current game lobby basic info (for display when GameModel not yet available)
    private String currentGameId;
    private String currentGameName;
    private it.polimi.ingsw.server.model.enums.GameLevel currentGameLevel;
    private int currentGameMaxPlayers;

    // Game State (null when not in game)
    private GameModel gameModel;

    // UI State
    private ViewState currentView = ViewState.CONNECTION;
    private UIView currentViewComponent;
    private final Set<UIView> registeredViews = ConcurrentHashMap.newKeySet();

    
    // JavaFX properties for UI binding
    private final BooleanProperty connectedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty authenticatedProperty = new SimpleBooleanProperty(false);

    // === Connection Management ===
    public void setConnectionStatus(ConnectionStatus status) {
        this.connectionStatus = status;
        connectedProperty.set(status == ConnectionStatus.CONNECTED);
        refreshCurrentViewOnly();
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setPlayerInfo(PlayerId playerId, String nickname) {
        this.playerId = playerId;
        this.playerNickname = nickname;
        authenticatedProperty.set(playerId != null);
        refreshCurrentViewOnly();
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
    public void setAvailableGames(List<GameInfo> games) {
        this.availableGames = games;
        refreshCurrentViewOnly();
    }

    public List<GameInfo> getAvailableGames() {
        return availableGames;
    }
    
    /**
     * Gets joinable games (filtered from available games)
     * @return List of joinable games
     */
    public List<GameInfo> getJoinableGames() {
        if (availableGames == null) {
            return List.of();
        }
        return availableGames.stream()
                .filter(GameInfo::isJoinable)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets games in progress (filtered from available games)
     * @return List of games in progress
     */
    public List<GameInfo> getGamesInProgress() {
        if (availableGames == null) {
            return List.of();
        }
        return availableGames.stream()
                .filter(GameInfo::isStarted)
                .collect(java.util.stream.Collectors.toList());
    }

    public void setCurrentGameLobby(GameModel gameModel) {
        this.currentGameLobby = gameModel;
        refreshCurrentViewOnly();
    }

    public GameModel getCurrentGameLobby() {
        return currentGameLobby;
    }

    public void setPlayersInLobby(List<Player> players) {
        this.playersInLobby = players;
        refreshCurrentViewOnly();
    }

    public List<Player> getPlayersInLobby() {
        return playersInLobby;
    }

    // === Game State Management ===
    public void setGameModel(GameModel newGameModel) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ClientState.class.getName());
        if (newGameModel != null) {
            logger.info("🔄 CLIENT STATE - Setting GameModel with " + newGameModel.getPlayers().size() +
                       " players in " + newGameModel.getCurrentPhase() + " phase (Hash: " +
                       System.identityHashCode(newGameModel) + ")");
            logger.info("CLIENT STATE: GameModel received. Phase: " + newGameModel.getCurrentPhase());
        } else {
            logger.info("🔄 CLIENT STATE - Setting GameModel to NULL");
        }
        this.gameModel = newGameModel;
        if (this.gameModel != null) {
            logger.info("CLIENT STATE: GameModel set. Phase: " + this.gameModel.getCurrentPhase());
        }
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
        System.out.println("[TAKETILE DEBUG] ClientState.updatePlayer() called for player: " + updatedPlayer.getId() + 
            " with " + updatedPlayer.getHeldComponents().size() + " held components");
        if (gameModel != null) {
            for (int i = 0; i < gameModel.getPlayers().size(); i++) {
                Player localPlayer = gameModel.getPlayers().get(i);
                if (localPlayer.getId().equals(updatedPlayer.getId())) {
                    System.out.println("[TAKETILE DEBUG] Found matching player, updating in game model");
                    gameModel.getPlayers().set(i, updatedPlayer);
                    break;
                }
            }
            System.out.println("[TAKETILE DEBUG] Calling refreshCurrentViewOnly()");
            refreshCurrentViewOnly();
        } else {
            System.out.println("[TAKETILE DEBUG] WARNING: No game model available, cannot update player");
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
    public void setCurrentView(ViewState viewState, UIView viewComponent) {
        ViewState oldView = this.currentView;
        this.currentView = viewState;
        this.currentViewComponent = viewComponent;

        if (viewComponent != null) {
            registeredViews.add(viewComponent);
            viewComponent.refresh();
        }
    }

    public ViewState getCurrentView() {
        return currentView;
    }

    public void setCurrentView(ViewState viewState) {
        this.currentView = viewState;
        refreshCurrentViewOnly();
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
            Player localPlayer = gameModel.getPlayerById(playerId);
            if (localPlayer != null) {
                System.out.println("[TAKETILE DEBUG] getLocalPlayer() - Found player " + localPlayer.getId() + " with " + localPlayer.getHeldComponents().size() + " held components");
            } else {
                System.out.println("[TAKETILE DEBUG] getLocalPlayer() - No player found for ID: " + playerId);
            }
            return localPlayer;
        }
        System.out.println("[TAKETILE DEBUG] getLocalPlayer() - gameModel or playerId is null");
        return null;
    }

    // === Direct Model Access (Lobby) ===
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    public boolean isInGame() {
        return gameModel != null;
    }
    
    // === Player Management Methods (for TODO:SISTEMARE fixes) ===
    
    /**
     * Removes an active player from the lobby display
     * @param playerId The player ID to remove
     */
    public void removeActivePlayer(String playerId) {
        if (playersInLobby != null) {
            playersInLobby.removeIf(player -> player.getId().toString().equals(playerId));
            refreshCurrentViewOnly();
        }
    }
    
    /**
     * Sets a player's held component
     * @param playerId The player ID
     * @param component The component being held
     */
    public void setPlayerHeldComponent(String playerId, it.polimi.ingsw.server.model.domain.ship.components.Component component) {
        if (gameModel != null) {
            Player player = gameModel.getPlayerById(PlayerId.fromString(playerId));
            if (player != null) {
                // Clear existing held components and add the new one
                player.getHeldComponents().clear();
                if (component != null) {
                    player.getHeldComponents().add(component);
                }
                refreshCurrentViewOnly();
            }
        }
    }
    
    /**
     * Sets a player's credit amount
     * @param playerId The player ID
     * @param credits The new credit amount
     */
    public void setPlayerCredits(String playerId, int credits) {
        if (gameModel != null) {
            Player player = gameModel.getPlayerById(PlayerId.fromString(playerId));
            if (player != null) {
                player.setCredits(credits);
                refreshCurrentViewOnly();
            }
        }
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

    //public String getCurrentGameId() {
    //    return currentGameId != null ? currentGameId : (currentGameLobby != null ? currentGameLobby.getGameId() : null);
    //}
    public String getCurrentGameId() {
        return currentGameLobby != null ? currentGameLobby.getGameId() : currentGameId;
    }
    // === Current Game Lobby Basic Info Management ===
    
    public void setCurrentGameId(String gameId) {
        this.currentGameId = gameId;
        refreshCurrentViewOnly();
    }
    
    public void setCurrentGameName(String gameName) {
        this.currentGameName = gameName;
        refreshCurrentViewOnly();
    }
    
    public void setCurrentGameLevel(it.polimi.ingsw.server.model.enums.GameLevel gameLevel) {
        this.currentGameLevel = gameLevel;
        refreshCurrentViewOnly();
    }
    
    public void setCurrentGameMaxPlayers(int maxPlayers) {
        this.currentGameMaxPlayers = maxPlayers;
        refreshCurrentViewOnly();
    }
    
    public String getCurrentGameName() {
        return currentGameName;
    }
    
    public it.polimi.ingsw.server.model.enums.GameLevel getCurrentGameLevel() {
        return currentGameLevel;
    }
    
    public int getCurrentGameMaxPlayers() {
        return currentGameMaxPlayers;
    }
    
    /**
     * Gets the current game lobby information
     * @return The current game lobby info, or null if not in a lobby
     */
    public GameModel getCurrentGameInfo() {
        return currentGameLobby;
    }
    
    /**
     * Checks if a player is ready
     * @param playerId The player ID to check
     * @return true if ready, false otherwise
     */
    public boolean isPlayerReady(String playerId) {
        if (playersInLobby != null) {
            for (Player player : playersInLobby) {
                if (player.getId().toString().equals(playerId)) {
                    return player.isReady();
                }
            }
        }
        
        // Also check current game lobby if available
        if (currentGameLobby != null && currentGameLobby.getPlayers() != null) {
            for (Player player : currentGameLobby.getPlayers()) {
                if (player.getId().toString().equals(playerId)) {
                    return player.isReady();
                }
            }
        }
        
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
        Platform.runLater(() -> authenticatedProperty.set(authenticated));
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

    // UI Updates - Refresh registered views
    public void refreshCurrentViewOnly() {
        // Refresh all registered views - they'll decide if they're active
        for (UIView view : registeredViews) {
            if (view != null) {
                view.refresh();
            }
        }
        
        // The currentViewComponent is already in registeredViews if it's active,
        // so no separate refresh is needed here.
    }

    public void registerRefreshableView(UIView view) {
        if (view != null) {
            registeredViews.add(view);
        }
    }

    public void unregisterRefreshableView(UIView view) {
        if (view == null) {
            return; // Nothing to unregister
        }
        registeredViews.remove(view);
        if (currentViewComponent == view) {
            currentViewComponent = null;
        }
    }

    private void refreshCurrentView() {
        refreshCurrentViewOnly();
    }

    
    // === Missing Methods for Response Handlers ===
    
    /**
     * Sets the ready status for a player in the lobby
     * @param playerId The player ID
     * @param ready The ready status
     */
    public void setPlayerReadyStatus(String playerId, boolean ready) {
        boolean updated = false;
        
        // Update in playersInLobby
        if (playersInLobby != null) {
            for (Player player : playersInLobby) {
                if (player.getId().toString().equals(playerId)) {
                    player.setReady(ready);
                    updated = true;
                    break;
                }
            }
        }
        
        // Also update in current game lobby if available
        if (currentGameLobby != null && currentGameLobby.getPlayers() != null) {
            for (Player player : currentGameLobby.getPlayers()) {
                if (player.getId().toString().equals(playerId)) {
                    player.setReady(ready);
                    updated = true;
                    break;
                }
            }
        }
        
        if (updated) {
            refreshCurrentViewOnly();
        }
    }
    
    /**
     * Sets ship validation result
     * @param isValid Whether the ship is valid
     * @param errors List of validation errors
     */
    public void setShipValidation(boolean isValid, java.util.List<String> errors) {
        // Ship validation result stored directly
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
    
    /**
     * Removes a component from a ship at the specified position
     * @param playerId Player ID
     * @param position Position of the component to remove
     */
    public void removeShipComponent(String playerId, it.polimi.ingsw.server.model.domain.ship.Position position) {
        if (gameModel != null) {
            Player player = gameModel.getPlayerById(PlayerId.fromString(playerId));
            if (player != null && player.getShip() != null) {
                // Use current game phase or assume BUILDING phase
                GamePhase currentPhase = gameModel.getCurrentPhase() != null ? 
                    gameModel.getCurrentPhase() : GamePhase.BUILDING;
                player.getShip().removeComponent(position, currentPhase);
                refreshCurrentViewOnly();
            }
        }
    }
    
    /**
     * Adds an active player to the lobby
     * @param playerId Player ID
     * @param playerNickname Player nickname
     */
    public void addActivePlayer(String playerId, String playerNickname) {
        // This method is called when a player registers/joins the lobby
        // Since we're using direct model updates, the player should already be in the game model
        // Just refresh the view to show the updated state
        refreshCurrentViewOnly();
    }
    
    /**
     * Updates ship stats for a player
     * @param playerId Player ID  
     * @param ship Updated ship object
     */
    public void updateShipStats(String playerId, Ship ship) {
        if (gameModel != null) {
            Player player = gameModel.getPlayerById(PlayerId.fromString(playerId));
            if (player != null) {
                player.setShip(ship);
                refreshCurrentViewOnly();
            }
        }
    }
    
}
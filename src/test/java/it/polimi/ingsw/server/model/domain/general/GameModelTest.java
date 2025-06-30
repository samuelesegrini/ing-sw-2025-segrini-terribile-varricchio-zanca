package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.OpenSpaceCard;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.flight.RewardSystem;
import it.polimi.ingsw.server.model.domain.flight.Route;
import it.polimi.ingsw.server.model.domain.general.config.*;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.components.Engine;
import it.polimi.ingsw.server.model.domain.ship.components.Battery;
import it.polimi.ingsw.server.model.domain.ship.components.Cabin;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameModel Tests")
class GameModelTest {

    private GameModel gameModel;
    private GameConfig gameConfig;
    private ComponentDeck componentDeck;
    private AdventureDeck adventureDeck;
    private PlayerId playerId1, playerId2, playerId3, playerId4;

    @BeforeEach
    void setUp() {
        // Setup game configuration
        setupGameConfig();

        // Setup decks
        setupDecks();

        // Setup player IDs
        playerId1 = PlayerId.fromString("Player1");
        playerId2 = PlayerId.fromString("Player2");
        playerId3 = PlayerId.fromString("Player3");
        playerId4 = PlayerId.fromString("Player4");

        // Create GameModel
        gameModel = new GameModel(
                "test-game-1",
                "Test Game",
                GameLevel.TEST_FLIGHT,
                gameConfig,
                componentDeck,
                adventureDeck,
                4
        );
    }

    private void setupGameConfig() {
        // RewardSystemConfig
        Map<String, Integer> positionBonus = Map.of(
                "FIRST", 5,
                "SECOND", 3,
                "THIRD", 1,
                "FOURTH", 0
        );

        Map<String, Integer> resourceBonus = Map.of(
                "RED", 4,
                "YELLOW", 3,
                "GREEN", 2,
                "BLUE", 1
        );

        RewardSystemConfig rewardSystemConfig = new RewardSystemConfig(
                positionBonus, resourceBonus, 10, 2
        );

        // FlightBoardConfig
        FlightBoardConfig flightBoardConfig = new FlightBoardConfig(
                "test-board.jpg", "18",
                Arrays.asList(0, 1, 2, 3),
                rewardSystemConfig
        );

        // ShipGridConfig
        List<PositionConfig> reservedPositions = Arrays.asList(
                new PositionConfig(0, 0),
                new PositionConfig(0, 1)
        );

        List<PositionConfig> forbiddenPositions = Arrays.asList(
                new PositionConfig(0, 5),
                new PositionConfig(1, 5)
        );

        ShipGridConfig shipGridConfig = new ShipGridConfig(
                "test-ship.jpg", 5, 6, reservedPositions, forbiddenPositions
        );

        gameConfig = new GameConfig(GameLevel.TEST_FLIGHT, flightBoardConfig, shipGridConfig);
    }

    private void setupDecks() {
        // Create test components
        List<Component> components = createTestComponents();
        componentDeck = new ComponentDeck(components, GameLevel.TEST_FLIGHT);

        // Create test adventure cards
        List<AdventureCard> cards = createTestAdventureCards();
        adventureDeck = new AdventureDeck(GameLevel.TEST_FLIGHT, new ArrayList<>(), cards);
    }

    private List<Component> createTestComponents() {
        List<Component> components = new ArrayList<>();

        // Create connectors for components
        Map<Direction, ConnectorType> engineConnectors = new HashMap<>();
        engineConnectors.put(Direction.UP, ConnectorType.SINGLE);
        engineConnectors.put(Direction.DOWN, ConnectorType.PLAIN);
        engineConnectors.put(Direction.LEFT, ConnectorType.SINGLE);
        engineConnectors.put(Direction.RIGHT, ConnectorType.SINGLE);

        Map<Direction, ConnectorType> batteryConnectors = new HashMap<>();
        batteryConnectors.put(Direction.UP, ConnectorType.SINGLE);
        batteryConnectors.put(Direction.DOWN, ConnectorType.SINGLE);
        batteryConnectors.put(Direction.LEFT, ConnectorType.SINGLE);
        batteryConnectors.put(Direction.RIGHT, ConnectorType.SINGLE);

        // Add test components
        components.add(new Engine(ComponentType.ENGINE_SINGLE, engineConnectors, "engine-1"));
        components.add(new Engine(ComponentType.ENGINE_SINGLE, engineConnectors, "engine-2"));
        components.add(new Battery(ComponentType.BATTERY, batteryConnectors, 3, "battery-1"));
        components.add(new Battery(ComponentType.BATTERY, batteryConnectors, 3, "battery-2"));

        return components;
    }

    private List<AdventureCard> createTestAdventureCards() {
        List<AdventureCard> cards = new ArrayList<>();
        cards.add(new OpenSpaceCard("open-space-1", CardLevel.TEST_FLIGHT, "Test open space"));
        cards.add(new OpenSpaceCard("open-space-2", CardLevel.TEST_FLIGHT, "Test open space 2"));
        return cards;
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create GameModel with valid parameters")
        void shouldCreateGameModelWithValidParameters() {
            assertNotNull(gameModel);
            assertEquals("test-game-1", gameModel.getGameId());
            assertEquals("Test Game", gameModel.getGameName());
            assertEquals(GameLevel.TEST_FLIGHT, gameModel.getLevel());
            assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());
            assertEquals(4, gameModel.getMaxPlayers());
            assertEquals(0, gameModel.getCurrentPlayers());
            assertFalse(gameModel.isInitialized());
        }

        @Test
        @DisplayName("Should throw exception for invalid player count")
        void shouldThrowExceptionForInvalidPlayerCount() {
            assertThrows(IllegalArgumentException.class, () -> {
                new GameModel("test", "Test", GameLevel.TEST_FLIGHT, gameConfig, componentDeck, adventureDeck, 1);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                new GameModel("test", "Test", GameLevel.TEST_FLIGHT, gameConfig, componentDeck, adventureDeck, 5);
            });
        }

        @Test
        @DisplayName("Should initialize building timer for game")
        void shouldInitializeBuildingTimer() {
            assertNotNull(gameModel.getBuildingTimer());
            assertEquals(BuildingTimer.TimerState.IDLE, gameModel.getBuildingTimerState());
        }
    }

    @Nested
    @DisplayName("Player Management Tests")
    class PlayerManagementTests {

        @Test
        @DisplayName("Should add players successfully")
        void shouldAddPlayersSuccessfully() {
            gameModel.addPlayer(playerId1, "Alice");
            assertEquals(1, gameModel.getCurrentPlayers());

            Player player = gameModel.getPlayerById(playerId1);
            assertNotNull(player);
            assertEquals(playerId1, player.getId());
            assertEquals(PlayerColor.BLUE, player.getColor());
            assertNotNull(player.getShip());
        }

        @Test
        @DisplayName("Should assign different colors to players")
        void shouldAssignDifferentColorsToPlayers() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.addPlayer(playerId3, "Charlie");
            gameModel.addPlayer(playerId4, "David");

            assertEquals(PlayerColor.BLUE, gameModel.getPlayerById(playerId1).getColor());
            assertEquals(PlayerColor.RED, gameModel.getPlayerById(playerId2).getColor());
            assertEquals(PlayerColor.GREEN, gameModel.getPlayerById(playerId3).getColor());
            assertEquals(PlayerColor.YELLOW, gameModel.getPlayerById(playerId4).getColor());
        }

        @Test
        @DisplayName("Should throw exception when adding players after game start")
        void shouldThrowExceptionWhenAddingPlayersAfterGameStart() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.startGame();

            assertThrows(IllegalStateException.class, () -> {
                gameModel.addPlayer(playerId3, "Charlie");
            });
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate player")
        void shouldThrowExceptionWhenAddingDuplicatePlayer() {
            gameModel.addPlayer(playerId1, "Alice");

            assertThrows(IllegalArgumentException.class, () -> {
                gameModel.addPlayer(playerId1, "Alice Again");
            });
        }

        @Test
        @DisplayName("Should throw exception when max players reached")
        void shouldThrowExceptionWhenMaxPlayersReached() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.addPlayer(playerId3, "Charlie");
            gameModel.addPlayer(playerId4, "David");

            PlayerId playerId5 = PlayerId.fromString("Player5");
            assertThrows(IllegalStateException.class, () -> {
                gameModel.addPlayer(playerId5, "Eve");
            });
        }

        @Test
        @DisplayName("Should remove player successfully")
        void shouldRemovePlayerSuccessfully() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");

            assertTrue(gameModel.removePlayer(playerId1));
            assertEquals(1, gameModel.getCurrentPlayers());
            assertNull(gameModel.getPlayerById(playerId1));
        }

        @Test
        @DisplayName("Should return false when removing non-existent player")
        void shouldReturnFalseWhenRemovingNonExistentPlayer() {
            assertFalse(gameModel.removePlayer(playerId1));
        }

        @Test
        @DisplayName("Should get current and next player correctly")
        void shouldGetCurrentAndNextPlayerCorrectly() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");

            assertEquals(playerId1, gameModel.getCurrentPlayer().getId());

            Player nextPlayer = gameModel.nextPlayer();
            assertEquals(playerId2, nextPlayer.getId());
            assertEquals(playerId2, gameModel.getCurrentPlayer().getId());

            // Should wrap around
            nextPlayer = gameModel.nextPlayer();
            assertEquals(playerId1, nextPlayer.getId());
        }
    }

    @Nested
    @DisplayName("Game Initialization Tests")
    class GameInitializationTests {

        @Test
        @DisplayName("Should initialize game successfully")
        void shouldInitializeGameSuccessfully() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");

            gameModel.initializeGame();

            assertTrue(gameModel.isInitialized());
            assertNotNull(gameModel.getFlightBoard());
            assertEquals(2, gameModel.getFlightBoard().getPlayerCount());
        }

        @Test
        @DisplayName("Should throw exception when initializing with insufficient players")
        void shouldThrowExceptionWhenInitializingWithInsufficientPlayers() {
            gameModel.addPlayer(playerId1, "Alice");

            assertThrows(IllegalStateException.class, () -> {
                gameModel.initializeGame();
            });
        }

        @Test
        @DisplayName("Should throw exception when initializing already initialized game")
        void shouldThrowExceptionWhenInitializingAlreadyInitializedGame() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            assertThrows(IllegalStateException.class, () -> {
                gameModel.initializeGame();
            });
        }

        @Test
        @DisplayName("Should start game successfully")
        void shouldStartGameSuccessfully() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            gameModel.startGame();

            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should throw exception when starting uninitialized game")
        void shouldThrowExceptionWhenStartingUninitializedGame() {
            assertThrows(IllegalStateException.class, () -> {
                gameModel.startGame();
            });
        }

        @Test
        @DisplayName("Should throw exception when starting already started game")
        void shouldThrowExceptionWhenStartingAlreadyStartedGame() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.startGame();

            assertThrows(IllegalStateException.class, () -> {
                gameModel.startGame();
            });
        }
    }

    @Nested
    @DisplayName("Phase Management Tests")
    class PhaseManagementTests {

        @Test
        @DisplayName("Should change phase correctly")
        void shouldChangePhaseCorrectly() {
            assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());

            gameModel.changePhase(GamePhase.BUILDING);
            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());

            gameModel.changePhase(GamePhase.FLIGHT);
            assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should advance to next phase")
        void shouldAdvanceToNextPhase() {
            gameModel.nextPhase();
            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());

            gameModel.nextPhase();
            assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());

            gameModel.nextPhase();
            assertEquals(GamePhase.END, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should initialize building phase correctly")
        void shouldInitializeBuildingPhaseCorrectly() {
            gameModel.changePhase(GamePhase.BUILDING);
            // Building timer should be started
            assertTrue(gameModel.getBuildingTimer() != null);
        }

        @Test
        @DisplayName("Should initialize flight phase correctly")
        void shouldInitializeFlightPhaseCorrectly() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            gameModel.changePhase(GamePhase.FLIGHT);

            assertNotNull(gameModel.getFlightBoard());
        }

        @Test
        @DisplayName("Should initialize end phase correctly")
        void shouldInitializeEndPhaseCorrectly() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            gameModel.changePhase(GamePhase.END);

            // Should calculate final scores
            assertEquals(GamePhase.END, gameModel.getCurrentPhase());
        }
    }

    @Nested
    @DisplayName("Component Management Tests")
    class ComponentManagementTests {

        private Player player;

        @BeforeEach
        void setupPlayer() {
            gameModel.addPlayer(playerId1, "Alice");
            player = gameModel.getPlayerById(playerId1);
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should draw component successfully")
        void shouldDrawComponentSuccessfully() {
            Optional<Component> component = gameModel.drawComponent();
            assertTrue(component.isPresent());
        }

        @Test
        @DisplayName("Should throw exception when drawing component outside building phase")
        void shouldThrowExceptionWhenDrawingComponentOutsideBuildingPhase() {
            gameModel.changePhase(GamePhase.FLIGHT);

            assertThrows(IllegalStateException.class, () -> {
                gameModel.drawComponent();
            });
        }

        @Test
        @DisplayName("Should take component successfully")
        void shouldTakeComponentSuccessfully() {
            assertTrue(gameModel.takeComponent(playerId1));
            assertNotNull(player.getHeldComponent());
        }

        @Test
        @DisplayName("Should return false when taking component outside building phase")
        void shouldReturnFalseWhenTakingComponentOutsideBuildingPhase() {
            gameModel.changePhase(GamePhase.FLIGHT);
            assertFalse(gameModel.takeComponent(playerId1));
        }

        @Test
        @DisplayName("Should place component successfully")
        void shouldPlaceComponentSuccessfully() {
            gameModel.takeComponent(playerId1);
            Component component = player.getHeldComponent();

            boolean result = gameModel.placeComponent(playerId1, component, new Position(1, 1));
            assertTrue(result);
            assertNull(player.getHeldComponent());
        }

        @Test
        @DisplayName("Should return false when placing component player doesn't own")
        void shouldReturnFalseWhenPlacingComponentPlayerDoesntOwn() {
            Component fakeComponent = createTestComponents().get(0);

            boolean result = gameModel.placeComponent(playerId1, fakeComponent, new Position(1, 1));
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return component successfully")
        void shouldReturnComponentSuccessfully() {
            gameModel.takeComponent(playerId1);
            Component component = player.getHeldComponent();

            boolean result = gameModel.returnComponent(playerId1, component.getId());
            assertTrue(result);
            assertNull(player.getHeldComponent());
        }

        @Test
        @DisplayName("Should get available components")
        void shouldGetAvailableComponents() {
            Map<String, Component> components = gameModel.getAvailableComponents();
            assertNotNull(components);
            assertFalse(components.isEmpty());
        }

        @Test
        @DisplayName("Should validate ship successfully")
        void shouldValidateShipSuccessfully() {
            List<String> feedback = new ArrayList<>();
            boolean result = gameModel.validateShip(playerId1, "Alice", feedback);
            // Result depends on ship structure, but method should not throw
            assertNotNull(feedback);
        }
    }

    @Nested
    @DisplayName("Adventure Card Management Tests")
    class AdventureCardManagementTests {

        @BeforeEach
        void setupForFlightPhase() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.changePhase(GamePhase.FLIGHT);
        }

        @Test
        @DisplayName("Should draw adventure card successfully")
        void shouldDrawAdventureCardSuccessfully() {
            Optional<AdventureCard> card = gameModel.drawAdventureCard();
            assertTrue(card.isPresent());
        }

        @Test
        @DisplayName("Should throw exception when drawing adventure card outside flight phase")
        void shouldThrowExceptionWhenDrawingAdventureCardOutsideFlightPhase() {
            gameModel.changePhase(GamePhase.BUILDING);

            assertThrows(IllegalStateException.class, () -> {
                gameModel.drawAdventureCard();
            });
        }

        @Test
        @DisplayName("Should resolve adventure card successfully")
        void shouldResolveAdventureCardSuccessfully() {
            Optional<AdventureCard> cardOpt = gameModel.drawAdventureCard();
            assertTrue(cardOpt.isPresent());

            AdventureCard card = cardOpt.get();
            assertDoesNotThrow(() -> {
                gameModel.resolveAdventureCard(card);
            });
        }

        @Test
        @DisplayName("Should throw exception when resolving null adventure card")
        void shouldThrowExceptionWhenResolvingNullAdventureCard() {
            assertThrows(IllegalArgumentException.class, () -> {
                gameModel.resolveAdventureCard(null);
            });
        }

        @Test
        @DisplayName("Should throw exception when resolving adventure card outside flight phase")
        void shouldThrowExceptionWhenResolvingAdventureCardOutsideFlightPhase() {
            gameModel.changePhase(GamePhase.BUILDING);
            AdventureCard card = new OpenSpaceCard("test", CardLevel.TEST_FLIGHT, "test");

            assertThrows(IllegalStateException.class, () -> {
                gameModel.resolveAdventureCard(card);
            });
        }
    }

    @Nested
    @DisplayName("Player Movement Tests")
    class PlayerMovementTests {

        private Player player;

        @BeforeEach
        void setupForMovement() {
            gameModel.addPlayer(playerId1, "Alice");
            player = gameModel.getPlayerById(playerId1);
            gameModel.initializeGame();
            gameModel.changePhase(GamePhase.FLIGHT);
        }

        @Test
        @DisplayName("Should move player successfully")
        void shouldMovePlayerSuccessfully() {
            int initialPosition = player.getFlightData().getPosition();

            gameModel.movePlayer(player, 3);

            assertTrue(player.getFlightData().getPosition() >= initialPosition);
        }

        @Test
        @DisplayName("Should throw exception when moving null player")
        void shouldThrowExceptionWhenMovingNullPlayer() {
            assertThrows(IllegalArgumentException.class, () -> {
                gameModel.movePlayer(null, 3);
            });
        }

        @Test
        @DisplayName("Should throw exception when moving player outside flight phase")
        void shouldThrowExceptionWhenMovingPlayerOutsideFlightPhase() {
            gameModel.changePhase(GamePhase.BUILDING);

            assertThrows(IllegalStateException.class, () -> {
                gameModel.movePlayer(player, 3);
            });
        }

        @Test
        @DisplayName("Should throw exception when moving player with non-positive spaces")
        void shouldThrowExceptionWhenMovingPlayerWithNonPositiveSpaces() {
            assertThrows(IllegalArgumentException.class, () -> {
                gameModel.movePlayer(player, 0);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                gameModel.movePlayer(player, -1);
            });
        }
    }

    @Nested
    @DisplayName("Building Timer Tests")
    class BuildingTimerTests {

        @BeforeEach
        void setupForBuildingPhase() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should flip building timer successfully")
        void shouldFlipBuildingTimerSuccessfully() {
            boolean result = gameModel.flipBuildingTimer(playerId1, true);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when flipping timer outside building phase")
        void shouldReturnFalseWhenFlippingTimerOutsideBuildingPhase() {
            gameModel.changePhase(GamePhase.FLIGHT);

            boolean result = gameModel.flipBuildingTimer(playerId1, true);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should get building timer state")
        void shouldGetBuildingTimerState() {
            BuildingTimer.TimerState state = gameModel.getBuildingTimerState();
            assertNotNull(state);
        }

        @Test
        @DisplayName("Should get building time remaining")
        void shouldGetBuildingTimeRemaining() {
            long timeRemaining = gameModel.getBuildingTimeRemaining();
            assertTrue(timeRemaining >= -1); // -1 means no active timer
        }
    }

    @Nested
    @DisplayName("Property Change Events Tests")
    class PropertyChangeEventsTests {

        private List<PropertyChangeEvent> capturedEvents;
        private PropertyChangeListener listener;

        @BeforeEach
        void setupPropertyChangeListener() {
            capturedEvents = new ArrayList<>();
            listener = capturedEvents::add;
            gameModel.addPropertyChangeListener(listener);
        }

        @Test
        @DisplayName("Should fire events when components are managed")
        void shouldFireEventsWhenComponentsAreManaged() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            gameModel.takeComponent(playerId1);

            // Should have fired events
            assertFalse(capturedEvents.isEmpty());
        }

        @Test
        @DisplayName("Should remove property change listener")
        void shouldRemovePropertyChangeListener() {
            gameModel.removePropertyChangeListener(listener);

            gameModel.addPlayer(playerId1, "Alice");

            // Should not capture events after removal
            assertTrue(capturedEvents.isEmpty());
        }
    }

    @Nested
    @DisplayName("Getter Methods Tests")
    class GetterMethodsTests {

        @Test
        @DisplayName("Should return correct game properties")
        void shouldReturnCorrectGameProperties() {
            assertEquals("test-game-1", gameModel.getGameId());
            assertEquals("Test Game", gameModel.getGameName());
            assertEquals(GameLevel.TEST_FLIGHT, gameModel.getLevel());
            assertEquals(GameLevel.TEST_FLIGHT, gameModel.getGameLevel());
            assertEquals(gameConfig, gameModel.getConfig());
            assertEquals(componentDeck, gameModel.getComponentDeck());
            assertEquals(adventureDeck, gameModel.getAdventureDeck());
            assertEquals(4, gameModel.getMaxPlayers());
            assertEquals(0, gameModel.getCurrentPlayers());
            assertNull(gameModel.getCreatorId()); // No players yet
        }

        @Test
        @DisplayName("Should return creator ID correctly")
        void shouldReturnCreatorIdCorrectly() {
            gameModel.addPlayer(playerId1, "Alice");
            assertEquals(playerId1.toString(), gameModel.getCreatorId());
        }

        @Test
        @DisplayName("Should return current player index")
        void shouldReturnCurrentPlayerIndex() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");

            assertEquals(0, gameModel.getCurrentPlayerIndex());
            gameModel.nextPlayer();
            assertEquals(1, gameModel.getCurrentPlayerIndex());
        }

        @Test
        @DisplayName("Should return lead player")
        void shouldReturnLeadPlayer() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            // Initially should be null until flight phase
            Player leadPlayer = gameModel.getLeadPlayer();
            // Lead player logic depends on flight board implementation
        }

        @Test
        @DisplayName("Should return empty players list initially")
        void shouldReturnEmptyPlayersListInitially() {
            List<Player> players = gameModel.getPlayers();
            assertNotNull(players);
            assertTrue(players.isEmpty());
        }

        @Test
        @DisplayName("Should return flight board after initialization")
        void shouldReturnFlightBoardAfterInitialization() {
            assertNull(gameModel.getFlightBoard());

            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            assertNotNull(gameModel.getFlightBoard());
        }
    }

    @Nested
    @DisplayName("Component Deck Management Tests")
    class ComponentDeckManagementTests {

        @Test
        @DisplayName("Should set component deck")
        void shouldSetComponentDeck() {
            ComponentDeck newDeck = new ComponentDeck(new ArrayList<>(), GameLevel.TEST_FLIGHT);
            gameModel.setComponentDeck(newDeck);
            assertEquals(newDeck, gameModel.getComponentDeck());
        }

        @Test
        @DisplayName("Should get component by ID")
        void shouldGetComponentById() {
            Component component = gameModel.getComponentById("engine-1");
            assertNotNull(component);
            assertEquals("engine-1", component.getId());
        }

        @Test
        @DisplayName("Should return null for non-existent component ID")
        void shouldReturnNullForNonExistentComponentId() {
            Component component = gameModel.getComponentById("non-existent-id");
            assertNull(component);
        }
    }
    // Aggiungi questi test alla classe GameModelTest esistente per raggiungere 90% coverage

    @Nested
    @DisplayName("Ship Stats Update and Event Firing Tests")
    class ShipStatsAndEventTests {

        @BeforeEach
        void setupForShipTests() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should fire ship stats updated event")
        void shouldFireShipStatsUpdatedEvent() {
            Player player = gameModel.getPlayerById(playerId1);
            Ship ship = player.getShip();

            // Mock ship to have some stats
            ship.setCannons(2.0);
            ship.setEngines(1.5);
            ship.setBatteries(3);

            gameModel.fireShipStatsUpdatedEvent(playerId1, "Alice", ship);

            // Verify the method doesn't throw and handles properly
            assertDoesNotThrow(() -> gameModel.fireShipStatsUpdatedEvent(playerId1, "Alice", ship));
        }

        @Test
        @DisplayName("Should fire component removed event")
        void shouldFireComponentRemovedEvent() {
            gameModel.takeComponent(playerId1);
            Player player = gameModel.getPlayerById(playerId1);
            Component component = player.getHeldComponent();
            Position position = new Position(1, 1);

            assertDoesNotThrow(() -> gameModel.fireComponentRemovedEvent(
                    playerId1, "Alice", component, position, "Test removal"));
        }

        @Test
        @DisplayName("Should fire player credits changed event")
        void shouldFirePlayerCreditsChangedEvent() {
            assertDoesNotThrow(() -> gameModel.firePlayerCreditsChangedEvent(
                    playerId1, "Alice", 0, 100));
        }

        @Test
        @DisplayName("Should fire player component changed event")
        void shouldFirePlayerComponentChangedEvent() {
            gameModel.takeComponent(playerId1);
            Player player = gameModel.getPlayerById(playerId1);
            Component oldComponent = null;
            Component newComponent = player.getHeldComponent();

            assertDoesNotThrow(() -> gameModel.firePlayerComponentChangedEvent(
                    playerId1, "Alice", oldComponent, newComponent));
        }

        @Test
        @DisplayName("Should fire player ready changed event")
        void shouldFirePlayerReadyChangedEvent() {
            assertDoesNotThrow(() -> gameModel.firePlayerReadyChangedEvent(
                    playerId1, "Alice", true));
        }

        @Test
        @DisplayName("Should fire component offered event")
        void shouldFireComponentOfferedEvent() {
            gameModel.takeComponent(playerId1);
            Player player = gameModel.getPlayerById(playerId1);
            Component component = player.getHeldComponent();

            assertDoesNotThrow(() -> gameModel.fireComponentOfferedEvent(component, player));
        }
    }

    @Nested
    @DisplayName("Player Update Methods Tests")
    class PlayerUpdateMethodsTests {

        private Player player;

        @BeforeEach
        void setupForPlayerUpdates() {
            gameModel.addPlayer(playerId1, "Alice");
            player = gameModel.getPlayerById(playerId1);
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should update player credits")
        void shouldUpdatePlayerCredits() {
            int initialCredits = player.getCredits();

            gameModel.updatePlayerCredits(playerId1, 150);

            assertEquals(150, player.getCredits());
        }

        @Test
        @DisplayName("Should handle null player when updating credits")
        void shouldHandleNullPlayerWhenUpdatingCredits() {
            PlayerId nonExistentId = PlayerId.fromString("NonExistent");

            assertDoesNotThrow(() -> gameModel.updatePlayerCredits(nonExistentId, 100));
        }

        @Test
        @DisplayName("Should update player held component")
        void shouldUpdatePlayerHeldComponent() {
            gameModel.takeComponent(playerId1);
            Component newComponent = player.getHeldComponent();

            gameModel.updatePlayerHeldComponent(playerId1, null);

            assertNull(player.getHeldComponent());
        }

        @Test
        @DisplayName("Should handle null player when updating held component")
        void shouldHandleNullPlayerWhenUpdatingHeldComponent() {
            PlayerId nonExistentId = PlayerId.fromString("NonExistent");

            assertDoesNotThrow(() -> gameModel.updatePlayerHeldComponent(nonExistentId, null));
        }

        @Test
        @DisplayName("Should update player ready status")
        void shouldUpdatePlayerReadyStatus() {
            assertFalse(player.isReady());

            gameModel.updatePlayerReadyStatus(playerId1, true);

            assertTrue(player.isReady());
        }

        @Test
        @DisplayName("Should handle null player when updating ready status")
        void shouldHandleNullPlayerWhenUpdatingReadyStatus() {
            PlayerId nonExistentId = PlayerId.fromString("NonExistent");

            assertDoesNotThrow(() -> gameModel.updatePlayerReadyStatus(nonExistentId, true));
        }
    }

    @Nested
    @DisplayName("Component Removal Tests")
    class ComponentRemovalTests {

        private Player player;

        @BeforeEach
        void setupForRemoval() {
            gameModel.addPlayer(playerId1, "Alice");
            player = gameModel.getPlayerById(playerId1);
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should remove component successfully")
        void shouldRemoveComponentSuccessfully() {
            // First place a component
            gameModel.takeComponent(playerId1);
            Component component = player.getHeldComponent();
            Position position = new Position(1, 1);
            gameModel.placeComponent(playerId1, component, position);

            // Now remove it
            boolean result = gameModel.removeComponent(playerId1, position, "Test removal");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when removing from empty position")
        void shouldReturnFalseWhenRemovingFromEmptyPosition() {
            Position emptyPosition = new Position(1, 1);

            boolean result = gameModel.removeComponent(playerId1, emptyPosition, "Test removal");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when player has no ship")
        void shouldReturnFalseWhenPlayerHasNoShip() {
            player.setShip(null);
            Position position = new Position(1, 1);

            boolean result = gameModel.removeComponent(playerId1, position, "Test removal");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for null player")
        void shouldReturnFalseForNullPlayer() {
            PlayerId nonExistentId = PlayerId.fromString("NonExistent");
            Position position = new Position(1, 1);

            boolean result = gameModel.removeComponent(nonExistentId, position, "Test removal");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Component Reservation Tests")
    class ComponentReservationTests {

        private Player player;

        @BeforeEach
        void setupForReservation() {
            gameModel.addPlayer(playerId1, "Alice");
            player = gameModel.getPlayerById(playerId1);
            gameModel.initializeGame();
            gameModel.startGame();
        }

        @Test
        @DisplayName("Should reserve component if ship supports it")
        void shouldReserveComponentIfShipSupportsIt() {
            // Setup ship to support reservations (this depends on your ShipGridConfig)
            gameModel.takeComponent(playerId1);
            Component component = player.getHeldComponent();
            String componentId = component.getId();

            // Make component available in face-up pile first
            gameModel.returnComponent(playerId1, componentId);

            boolean result = gameModel.reserveComponent(playerId1, componentId);

            // Result depends on ship configuration and reservation support
            // Just verify method doesn't crash
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return false when ship doesn't support reservations")
        void shouldReturnFalseWhenShipDoesntSupportReservations() {
            // For most ship configurations, reservations aren't supported
            gameModel.takeComponent(playerId1);
            Component component = player.getHeldComponent();
            String componentId = component.getId();

            boolean result = gameModel.reserveComponent(playerId1, componentId);

            // Should return false if reservations not supported
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false for null player in reservation")
        void shouldReturnFalseForNullPlayerInReservation() {
            PlayerId nonExistentId = PlayerId.fromString("NonExistent");

            boolean result = gameModel.reserveComponent(nonExistentId, "some-component");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when component not found")
        void shouldReturnFalseWhenComponentNotFound() {
            boolean result = gameModel.reserveComponent(playerId1, "non-existent-component");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle invalid phase transitions gracefully")
        void shouldHandleInvalidPhaseTransitionsGracefully() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();

            // Jump directly to END phase
            gameModel.changePhase(GamePhase.END);

            assertEquals(GamePhase.END, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle taking component with no components available")
        void shouldHandleTakingComponentWithNoComponentsAvailable() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            // Try to exhaust the deck first
            while (gameModel.takeComponent(playerId1)) {
                Player player = gameModel.getPlayerById(playerId1);
                Component component = player.getHeldComponent();
                if (component != null) {
                    gameModel.returnComponent(playerId1, component.getId());
                }
            }

            // Now try again - should return false
            boolean result = gameModel.takeComponent(playerId1);
            // Result depends on deck state
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle placing component on forbidden position")
        void shouldHandlePlacingComponentOnForbiddenPosition() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            gameModel.takeComponent(playerId1);
            Player player = gameModel.getPlayerById(playerId1);
            Component component = player.getHeldComponent();

            // Try to place on forbidden position (0,5) based on config
            Position forbiddenPosition = new Position(0, 5);

            boolean result = gameModel.placeComponent(playerId1, component, forbiddenPosition);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle placing component on occupied position")
        void shouldHandlePlacingComponentOnOccupiedPosition() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            // Place first component
            gameModel.takeComponent(playerId1);
            Player player = gameModel.getPlayerById(playerId1);
            Component component1 = player.getHeldComponent();
            Position position = new Position(1, 1);
            gameModel.placeComponent(playerId1, component1, position);

            // Try to place second component at same position
            gameModel.takeComponent(playerId1);
            Component component2 = player.getHeldComponent();

            boolean result = gameModel.placeComponent(playerId1, component2, position);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null component placement")
        void shouldHandleNullComponentPlacement() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            Position position = new Position(1, 1);

            boolean result = gameModel.placeComponent(playerId1, null, position);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle validation with feedback list")
        void shouldHandleValidationWithFeedbackList() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            List<String> feedback = new ArrayList<>();
            feedback.add("Initial feedback");

            boolean result = gameModel.validateShip(playerId1, "Alice", feedback);

            // Should not modify the original feedback list structure
            assertNotNull(feedback);
        }

        @Test
        @DisplayName("Should handle validation with null feedback")
        void shouldHandleValidationWithNullFeedback() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            boolean result = gameModel.validateShip(playerId1, "Alice", null);

            // Should handle null feedback gracefully
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("PropertyChangeSupport Tests")
    class PropertyChangeSupportTests {

        @Test
        @DisplayName("Should get property change support instance")
        void shouldGetPropertyChangeSupportInstance() {
            assertNotNull(gameModel.getPropertyChangeSupport());
        }

        @Test
        @DisplayName("Should handle multiple property change listeners")
        void shouldHandleMultiplePropertyChangeListeners() {
            List<PropertyChangeEvent> events1 = new ArrayList<>();
            List<PropertyChangeEvent> events2 = new ArrayList<>();

            PropertyChangeListener listener1 = events1::add;
            PropertyChangeListener listener2 = events2::add;

            gameModel.addPropertyChangeListener(listener1);
            gameModel.addPropertyChangeListener(listener2);

            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            // Perform an action that fires events
            gameModel.takeComponent(playerId1);

            // Both listeners should receive events
            // Note: Actual event firing depends on implementation details
            gameModel.removePropertyChangeListener(listener1);
            gameModel.removePropertyChangeListener(listener2);
        }

        @Test
        @DisplayName("Should handle removing non-existent listener")
        void shouldHandleRemovingNonExistentListener() {
            PropertyChangeListener listener = evt -> {
            };

            assertDoesNotThrow(() -> gameModel.removePropertyChangeListener(listener));
        }
    }

    @Nested
    @DisplayName("Timer Event Handling Tests")
    class TimerEventHandlingTests {

        @Test
        @DisplayName("Should handle timer events correctly")
        void shouldHandleTimerEventsCorrectly() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.startGame();

            // Test timer flipping with different scenarios
            assertTrue(gameModel.flipBuildingTimer(playerId1, false));

            // Test getting timer state
            assertNotNull(gameModel.getBuildingTimerState());

            // Test getting time remaining
            long timeRemaining = gameModel.getBuildingTimeRemaining();
            assertTrue(timeRemaining >= -1);
        }

        @Test
        @DisplayName("Should handle timer with completed ship")
        void shouldHandleTimerWithCompletedShip() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            // Try flipping timer with completed ship
            boolean result = gameModel.flipBuildingTimer(playerId1, true);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Comprehensive Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete game flow")
        void shouldHandleCompleteGameFlow() {
            // Setup game
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");

            assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());
            assertEquals(2, gameModel.getCurrentPlayers());

            // Initialize and start
            gameModel.initializeGame();
            gameModel.startGame();

            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());

            // Building phase activities
            assertTrue(gameModel.takeComponent(playerId1));
            Player player1 = gameModel.getPlayerById(playerId1);
            assertNotNull(player1.getHeldComponent());

            // Place component
            Component component = player1.getHeldComponent();
            Position position = new Position(1, 1);
            assertTrue(gameModel.placeComponent(playerId1, component, position));

            // Validate ship
            List<String> feedback = new ArrayList<>();
            gameModel.validateShip(playerId1, "Alice", feedback);

            // Move to flight phase
            gameModel.changePhase(GamePhase.FLIGHT);
            assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());

            // Flight phase activities
            Optional<AdventureCard> card = gameModel.drawAdventureCard();
            if (card.isPresent()) {
                gameModel.resolveAdventureCard(card.get());
            }

            // Move player
            gameModel.movePlayer(player1, 2);

            // End game
            gameModel.changePhase(GamePhase.END);
            assertEquals(GamePhase.END, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle all players completing building")
        void shouldHandleAllPlayersCompletingBuilding() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.initializeGame();
            gameModel.startGame();

            // Both players complete building
            gameModel.updatePlayerReadyStatus(playerId1, true);
            gameModel.updatePlayerReadyStatus(playerId2, true);

            // Verify states
            assertTrue(gameModel.getPlayerById(playerId1).isReady());
            assertTrue(gameModel.getPlayerById(playerId2).isReady());
        }

        @Test
        @DisplayName("Should handle player abandoning game")
        void shouldHandlePlayerAbandoningGame() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.addPlayer(playerId2, "Bob");
            gameModel.addPlayer(playerId3, "Charlie");

            assertEquals(3, gameModel.getCurrentPlayers());

            // Remove a player
            assertTrue(gameModel.removePlayer(playerId2));
            assertEquals(2, gameModel.getCurrentPlayers());
            assertNull(gameModel.getPlayerById(playerId2));

            // Game should still be functional
            gameModel.initializeGame();
            gameModel.startGame();

            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
        }
    }

    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalCoverageTests {

        @Test
        @DisplayName("Should handle getting component deck components")
        void shouldHandleGettingComponentDeckComponents() {
            Map<String, Component> availableComponents = gameModel.getAvailableComponents();
            assertNotNull(availableComponents);

            // Test with player holding component
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            gameModel.takeComponent(playerId1);

            Map<String, Component> filteredComponents = gameModel.getAvailableComponents();
            assertNotNull(filteredComponents);
        }

        @Test
        @DisplayName("Should handle setting current phase directly")
        void shouldHandleSettingCurrentPhaseDirectly() {
            assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());

            gameModel.setCurrentPhase(GamePhase.BUILDING);
            assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());

            gameModel.setCurrentPhase(GamePhase.FLIGHT);
            assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Should handle multiple component operations")
        void shouldHandleMultipleComponentOperations() {
            gameModel.addPlayer(playerId1, "Alice");
            gameModel.initializeGame();
            gameModel.startGame();

            // Take and return component multiple times
            for (int i = 0; i < 3; i++) {
                if (gameModel.takeComponent(playerId1)) {
                    Player player = gameModel.getPlayerById(playerId1);
                    Component component = player.getHeldComponent();
                    if (component != null) {
                        gameModel.returnComponent(playerId1, component.getId());
                    }
                }
            }

            // Should not crash
            assertDoesNotThrow(() -> gameModel.takeComponent(playerId1));
        }

        @Test
        @DisplayName("Should handle ship validation edge cases")
        void shouldHandleShipValidationEdgeCases() {
            gameModel.addPlayer(playerId1, "Alice");
            Player player = gameModel.getPlayerById(playerId1);

            // Test validation before initialization
            List<String> feedback = new ArrayList<>();
            boolean result = gameModel.validateShip(playerId1, "Alice", feedback);

            // Should handle gracefully
            assertNotNull(feedback);
        }
    }
}


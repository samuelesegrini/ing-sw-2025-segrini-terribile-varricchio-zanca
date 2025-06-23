# Galaxy Trucker Ship Building Implementation Plan

     ## Executive Summary

     This plan details the implementation of the ship building system for Galaxy Trucker. Based on analysis of the
     existing architecture, most server-side logic and message infrastructure is complete. The primary work involves
     implementing client-side state management and UI components.

     ## Current State Analysis

     ### ✅ **Already Implemented**
     - **Server-side ship building logic** - Complete with 5x7 grid, component system, validation
     - **Message system** - Comprehensive request/response and event patterns
     - **Network protocols** - Socket and RMI communication infrastructure
     - **UI framework** - Complete view management with JavaFX GUI and Terminal TUI support
     - **Game rules engine** - Connection validation, ship validation, timer management

     ### ❌ **Missing Implementation**
     - **Client-side ship state management** - LocalGameState is minimal stub
     - **Ship building UI components** - Views are placeholder implementations
     - **UI-state integration** - Event handling and state synchronization
     - **Component inventory management** - Client-side tile tracking

     ## Implementation Strategy

     ### **Phase 1: Core State Management**
     *Priority: Critical | Estimated effort: 2-3 days*

     #### 1.1 Enhanced LocalGameState
     **Location**: `src/main/java/it/polimi/ingsw/client/core/state/LocalGameState.java`

     **Required Implementation**:
     ```java
     public class LocalGameState {
         // Ship building state
         private Component[][] shipGrid = new Component[5][7];
         private List<ComponentType> availableTiles = new ArrayList<>();
         private List<ComponentType> heldTiles = new ArrayList<>();
         private boolean buildingTimerFlipped = false;
         private long buildingTimeRemaining = 0;
         private boolean shipValidated = false;
         private List<String> validationErrors = new ArrayList<>();

         // State management methods
         public void placeTile(ComponentType component, Position position, int rotation);
         public void removeTile(Position position);
         public void addAvailableTile(ComponentType component);
         public void removeAvailableTile(ComponentType component);
         public void addHeldTile(ComponentType component);
         public void removeHeldTile(ComponentType component);
         public void updateBuildingTimer(long timeRemaining);
         public void setShipValidation(boolean valid, List<String> errors);

         // State query methods
         public Component getComponentAt(Position position);
         public boolean canPlaceComponent(ComponentType component, Position position);
         public int getShipStats(ComponentStatType statType); // engines, cannons, etc.
         public boolean isBuildingPhaseActive();
     }
     ```

     #### 1.2 ClientModel Integration
     **Location**: `src/main/java/it/polimi/ingsw/client/ClientModel.java`

     **Required Additions**:
     ```java
     public class ClientModel {
         // Add ship building state
         private GamePhase currentPhase = GamePhase.LOBBY;
         private long buildingPhaseStartTime = 0;
         private boolean isPlayerReady = false;

         // Ship building methods
         public void startBuildingPhase();
         public void endBuildingPhase();
         public void updatePlayerReadyStatus(String playerId, boolean ready);

         // Getters for UI binding
         public boolean isBuildingPhaseActive();
         public long getBuildingTimeRemaining();
     }
     ```

     #### 1.3 Event Handler Updates
     **Location**: Event handlers in message classes

     **Required Implementation**:
     - Uncomment and implement UI update code in event handlers
     - Add proper state updates for `TilePlacedEvent`, `BuildingTimerFlippedEvent`, etc.
     - Ensure events properly update `LocalGameState` and trigger UI refreshes

     ### **Phase 2: Basic UI Implementation**
     *Priority: High | Estimated effort: 3-4 days*

     #### 2.1 GUI Ship Building View
     **Location**: `src/main/java/it/polimi/ingsw/client/ui/gui/views/GuiShipBuildingView.java`

     **Required Components**:
     ```java
     public class GuiShipBuildingView extends BorderPane implements View {
         // UI Components
         private GridPane shipGrid;
         private ListView<ComponentType> availableTiles;
         private ListView<ComponentType> heldTiles;
         private Label timerLabel;
         private Label shipStatsLabel;
         private Button validateShipButton;
         private Button flipTimerButton;

         // Core functionality
         private void initializeShipGrid();
         private void setupDragAndDrop();
         private void updateTimerDisplay();
         private void updateShipStats();
         private void handleTilePlacement(ComponentType component, Position position);
         private void handleComponentSelection();
     }
     ```

     **Key Features**:
     - 5x7 visual ship grid with drag-and-drop support
     - Component inventory panels for available and held tiles
     - Real-time timer display
     - Ship statistics (engines, cannons, crew, etc.)
     - Visual validation feedback

     #### 2.2 TUI Ship Building View
     **Location**: `src/main/java/it/polimi/ingsw/client/ui/tui/views/TuiShipBuildingView.java`

     **Required Implementation**:
     ```java
     public class TuiShipBuildingView implements View {
         // Display methods
         private void displayShipGrid();
         private void displayAvailableTiles();
         private void displayHeldTiles();
         private void displayTimer();
         private void displayShipStats();

         // Command handling
         private void handlePlaceCommand(String[] args);
         private void handleTakeCommand(String[] args);
         private void handleReturnCommand(String[] args);
         private void handleValidateCommand();
         private void handleFlipTimerCommand();
     }
     ```

     **Key Features**:
     - ASCII art ship grid display
     - Text-based component selection and placement
     - Command-line interface for all ship building actions
     - Clear status and statistics display

     #### 2.3 View State Management
     **Updates required in**:
     - `GuiManager.java` - Route to functional ship building view
     - `TuiManager.java` - Route to functional ship building view
     - `ViewManager.java` - Handle view transitions properly

     ### **Phase 3: Network Integration**
     *Priority: High | Estimated effort: 2 days*

     #### 3.1 Request Sending
     **Integration points**:
     - UI actions → Request creation → Network sending
     - Button clicks and drag-drop → `PlaceTileRequest`, `TakeTileRequest`, etc.
     - Timer management → `FlipBuildingTimerRequest`
     - Ship validation → `ValidateShipRequest`

     #### 3.2 Event Handling
     **Required Implementation**:
     - `TilePlacedEvent` → Update ship grid display
     - `BuildingTimerFlippedEvent` → Update timer display
     - `BuildingPhaseEndedEvent` → Transition to flight phase
     - `ShipValidationEvent` → Display validation results

     #### 3.3 State Synchronization
     - Ensure client state stays synchronized with server
     - Handle reconnection scenarios
     - Implement error recovery for failed requests

     ### **Phase 4: Advanced Features**
     *Priority: Medium | Estimated effort: 2-3 days*

     #### 4.1 Component Reservation System
     - Implement 2-component reservation limit per rules
     - Add UI for reserving and managing reserved components
     - Handle reservation conflicts and timeouts

     #### 4.2 Ship Validation Enhancements
     - Real-time validation feedback as player builds
     - Visual highlighting of connection errors
     - Detailed validation error messages

     #### 4.3 Multiplayer Features
     - Display other players' building progress
     - Show component availability conflicts
     - Real-time player ready status

     #### 4.4 Polish and UX
     - Animations for tile placement (GUI)
     - Audio feedback for actions
     - Improved visual design and themes
     - Keyboard shortcuts and accessibility

     ## Message Flow Examples

     ### Typical Ship Building Session
     1. `GameStartedEvent` → Client enters building phase
     2. User clicks "Take Tile" → Send `TakeTileRequest` → Receive `TakeTileResponse`
     3. User drags tile to ship → Send `PlaceTileRequest` → Receive `TilePlacedEvent`
     4. User clicks "Flip Timer" → Send `FlipBuildingTimerRequest` → Timer updates
     5. User clicks "Validate Ship" → Send `ValidateShipRequest` → Receive validation results
     6. Server sends `BuildingPhaseEndedEvent` → Transition to flight phase

     ### Error Scenarios
     - Invalid tile placement → `PlaceTileResponse` with error → Show error message
     - Network timeout → Retry request → Show connection status
     - Disconnection during building → Reconnect → Restore game state

     ## Implementation Dependencies

     ### **Critical Path**:
     1. LocalGameState implementation (enables all other work)
     2. Basic UI views (provides user interface)
     3. Network integration (enables multiplayer functionality)
     4. Advanced features (polish and enhancement)

     ### **Parallel Work Possible**:
     - GUI and TUI views can be developed simultaneously
     - Message integration can happen alongside UI development
     - Server-side enhancements can be made independently

     ## Testing Strategy

     ### **Unit Tests**:
     - LocalGameState methods
     - Component placement validation
     - Timer management logic
     - Ship statistics calculation

     ### **Integration Tests**:
     - Client-server message flow
     - UI state synchronization
     - Multiplayer scenarios
     - Error handling and recovery

     ### **User Testing**:
     - Complete ship building sessions
     - UI usability testing
     - Performance testing with multiple players
     - Network reliability testing

     ## Success Criteria

     ### **Minimum Viable Product (MVP)**:
     - Players can build ships using basic UI
     - Timer management works
     - Ship validation functions
     - Multiplayer synchronization works
     - Transition to flight phase succeeds

     ### **Full Feature Set**:
     - Intuitive drag-and-drop interface (GUI)
     - Efficient command-line interface (TUI)
     - Real-time multiplayer synchronization
     - Component reservation system
     - Comprehensive error handling
     - Polish and visual feedback

     ## Estimated Timeline

     - **Phase 1 (State Management)**: 2-3 days
     - **Phase 2 (Basic UI)**: 3-4 days
     - **Phase 3 (Network Integration)**: 2 days
     - **Phase 4 (Advanced Features)**: 2-3 days
     - **Testing and Polish**: 2-3 days

     **Total Estimated Effort**: 11-15 days

     ## Risk Mitigation

     ### **High-Risk Items**:
     1. **Complex UI state synchronization** - Mitigate with simple state management patterns
     2. **Network timing issues** - Implement proper timeout and retry logic
     3. **Performance with frequent updates** - Use efficient UI update mechanisms
     4. **Cross-platform compatibility** - Test on multiple environments

     ### **Contingency Plans**:
     - If drag-and-drop proves complex, fall back to click-based placement
     - If real-time synchronization is problematic, use polling-based updates
     - If performance issues arise, implement UI update throttling

     This implementation plan provides a clear roadmap for completing the Galaxy Trucker ship building system while
     leveraging the existing solid foundation.
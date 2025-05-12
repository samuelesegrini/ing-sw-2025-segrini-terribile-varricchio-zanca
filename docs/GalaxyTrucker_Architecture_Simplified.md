# Galaxy Trucker: Simplified Architecture Documentation

## 1. System Overview

Galaxy Trucker is a digital implementation of the board game that follows a client-server architecture with the Model-View-Controller (MVC) pattern at its core. This document provides a focused explanation of the system's architecture, component interactions, and information flow.

## 2. Architecture Principles

### 2.1 Core Design Patterns

#### MVC Architecture
- **Model**: Represents the game state (GameModel, Player, Ship, Components)
- **View**: User interface elements that display game information to players
- **Controller**: Manages game logic, user inputs, and updates to model and view

#### Client-Server Architecture
- **Server**: The authoritative source of game state, manages game logic and rule enforcement
- **Client**: Handles UI rendering and user interaction, communicates with server for game actions

#### Additional Patterns
- **Command Pattern**: Encapsulates game actions as command objects for consistent processing
- **Visitor Pattern**: Enables type-safe processing of adventure card effects
- **Factory Pattern**: Creates game elements (components, cards) to ensure proper initialization
- **Observer Pattern**: Notifies clients of game state changes

## 3. Core System Components

### 3.1 Game Model Components

#### GameModel
- Central repository of game state
- Manages players, components, adventure cards
- Tracks game phase and coordinates phase transitions
- Key Methods: startGame(), advancePhase(), getCurrentPlayer()

#### Player
- Represents a game participant
- Contains player identity, ship, credits, and game ranking
- Manages player-specific resources and actions

#### Ship
- Represents a player's spacecraft
- Contains component grid and structural integrity information
- Validates proper component connections and ship structure
- Key Methods: placeComponent(), removeComponent(), validateStructure()

#### Component Hierarchy
- Abstract Component class with specialized implementations:
  - **Cabin**: Houses crew members
  - **Engine**: Provides propulsion power
  - **Cannon**: Offers defense against meteors and enemies
  - **Shield**: Protects against certain hazards
  - **CrewCabin**: Specialized cabin for extra crew
  - **Batteries**: Stores energy for components
  - **CargoHold**: Stores goods for trade
  - **StructuralComponent**: Provides structural integrity

### 3.2 Server-Side Components

#### ServerApp
- Entry point for server application
- Initializes and coordinates server-side components
- Manages server lifecycle (startup, shutdown)
- Loads configuration settings

#### MultiGameCoordinator
- Manages multiple game instances
- Handles game creation and player assignment
- Coordinates game lobbies and session management
- Acts as a facade for game-related operations

#### GameInstanceController
- Controls a single game instance
- Processes player commands and applies game rules
- Manages game state transitions
- Broadcasts updates to connected clients

#### RuleEngine
- Validates game actions against rules
- Enforces game mechanics and constraints
- Calculates outcomes of adventure cards and events
- Ensures consistent rule application

#### SessionManager
- Handles player authentication and authorization
- Manages disconnection and reconnection scenarios
- Maintains session state for active players
- Ensures secure communication

#### NetworkManager
- Manages network connections (sockets or RMI)
- Handles message serialization and deserialization
- Coordinates client handlers
- Manages connection pooling and threading

### 3.3 Client-Side Components

#### ClientApp
- Entry point for client application
- Initializes client UI and communication components
- Manages client lifecycle

#### ClientController
- Central coordinator for client operations
- Processes server updates and refreshes UI
- Sends player commands to server
- Manages view transitions

#### ServerProxy
- Abstracts server communication details
- Provides consistent interface for network operations
- Implementations:
  - **SocketServerProxy**: Socket-based communication
  - **RMIServerProxy**: Java RMI-based communication

#### ViewModelCache
- Maintains local copy of game state for UI rendering
- Reduces need for frequent server communication
- Provides data for view components

#### ViewController
- Manages UI component updates
- Handles user input and converts to commands
- Coordinates view transitions based on game state

## 4. Key Interactions and Flows

### 4.1 Game Initialization Flow

1. **Server Startup**:
   - ServerApp initializes components
   - NetworkManager opens communication channels
   - MultiGameCoordinator prepares for game creation

2. **Client Connection**:
   - ClientApp connects to server via ServerProxy
   - Server authenticates client and establishes session
   - Client joins game lobby or creates new game

3. **Game Setup**:
   - MultiGameCoordinator creates GameInstanceController
   - GameModel initialized with game parameters
   - Players assigned to game instance
   - Initial game state broadcast to all clients

### 4.2 Game Action Flow

1. **Player Action Initiation**:
   - User interacts with UI
   - ViewController captures interaction
   - ClientController creates command object
   - ServerProxy sends command to server

2. **Server-Side Processing**:
   - NetworkManager receives command
   - Command routed to appropriate GameInstanceController
   - RuleEngine validates command
   - GameModel updated based on command
   - Results calculated and prepared for broadcast

3. **Client Update**:
   - Server broadcasts state changes to all clients
   - ServerProxy receives updates
   - ClientController processes updates
   - ViewModelCache updated with new state
   - ViewController refreshes UI components

### 4.3 Game Phase Transitions

#### Build Phase
- Timer management for component selection
- Simultaneous component selection by players
- Ship construction with rule validation
- Phase completion when timer expires or all players ready

#### Flight Phase
- Sequential adventure card resolution
- Ship modifications based on encounters
- Player decision points (using crew, batteries, etc.)
- Resource tracking and damage application

#### Game Over Phase
- Final scoring calculation
- Winner determination
- Statistics gathering and display

## 5. Network Communication

### 5.1 Communication Methods

#### Socket-based Communication
- TCP/IP sockets for reliable message delivery
- Custom protocol for message formatting
- Thread-per-client handling on server

#### RMI-based Communication
- Java Remote Method Invocation
- Object-oriented remote procedure calls
- Transparent proxying of remote objects

### 5.2 Data Transfer Objects (DTOs)

- Used to transmit game state between client and server
- Provides clean separation between network and domain layers
- Efficiently serialized for network transmission
- Examples:
  - **GameStateDTO**: Overall game state snapshot
  - **PlayerDTO**: Player information
  - **ShipDTO**: Ship configuration and status
  - **CommandDTO**: Encapsulated player actions

## 6. Class Diagrams

### 6.1 Core Model Structure
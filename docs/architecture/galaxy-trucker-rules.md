# Galaxy Trucker - Digital Implementation Rules
## Table of Contents
1. [Game Overview](#game-overview)
2. [Technical Implementation](#technical-implementation)
3. [Game Levels and Configurations](#game-levels-and-configurations)
4. [Components](#components)
5. [Game Setup](#game-setup)
6. [Multiplayer System](#multiplayer-system)
7. [Building Phase](#building-phase)
8. [Flight Phase](#flight-phase)
9. [Adventure Cards](#adventure-cards)
10. [Scoring System](#scoring-system)
11. [Special Rules](#special-rules)
12. [Network Protocol](#network-protocol)
13. [Component Reference](#component-reference)
14. [Card Reference](#card-reference)

---

## Game Overview

### Objective
Players are galaxy truckers working for Corporation Incorporated, building spaceships from sewer pipe components
 and flying them through dangerous space. This digital implementation supports 2-4 players in real-time
multiplayer with dual UI support (GUI and Terminal).

### Digital Implementation Features
- **Client-Server Architecture** - Centralized game state with distributed clients
- **Dual UI Support** - Both JavaFX GUI and Terminal-based TUI interfaces
- **Real-time Multiplayer** - Synchronized building phase and turn-based flight phase
- **Network Flexibility** - Socket and RMI protocol support
- **Comprehensive Validation** - Automated rule enforcement and ship validation

### Game Flow
Each game consists of two main phases:
1. **Building Phase** - Real-time simultaneous construction with network synchronization
2. **Flight Phase** - Turn-based adventure resolution with event broadcasting

### Winning Conditions
- **Position-based Victory** - Credits awarded based on finish position
- **Resource Accumulation** - Additional credits from goods and ship condition
- **Multiplayer Ranking** - Highest total credits wins among all connected players

---

## Technical Implementation

### Architecture Overview
- **Java 23** with Platform Module System (JPMS)
- **Maven Build System** for dependency management and packaging
- **Client-Server Model** with centralized game state authority
- **Message-Based Communication** using Request-Response and Event patterns
- **Thread-Safe Operations** for concurrent multiplayer gameplay

### Network Protocols
- **Primary Protocol**: TCP Socket-based communication
- **Secondary Protocol**: Java RMI with callback support
- **Connection Monitoring**: Heartbeat system with ping/pong messages
- **Reconnection Support**: Automatic reconnection with session persistence

### User Interface Options
1. **GUI Mode (JavaFX)**:
   - Modern graphical interface with drag-and-drop ship building
   - Visual component placement and connection validation
   - Real-time multiplayer status and game state display
   - Custom themes and high-quality game assets

2. **TUI Mode (Terminal)**:
   - Command-line interface using JLine framework
   - Text-based ship representation and menu navigation
   - ANSI color support for enhanced visual feedback
   - Keyboard-driven gameplay suitable for server environments

### Data Persistence
- **JSON Configuration Files**: Game rules, components, and adventure cards
- **Session Management**: Player authentication and game state persistence
- **Asset Management**: Bundled images, fonts, and configuration files

---

## Game Levels and Configurations

### Test Flight Level
**Purpose**: Learning and tutorial mode with simplified rules

**Configuration**:
- **Flight Board**: 18 spaces total
- **Ship Grid**: 5×7 grid with specific forbidden positions
- **Component Set**: Reduced complexity with basic components only
- **Adventure Cards**: Simplified encounter set for learning
- **Building Time**: Extended timer for new players
- **Position Rewards**: 4, 3, 2, 1 credits for 1st through 4th place
- **Best Ship Bonus**: 2 credits for most aesthetically pleasing ship

**Forbidden Ship Positions**:
- Positions (0,0), (0,6), (4,0), (4,6) - corner positions unavailable
- Starting cabin fixed at center position (2,3)

### Level II (Standard Game)
**Purpose**: Full game experience with complete rule set

**Configuration**:
- **Flight Board**: 24 spaces total
- **Ship Grid**: 5×7 grid with different position constraints
- **Component Set**: Complete component set with all types
- **Adventure Cards**: Full adventure deck with all encounter types
- **Building Time**: Standard timer with flip-based progression
- **Position Rewards**: 8, 6, 4, 2 credits for 1st through 4th place
- **Best Ship Bonus**: 4 credits for most aesthetically pleasing ship

**Ship Constraints**:
- All grid positions available except starting cabin location
- Advanced component placement rules apply
- Component reservation system (up to 2 components)

### Component Availability by Level
- **Test Flight**: Basic engines, cannons, cargo holds, crew cabins, structural components
- **Level II**: All components including advanced batteries, shields, life support, alien cabins

---

## Components

### Ship Boards
- **Class I Ships** - Beginner level, smaller ships
- **Class II Ships** - Intermediate level, medium ships
- **Class III Ships** - Advanced level, large ships

### Component Tiles
- **Engines** - Provide thrust for movement and planet landing
- **Cannons** - Attack enemies and destroy meteors
- **Crew Cabins** - House crew members and provide life support
- **Cargo Holds** - Store goods collected during flight
- **Batteries** - Power ship systems and provide energy
- **Shields** - Protect against meteor damage
- **Structural Components** - Provide connections and structural integrity

### Adventure Cards
- **Level I Cards** - 8 basic cards for learning
- **Level II Cards** - Intermediate difficulty adventures
- **Level III Cards** - Advanced challenges and complex scenarios

### Other Components
- **Flight Boards** - Track ship positions during adventures
- **Rocket Markers** - Represent ships on flight board
- **Astronaut Figures** - Represent crew members
- **Alien Figures** - Special passengers with unique abilities
- **Dice** - Resolve combat and random events
- **Timer** - Controls building phase duration
- **Goods Blocks** - Valuable cargo in different colors
- **Cosmic Credits** - Game currency
- **Battery Tokens** - Track stored energy

---

## Game Setup

### Choose Difficulty Level
1. **Level I** - Learning game with basic components and simple adventures
2. **Level II** - Standard game with full component set and moderate challenges
3. **Level III** - Expert game with all components and complex adventures

### Preparation Steps
1. **Place Flight Board** - Position appropriate level flight board in center
2. **Prepare Components** - Shuffle component tiles face-down by type
3. **Distribute Ship Boards** - Each player gets ship board matching chosen level
4. **Starting Equipment** - Each player gets 1 rocket marker and starting cabin
5. **Adventure Deck** - Shuffle adventure cards matching chosen level
6. **Starting Credits** - Each player begins with same number of credits
7. **Timer Setup** - Prepare sand timer for building phase

### Digital Game Setup
1. **Server Initialization**
   - Start Galaxy Trucker server application
   - Configure network ports (Socket and RMI)
   - Load game configurations and adventure cards
   - Initialize game session manager

2. **Client Connection**
   - Launch client application (GUI or TUI mode)
   - Connect to server using preferred protocol
   - Authenticate with player nickname
   - Join game lobby or create new game

3. **Game Lobby**
   - Browse available games or create new game
   - Select game level (Test Flight or Level II)
   - Wait for 2-4 players to join
   - Host starts game when all players ready

---

## Multiplayer System

### Connection Management
- **Player Authentication**: Unique nickname-based login system
- **Session Persistence**: Maintain player state across connections
- **Reconnection Support**: Automatic reconnection for network interruptions
- **Connection Monitoring**: Heartbeat system to detect disconnected players

### Game Session Flow
1. **Lobby Phase**
   - Players join game sessions via lobby browser
   - Game host configures level and starts game
   - Real-time player list updates for all participants

2. **Building Phase Synchronization**
   - Synchronized timer start across all clients
   - Real-time component placement validation
   - Component reservation system with conflict resolution
   - Simultaneous building phase completion

3. **Flight Phase Coordination**
   - Turn-based adventure card resolution
   - Server-authoritative game state updates
   - Event broadcasting for all game state changes
   - Automated damage and reward calculation

### Network Protocol Messages

#### Authentication Messages
- **LoginRequest/LoginResponse**: Player authentication
- **ReconnectRequest/ReconnectResponse**: Session restoration

#### Game Management Messages
- **CreateGameRequest/CreateGameResponse**: New game creation
- **JoinGameRequest/JoinGameResponse**: Join existing game
- **ListGamesRequest/ListGamesResponse**: Browse available games
- **LeaveGameRequest/LeaveGameResponse**: Exit current game

#### Gameplay Messages
- **PlaceTileRequest/PlaceTileResponse**: Component placement
- **ValidateShipRequest/ValidateShipResponse**: Ship validation
- **StartFlightRequest**: Begin flight phase

#### Event Broadcasting
- **PlayerJoinedGameEvent**: New player joins game
- **PlayerLeftGameEvent**: Player leaves game
- **GameCreatedEvent**: New game available
- **TilePlacedEvent**: Component placed by player
- **ShipValidationEvent**: Ship validation results
- **GameEndedEvent**: Game completion notification

### Fault Tolerance
- **Connection Recovery**: Automatic reconnection with exponential backoff
- **State Synchronization**: Full game state recovery after reconnection
- **Timeout Handling**: Graceful handling of unresponsive players
- **Graceful Degradation**: Continue game with fewer players if possible

---

## Game Phases

### Phase Overview
Each round alternates between Building and Flight phases:

1. **Building Phase** (Simultaneous, Real-time)
   - Players construct ships using component tiles
   - Timer controls available building time
   - Components must follow connection rules

2. **Flight Phase** (Turn-based, Sequential)
   - Adventure cards resolved in order
   - Ships move along flight board
   - Encounters resolved based on ship capabilities

---

## Building Phase

### Digital Building Rules
- **Synchronized Real-time Construction** - All networked players build simultaneously
- **Network-validated Placement** - Server validates all component placements
- **Timer Synchronization** - Centralized timer broadcast to all clients
- **Deferred Validation** - Final validation performed at phase completion

### Digital Implementation Features
- **Visual Drag-and-Drop (GUI)** - Intuitive component placement with visual feedback
- **Command-based Placement (TUI)** - Text commands for component positioning
- **Real-time Conflict Resolution** - Handle simultaneous component selection
- **Component Reservation System** - Reserve up to 2 components during building
- **Automated Correction** - System automatically fixes minor rule violations

### Connection Rules
Components connect via pipe systems with digital validation:

#### Connector Types
1. **Universal Connectors** - Connect to any other connector type
2. **Single Pipe** - Connect only to single pipe or universal
3. **Double Pipe** - Connect only to double pipe or universal
4. **Smooth Sides** - Cannot connect to anything

#### Digital Connection Validation
- **Real-time Feedback** - Immediate visual/text feedback for invalid placements
- **Connection Highlighting** - Show compatible connection points
- **Structural Integrity Check** - Automated validation of ship connectivity
- **End-of-Phase Validation** - Comprehensive rule check before flight phase

### Component Placement Rules

#### Engines
- **Must face rear** - Engine thrust points toward back of ship
- **Provide Movement** - Total engine strength determines flight capabilities
- **Power Requirements** - Larger engines may require battery power

#### Cannons
- **Must face forward** - Cannon muzzles point toward front of ship
- **Clear Line of Fire** - No obstructions in front of cannon
- **Combat Strength** - Cannon firepower determines attack capability

#### Crew Cabins
- **Life Support** - Required for crew members and aliens
- **Alien Housing** - Some aliens require specific cabin types
- **Crew Capacity** - Each cabin houses limited number of crew

#### Cargo Holds
- **Goods Storage** - Store valuable cargo collected during flight
- **Capacity Limits** - Each hold stores specific number of goods
- **Protection** - Interior placement provides better protection

#### Batteries
- **Energy Storage** - Power various ship systems
- **Capacity Ratings** - Different batteries store different amounts
- **Power Distribution** - Must connect to powered components

#### Shields
- **Directional Protection** - Protect against meteors from specific directions
- **Coverage Area** - Shield strength covers adjacent ship areas
- **Meteor Defense** - Primary defense against meteor swarms

### Building Strategies
- **Structural Planning** - Plan component layout before building
- **Protection Priority** - Protect valuable components in ship interior
- **Balanced Design** - Balance offense, defense, and utility
- **Contingency Planning** - Plan for component loss during flight

### Component Reserving
- **Reserve Option** - Take component tile but don't place immediately
- **One Reserved** - Maximum one component reserved per player
- **Placement Requirement** - Must place reserved component before taking another

---

## Flight Phase

### Phase Structure
Adventure cards resolved in sequential order:
1. **Reveal Card** - Draw next adventure card
2. **Determine Order** - Player order based on ship capabilities
3. **Resolve Encounters** - Each player resolves adventure individually
4. **Apply Consequences** - Damage, rewards, and penalties applied

### Adventure Types

#### Planets
**Objective**: Land on planet to collect valuable goods

**Resolution Process**:
1. **Landing Requirement** - Must have sufficient engine power
2. **Crew Requirement** - Must have available crew members
3. **Goods Collection** - Collect goods based on planet type
4. **Flight Day Cost** - Pay credits for time spent on planet

**Planet Types**:
- **Green Planets** - Abundant goods, low requirements
- **Blue Planets** - Moderate goods, moderate requirements
- **Red Planets** - Valuable goods, high requirements

#### Abandoned Ships and Stations
**Objective**: Explore derelict structures for valuable salvage

**Resolution Process**:
1. **Crew Requirement** - Must send crew members to explore
2. **Risk Assessment** - Determine exploration risks
3. **Reward Collection** - Collect credits, goods, or components
4. **Crew Risk** - Possible crew loss during exploration

**Structure Types**:
- **Abandoned Ships** - Moderate risk, moderate reward
- **Space Stations** - High risk, high reward
- **Derelict Hulks** - Low risk, low reward

#### Combat Encounters

##### Smugglers
**Characteristics**: Weak opponents, moderate rewards
- **Combat Strength**: Low
- **Reward**: Credits and goods
- **Special**: May flee if outgunned

##### Pirates
**Characteristics**: Moderate opponents, good rewards
- **Combat Strength**: Medium
- **Reward**: Credits and valuable goods
- **Special**: May board ship if defeated

##### Slavers
**Characteristics**: Strong opponents, crew threats
- **Combat Strength**: High
- **Reward**: Significant credits
- **Special**: Steal crew members if victorious

**Combat Resolution**:
1. **Determine Combat Strength** - Count total cannon firepower
2. **Roll Dice** - Add dice results to combat strength
3. **Compare Totals** - Higher total wins combat
4. **Apply Results** - Winner gains rewards, loser takes damage

#### Open Space
**Objective**: Navigate through empty space using engine power

**Resolution Process**:
1. **Engine Check** - Count total engine power
2. **Movement Determination** - Engine power determines movement distance
3. **Position Update** - Move rocket marker on flight board
4. **Bonus Opportunity** - Strong engines may provide bonus movement

#### Meteor Swarms
**Objective**: Survive meteor bombardment using shields and cannons

**Meteor Types**:
- **Small Meteors** - Weak impact, easy to destroy
- **Large Meteors** - Strong impact, difficult to destroy
- **Meteor Storms** - Multiple meteors from various directions

**Resolution Process**:
1. **Determine Direction** - Meteors approach from specific direction
2. **Shield Defense** - Shields absorb meteor damage
3. **Cannon Defense** - Cannons destroy meteors before impact
4. **Damage Application** - Unblocked meteors damage ship

#### Combat Zones
**Objective**: Survive dangerous areas with multiple threats

**Zone Types**:
- **War Zones** - Military conflicts requiring combat readiness
- **Pirate Nebulas** - Criminal activity requiring defensive measures
- **Quarantine Zones** - Disease outbreaks requiring life support

**Resolution Process**:
1. **Multiple Checks** - May require engines, cannons, crew, or shields
2. **Cumulative Penalties** - Failing multiple checks increases penalties
3. **Severe Consequences** - Major component loss or crew casualties

#### Special Events

##### Stardust
**Effect**: Cosmic phenomenon affecting ship systems
- **Navigation Disruption** - Reduces engine effectiveness
- **Communication Interference** - Affects crew coordination
- **System Overload** - May damage electronic components

##### Epidemic
**Effect**: Disease outbreak affecting crew members
- **Crew Illness** - Reduces available crew members
- **Quarantine Measures** - Limits crew activities
- **Medical Treatment** - Requires medical facilities or loses crew

##### Sabotage
**Effect**: Internal ship damage from saboteurs
- **System Damage** - Random component damage
- **Security Breach** - Affects ship integrity
- **Crew Loyalty** - May affect crew performance

### Ship Movement
- **Movement Order** - Determined by relevant ship capabilities
- **Position Tracking** - Rocket markers show current position
- **Finish Line** - First ships to finish gain position bonuses

---

## Adventure Cards

### Card Structure
Each adventure card contains:
- **Adventure Type** - Identifies encounter category
- **Requirements** - Necessary ship capabilities
- **Rewards** - Benefits for successful completion
- **Penalties** - Consequences for failure
- **Special Rules** - Unique mechanics for this adventure

### Difficulty Scaling
- **Level I** - Simple requirements, clear outcomes
- **Level II** - Moderate complexity, multiple options
- **Level III** - Complex scenarios, difficult decisions

### Card Categories

#### Combat Cards
- **Enemy Strength** - Combat difficulty rating
- **Reward Structure** - Credits and goods for victory
- **Combat Modifiers** - Special combat rules
- **Damage Consequences** - Penalties for defeat

#### Exploration Cards
- **Crew Requirements** - Necessary crew members
- **Risk Factors** - Potential crew loss
- **Discovery Rewards** - Valuable findings
- **Time Costs** - Flight day penalties

#### Environmental Cards
- **Directional Threats** - Meteors and hazards
- **System Requirements** - Necessary ship systems
- **Damage Patterns** - Specific damage types
- **Survival Rewards** - Bonuses for success

#### Event Cards
- **Unique Mechanics** - Special rules for this event
- **Universal Effects** - Affects all players
- **Conditional Triggers** - Activate under specific conditions
- **Ongoing Effects** - May affect multiple adventures

---

## Scoring System

### Digital Implementation Scoring

#### Position-Based Rewards
**Test Flight Level**:
- **1st Place**: 4 credits
- **2nd Place**: 3 credits
- **3rd Place**: 2 credits
- **4th Place**: 1 credit

**Level II**:
- **1st Place**: 8 credits
- **2nd Place**: 6 credits
- **3rd Place**: 4 credits
- **4th Place**: 2 credits

#### Goods Values (Fixed Digital Pricing)
- **Red Goods**: 4 credits each
- **Yellow Goods**: 3 credits each
- **Green Goods**: 2 credits each
- **Blue Goods**: 1 credit each

#### Best-Looking Ship Bonus
**Automated Aesthetic Evaluation**:
- **Test Flight**: 2 credit bonus for best ship design
- **Level II**: 4 credit bonus for best ship design
- **Evaluation Criteria**: Symmetry, component distribution, visual appeal
- **Server Calculation**: Automated assessment based on ship layout algorithms

### Credit Penalties

#### Rule Violation Penalties
- **Building Rule Violations**: 1 credit penalty per violation
- **Invalid Component Placement**: Automatically corrected with penalty
- **Connection Violations**: 1 credit per invalid connection
- **Ship Grid Violations**: 1 credit per forbidden position used

#### Exposed Connector Penalty
- **Calculation**: 1 credit penalty per exposed connector on final ship
- **Automated Assessment**: Server automatically counts exposed connectors
- **End-of-Game Evaluation**: Applied during final scoring calculation

### Victory Conditions

#### Single Flight Victory
- **Profit Threshold** - Any positive credit balance counts as victory
- **Highest Credits** - Player with most credits wins
- **Tie Breakers** - Ship condition, then component count

#### Campaign Victory
- **Cumulative Credits** - Total credits across all flights
- **Title Tiles** - Special achievements provide additional victory points
- **Transgalactic Champion** - Complete all three levels successfully

---

## Special Rules

### Aliens
**Alien Passengers**: Special characters with unique abilities

#### Alien Types
- **Green Aliens** - Provide engine power bonuses
- **Blue Aliens** - Enhance combat capabilities
- **Purple Aliens** - Improve cargo capacity
- **Brown Aliens** - Provide life support efficiency

#### Alien Requirements
- **Life Support** - Aliens require crew cabin space
- **Special Cabins** - Some aliens need specific cabin types
- **Crew Interaction** - Aliens may affect crew capabilities

#### Alien Benefits
- **Passive Bonuses** - Continuous improvements to ship performance
- **Active Abilities** - Special actions during specific encounters
- **End Game Bonuses** - Additional scoring opportunities

### Giving Up
Players may give up voluntarily or be forced to give up:

#### Forced Giving Up
- **No Crew** - No crew members remaining to operate ship
- **No Engines** - No functional engines to continue flight
- **Getting Lapped** - Falling too far behind other players

#### Voluntary Giving Up
- **Strategic Decision** - Avoid further losses
- **Damage Mitigation** - Preserve remaining components
- **Time Considerations** - Speed up game completion

#### Giving Up Consequences
- **No Further Rewards** - Cannot collect additional credits or goods
- **Position Penalty** - Finish last for position bonuses
- **Component Preservation** - Keep remaining components for scoring

### Goods Shortage
In higher difficulty levels, goods may be limited:

#### Shortage Rules
- **First Come, First Served** - Earlier players get priority
- **Limited Supply** - Not all players may get goods
- **Market Dynamics** - Affects goods values and availability

### Rule Violations

#### Building Phase Violations
- **Structural Integrity** - Components must connect properly
- **Placement Rules** - Components must follow orientation rules
- **Connection Matching** - Connectors must be compatible

#### Flight Phase Violations
- **Capability Requirements** - Must have necessary ship systems
- **Crew Limitations** - Cannot exceed crew capacity
- **Component Functionality** - Must have functional components

#### Violation Penalties
- **Building Violations** - Fix during building or face flight penalties
- **Flight Violations** - Immediate penalties during adventure resolution
- **Repeated Violations** - Escalating penalties for multiple infractions

---

## Digital Implementation Features

### Component Reservation System
**Advanced Building Strategy**: Reserve components during building phase

#### Reservation Rules
- **Maximum Reservations**: Up to 2 components per player
- **Reservation Duration**: Until placed or building phase ends
- **Conflict Resolution**: First-come-first-served with network latency compensation
- **Visual Indicators**: Clear marking of reserved components in both UI modes

### Flight Forecast System
**Strategic Planning Tool**: Preview upcoming adventure cards

#### Forecast Features
- **Limited Preview**: View select upcoming adventures before building
- **Strategic Building**: Plan ship design based on known challenges
- **Dynamic Updates**: Forecast updates as game progresses
- **Fair Information**: All players receive same forecast information

### Alien Crew Members
**Advanced Gameplay Mechanic**: Special crew with unique abilities

#### Alien Types and Abilities
- **Navigation Aliens**: Provide bonus engine power for movement
- **Combat Aliens**: Enhance combat effectiveness
- **Engineering Aliens**: Improve ship repair capabilities
- **Trade Aliens**: Increase goods collection efficiency

#### Alien Requirements
- **Life Support Systems**: Aliens require specialized cabins
- **Crew Capacity**: Aliens count toward crew limits
- **Interaction Rules**: Aliens may affect crew member capabilities

### Automated Game Management
**Digital Convenience Features**

#### Automated Calculations
- **Damage Resolution**: Automatic component loss calculation
- **Score Calculation**: Real-time credit tracking and final scoring
- **Ship Validation**: Comprehensive rule checking with violation reporting
- **Turn Management**: Automated turn order and phase transitions

#### Quality of Life Features
- **Auto-save**: Continuous game state preservation
- **Reconnection**: Seamless reconnection to ongoing games
- **Spectator Mode**: Observe games in progress
- **Game History**: Track previous game results and statistics

### Network Optimization Features
**Performance and Reliability**

#### Connection Management
- **Protocol Selection**: Choose between Socket and RMI protocols
- **Bandwidth Optimization**: Efficient message compression and batching
- **Latency Compensation**: Fair handling of network delays
- **Connection Pooling**: Efficient resource utilization

#### Fault Tolerance
- **Graceful Degradation**: Continue games with reduced player count
- **State Recovery**: Complete game state restoration after disconnection
- **Timeout Handling**: Intelligent handling of unresponsive players
- **Error Recovery**: Automatic recovery from transient network issues

---

## Network Protocol

### Communication Architecture
**Message-Based Protocol**: All client-server communication uses structured message objects

#### Protocol Types
1. **Request-Response Pattern**
   - Client sends request message
   - Server processes and returns response
   - Correlation IDs track message pairs
   - Timeout handling for failed requests

2. **Event Broadcasting**
   - Server broadcasts events to relevant clients
   - Real-time game state updates
   - Player action notifications
   - Game lifecycle events

### Message Categories

#### Authentication Protocol
```java
// Player login and session management
LoginRequest(nickname) → LoginResponse(success, playerId, sessionToken)
ReconnectRequest(sessionToken) → ReconnectResponse(success, gameState)
```

#### Game Management Protocol
```java
// Game lifecycle management
CreateGameRequest(levelType) → CreateGameResponse(gameId, success)
JoinGameRequest(gameId) → JoinGameResponse(success, playerList)
ListGamesRequest() → ListGamesResponse(availableGames[])
LeaveGameRequest() → LeaveGameResponse(success)
```

#### Gameplay Protocol
```java
// In-game actions and state updates
PlaceTileRequest(componentId, position, rotation) → PlaceTileResponse(success, violations[])
ValidateShipRequest() → ValidateShipResponse(isValid, violations[], penaltyCost)
StartFlightRequest() → (begins flight phase)
```

#### Event Broadcasting Protocol
```java
// Server-initiated state updates
PlayerJoinedGameEvent(playerId, nickname)
PlayerLeftGameEvent(playerId)
TilePlacedEvent(playerId, componentId, position)
ShipValidationEvent(playerId, violations[], penalties)
GameEndedEvent(finalScores[], winners[])
```

### Connection Management

#### Heartbeat System
- **PingMessage**: Sent every 30 seconds by server
- **PongMessage**: Client response to maintain connection
- **Timeout Detection**: 60-second timeout for unresponsive clients
- **Graceful Disconnection**: Clean session termination

#### Session Persistence
- **Session Tokens**: Maintain authentication across connections
- **State Recovery**: Full game state restoration after reconnection
- **Player Registry**: Centralized player session management
- **Game State Backup**: Continuous state preservation for recovery

### Network Adapters

#### Socket Implementation
- **TCP/IP Protocol**: Reliable stream-based communication
- **Object Serialization**: Java serialization for message transport
- **Thread Management**: Dedicated threads for message handling
- **Connection Pooling**: Efficient resource utilization

#### RMI Implementation
- **Remote Method Invocation**: Direct method calls across network
- **Callback Interface**: Server-to-client event delivery
- **Registry Service**: Service discovery and binding
- **Exception Handling**: Network failure recovery

### Security and Validation

#### Message Validation
- **Input Sanitization**: Validate all incoming message data
- **Type Safety**: Ensure message type correctness
- **Range Checking**: Validate numeric parameters
- **State Validation**: Verify game state consistency

#### Connection Security
- **Session Management**: Secure session token handling
- **Rate Limiting**: Prevent message flooding
- **Connection Limits**: Maximum connections per client
- **Graceful Degradation**: Handle malicious or malformed messages

---

## Component Reference

### Detailed Component Descriptions

#### Engines
**Type A Engines**:
- **Power Output**: 1 engine strength
- **Size**: Single space
- **Requirements**: None
- **Special**: Basic propulsion

**Type B Engines**:
- **Power Output**: 2 engine strength
- **Size**: Single space
- **Requirements**: None
- **Special**: Improved propulsion

**Type C Engines**:
- **Power Output**: 3 engine strength
- **Size**: Double space
- **Requirements**: Battery power
- **Special**: High-power propulsion

**Type D Engines**:
- **Power Output**: 4 engine strength
- **Size**: Double space
- **Requirements**: Battery power
- **Special**: Maximum propulsion

#### Cannons
**Type A Cannons**:
- **Combat Strength**: 1 firepower
- **Size**: Single space
- **Requirements**: None
- **Special**: Basic weapons

**Type B Cannons**:
- **Combat Strength**: 2 firepower
- **Size**: Single space
- **Requirements**: None
- **Special**: Improved weapons

**Type C Cannons**:
- **Combat Strength**: 3 firepower
- **Size**: Double space
- **Requirements**: Battery power
- **Special**: Heavy weapons

**Type D Cannons**:
- **Combat Strength**: 4 firepower
- **Size**: Double space
- **Requirements**: Battery power
- **Special**: Maximum firepower

#### Crew Cabins
**Basic Cabins**:
- **Crew Capacity**: 1 crew member
- **Size**: Single space
- **Requirements**: None
- **Special**: Standard living quarters

**Double Cabins**:
- **Crew Capacity**: 2 crew members
- **Size**: Double space
- **Requirements**: None
- **Special**: Shared living quarters

**Alien Cabins**:
- **Crew Capacity**: 1 alien passenger
- **Size**: Single space
- **Requirements**: Life support
- **Special**: Specialized for alien physiology

#### Cargo Holds
**Type A Cargo**:
- **Capacity**: 1 goods block
- **Size**: Single space
- **Requirements**: None
- **Special**: Basic storage

**Type B Cargo**:
- **Capacity**: 2 goods blocks
- **Size**: Single space
- **Requirements**: None
- **Special**: Compact storage

**Type C Cargo**:
- **Capacity**: 3 goods blocks
- **Size**: Double space
- **Requirements**: None
- **Special**: Large storage

#### Batteries
**Small Batteries**:
- **Energy Storage**: 1 energy unit
- **Size**: Single space
- **Requirements**: None
- **Special**: Basic power storage

**Large Batteries**:
- **Energy Storage**: 2 energy units
- **Size**: Single space
- **Requirements**: None
- **Special**: Enhanced power storage

**Power Cores**:
- **Energy Storage**: 3 energy units
- **Size**: Double space
- **Requirements**: None
- **Special**: Maximum power storage

#### Shields
**Type A Shields**:
- **Protection**: 1 shield strength
- **Size**: Single space
- **Requirements**: None
- **Special**: Basic protection

**Type B Shields**:
- **Protection**: 2 shield strength
- **Size**: Single space
- **Requirements**: None
- **Special**: Improved protection

**Type C Shields**:
- **Protection**: 3 shield strength
- **Size**: Double space
- **Requirements**: Battery power
- **Special**: Heavy protection

#### Structural Components
**Connectors**:
- **Function**: Provide connections between components
- **Size**: Single space
- **Requirements**: None
- **Special**: No special function, just connections

**Structural Supports**:
- **Function**: Provide structural integrity
- **Size**: Various sizes
- **Requirements**: None
- **Special**: Prevent component disconnection

---

## Card Reference

### Adventure Card Details

#### Level I Cards (Learning Set)
1. **Smugglers** - Weak pirates, good for learning combat
2. **Abandoned Ship** - Simple exploration encounter
3. **Small Meteors** - Basic meteor swarm
4. **Open Space** - Engine power test
5. **Peaceful Planet** - Easy goods collection
6. **Stardust** - Mild space phenomenon
7. **Pirates** - Moderate combat encounter
8. **Meteor Storm** - Challenging meteor swarm

#### Level II Cards (Standard Set)
- **Combat Encounters**: Various pirates, smugglers, and military
- **Exploration**: Multiple ship and station types
- **Environmental**: Meteor swarms, space storms, nebulas
- **Planets**: Diverse world types with varying requirements
- **Events**: Epidemics, sabotage, system failures

#### Level III Cards (Expert Set)
- **Complex Combat**: Multi-stage battles, fleet encounters
- **Dangerous Exploration**: High-risk, high-reward scenarios
- **Severe Environmental**: Massive meteor storms, cosmic phenomena
- **Exotic Planets**: Alien worlds with unique challenges
- **Major Events**: Campaign-changing consequences

### Combat Reference Table

#### Enemy Strength Ratings
- **Strength 1-2**: Weak enemies (Scouts, Smugglers)
- **Strength 3-4**: Moderate enemies (Pirates, Patrol Ships)
- **Strength 5-6**: Strong enemies (Warships, Slaver Ships)
- **Strength 7+**: Extremely dangerous (Battleships, Alien Dreadnoughts)

#### Combat Resolution
1. **Player Combat Strength** = Total Cannon Firepower + Dice Roll
2. **Enemy Combat Strength** = Enemy Rating + Dice Roll
3. **Victory**: Player strength ≥ Enemy strength
4. **Defeat**: Player strength < Enemy strength

### Meteor Damage Table

#### Meteor Strength vs Shield Strength
- **Meteor > Shield**: Component destroyed
- **Meteor = Shield**: Shield destroyed, no further damage
- **Meteor < Shield**: No damage, meteor destroyed

#### Directional Damage
- **Front Meteors**: Threaten front-facing components
- **Side Meteors**: Threaten side-facing components
- **Rear Meteors**: Threaten rear-facing components
- **All Directions**: Universal threat to all components

---

## Gameplay Examples

### Example: Building Phase
1. **Timer Start**: Players begin building simultaneously
2. **Component Selection**: Player picks engine tile
3. **Placement**: Engine placed facing rear of ship
4. **Connection Check**: Engine connectors match ship board
5. **Continue Building**: Add cannons, crew cabins, cargo holds
6. **Timer Ends**: All players stop building immediately

### Example: Combat Encounter
1. **Encounter**: Pirates with strength 4
2. **Player Ship**: 2 cannons (strength 2 each) = 4 total
3. **Dice Roll**: Player rolls 2, Pirates roll 1
4. **Resolution**: Player (4+2=6) vs Pirates (4+1=5)
5. **Victory**: Player wins, gains reward credits
6. **Damage**: No damage taken by player

### Example: Meteor Swarm
1. **Encounter**: Meteors from front, strength 3
2. **Player Defense**: Front cannon (strength 2) + front shield (strength 1)
3. **Resolution**: Total defense (2+1=3) equals meteor strength (3)
4. **Result**: Meteors destroyed, no damage to ship
5. **Alternative**: If defense was only 2, meteor would destroy 1 component

### Example: Planet Landing
1. **Encounter**: Green planet requiring 2 engine strength
2. **Player Ship**: 3 engines with total strength 5
3. **Crew Check**: Player has 2 crew members available
4. **Landing**: Requirements met, player lands successfully
5. **Goods Collection**: Player collects 2 green goods blocks
6. **Flight Day Cost**: Player pays 1 credit for time spent

---

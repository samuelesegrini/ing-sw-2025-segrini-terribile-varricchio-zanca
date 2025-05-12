# Galaxy Trucker Client - Dual UI Implementation

This client application supports both Graphical User Interface (GUI) and Terminal User Interface (TUI) modes. This allows players to choose how they interact with the game.

## Running the Application

The application can be started in several ways:

### Using the Launcher (Recommended)

The Launcher provides an interactive way to select the UI mode:

```bash
java -jar galaxy-trucker-client.jar
```

This will prompt you to select either GUI or TUI mode.

### Command Line Arguments

You can specify the UI mode directly with command line arguments:

```bash
# Start with GUI
java -jar galaxy-trucker-client.jar --gui

# Start with TUI
java -jar galaxy-trucker-client.jar --tui
```

## Architecture

The application uses a layered architecture:

1. **UI Layer**: Abstracted through the `UserInterface` interface with implementations:
   - `JavaFXGUI` - Graphical interface using JavaFX
   - `TerminalUI` - Text-based interface for terminals

2. **Core Logic**: 
   - `ClientApp` - Application core, UI-agnostic
   - `ClientViewModel` - State management and data binding
   - `ClientLoginHandler` - Login and connection logic

3. **Network Layer**:
   - `ClientNetworkManager` - Network communication management
   - Network adapters (Socket, RMI)

## TUI Commands

When using the Terminal UI, the following commands are available:

### Connection
```
<host> <port> <nickname> <technology>
```
Example: `localhost 1234 Player1 Socket`

### Game Browser
```
list                    # List available games
create <name> <maxPlayers>   # Create a new game
join <gameId>           # Join an existing game
refresh                 # Refresh game list
```

### Game Lobby
```
players                 # List players in lobby
start                   # Start game (host only)
leave                   # Leave the lobby
```

## Network Technologies

The application supports multiple network technologies:

- **Socket**: Currently implemented
- **RMI**: Planned for future implementation 
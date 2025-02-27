<!-- Add mermaid plugin in IntelliJ to visualize the class diagram -->
```mermaid
classDiagram
    %% Adding a comment
    class Test {
        -String gameId
        -GameState state
        -List~Player~ players
        -FlightBoard flightBoard
        -AdventureDeck adventureDeck
        -ComponentDeck componentDeck
        -GamePhaseManager phaseManager
        -GameConfiguration config
        +startGame()
        +addPlayer(Player player)
        +removePlayer(Player player)
        +getCurrentPhase()
        +advanceToNextPhase()
        +handlePlayerAction(PlayerAction action)
        +getGameState()
    }

```
### Then can create an entire document

```java
import javafx.application.Application;

class Test implements Application {
    //code
}
```

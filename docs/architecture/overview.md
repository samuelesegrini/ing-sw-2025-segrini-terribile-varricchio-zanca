# Architecture

The system is a distributed client-server application built on MVC, as required by
`requirements.pdf` § 2.2. This document states the target design and the reasoning
behind each decision, so that reviewers can judge the design rather than reverse
engineer it.

## 1. Shape of the system

```
┌──────────────────────────── client JVM (one per player) ───────────────────────────┐
│                                                                                    │
│   View (TUI / GUI)  ──user intent──▶  ClientController  ──Command──▶  Transport    │
│        ▲                                     │                            │        │
│        └──────ClientGameState (read-only projection)◀────Event───────────┘        │
└────────────────────────────────────────────────────────────────────────────────────┘
                                          │  Socket TCP  or  RMI
┌──────────────────────────────────── server JVM ────────────────────────────────────┐
│                                                                                    │
│   Transport ──Command──▶ GameController ──mutates──▶ Model (authoritative)          │
│        ▲                      │                          │                          │
│        └────────Event─────────┴──observes────────────────┘                          │
│                                                                                    │
│   Lobby — the game registry (many games)    SnapshotStore (persistence)             │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Two rules hold everywhere and are worth stating up front, because most of the
package structure follows from them:

**The model lives only on the server.** No domain object is ever serialised to a
client. Clients receive immutable, purpose-built projections. This is what makes
cheating structurally impossible and keeps the wire format independent of internal
refactoring.

**The controller never knows which transport it is talking to.** Socket and RMI are
two implementations of one interface. Requirement S5 — one game, players on
different transports — falls out for free instead of being special-cased.

## 2. Packages

```
it.polimi.ingsw
├── ServerMain / ClientMain          entry points
│
├── common                           shared by both sides
│   ├── game                         the vocabulary: colours, geometry, kinds
│   ├── protocol
│   │   ├── command                  client → server intents
│   │   ├── event                    server → client facts
│   │   └── view                     immutable projections of model state
│   └── transport                    transport-neutral connection abstraction
│       ├── socket                   object streams over TCP
│       └── rmi                      the same messages over a registry
│
├── server
│   ├── model                        the authoritative domain
│   │   ├── ship                     grid, components, validation, attributes
│   │   ├── component                the component hierarchy
│   │   ├── goods                    the cube bank
│   │   ├── board                    what the printed boards say
│   │   ├── building                 the pool, the timer, the shipyard
│   │   ├── flight                   route, board, positions, rewards
│   │   ├── adventure                cards and their resolutions
│   │   └── game                     Game aggregate, phases, players
│   ├── controller                   command handling, turn arbitration
│   ├── lobby                        game creation, joining, nickname registry
│   ├── network                      turning a connection into a session
│   ├── persistence                  snapshot writing and recovery
│   └── data                         JSON loading of cards, tiles, boards
│
└── client
    ├── controller                   translates user intent into commands
    ├── state                        local read-only projection
    ├── network                      dialling a server, dispatching its events
    └── view
        ├── tui
        └── gui
```

`common` holds no game **state** and no **authority**.

That is a narrower rule than the one this document first stated, which was that `common`
holds no logic at all. The narrower rule is the one worth keeping, and it is worth saying
why. Twelve value types — the player and goods colours, the four directions, rotation,
position, connector shape, component kind, hit kind, violation kind, level, ship attribute
— are needed by every message the two sides exchange. Leaving them under `server.model`
would mean either that the client depends on the server model, or that all twelve are
duplicated and mapped. The first makes *"the client never receives a `Ship`"* a convention
instead of something the compiler knows; the second buys nothing but twelve pairs that can
drift apart.

Some of those types carry small predicates: whether two connectors join, whether a shield
stops a hit, which hold takes red. A literal no-logic rule would push each into a static
helper on the server and make the server read worse to satisfy a slogan. What actually
matters is that the client cannot *decide* anything. Those predicates let a view grey out
an illegal placement before the player clicks it, which is good interface design; the
server then revalidates the command as though the client had computed nothing, so a client
that lies about geometry gets exactly the answer an honest one gets.

The test is therefore: **does this class hold game state, or can the server's answer depend
on trusting it?** If either, it belongs on the server.

## 3. Design decisions

### 3.1 Server-authoritative model, projections on the wire

The client never receives a `Ship`. It receives a `ShipView`: an immutable record
carrying exactly what a view needs to draw, including the parts of other players'
ships that requirement G6 makes public.

*Why.* Sending domain objects couples the wire format to internal design, forces
every domain class to be `Serializable`, and leaks information a player should not
have (the face-down pool, other players' reserved tiles). Projections cost one
mapping layer and buy independence.

### 3.2 Commands and events, not remote method calls

Client to server is a closed set of `Command` records. Server to client is a closed
set of `Event` records. RMI is used as a *transport for these messages*, not as a
way to expose model methods remotely.

*Why.* One protocol, documented once ([`docs/protocol/`](../protocol/README.md)),
works identically on both transports. It also makes the whole controller testable
without a network: feed it commands, assert on emitted events.

Both sets are sealed in two layers, and the families match the phases of § 3.3. That
is what turns "each phase decides which commands are legal" from a convention into
something the compiler knows: a phase switches over the family it owns and is told at
compile time when a message is added to it.

*Patterns.* Command; Observer for event dispatch; Adapter for the two transports.

### 3.2.1 Facts, then truth

Events arrive in batches, and every batch ends with one `StateChanged` carrying the
whole picture. The events before it — a card revealed, a seven rolled, a cabin
destroyed — exist so a view can narrate. A client that ignores all of them and reads
only the state is still correct.

*Why.* Nothing is then ever expressed only as a delta, so a client cannot drift out of
step by missing a message. And reconnecting becomes the same operation as joining:
send the state. That removes the replay log, the sequence numbers and the
resynchronisation handshake that AF4 would otherwise need, along with everything that
can go wrong in them.

*What it costs.* A `GameView` on every command instead of a few bytes. During building
that view is small — no route, no cards, no scores — and during the flight the batches
are far apart. It is not a trade worth optimising before there is a measurement saying
so.

### 3.2.2 Nothing on the wire is an `Optional`

A value that may be absent is `null` on the wire, read through an `…IfAny()` accessor.

*Why.* RMI marshals with Java serialization and `java.util.Optional` is deliberately
not serializable. A message carrying one compiles, passes its unit tests, works over a
socket, and fails the first time two people play over RMI — which is the worst possible
place to find out. `ProtocolContractTest` walks every record component of both sealed
hierarchies and fails the build instead.

*Why not fix it in the transport.* An RMI interface that passes JSON strings would let
the protocol keep `Optional`, and would also make RMI a socket with extra steps. Typed
messages on both transports is the design worth having; this is its price, and it is
four fields.

### 3.3 Game phases as a state machine

`Game` delegates to a `GamePhase`: `Lobby → Building → Validation → CrewPlacement →
Flight → Scoring → Finished`. Each phase decides which commands are legal and what
the next phase is.

*Why.* The alternative — a phase enum plus `if` chains in the controller — is where
this kind of project usually accumulates its bugs. A phase object cannot forget to
reject a command that belongs to another phase.

*Pattern.* State.

### 3.4 Adventure cards as resolutions, not as a visitor

Each card type owns an `AdventureResolution`: a small state machine that emits
`PlayerPrompt`s, consumes `PlayerChoice`s, and applies effects to the model. The
controller drives it without knowing which card it is.

```java
interface AdventureResolution {
    /** The decision the game is currently waiting for, or empty when the card is done. */
    Optional<PlayerPrompt> pending();

    /** Applies a player's answer and advances to the next step. */
    void submit(PlayerId player, PlayerChoice choice);
}
```

*Why not a visitor.* A visitor over 13 card types concentrates all card logic in one
class — the previous implementation's `AdventureCardVisitor` reached 1147 lines and
`AdventureCardController` another 935. Card logic belongs with the card. Adding a
card must mean adding a file, not editing a switch.

*Patterns.* State per card; Strategy at the seam; Template Method for the shared
"in route order, ask each player in turn" skeleton that most cards need.

### 3.5 Deep ship module

`Ship` exposes intent, not structure:

```java
ValidationReport validate();
PlacementOutcome place(Component c, Position p, Rotation r);
DamageReport applyHit(Hit hit, ShieldChoice shields);
Attributes attributes(BatteryActivation activation);
int exposedConnectors();
```

The grid, the connector matrix and the connectivity search stay private. Callers
never iterate cells.

*Why.* The previous `Ship` was 1558 lines with two competing validators
(`ShipValidationService` and `UnifiedShipValidationService`) precisely because
callers reached into its internals and each caller grew its own rules. A narrow
interface over a large implementation is the point.

### 3.6 Component hierarchy

`Component` is a sealed interface. Concrete kinds — `StructuralModule`, `Cabin`,
`Cannon`, `Engine`, `Battery`, `CargoHold`, `Shield`, `LifeSupport` — are records
holding immutable tile data plus a mutable, well-scoped state where the rules
demand one (charges, cargo, occupants).

*Why sealed.* Exhaustive pattern matching means a new component kind produces
compile errors at every place that must handle it, instead of a silent default
branch.

### 3.7 Exact firepower

Firepower is stored as an **integer count of halves**. `5½` is `11`. Nothing rounds.

*Why.* `[p.11]` is explicit that 5½ beats 5 and loses to 6. Floating point in a
comparison that decides who takes cannon fire is not acceptable, and rounding is a
rule violation.

### 3.8 Game data loaded, not coded

Cards, tiles, boards and reward tables come from JSON under
`src/main/resources/data/`, loaded through Jackson into immutable records by a
factory per kind.

*Why.* The rules are data. Hard-coding 156 tiles and 40 cards makes them
untestable and unreviewable; as data they can be validated against the artwork by a
test.

*Pattern.* Factory; Builder for the deck composition described in § 6.3 of the rules
spec.

### 3.9 Sessions decoupled from players

The model knows `PlayerId`. A `PlayerSession` maps a nickname to whatever
connection currently serves it, or to nothing while the player is away.

*Why.* This is what makes AF4 (disconnection resilience) a small change rather than
a rewrite: a disconnect clears the connection, the game keeps running and skips
that player's turns, a reconnect re-binds and replays the current state.

### 3.10 Persistence as a snapshot of the model

After each resolved adventure card and at each phase transition, the server writes
a JSON snapshot of the game aggregate. Recovery reconstructs the aggregate and
waits for the original nicknames to reconnect.

*Why a snapshot rather than an event log.* The requirement only asks to resume from
where execution stopped. Snapshots at natural quiescent points are simpler to make
correct than replaying a log through a state machine, and the disk is assumed
reliable (`requirements.pdf` § 2.3).

*Pattern.* Memento.

## 4. Concurrency

- One game is confined to a **single thread** — a serialized command queue per game.
  Commands are enqueued by network threads and applied one at a time.
- The model has **no synchronization of its own**, because it is never touched by
  two threads.
- The building phase is simultaneous, but every action is still a command on the
  same queue, so "two players grab the same tile" resolves deterministically by
  arrival order.
- The **lobby** is the game registry — the only structure shared across games. It is
  guarded not by a lock but by a thread: every command from every table is applied by
  the same worker, one at a time, so there is no interleaving to reason about.
- A game nobody is connected to is **reclaimed**, whatever the disconnection policy
  says. Carrying on is for a table somebody may come back to; a table with every seat
  empty has nobody to carry on for, and holding it open costs a thread, a game object
  and every nickname at it for as long as the server runs.

*Why.* Fine-grained locking inside a domain model is where subtle, unreproducible
multiplayer bugs live. A queue per game gives the model single-threaded semantics
for free and makes the whole thing deterministic under test.

## 5. Testing strategy

| Layer | Approach |
|:--|:--|
| Model | Plain JUnit. No mocks — the model has no collaborators to mock. One test class per rule area, test names stating the rule. |
| Rules conformance | Table-driven tests generated from `docs/specs/game-rules.md` scenarios, one per card type and per edge case listed there. |
| Game data | Tests asserting the JSON matches the manual: tile counts per kind, 8 test-flight cards, deck composition, board geometry. |
| Controller | Feed commands, assert emitted events. No network. |
| Transport | The same suite run twice, once over Socket, once over RMI, plus a mixed-transport game. |
| Persistence | Snapshot, reload, assert structural equality with the original. |
| View | Logic extracted from rendering; renderers tested on projections, not on live games. |

Every test name states the behaviour and the component under test, as
`requirements.pdf` § 3 requires:

```java
@Test
void bigMeteorFromNorth_isDestroyedOnlyByForwardCannonInSameColumn() { … }
```

## 6. What this replaces

The previous implementation is preserved on `main`. Its structural problems, and
how this design answers them:

| Problem | Answer |
|:--|:--|
| `main` does not compile (`ClientApp` imports a missing `newGUI`) | CI gates every push and PR |
| Two competing validators for the same rules | One `ShipValidator`, owned by `Ship` |
| 1882-line TUI view, 1558-line `Ship`, 1147-line card visitor | Card logic per card; narrow module interfaces |
| `newTUI`, `newGUI`, `serverOld.jar` — versions carried in names | Versions live in git tags |
| Model classes shipped to clients | Projections only |

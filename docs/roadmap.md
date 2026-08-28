# Roadmap

Fifteen milestones, ordered so that each one is independently demonstrable and
nothing is built before the thing it depends on. The first twelve carry the
submission; M12 was the first that changed no behaviour at all. Milestones map one-to-one onto
GitHub milestones; every issue belongs to exactly one.

The ordering principle: **the model is finished and fully tested before a single
byte crosses the network**. Rules bugs found through a UI are expensive; rules bugs
found in a unit test are free.

| # | Milestone | Ships | Tag |
|:--|:--|:--|:--|
| M0 | Foundations | Clean build, CI, specifications, verified game data | `v0.1.0` |
| M1 | Ship and building phase | Components, grid, validation, building rules | `v0.2.0` |
| M2 | Flight core | Route, attributes, damage, giving up, scoring | `v0.3.0` |
| M3 | Adventure cards | All 11 card types, level II and test flight | `v0.4.0` |
| M4 | Protocol and networking | Commands, events, Socket, RMI, lobby | `v0.5.0` |
| M5 | TUI | Full playability from the terminal | `v0.6.0` |
| M6 | GUI | Full playability from JavaFX | `v0.7.0` |
| M7 | AF — Test flight | Level I board and rule set | `v0.8.0` |
| M8 | AF — Multiple games | Concurrent games, game selection | `v0.9.0` |
| M9 | AF — Disconnection resilience | Rejoin, turn skipping, solo timeout | `v0.10.0` |
| M10 | AF — Persistence | Snapshot and recovery | `v0.11.0` |
| M11 | Deliverables | UML, protocol doc, Javadoc, jars, peer reviews | `v1.0.0` |
| M12 | Polymorphic dispatch | Dispatch moved back into the hierarchies that own it | `v1.1.0` |
| M13 | Seams and silent failures | A missing transport seam, two unchecked casts, a thread that dies quietly | `v1.2.0` |
| M14 | What the jar actually does | An advanced feature the shipped server could not reach | `v1.3.0` |
| M15 | What the requirements actually say | The derived spec read back against the PDF it came from | `v1.5.0` |

## M0 — Foundations

The build has to be trustworthy before anything is built on it, and the game data
has to be verified before rules are written against it.

- Maven build targeting a clean module layout, JaCoCo coverage, Javadoc, jar
  assembly for both server and client.
- CI on every push and PR.
- `docs/specs/game-rules.md` and `docs/specs/requirements.md` as the normative pair.
- Game data moved to `src/main/resources/data/`, loaded through immutable records,
  and **validated against the artwork by tests** — tile counts per kind, 8 test
  flight cards, deck composition, board geometry, reward tables.
- Four data defects found and fixed (B1 to B4 in the rules spec).

Exit criterion: `./mvnw verify` green, data tests asserting every count in § 1 and
§ 3 of the rules spec.

## M1 — Ship and building phase

- `Component` hierarchy, connectors, rotation.
- `ShipGrid` with the two board geometries.
- `ShipValidator` implementing all seven violation classes of § 5.
- Building: draw, place, weld, return, reserve, peek, timer, finish.
- Attributes: firepower in halves, engine power, crew, exposed connectors.

Exit criterion: a ship can be built, validated and scored entirely through unit
tests, with every rule in § 2-5 covered.

## M2 — Flight core

- Route as a closed loop, empty-space movement, lapping.
- Route order and leader handover.
- Damage: hit resolution, disconnection, splitting, choosing a fragment.
- Crew placement and aliens.
- Giving up, forced and voluntary.
- End-of-flight scoring.

Exit criterion: a full flight can be simulated in a test with a scripted card
sequence.

## M3 — Adventure cards

One issue per card type, each closed by tests derived from § 8 of the rules spec.
Planets, Abandoned Ship, Abandoned Station, Smugglers, Pirates, Slavers, Open
Space, Meteor Swarm, Combat Zone, Stardust and Epidemic — plus goods shortage,
which cuts across several of them.

Sabotage is not in the list: it is a level III card and out of scope with level III
(`requirements.pdf` § 2.1).

Exit criterion: complete level II rules, headless, fully tested. This is the point
at which the project satisfies "regole complete".

## M4 — Protocol and networking

- Command and event catalogues, with the protocol document written **alongside**
  the code, not after.
- Transport interface; Socket implementation; RMI implementation.
- Lobby: nickname uniqueness, player count, game start.
- Mixed-transport game (requirement S5).

Exit criterion: two headless clients, one on Socket and one on RMI, play a complete
game.

## M5 — TUI

Every phase playable: lobby, building with the shared pool, ship inspection for all
players (G6), card resolution prompts, scoring.

## M6 — GUI

Same coverage in JavaFX, with the real artwork.

## M7-M10 — Advanced features

Ordered by how much they constrain the design that precedes them. AF4 and AF3 are
last because § 3.9 and § 3.10 of the architecture were designed to make them
additive.

## M11 — Deliverables

High-level and generated UML, protocol document, Javadoc, peer review documents,
final jars in `deliverables/`, README rewritten against reality.

## M12 — Polymorphic dispatch

The first milestone that ships no behaviour. Four places where dispatch had
escaped the type that should have been doing it, found by reading all 32
`instanceof` and 21 `switch` sites in `server` and `common`.

The bar is deliberately narrow, because this codebase already chose sealed
hierarchies with exhaustive switches and defends the choice in its Javadoc —
*"a ninth kind of component cannot be added without somebody being made to say
what it looks like."* That is sound. An exhaustive switch over a sealed type
buys exactly the safety that avoiding `instanceof` is supposed to buy, so **the
presence of a switch is not a defect**. Three things are:

1. The same hierarchy answered in more than one file. The compiler is satisfied
   and locality is gone anyway: everything about one variant lives everywhere
   except the variant.
2. A `default` arm, a `super` fall-through, or an unchecked cast. The seal is
   reopened and the compiler stops helping — silently, at the one point where
   somebody was relying on it.
3. Dispatch on an enum name or a null check while a polymorphic object is
   already in the room.

- `PlayerPrompt` gains `passiveAnswer`, `describePassing` and `accepts`, which
  deletes `SkippedTurn` and `Game.describe` and closes the
  `default -> applyExtra` fall-through under `EnemyResolution` (#138).
- `Phase.finishFor` replaces a `switch (phase.name())` inside the class that
  holds the phase (#139).
- `ShipComponent.surrenderTo` replaces the one non-exhaustive switch over the
  component hierarchy, where a missed case leaks goods out of the bank (#140).
- `ConnectionState` replaces three nullable fields and the two guard clauses
  they forced on `Lobby.handle` (#141).

Exit criterion: no behaviour changed, no test rewritten to accommodate the
refactor, and every one of the four sites either fails the build or cannot be
written when a variant is added.

## M13 — Seams and silent failures

What M12 left, plus one thing M12 turned up. Three items, and the thread
between them is that in each case something the compiler or the runtime
should have caught was arranged so that it could not.

- **A seam with two adapters and no interface** (#149). `SocketServer` and
  `RmiServer` present the same four members and share most of their
  implementation; `RmiServer`'s Javadoc says it is *"the same shape as
  `SocketServer` on purpose"*, which is a comment doing an interface's job.
  `Doorway`, `Doorman` and `AbstractListeningPost` say it in the type system
  instead — the shape `AbstractChannel` already uses one layer down.
- **Two sealed unions unwrapped by hand** (#150). `Envelope` and `Reaction`
  are each taken apart by an `instanceof` chain ending in an unchecked cast,
  so a new variant compiles and fails at runtime. The `Envelope` one fails on
  a transport thread, inside the module whose whole job is that a dropped
  connection never throws.
- **A keeper that kills a game thread** (#147). A snapshot write that fails
  takes the game's single thread with it, and that game then stops applying
  commands with nobody told. Found while working M12; it predates it.

Exit criterion: no behaviour changed except where #147 deliberately changes
it, both listening posts reachable through one interface, and no unchecked
cast over a sealed type left in the tree.

## M14 — What the jar actually does

Found by driving the built artifacts rather than the test suite: a real
server, a real socket client and a real RMI client, playing a game and being
killed at various points.

Two things came out of it, and they are the same kind of thing. A test can
pass while the product cannot do what the test proves, because the test
reaches a constructor the product never calls.

- **A test that checked the rarest path** (#155). `ScrappingTest` covered
  `Ship.discard` — a mistake put right in the shipyard. The two routes that
  fire on most cards of most flights, enemy fire and a ship coming apart, were
  not covered. Deleting `scrapped` from `Ship.destroy` left the whole suite
  green.
- **Persistence the shipped server cannot reach** (#157). AF3 says the server
  writes game state to disk and resumes after a crash. It does, in
  `PersistenceTest`. Every `Server.start` overload funnels into the one `Lobby`
  constructor that keeps nothing, so `java -jar server.jar` has never written
  a snapshot in its life. Confirmed against the jar: no file appears, and a
  restarted server has no game to give back.

The groundwork is one settings record (#159) — `Server` could not reach a
persisting `Lobby` without either a seventh positional parameter or that.

**The bound, stated on purpose.** Snapshots are written at phase changes and
after each resolved card, not after each command. A server killed mid-card
resumes at the start of that card. That is a coherent game rather than a
corrupt one, and the alternative rewrites the whole command history on every
command; § 3.10 has the reasoning.

Exit criterion: kill the running server jar mid-flight, start it again on the
same ports, and log in with the nickname you had — by hand, not only in a
test.

## M15 — What the requirements actually say

M14 was found by running the project instead of reading the tests. M15 was
found by reading [`docs/rules/requirements.pdf`](rules/requirements.pdf) instead
of reading `docs/specs/requirements.md`.

Most of the PDF is met, and was checked item by item: validation done by the
application rather than by the other players, every player able to see every
ship in every phase through both interfaces, nickname uniqueness enforced
server-side, one game at a time per client, the creating player choosing the
level and the seat count, both transports in one game, both interfaces
selectable at startup, and all four advanced features.

What was not met is smaller and more awkward than a missing feature.

- **The derived spec had drifted from the PDF** (#169), and not harmlessly:
  it states as requirement two things the PDF does not say, and reading it
  rather than the source produced a wrong recommendation that had to be
  withdrawn. A spec that yields wrong answers is worse than no spec, because
  it is trusted.
- **A policy did not do what it documents** (#170). `ENDS_THE_GAME` models the
  baseline rule that a disconnection ends the game *"anche se in fase di
  avvio"* — even while it is still filling —, and honoured it only for games that had already started.
- **Half the test classes omit something the PDF asks for by name** (#171).
  §3 wants each test to state the functionality tested **and the components
  involved**; 44 of 93 did both, 49 did only the first.

**The rule this milestone leaves behind.** `requirements.pdf` is normative.
`docs/specs/requirements.md` is a reading of it, is labelled as one, and does
not put words in its mouth. Where the PDF is silent — and it is silent about
what happens when a table empties completely — the team's answer is recorded
as a decision rather than dressed up as a requirement.

Exit criterion: every claim in the derived spec is either a quotation or
labelled a decision, and the two defects above are closed with tests that
failed first.

## Working agreement

- One issue, one branch, one PR (`CONTRIBUTING.md`).
- No milestone is closed while any issue in it is open.
- Each milestone ends with a `v0.x.0` tag on `develop` and a GitHub release listing
  its issues.
- `develop` is always green.
- `main` keeps the previous submission untouched until the rebuild is deliverable;
  `v1.0.0` is the one release that merges into it. Milestones after it are tagged
  on `develop` and merged to `main` only when a new submission is cut.

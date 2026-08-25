# Roadmap

Eleven milestones, ordered so that each one is independently demonstrable and
nothing is built before the thing it depends on. Milestones map one-to-one onto
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

## Working agreement

- One issue, one branch, one PR (`CONTRIBUTING.md`).
- No milestone is closed while any issue in it is open.
- Each milestone ends with a `v0.x.0` tag on `develop` and a GitHub release listing
  its issues.
- `develop` is always green.
- `main` keeps the previous submission untouched until the rebuild is deliverable;
  `v1.0.0` is the one release that merges into it.

# Roadmap

Sixteen milestones, ordered so that each one is independently demonstrable and
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
| M16 | Where the defects kept landing | Two deepenings the earlier reviews proposed and nobody built, and two smaller debts | `v1.6.0` |

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

## M16 — Where the defects kept landing

Two of the four things in this milestone were proposed before M12 and passed
over. That was the right call at the time and it stopped being right, which is
the interesting part: the argument for a deepening can strengthen while the
code sits still.

- **A game comes into being twice** (#183). `Lobby.start` and
  `Lobby.recoverWhatWasKept` each build a keeper, a controller and two registry
  entries, from different sources, fifty lines apart, with nothing checking
  that they agree. When this was first proposed persistence was unreachable
  from the shipped server, so it was a tidiness argument and was descoped.
  **M14 made it real, and both defects M14 turned up were in this wiring** —
  a shutdown deleting every snapshot, and an unreplayable snapshot read again
  at every startup. Neither had a module to live in, so both were fixed inside
  the two methods that do the same job twice.
- **Where a nickname is has no name** (#184). Four maps, fourteen methods
  keeping them in step by hand, three of those methods added since the
  proposal. The invariants holding them together are comments.
- **A verb and its documentation were two separate things** (#185). This item
  was proposed as a `Screen` per phase, on the grounds that the client
  dispatched a typed line through thirty branches in a chain. It does not, and
  reading it said so: the dispatch is four comparisons handing off to five
  handlers. The `Screen` tier was **dropped rather than deferred**, and what
  shipped is the part that was real — a verb that carries the words meaning it
  alongside its description, and a check that the terminal accepts exactly what
  the help declares, in both directions. It found an undocumented command on
  its first run.
- **A duplication a merged PR says it removed** (#182). It does not; the claim
  is the only reason the item exists.

**The order is deliberate.** #183 first: it is the smaller change and it makes
#184 smaller still, because once the desk stops owning seeds and snapshot
recipes, what is left of it is much closer to the roster and the switch that
#184 wants to separate.

Exit criterion: no behaviour changed anywhere except where a test says
otherwise, and each of the four either removes a place a defect has already
landed or makes a claim in the history true.

## Working agreement

- One issue, one branch, one PR (`CONTRIBUTING.md`).
- No milestone is closed while any issue in it is open.
- Each milestone ends with a `v0.x.0` tag on `develop` and a GitHub release listing
  its issues.
- `develop` is always green.
- `main` keeps the previous submission untouched until the rebuild is deliverable;
  `v1.0.0` is the one release that merges into it. Milestones after it are tagged
  on `develop` and merged to `main` only when a new submission is cut.

## M17 — What a whole game shows

Nothing here was found by reading the code or by running the suite. All four
came out of one sitting: two players, socket and RMI, a level II game played
from the lobby to the final ledger through the built jars, and then the server
stopped and started again.

- **The terminal goes silent after two hundred events** (#195). The client
  keeps the last two hundred events; the screen kept a count of everything it
  had ever printed and handed that count back as an index into the window. They
  agree until the two hundredth event and never again. The board went on
  redrawing — a board is read off the last state, not off the story — so the
  game looked alive while saying nothing: no phase announced itself, no card
  was turned over, no prompt arrived, no ledger. The server had sent each player
  1846 events.
- **A saved game could not be put back** (#201). `DeckComposition` built its
  counts in an `EnumMap` and threw that order away on the last line with
  `Map.copyOf`, whose iteration order Java reshuffles once per JVM on purpose.
  The deal walked that map to fill the piles, so the deck was settled by a coin
  toss taken at process start rather than by the seed. Replaying one snapshot in
  four fresh JVMs dealt three different first cards. Restarting the server put a
  level II game back about half the time and set it aside as broken the rest.
- **The final ledger was misaligned** (#197). `"prettiest"` is nine characters
  in an eight-wide column, and `String.format` pads but never truncates, so
  every heading after it sat one place left of the numbers it named. The
  headings and the rows were two format strings kept in step by hand.
- **The validation help described a command that does not exist** (#198).
  `keep <row> <col>` where the handler reads a piece number, in the one phase
  whose job is putting a broken ship right — and a hint after an unrecognised
  word that named `scrap` and never `keep` at all.

**What the suite could not have caught, and why.** Three of the four are
invisible from inside a single test run. The narration cursor only diverges past
the two-hundredth event and no test goes that far. The deck order is drawn once
and held for the life of the process, so any two decks dealt in one JVM always
agree — only a real restart can see it, and nothing in the suite restarts. The
validation hint needs a ship that actually broke, and every ship the tests build
is whole. `VocabularyTest` compares the words a handler matches against the
words the help declares, both ways, and has nothing to say about the arguments
after them.

Exit criterion: a level II game played end to end through the jars over both
transports, and a server restarted mid-flight that picks the game up again —
both observed, not inferred.

## M18 — Before 2.0.0

M17 played a game through the terminal and found four things. This one asked the
same question of everything that had never been played: the two advanced
features no test drives end to end, and the other interface.

- **Disconnection, driven rather than modelled** (AF4). A player killed mid-game
  and brought back: `GREEN has dropped`, then
  `GREEN is away — stopped building where they were for them`, then
  `waiting for somebody to come back; 120s before the last player standing takes
  it`, then `somebody is back; carrying on` — with the tile they had welded
  still on the board. Nothing wrong with it.
- **Three games at once** (AF2). Six clients, mixed transports, three tables
  opened and joined together. Correct seating, three independent snapshots, and
  no cross-talk: each player saw only their own game's names. Nothing wrong with
  it either.
- **The window could not finish a game** (#205). It never constructed a single
  `PreparationCommand`, so a player with only the GUI reached validation, or
  crew placement, and had nothing to press. Both phases are mandatory between
  the shipyard and the flight. `SceneRouter` routed to a `REPAIRS` and a `CREW`
  screen, both had titles, and `Screens.build` mapped both to the *shipyard's*
  pane — `Draw from the heap` and `Weld it down`, under a heading reading "Crew
  the ship".
- **The ports, and the images** (#208, #207). The one package at nothing was the
  two `main` classes; what is worth reading back there is how the positional
  ports are taken off the command line, which is quiet about being got wrong.
  Three checked-in UML images were a milestone or two behind, and the generator
  was drawing one private method as public.

**The one that matters is #205, and what it was.** It is the same gap the
terminal had. `RepairAndCrewByTypingTest` opens by saying so — *"These two
phases had no commands at all until a test tried to play through them and found
the game stuck in crew placement with nothing that would move it. Every screen
was drawn and every renderer was tested; there was simply no way to say
anything."* Found there by playing through, fixed there, and never looked for in
the other interface. Seven GUI test classes were green throughout: they check
that windows *build* and that pure functions *decide*, and not one pressed a
control and asked whether the game moved.

**No TestFX.** Its bundled Monocle is a JDK 8 artifact and cannot go headless on
this toolchain, so it would have opened real windows on every build.
`ScreensTest` already had the harness worth having — start the toolkit, build on
its thread, walk the node tree — and `GuiGameTest` presses buttons through it
against a real server.

Exit criterion: every advanced feature driven through the jars rather than
argued from a test, and a game played through the window by something that runs
in the suite. Both met; a person has still not watched the GUI play one, which
is the last thing worth doing by hand.

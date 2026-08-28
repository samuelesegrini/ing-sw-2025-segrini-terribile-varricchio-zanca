# Project Requirements — Traceability

Derived from [`docs/rules/requirements.pdf`](../rules/requirements.pdf) (A.Y. 2024/2025,
Prof. Cugola). This document restates every requirement as a checkable statement so
that each one can be traced to an issue, a test and a piece of code.

> **The PDF is normative. This file is a reading of it.**
>
> Where the two disagree the PDF wins, and this file is wrong. That is not a formality: a
> claim here that the PDF does not make once produced a design recommendation that had to
> be withdrawn after somebody read the source (#169). Two rules follow from it, and they
> are why the wording below is fussier than it used to be.
>
> - A sentence that says *the requirements ask for X* must be traceable to words in the PDF,
>   and quotes them where the point is contested.
> - Where the PDF is **silent**, this file says so and records the team's answer as a
>   **decision**. A decision can be revisited; a requirement cannot, and dressing one as the
>   other removes the choice from whoever reads this next.

## Grading table (`requirements.pdf` Table 1)

| Requirements satisfied | Max grade |
|:--|:--:|
| Simplified rules + TUI + RMI *or* Socket | 18 |
| Complete rules + TUI + RMI *or* Socket | 20 |
| Complete rules + TUI + RMI *or* Socket + 1 AF | 22 |
| Complete rules + TUI + GUI + RMI *or* Socket + 1 AF | 24 |
| Complete rules + TUI + GUI + RMI + Socket + 1 AF | 27 |
| Complete rules + TUI + GUI + RMI + Socket + 2 AF | 30 |
| **Complete rules + TUI + GUI + RMI + Socket + 3 AF** | **30 cum laude** |

AF = advanced feature. **Target: all four advanced features**, so that the 30L
threshold survives one of them being judged incomplete.

Beyond the table, the grade is driven by: design quality (interfaces, inheritance,
composition, design patterns, separation of responsibilities), correctness and
stability, code readability (naming, English comments, no duplication, no
overlong methods), documentation quality (Javadoc, protocol document, peer
reviews), test effectiveness and coverage, use of tooling (IntelliJ, Git, Maven),
and demonstrated autonomy and communication.

## G — Game-specific requirements (§ 2.1)

| ID | Requirement | Verified by |
|:--|:--|:--|
| G1 | Complete level II rules, per the Italian manual | `docs/specs/game-rules.md`, model test suite |
| G2 | Trasvolata Intergalattica excluded (pp. 21-24) | Out of scope, no code |
| G3 | Levels I and III excluded as standalone flights | Out of scope, no code |
| G4 | Ship correctness is validated **by the application**, not by other players | § 5 of the rules spec, `ShipValidator` tests |
| G5 | Illegal ships are corrected following the manual, with its penalties | § 5 of the rules spec |
| G6 | Each client can view **every** player's ship, in **all** game phases | TUI and GUI acceptance tests |
| G7 | Nicknames set client-side, uniqueness enforced **by the server** at join time | Server lobby tests |

## S — Server (§ 2.2.1)

| ID | Requirement | Verified by |
|:--|:--|:--|
| S1 | Rules implemented in Java SE | Build |
| S2 | One server instance; multiple concurrent games once AF2 is implemented | `GameRegistryTest` |
| S3 | Players take turns through client instances over the network | Integration tests |
| S4 | Socket TCP/IP **and** RMI supported | Transport test suite run twice, once per transport |
| S5 | A single game must accept players using **different** transports at the same time | Mixed-transport integration test |

## C — Client (§ 2.2.2)

| ID | Requirement | Verified by |
|:--|:--|:--|
| C1 | Java SE, multiple instances on the **same machine** | Manual launch of N clients, no port or state collisions |
| C2 | GUI in Swing or JavaFX | JavaFX |
| C3 | Interface type (TUI/GUI) chosen **at startup** | Launcher tests |
| C4 | Transport (Socket/RMI) chosen **at startup** | Launcher tests |
| C5 | Players know the server IP/URL | CLI arguments |

## L — Lobby and lifecycle (§ 2.2.2)

| ID | Requirement | Verified by |
|:--|:--|:--|
| L1 | Connecting with no game starting creates one; otherwise the player joins the starting game | `LobbyTest` |
| L2 | The creating player chooses the player count (2-4) | `LobbyTest.creatingATable` |
| L3 | The game starts as soon as the expected player count is reached | `LobbyTest.reachingTheExpectedCountStartsTheGame` |
| L4 | A player leaving **or** a dropped connection ends the game, including during setup, and every player is notified | `LobbyTest.theBaselinePolicy`, `LobbyTest.aTableStillFillingEndsWhenSomebodyDrops` |

**L1 and AF2 want different things**, and AF2 wins where they disagree. The baseline
describes a server running one game at a time, where there is nothing to choose between.
AF2 asks for several at once, which means a joining player has to be able to pick. So the
protocol offers `ListGames`, `CreateGame` and `JoinGame`, and L1's behaviour is what a
client does by default over that: list, then join the first open game or create one. The
server does not decide for them, because with more than one game open it would be deciding
wrongly.

**L4 is the baseline that AF4 replaces.** § 2.3 makes the advanced features optional and,
where implemented, they supersede the behaviour they replace — so a project implementing AF4
is not obliged to end games on disconnection at all, and the PDF asks nothing about keeping
the superseded behaviour selectable.

*Decision (not a requirement).* Both are kept, as one `DisconnectionPolicy` passed to the
`Lobby`, so that the baseline stays exercised by tests rather than deleted and asserted about
in prose. `ENDS_THE_GAME` is **not** offered at runtime: `ServerMain` composes
`GAME_CARRIES_ON` and there is no flag to change it. AF4 is strictly the better behaviour and
a switch that makes the server worse serves no requirement. An earlier version of this file
claimed the requirements were *"explicit that both must stay reachable"*. They are not. They
say nothing.

| policy | what a dropped connection does |
|:--|:--|
| `ENDS_THE_GAME` | L4, including a table still filling. Everybody still at it is told the game is over; in a started game they are then hung up on. Tests only. |
| `GAME_CARRIES_ON` | AF4, and what the server runs. A disconnected player may reconnect and carry on; while away their turns are skipped; with one player left the game suspends until somebody returns or a timeout awards them the win. |

**What AF4 actually says, and where it stops.** Quoted verbatim, because this is the sentence
the wording used to overreach — *disconnected players may reconnect and continue; while a
player is away the game carries on skipping their turns; if only one player is left the game
suspends until somebody else reconnects or a timeout awards the win to the only one still
connected*:

> I giocatori disconnessi … possono ricollegarsi e continuare la partita. Mentre un giocatore
> non è collegato, il gioco continua saltando i turni di quel giocatore. **Se rimane attivo un
> solo giocatore**, il gioco viene sospeso fino a che non si ricollega almeno un altro
> giocatore oppure scade un timeout che decreta la vittoria **dell'unico giocatore rimasto
> connesso**.

The exception is defined down to *one* player and no further.

*The PDF is silent on what happens when the last player leaves.* Its own timeout cannot
resolve that case — there is no *unico giocatore rimasto connesso* to award anything to — so
§ 2.2's baseline is what is left standing.

*Decision.* A table with nobody connected is reclaimed and its snapshot deleted: the game is
over. This was questioned and the current behaviour kept, precisely because the argument for
changing it rested on this file's old wording rather than on the PDF.

## AF — Advanced features (§ 2.3)

| ID | Feature | Acceptance |
|:--|:--|:--|
| AF1 | **Test flight** — the first player chooses standard or test flight; the server implements both rule sets with their own boards | Test flight rules per `game-rules.md`, both boards rendered in TUI and GUI |
| AF2 | **Multiple games** — the server runs several games concurrently; joining players may pick an open game or create a new one | Concurrency tests with ≥ 3 simultaneous games |
| AF3 | **Persistence** — the server periodically writes game state to disk and resumes after a crash; players rejoin with the same nicknames. Disk is assumed reliable | Kill the server mid-flight, restart, resume with identical state |
| AF4 | **Disconnection resilience** — disconnected players may rejoin and continue; their turns are skipped while away; with one player left the game suspends until another rejoins or a timeout awards them the win | Disconnect/rejoin tests, single-player timeout test |

## D — Deliverables (§ 1)

| ID | Deliverable | Location |
|:--|:--|:--|
| D1 | High-level UML diagrams showing the overall design | `docs/uml/high-level/` |
| D2 | Detailed UML diagrams, may be tool-generated from source | `docs/uml/detailed/` |
| D3 | Working implementation conforming to rules and specification | `src/main/java` |
| D4 | Client-server communication protocol documentation | `docs/protocol/` |
| D5 | Two peer review documents | `deliverables/peer-review/` |
| D6 | Source code | `src/` |
| D7 | Javadoc generated from source, in English | `./mvnw javadoc:javadoc` |
| D8 | Unit test source | `src/test/java` |
| D9 | Runnable jars | `deliverables/` |

## Cross-cutting constraints

- **English only** in identifiers, comments, Javadoc and technical documentation
  (§ 2.2). The UI language may be English or Italian; this project uses English
  throughout.
- **MVC** is mandatory for the whole system (§ 2.2). Model lives server-side and is
  authoritative; clients hold a read-only projection.
- Test names and comments must state what is tested and which components are
  involved (§ 3).

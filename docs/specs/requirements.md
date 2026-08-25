# Project Requirements — Traceability

Derived from `docs/rules/requirements.pdf` (A.Y. 2024/2025,
Prof. Cugola). This document restates every requirement as a checkable statement so
that each one can be traced to an issue, a test and a piece of code.

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
| S2 | One server instance; multiple concurrent games once AF2 is implemented | `GameRegistry` tests |
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
| L1 | Connecting with no game starting creates one; otherwise the player joins the starting game | Lobby tests |
| L2 | The creating player chooses the player count (2-4) | Lobby tests |
| L3 | The game starts as soon as the expected player count is reached | Lobby tests |
| L4 | A player leaving **or** a dropped connection ends the game, including during setup, and every player is notified | Disconnection tests |

L4 is the *baseline* behaviour. Advanced feature AF4 (§ AF below) replaces it with
reconnection; both behaviours must remain reachable and the active one must be
explicit, not accidental.

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

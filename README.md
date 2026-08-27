# Galaxy Trucker — Software Engineering Project, A.Y. 2024/2025

Java implementation of the board game *Galaxy Trucker* by Cranio Creations, final
project for the Software Engineering course of the Computer Science Engineering
degree at Politecnico di Milano.

*Professor*: Gianpaolo Saverio Cugola — *Group ID*: GC26

## The Team

* [Samuele Segrini](https://github.com/samuelesegrini)
* [Manuela Terribile](https://github.com/manuelater)
* [Alessandra Varricchio](https://github.com/alessandravarricchio)
* [Diego Zanca](https://github.com/diego-zanca)

![logo](src/main/resources/assets/images/logo.png)

## Status

The rebuild is finished. Every milestone from the build to the deliverables is closed,
and `v1.0.0` is the result. The previous implementation is still reachable in the history
and under the tags that precede `v0.2.0`.

[docs/roadmap.md](docs/roadmap.md) records how it was built, milestone by milestone. The
table below reflects what is **implemented and tested**, not what is planned.

The game is playable both ways. A person claims a name, builds a ship tile by tile,
launches, flies the cards and reads the final ledger — in a terminal or in a window, over a
socket or over RMI, at a table with people who chose differently on every count.

### Core requirements

| Functionality | Status |
|:--|:--:|
| Complete rules (level II) | ✅ |
| Socket | ✅ |
| RMI | ✅ |
| TUI | ✅ |
| GUI (JavaFX) | ✅ |

### Advanced features

| Functionality | Status |
|:--|:--:|
| Test flight | ✅ |
| Multiple concurrent games | ✅ |
| Disconnection resilience | ✅ |
| Persistence | ✅ |

All four are reachable by a player, not just by the server. `new 2 test` opens a test flight;
logging back in with the name of a seat somebody dropped out of puts them back in it, mid-card,
with the question they left unanswered still waiting; a game whose players have all gone quiet
answers for them rather than freezing; and a server that stops can be started again on the same
games.

✅ done and tested · 🚧 in progress · ⬜ not started

## Documentation

| Document | What it is |
|:--|:--|
| [docs/specs/game-rules.md](docs/specs/game-rules.md) | Normative game specification. Single source of truth for behaviour, traceable to the manual page by page. |
| [docs/specs/requirements.md](docs/specs/requirements.md) | Every project requirement restated as a checkable statement, with what verifies it. |
| [docs/architecture/overview.md](docs/architecture/overview.md) | Target architecture, design decisions and the reasoning behind them. |
| [docs/roadmap.md](docs/roadmap.md) | Milestones and their exit criteria. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branch model, commit convention, definition of done. |

Original sources: [game rules](docs/rules/galaxy-trucker-rules-it.pdf) ·
[quick reference](docs/rules/galaxy-trucker-summary-it.pdf) ·
[project requirements](docs/rules/requirements.pdf).

## Building

Requires JDK 23 or later. Maven is provided through the wrapper.

```bash
./mvnw clean verify
```

## Running

Server. It listens on a socket and through an RMI registry at the same time; a player uses
whichever they prefer and cannot tell what anybody else chose.

```bash
java -jar target/server.jar
```

Both ports can be given, socket first. A zero asks the operating system for a free one, which
is how the tests run several servers at once without picking numbers and hoping.

```bash
java -jar target/server.jar 4321 4322
```

Client — the interface and the transport are chosen at startup, and nothing after that can
tell which was picked.

```bash
java -jar target/client.jar --tui --socket --host localhost --port 4321
```

Both choices default: `--tui` over `--socket`, on localhost, on the port that matches the
transport. `--gui` opens a window on the same game — the board is the printed one, and where
each square sits on it was measured from the artwork rather than guessed.

In the terminal, type `help` at any point: it lists what is legal in the phase the game is
actually in, which is shorter and more useful than everything the game can do.

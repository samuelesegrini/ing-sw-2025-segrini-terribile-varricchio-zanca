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

The project is being rebuilt from the rules and the requirements on the `develop`
branch. The previous implementation remains on `main` for reference; note that
`main` does not currently compile.

Progress is tracked in [docs/roadmap.md](docs/roadmap.md) and in the repository's
milestones. The table below reflects what is **implemented and tested**, not what is
planned.

The server plays a complete game over both transports at once, and there is a test in which
four players — two on a socket, two on RMI — do exactly that from the first tile to the last
credit. What is missing is somebody to look at it: there is no interface yet, so the only
clients that exist are in the test suite.

### Core requirements

| Functionality | Status |
|:--|:--:|
| Complete rules (level II) | ✅ |
| Socket | ✅ |
| RMI | ✅ |
| TUI | 🚧 |
| GUI (JavaFX) | 🚧 |

### Advanced features

| Functionality | Status |
|:--|:--:|
| Test flight | ✅ |
| Multiple concurrent games | ✅ |
| Disconnection resilience | ✅ |
| Persistence | 🚧 |

The three advanced features marked done are done **on the server**. A player cannot yet choose
a test flight or reconnect to a game, because a player cannot yet do anything: those are
commands nothing sends until there is an interface.

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

Client — interface and transport are chosen at startup. Not yet implemented: there is no
interface to choose between, and until there is, the only clients are in the test suite.

```bash
java -jar target/client.jar
```

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

### Core requirements

| Functionality | Status |
|:--|:--:|
| Complete rules (level II) | 🚧 |
| Socket | 🚧 |
| RMI | 🚧 |
| TUI | 🚧 |
| GUI (JavaFX) | 🚧 |

### Advanced features

| Functionality | Status |
|:--|:--:|
| Test flight | 🚧 |
| Multiple concurrent games | 🚧 |
| Disconnection resilience | 🚧 |
| Persistence | 🚧 |

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

Server:

```bash
java -jar deliverables/server.jar
```

Client — interface and transport are chosen at startup:

```bash
java -jar deliverables/client.jar
```

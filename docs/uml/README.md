# UML

Two sets, for two different jobs.

| Directory | What it is | Kept by hand? |
|:--|:--|:--|
| [`high-level/`](high-level/) | The design as a person would explain it | Yes — the `.puml` files are the source |
| [`detailed/`](detailed/) | Every type in every package | No — generated from the source |

Both are checked in as `.png` so they can be read without tooling, and as `.puml` so they
can be changed without redrawing.

## Reproducing them

Both need [PlantUML](https://plantuml.com). No Graphviz: the diagrams ask for PlantUML's
own layout engine with `!pragma layout smetana`, so a single jar is the whole toolchain.

```bash
curl -sLo /tmp/plantuml.jar https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/1.2024.7/plantuml-1.2024.7.jar
```

### High level

Edit the `.puml` files, then render:

```bash
java -jar /tmp/plantuml.jar -tpng -o . docs/uml/high-level/*.puml
```

### Detailed

Regenerate from the source first, then render. Never edit these by hand — the next
regeneration would throw the edit away.

```bash
python3 tools/uml/generate-detailed.py src/main/java docs/uml/detailed
java -jar /tmp/plantuml.jar -tpng -o . docs/uml/detailed/*.puml
```

The generator ships with the repository rather than being an off-the-shelf tool, because
nothing available offline read modern Java records and sealed interfaces — and in this
codebase those are not incidental detail. A sealed interface is how the protocol says
"these messages and no others", and a diagram that omitted the permitted subtypes would be
omitting the point.

## What is in the high-level set

| File | Shows |
|:--|:--|
| `01-packages` | Which package depends on which — and, more usefully, which do not |
| `02-model` | The aggregate, the phases, and what a ship is made of |
| `03-joining` | Logging in, finding a table, and a game starting |
| `04-building` | Drawing, placing and welding, and the shipyard closing |
| `05-card` | One adventure card, as the sequence of questions it is |
| `06-disconnect` | Dropping out, being answered for, and coming back |

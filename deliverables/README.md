# Deliverables

What is handed in for evaluation.

| Artifact | What it is | Requirement |
|:--|:--|:--|
| `server.jar` | The server. `java -jar server.jar [socketPort] [rmiPort]` | D9 |
| `client.jar` | The client, both interfaces. `java -jar client.jar [--tui\|--gui] [--socket\|--rmi]` | D9 |
| `peer-review/` | The two peer review documents | D5 |
| [`../docs/uml/high-level/`](../docs/uml/high-level/) | High-level UML | D1 |
| [`../docs/uml/detailed/`](../docs/uml/detailed/) | Detailed UML, generated from source | D2 |
| [`../docs/protocol/`](../docs/protocol/) | The communication protocol | D4 |
| [`../src/`](../src/) | Source and tests | D3, D6, D8 |

Javadoc (D7) is generated rather than checked in:

```bash
./mvnw clean javadoc:javadoc
```

`clean` matters. On its own the goal is incremental and skips files whose output is already
up to date, so it can report success having checked almost nothing.

## The jars

Both are self-contained: no classpath, no module path, nothing to install but a JDK 23.

```bash
java -jar server.jar 4321 4322
java -jar client.jar --tui --socket --host localhost --port 4321
```

Rebuilt from the tagged commit with:

```bash
./mvnw clean package
cp target/server.jar target/client.jar deliverables/
```

Verified here by starting each of them and playing a little: the server on both ports at
once, and the client over a socket, over RMI, in the terminal and in a window. They have not
been run on a machine other than the one they were built on.

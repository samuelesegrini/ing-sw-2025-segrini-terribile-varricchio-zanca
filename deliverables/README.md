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

Both are quiet. `--debug` on either — anywhere on the command line, or `GT_DEBUG=1` in the
environment — turns on a trace of what that process is doing. The server traces to its
terminal; the client traces to `galaxy-trucker-client.log`, never to the terminal, because the
terminal is where it draws the ship.

The server keeps its games in `games/`, beside wherever it was started, and prints the
location on startup. Stopping it and starting it again on the same directory brings those
games back; players return by logging in with the nickname they had.

Rebuilt from the tagged commit with:

```bash
./mvnw clean package
cp target/server.jar target/client.jar deliverables/
```

Verified here, on these binaries: the server listening on both ports at once, one client over
a socket and one over RMI in the same game, a player dropped mid-card and answered for, that
player rejoining and being handed the whole picture, and the server killed with `kill -9`
during a flight, restarted, and resuming with the seats, the crew, the route positions and the
card still on the table. The window was not exercised this time round; the terminal was.

Also on these binaries: quiet by default, with nothing on stderr; `--debug` tracing every
message the server sees; and a client run with `--debug` writing nothing at all to its
terminal, writing to `galaxy-trucker-client.log` instead. They have not been run on a
machine other than the one they were built on.

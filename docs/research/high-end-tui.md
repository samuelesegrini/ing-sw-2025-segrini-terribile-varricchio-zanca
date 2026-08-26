# High-end terminal UI research and delivery plan

**Decision:** implement M5 with the JLine dependency already declared by the
project, using a small, custom full-screen frame renderer. Do not add a widget
toolkit for the first playable TUI. The resulting interface should feel like a
compact *ship console*: fast keyboard play, a stable spatial layout, strong
focus/selection feedback, and an information-dense board rather than a sequence
of `println` prompts.

This is research and a proposed implementation plan, not a change to the model,
protocol, or client. It was prepared on 2026-08-25 from the checked-out
`develop` branch and the repository's live GitHub milestones/issues.

## What this project needs

The codebase is a Maven/Java 23 application. `pom.xml` already pins
`org.jline:jline:3.29.0`, alongside JavaFX, and CI runs `./mvnw clean verify` in
headless mode. There is no current client package; `ClientMain` is explicitly a
M4 placeholder. The architecture document nevertheless supplies the correct
seam: a TUI emits user intent to `ClientController` and renders only immutable
`ClientGameState` / protocol projections. The TUI must never deserialize or
inspect server model objects, know the chosen transport, or decide whether a
game move is legal.

The existing JavaFX assets and CSS establish a useful product vocabulary, even
though they are not an implementation to reuse in the terminal: dark space
backgrounds, player-colour identity, purple Level II versus cyan test-flight
accents, a prominent ship grid, a shared component pool, a route strip, card
detail, status panels, and warning/selection highlights. Recreate those
*semantic* cues with cells, glyphs, borders and colour; do not try to stream the
bitmap tile/card artwork over a terminal. In particular, a terminal tile needs a
stable identifier, orientation, connector information, component glyph and
state markers so it remains legible in monochrome and through SSH.

The live M5 milestone is intentionally divided into three issues, which makes a
vertical delivery possible:

| GitHub issue | Required outcome | TUI implication |
|---|---|---|
| [#47 — shell, startup choices, lobby, navigation](https://github.com/samuelesegrini/ing-sw-2025-segrini-terribile-varricchio-zanca/issues/47) | startup selection, join/create, nickname retry, command reference, testable rendering | build the terminal runtime, page shell, focus model and low-size fallback once |
| [#48 — building phase](https://github.com/samuelesegrini/ing-sw-2025-segrini-terribile-varricchio-zanca/issues/48) | shared pool, tile actions, labelled grid, timer/finish state and other-ship inspection | prove the grid renderer, master/detail navigation and mutation-pending feedback |
| [#49 — flight phase](https://github.com/samuelesegrini/ing-sw-2025-segrini-terribile-varricchio-zanca/issues/49) | decision-relevant cards/prompts, legal choices, activations, route, losses and scoring | reuse the shell/grid and add prompt/card/score panels rather than a second UI |

The roadmap makes M4 a hard dependency: its exit criterion is a complete game
between headless clients using mixed transports. M5 should therefore consume a
proven, transport-neutral `ClientGameState` and event feed, not become a way to
debug rules or networking. The repository also says the M5 exit criterion is
full terminal playability: lobby, building, inspection of every ship, card
prompts and scoring.

## Library options, grounded in upstream sources

| Option | Strength for this codebase | Cost / limitation | Verdict |
|---|---|---|---|
| **JLine 3.29 already in `pom.xml`**, custom screen compositor | The upstream API exposes terminal capability/size/signal handling, raw input, attributed text and `Display`; JLine documents incremental display updates and capability-aware fallbacks. It has keyboard/mouse APIs and stream-backed virtual terminals. It adds no new dependency or licensing decision. | It is a terminal foundation, not a board-game layout engine: the project must own layout, focus and frame data. Continuous redraw on some Windows terminals has an upstream flicker report, so no animation loop. | **Choose.** The missing layer is small and product-specific, which is better than fighting a generic dialog toolkit. |
| **Lanterna 3.x**, preferably only its `Screen` layer | Its upstream repository provides a pure-Java terminal abstraction, full in-memory screen buffer, GUI layer, xterm-compatible terminal support and `VirtualTerminal`. It would make a grid prototype quick. | New LGPL-3.0 dependency; its higher GUI layer is window/modal-dialog shaped and the project would still need custom board rendering and state boundaries. Its own guide notes that terminal portability is difficult; it cannot remove capability testing. | A credible fallback if the team values an in-memory terminal test double more than dependency simplicity. If chosen, use `Screen`/`TextGraphics`, **not** `gui2` as the game architecture. |
| **Jexer** | Its first-party documentation describes a 100%-Java, MIT-licensed desktop-style TUI with xterm/Swing backends, a headless backend, widgets and a logical-to-physical display flush. | `TApplication` becomes the application driver. That is a much larger lifecycle/UI model than this client needs, while the board still needs bespoke rendering. | Reject for M5; reconsider only if a future product deliberately wants a desktop-window metaphor. |
| **Text-IO** | Officially supports constrained/typed interactive input, selection lists and terminal SPIs. It could make a very small prompt-only lobby quickly. | Its documented focus is interactive input, not a retained full-screen grid/card/route renderer. Introducing it would fragment input handling while retaining a separate renderer. | Do not use for M5; at most consider it for a throwaway command-line diagnostic, not the player UI. |
| Plain `System.in` plus ANSI strings | No dependency. | No terminal capability detection, raw-mode lifecycle, resize/signal handling, styled display diffing or good test seam. It would recreate the parts JLine already supplies. | Reject. |

The currently declared JLine 3.29.0 is sufficient for M5. Upstream has a newer
3.x release (3.30.16 at the time of research), so create a separate dependency
issue to test that upgrade under the project's Java 23/CI terminal matrix; do not
couple it to the first UI implementation. Treat any move to JLine 4 as another,
separate decision: its documentation describes a JDK-22+-capable FFM provider
that may require `--enable-native-access`, a launch-policy question the existing
JLine 3 line does not add. Also avoid JLine's Console UI module: the upstream
README marks it deprecated.

Primary-source evidence:

- [JLine terminal guide](https://jline.org/docs/terminal/) documents the
  terminal abstraction, terminfo capabilities, size changes, signals and
  system/virtual terminals.
- [JLine API overview](https://jline.org/docs/api/overview/) identifies
  `AttributedString`/`AttributedStyle` and `Display` as the styled-text and
  display APIs; [its troubleshooting guide](https://jline.org/docs/troubleshooting/)
  explicitly recommends batched/diff display updates rather than redrawing each
  line separately.
- [JLine mouse guide](https://jline.org/docs/advanced/mouse-support/) requires a
  capability check and keyboard alternatives; [the upstream repository](https://github.com/jline/jline3)
  documents its BSD license and the deprecation of Console UI.
- [Lanterna's upstream README](https://github.com/mabe02/lanterna) documents its
  three layers, pure-Java implementation, supported terminal family, Maven
  coordinates and LGPL-3.0 license. Its [terminal tutorial](https://github.com/mabe02/lanterna/blob/master/docs/tutorial/Tutorial01.md)
  documents `VirtualTerminal`; its [GUI tutorial](https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/tutorial/Tutorial04.java)
  documents the separate UI-thread constraint.
- [Jexer's first-party site](https://jexer.sourceforge.io/) and its
  [backend API](https://jexer.sourceforge.io/apidocs/api/jexer/backend/Backend.html)
  document its backends and logical-to-physical flush; its
  [application API](https://jexer.sourceforge.io/apidocs/api/jexer/package-summary.html)
  documents the `TApplication`-centred lifecycle.
- [Text-IO's upstream README](https://github.com/beryx/text-io) documents its
  input-reader and terminal-SPI focus.

## Recommended architecture

Keep the terminal-dependent code in `it.polimi.ingsw.client.view.tui` when the
M4 client packages arrive. It should be a leaf of the client graph:

```text
network event -> ClientController -> ClientGameState (immutable projection)
                                      |
                                      v
                         TuiPage + TuiRenderer (pure)
                                      |
                                      v
                              TuiFrame (cells/styles)
                                      |
                                      v
                    JLineTerminalRuntime (I/O, Display, raw mode)
                                      ^
                                      |
keyboard/mouse -> TuiInputMapper -> user intent -> ClientController
```

Suggested responsibilities:

- `TuiPage` is a closed navigation state (`startup`, `lobby`, `building`,
  `inspect`, `flight`, `scoreboard`, `help`, `tooSmall`), not a copy of the
  server game phase. It may contain local focus, scroll and selected-player
  state only.
- `TuiRenderer` is a pure function from an immutable projection, `TuiPage`,
  terminal size and colour capability to a `TuiFrame`. It performs no input,
  network call, timer mutation or terminal write.
- `TuiFrame` is a small value model of rows/cells/styles and cursor position.
  A single `JLineFramePresenter` turns it into `AttributedString` rows and calls
  `Display.update`. This gives the renderer a golden-testable output and keeps
  JLine replaceable.
- `TuiInputMapper` maps `KeyStroke` (and optional mouse events) to *intent*,
  never to raw protocol records. The controller is the sole command creator and
  legal-choice authority.
- `TuiTerminalRuntime` owns terminal creation, raw-mode/alternate-screen setup,
  resize and interrupt handlers, cursor restoration and `close()` in `finally`.
  There must be exactly one writer/render loop. Network callbacks only enqueue a
  latest-state render request; they never write to the terminal.

This is deliberately less abstraction than a generic UI framework. It directly
honours the architecture's existing instruction to extract render logic from live
games and test it on projections, while avoiding a repeat of the documented
1,882-line TUI view.

### Visual system

Use a responsive three-region layout at 100 columns × 32 rows or larger:

```text
 Galaxy Trucker  | phase • timer • connection          [H]elp [Q]uit
 route / turn order / compact player status strip
 ┌ shared pool ┐ ┌ active ship grid / inspected ship ┐ ┌ action + details ┐
 │ numbered     │ │ labelled rows/columns; selection  │ │ legal actions    │
 │ component    │ │ and validation/damage overlays    │ │ card/prompt      │
 │ glyph tiles  │ └──────────────────────────────────┘ │ event log        │
 └──────────────┘                                      └─────────────────┘
 F1/H help • arrows/tab move • Enter confirm • R rotate • Esc back
```

At smaller sizes, retain a single action/detail column and let `Tab` switch
between Pool, Ship and Details; below a documented minimum show a non-destructive
`Resize to at least …` page with a compact textual status. This is preferable to
wrapped cells, which make a coordinate board unreliable.

Visual polish comes from consistency rather than animation:

- Give every component a fixed two- or three-cell glyph, orientation arrow and
  state markers (`*` selected, `!` invalid/damaged, `+` placed, `#` reserved),
  with a legend. Never rely only on colour.
- Use the JavaFX palette semantically: dark space base; a high-contrast focus
  ring; player colour plus nickname/marker; purple Level II or cyan test-flight
  accent. Render 256-colour/true-colour only after a capability choice; render a
  bold/underline/inverse monochrome alternative otherwise.
- Keep the route and ownership status visible while a card prompt is active.
  Modal *state* belongs inside the details panel, so other information remains
  inspectable and a screen reader has a stable order.
- Make every action reversible at the UI level where the server permits it, and
  expose the exact legal choices supplied by the projection. After an intent,
  show `Waiting for server…` and reconcile only from the next event/state;
  never predict a successful placement or activation.
- Mouse tracking may be an optional accelerator only. Its failure or absence
  cannot remove keyboard operation.

## Compatibility, accessibility and performance requirements

Terminals differ; polished behaviour means designed degradation, not assuming a
particular macOS terminal. JLine itself recommends checking capabilities and
handling `WINCH` resize events. At startup record the effective terminal type,
size and colour mode in a debug/help page; offer a `--tui-safe` mode that uses
ASCII, no mouse and no alternate screen for hostile IDE consoles or restricted
SSH environments.

Minimum acceptance matrix for M5: macOS Terminal or iTerm2, a Linux
`xterm-256color` terminal over SSH, and Windows Terminal (if a team machine is
available), each at narrow and wide sizes; then `TERM=dumb`/safe mode. Test
colours disabled as well as enabled. This is an implementation acceptance matrix,
not a claim that all terminal emulators have identical capabilities.

Accessibility requirements should be explicit issue acceptance criteria:

- all game actions, inspection, help and navigation work through documented
  keyboard bindings; focus is visible and announced in a single status line;
- colour, hover and icons are redundant cues; error/pending text carries the
  state in words; no emoji is required for board meaning;
- presentation uses a deterministic top-to-bottom reading order and avoids
  decorative rapid redraws; a `--tui-safe` linear/prompt view is available for
  assistive technology or limited terminals;
- `Ctrl+C`, terminal close and failed initialization restore attributes/cursor
  and do not corrupt the user's shell. JLine's terminal docs specifically call
  out saving/restoring attributes, capability checks and terminal closure.

Render only on a projection or local-navigation change, batch one full frame
through `Display.update`, and coalesce bursts of network events to the newest
state. Do not build a 30/60-FPS loop and do not animate the route/timer: both are
unnecessary for turn-based play and risk flicker/bandwidth. Resize invalidates
the layout and causes one full render; ordinary updates should use display
diffing. The active JLine upstream issue tracker contains a report of Windows
flicker under constant `Display` updates, which reinforces this event-driven
policy rather than making a general performance claim.

## Verification strategy

The project already requires JUnit, headless CI and view logic tested on
projections. Add these tests as M5 work, without a graphical display:

1. **Renderer snapshots.** Given fixed `ClientGameState`/prompt fixtures and
   terminal sizes, assert the plain `TuiFrame` text/cell styles: grid labels,
   pool visibility, route order, legal-choice-only actions, privacy of hidden
   data, score breakdown, narrow fallback and monochrome rendering. Store
   readable text snapshots, not terminal escape sequences.
2. **Layout invariants.** Parameterize width/height and assert no cell is placed
   outside the frame, required focus/action status remains visible, and every
   visual state has a text alternative. Include wide Unicode only if an explicit
   width policy supports it; default board glyphs remain ASCII-width.
3. **Input mapping.** Feed key sequences to `TuiInputMapper`, assert the emitted
   intent, and assert invalid/unknown input is a local help/status response,
   never a network command. Test the same intent routes through both Socket and
   RMI controller integration suites once M4 exists.
4. **Runtime lifecycle.** Use stream-backed/virtual terminal construction for
   startup, resize, `Ctrl+C`, exception and close tests. Assert raw mode and
   cursor/screen cleanup are restored. Lanterna's `VirtualTerminal` is a
   testing advantage only if Lanterna is selected; it is not a reason to add it
   alongside JLine.
5. **Human acceptance.** Run the terminal matrix above with a scripted full game
   through lobby, building, inspection, each prompt family and scoring. Capture
   terminal text/screenshots in the issue or PR, while automated assertions
   remain the release gate.

## What can start now, safely in parallel

Create a fixture-driven visual lab while M2/M3 continues, but keep it off the
controller/protocol path until M4 defines the real client projections. The first
slice should contain only terminal primitives (`TuiFrame`, cells, rectangles,
semantic styles), responsive layout, the JLine presenter/runtime and static
showcase data. It must not import server model packages, invent commands, decide
legal moves or introduce a provisional `ClientGameState` that M4 will later have
to preserve.

The showcase should render four representative states: wide building, 80x24
building, a flight decision with the route still visible, and monochrome safe
mode. These are enough to settle tile glyphs, information hierarchy, focus,
keyboard hints, resizing and visual identity before networking exists. Store
readable frame snapshots in tests so the visual primitives can be promoted into
#47; treat any sample game data as disposable fixtures.

Track this as a visual-foundation subtask of #47 (or a dedicated spike issue
linked to #47), following the repository's one-issue/branch/PR rule. Review the
showcase in real terminals before merging production primitives. Controller
integration, navigation driven by live state, and action dispatch remain M4/M5
work.

## Phased work aligned with milestones

| Milestone / issue | Deliverable and exit evidence | Keeps scope safe by |
|---|---|---|
| **M2/M3, in parallel** | Build only the fixture-driven visual lab above. Also ensure every card resolution can express decision-relevant facts as data for the forthcoming M4 projection/catalogue: prompt title, affected player, legal options, cost/effect preview and result/loss narrative. Review this shape against #49 before freezing it. | advancing visual quality without inventing client state, protocol, or UI-derived rules. |
| **M4, protocol and networking** | Add immutable views/events and a client state reducer that expose M5 needs: phase, self/other ship views, shared-pool visibility, route/order, timer/finished players, `PlayerPrompt`, action availability and scoreboard. Drive them first with two headless clients over Socket/RMI. | making M5 a renderer over a tested, transport-neutral stream. |
| **M5 / #47** | `TuiTerminalRuntime`, `TuiFrame`, renderer shell, startup parser, lobby, connection/error states, focus/help/safe-mode and test harness. Prove it against a scripted `ClientGameState` fixture before a live server. | building the reusable mechanics once and preserving M4's headless exit criterion. |
| **M5 / #48** | Grid/pool/detail renderers, keyboard selection/rotation/placement commands, validation and pending/rejected feedback, player inspection, timer/status, responsive/narrow layouts and snapshots. | proving the hardest spatial UI before proliferating flight screens. |
| **M5 / #49** | Route strip, card/prompt detail, legal-choice selection, battery/shield/cannon action panels, per-player result log and final scoring. Reuse the same focus and action panel. | keeping every card prompt declarative and every action controller-mediated. |
| **M6–M10** | Keep the TUI's renderer/state adapters shared and additive: Level I geometry/theme (M7), game list in lobby (M8), reconnect banner/state refresh (M9), and recovered-game status (M10). Add snapshot fixtures for each. | avoiding a second TUI architecture for advanced features. |
| **M11** | Javadoc for public TUI seams, terminal matrix evidence, updated README launch/help documentation and final full-play acceptance. | treating usability/compatibility as deliverable evidence, not an unverified claim. |

Before opening the first #47 PR, make the acceptance criteria above explicit in
the GitHub issue and keep one issue/branch/PR as `CONTRIBUTING.md` requires. The
GitHub REST milestone and issue APIs are the authoritative live source used for
the issue state above; GitHub documents both the milestone and issue endpoints
([milestones](https://docs.github.com/en/rest/issues/milestones),
[issues](https://docs.github.com/en/rest/issues)).

## Recommendation in one sentence

Ship a keyboard-first, capability-aware JLine renderer over the M4 immutable
client projection; make it visually rich through a stable board composition and
semantic styling, not a generic widget toolkit or terminal animation.

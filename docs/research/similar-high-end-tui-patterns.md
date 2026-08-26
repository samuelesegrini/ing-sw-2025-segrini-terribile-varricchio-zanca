# High-end TUI references for the command-deck screens

**Design conclusion:** retain the approved C-style event sequence, and extend it
into the navigation spine of the whole flight UI. The strongest precedent is
not one product copied wholesale: use lazygit's stable master/detail shell,
K9s's explicit focus and status vocabulary, btop's compact instrumentation,
Posting's contextual keyboard help, and the map-first feedback of Brogue CE and
Cogmind.

The practical result should be a stable command deck with one dominant center
stage. The stage changes with the question the player must answer, while the
phase/route strip, fleet summary, context rail, and keyboard footer remain in
known places. During flight, the event spine makes cause and progress explicit:

```text
[x] REVEAL  ━━  [x] VECTOR  ━━  [>] CHOOSE  ━━  [ ] APPLY  ━━  [ ] REPORT
```

This is a research/design note, not an implementation plan. Sources are limited
to project-owned repositories, documentation, manuals, and developer sites.

## What each reference contributes

| Reference | Evidence from the project | Pattern worth borrowing | Limit of the comparison |
|---|---|---|---|
| **lazygit** | Its official keybinding specification distinguishes side-panel and main-panel interaction: `0` focuses the main view, `Esc` returns to the side panel, list views have consistent navigation/search, and the UI can cycle normal/half/fullscreen modes. Destructive or multi-form operations open an options/confirmation surface rather than being assigned directly to every key. [Official keybindings](https://github.com/jesseduffield/lazygit/blob/master/docs/keybindings/Keybindings_en.md) | Stable navigator plus promoted detail; identical keys mean the same thing within a class of panels; let the selected context enlarge without changing the information model. | It is a workbench, not a game. Its many panels and commands would be too much if copied literally. |
| **K9s** | K9s exposes command/filter modes, contextual mnemonics, `Esc` to leave a mode, and explicit confirmation for deletion. Its skin model separately styles ordinary/focused borders, breadcrumbs, cursor, counters, filters, and semantic states such as new/modified/error/completed. Icons can be disabled. [Commands](https://k9scli.io/topics/commands/) · [Skins](https://k9scli.io/topics/skins/) · [Hotkeys](https://k9scli.io/topics/hotkeys/) | Focus must be visible at the frame, title, and cursor levels; completed/current/future steps need separate styles; risky actions use select-then-confirm. | A free-form `:` command language is unnecessary for a bounded board game. |
| **btop** | btop combines stable boxes with a selected-item detail view, meters/graphs, layout presets, and a game-inspired menu. It explicitly degrades from truecolor to 256/16 colors and from Braille to block/TTY glyphs; its config also enables synchronized terminal output to reduce flicker. [Official README: features and terminal requirements](https://github.com/aristocratos/btop#features) · [Official configuration reference](https://github.com/aristocratos/btop#configurability) | Use tiny instruments such as `BAT ▰▰▱ 2/3`, not prose, for continuously relevant resources; define glyph and color fallbacks from day one; use one render transaction per state change. | Continuous telemetry and decorative gradients do not fit a turn-based game; do not add a refresh loop merely for motion. |
| **Posting** | Posting's jump mode overlays keys on focusable regions, contextual `F1` help explains only the focused widget, the footer reflects remapped keys, and focus can move automatically when a response arrives. Its themes define semantic `accent`, `error`, `success`, and `warning` tokens; horizontal/vertical and compact/standard layouts are explicit settings. [Navigation](https://posting.sh/guide/navigation/) · [Keymaps](https://posting.sh/guide/keymap/) · [Help system](https://posting.sh/guide/help_system/) · [Themes](https://posting.sh/guide/themes/) · [Configuration](https://posting.sh/guide/configuration/) | Show only currently valid commands in the footer; provide one-key contextual help; deliberately move focus when the game begins waiting for this player; separate semantic colors from literal palette values. | Posting is form-heavy. Galaxy Trucker should not look like a stack of input fields or padded GUI cards. |
| **Brogue CE** | First-party release notes show a map/sidebar/log working together: status changes are repeated in both combat messages and the sidebar, inspection adds details when space permits, important failures get more conspicuous message color, and the message archive remains navigable. The game also interrupts automation on alerts and improves automatic targeting without leaking hidden information. Its curses build supports 24-bit color, but it also has graphical modes. [Brogue CE repository](https://github.com/tmewett/BrogueCE) · [Official releases](https://github.com/tmewett/BrogueCE/releases) | Put consequences on the object they affect and repeat them in a short ledger; interrupt passive/waiting states when player input is required; never expose hidden card or ship information through previews. | Useful as game-UX evidence, but its release notes do not document a reusable terminal layout architecture. Treat it as a feedback reference, not a framework model. |
| **Cogmind** | Cogmind's developer identifies a large map and a persistent parts list as essential regions, and later uses semi-modal layouts to preserve the map while temporarily promoting secondary information. The manual says combat meaning is moved from the log onto the map; the UI uses embedded keyboard labels and redundant color, symbol, text, sound, and animation cues. Its compact layout keeps temporary logs recallable instead of permanently covering action. [UI requirements](https://www.gridsagegames.com/blog/2024/01/full-ui-upscaling-part-1-history-and-theory/) · [Compact/modal layout](https://www.gridsagegames.com/blog/2024/02/full-ui-upscaling-part-5-completion-and-demos/) · [ASCII UX and redundant feedback](https://www.gridsagegames.com/blog/2015/04/cogmind-roguelike/) · [Official manual](https://www.gridsagegames.com/cogmind/manual.txt) | This is the closest visual precedent: preserve the player's ship/fleet, promote the current tactical object, place damage and trajectories directly on the grid, and retain a terse causal log for review. | Cogmind renders an emulated terminal and historically targets very large grids; it validates design language, not portable ANSI/JLine behavior. |

## Patterns to adopt

### 1. One dominant surface, not equally loud panels

Cogmind's developer explicitly argues that an interface needs one primary area
of attention, while lazygit makes the main view the detail of a stable
navigator. Apply that rule rigorously:

- **BUILD:** the player's blueprint ship owns the stage; pool and validation are
  supporting rails.
- **START POSITIONS:** the flight board owns the stage; ship order and compact
  fleets support it.
- **ADVENTURE:** the revealed card/rule plaque owns the stage until its target is
  known.
- **PLAYER CHOICE:** the affected ship or flight-board location owns the stage;
  the choice is attached to it rather than floating in a detached dialog.
- **RESOLUTION:** the affected object remains on stage while overlays and a
  compact ledger explain what changed.

This preserves the existing rule that all four ships remain visible. They are
not all equally prominent: the local ship is a blueprint when it is the subject,
and all four are compact, complete grid maps in the fleet deck otherwise.

### 2. Make the C sequence causal and stateful

The sequence should work like K9s breadcrumbs/status styles and Brogue/Cogmind's
event feedback, not like a wizard that invents progress locally.

```text
✓ REVEAL     ✓ DIRECTION     ● COORDINATE     ○ DECIDE     ○ APPLY
SMALL METEOR     EAST → WEST      ROW 8               waiting          pending
```

- Completed steps retain their result, not only a checkmark.
- The current step uses a bright frame plus a textual marker such as `CURRENT`.
- Future steps are dim but still name what remains.
- The server-authoritative event/state selects the step. A sent command may show
  `SENT · WAITING FOR SERVER`, but it must not advance to `APPLY` optimistically.
- Resolution continues the same sequence instead of looking like an unrelated
  screen. It remains reviewable until the next card begins.

The same language can lightly unify BUILD (`DRAW → PLACE → ORIENT → CHECK →
FINISH`) and START POSITIONS (`FINISHED → VALIDATED → ORDERED → PLACED →
READY`) without pretending that every scene is a linear wizard.

### 3. Put effects on the board, then explain them once

Cogmind deliberately moves significant combat information onto the map, while
Brogue repeats consequential state in its sidebar/log. Player Choice and
Resolution should therefore share a visual grammar:

```text
      INCOMING E→W / ROW 8
             ──────◆──────▶
  8       [ENG]─[CAN]─[CAB]─[XXX]─[HLD]
                              ^ IMPACT

  1  FIRE DOUBLE CANNON   BAT ▰▰▰ → ▰▰▱   ship intact
  2  ACCEPT IMPACT        [8,4] component → destroyed
```

For resolution, replace the predictions with an ordered ledger:

```text
  01  CARD      Small Meteor: E→W
  02  TARGET    row 8 / component [8,4]
  03  DEFENCE   double cannon fired / battery -1
  04  RESULT    meteor destroyed / ship unchanged
```

The ledger is evidence, not the spectacle. The trajectory, impacted cell,
resource meter, route displacement, lost crew/goods, or detached component must
change on the relevant center-stage object. Use a brief event highlight only
when a new authoritative state arrives; retain the final markers until the
player can read them.

### 4. Use terminal-native focus and actions

Borrow lazygit's contextual keys, K9s's focus styling, and Posting's focused
help/footer:

- `Tab` / `Shift-Tab` moves among the few currently relevant regions.
- Arrows, with optional `hjkl`, move inside a grid/list without changing panels.
- Number keys select visible choices; selection does not send the command.
- `Enter` confirms the selected legal action; `Esc` backs out where the game
  permits it.
- `?` opens help for the focused region, not a wall of every game command.
- The footer lists only active bindings, e.g. `1-2 choose · Enter confirm ·
  Tab inspect fleet · ? help`.

Focus uses three redundant cues: a brighter border, `[FOCUS]` in the panel
title, and `>`/inverse styling on the selected item. Color alone is insufficient.
When a card starts waiting for this player, focus moves to its choice region and
the header displays `YOUR RESPONSE REQUIRED`; spectators keep their current
inspection focus and see `WAITING FOR <player>`.

### 5. Define a small semantic visual vocabulary

K9s and Posting both separate semantic state from literal colors; btop proves
that dense instruments work when their symbols degrade deliberately. Use a
small token set:

| Meaning | Rich terminal | Safe/ASCII fallback |
|---|---|---|
| current/focus | gold border, `●`, inverse row | `[FOCUS]`, `>`, inverse/bold |
| complete/safe | green, `✓` | `[x]` |
| pending/waiting | cyan, `○` | `[...]` |
| warning/choice required | amber, `!` | `WARNING:` |
| damage/failure | red, `×`/`XXX` | `DESTROYED` |
| capacity | `BAT ▰▰▱ 2/3` | `BAT ##- 2/3` |
| direction | `E→W` | `E->W` |
| structure | box drawing | `+---+`, `|` |

Player colors, Level II purple, and Test Flight cyan may identify context, but
nickname, phase text, markers, and coordinates remain present in monochrome.
Reserve bright red/amber for conditions demanding attention; do not use the
whole palette at equal intensity.

### 6. Design responsive density as scene composition

btop has layout presets and explicit TTY glyph fallbacks; Posting has compact
and alternate-axis layouts; Cogmind hides secondary windows only when necessary
and leaves a status-bearing affordance in their place. The command deck should
do the same:

- **Wide:** center stage plus both rails, route strip, and a four-ship fleet
  deck with complete compact 5×7 maps.
- **Medium:** keep center stage and all four maps; collapse prose/details into a
  tabbed context rail and shorten labels.
- **Too small for complete grids:** show a clear resize requirement and a linear
  safe view. Do not crop or wrap ship coordinates, because that changes board
  meaning.

Do not derive a terminal minimum from the browser mockup's pixel size. Measure
the real cell renderer with fixtures, then publish the minimum rows/columns.
Wide characters, Braille, truecolor, rounded borders, mouse input, and terminal
synchronized output are optional enhancements. Board meaning must survive a
UTF-8/ASCII-safe glyph set, 16/256 colors, SSH, and terminals that do not support
synchronized updates. Render on state/focus/resize changes; do not run a btop-like
continuous animation loop.

## Scene contract

| Scene | Center stage | Persistent context | Primary interaction | Completion feedback |
|---|---|---|---|---|
| **BUILD** | Local ship in blueprint resolution, focused cell and connector edges | Phase/timer, component pool, validation summary, all four ships | Grid navigation, select/rotate/place, finish | Placement appears only after authoritative update; validation marks sit on cells and in a terse list |
| **START POSITIONS** | Complete flight board with ordered tokens and starting gaps | All four ships, player readiness/status | Inspect route/player; leader proceeds when allowed | Sequence retains `ORDERED` and `PLACED` results, not only a toast |
| **ADVENTURE** | Card/rule plaque, then promoted target object as soon as targeting is known | Route and complete fleet deck | Reveal/read/inspect; no fake choice before one exists | Direction/coordinate/affected players populate the event spine |
| **PLAYER CHOICE** | Affected blueprint or route location with prediction overlay | Event spine, compact fleet deck, resource meters | Number selects; Enter confirms; Tab inspects without losing selection | Selected consequence is written in words and symbols; header shows pending after send |
| **RESOLUTION** | Same object as the choice, now with authoritative deltas | Completed sequence and causal ledger; all four ships remain available | Review/inspect; leader reveals next card when enabled | Before→after values, cell/route overlays, and ordered cause→effect lines persist until next event |

## Recommended synthesis

The approved **C** experiment is the right base because it is linear,
keyboard-native, and easy to render without GUI metaphors. Strengthen it with:

1. **Cogmind/Brogue:** consequences drawn directly on the ship or flight board,
   with a small recallable ledger.
2. **K9s:** semantic step/focus/status styles and select-then-confirm behavior.
3. **Posting:** automatic focus when a response is required, contextual `?`
   help, and a footer generated from active actions.
4. **lazygit:** a stable master/detail shell whose main stage can expand without
   changing the navigation model.
5. **btop:** compact meters and polished glyphs with capability-aware ASCII,
   color, and rendering fallbacks.

Avoid the obvious excesses of the references: no always-moving telemetry, no
large command language, no dense wall of globally active hotkeys, no color-only
meaning, and no assumption of Cogmind's unusually large emulated-terminal grid.
The premium quality should come from legibility, continuity, precise feedback,
and graceful degradation rather than ornament or animation.

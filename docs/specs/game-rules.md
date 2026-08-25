# Galaxy Trucker — Normative Game Specification

This is the **single source of truth** for game behaviour in this project. Every
rule below is traceable to the official manual or to the printed boards, and every
rule is phrased so that it can be turned directly into a test.

Where the code and this document disagree, this document wins. Where this document
and the manual disagree, the manual wins and this document is a bug.

## Sources

| Source | Use |
|:--|:--|
| `docs/rules/galaxy-trucker-rules-it.pdf` | Normative ruleset. The Italian edition prevails over the English one (`requirements.pdf` § 2.1). |
| `docs/rules/galaxy-trucker-summary-it.pdf` | Tie-breaker for ambiguities (`requirements.pdf` § 2.1). |
| `src/main/resources/assets/cardboard/*` | Normative for board geometry, rewards and price lists. Values below were read off the artwork. |
| `src/main/resources/assets/cards/*`, `assets/tiles/*` | Normative for card and component data. |
| `docs/rules/requirements.pdf` | Project requirements. |

Citations are written as `[p.N]` for the manual and `[QR]` for the quick reference.

## Scope

In scope:

- **Level II single flight** — this is what `requirements.pdf` calls *regole complete*.
- **Test flight** (*volo di prova*, `[p.1-15]`) — implemented as the advanced feature
  *Volo di prova*.

Out of scope, by explicit requirement:

- Level I and level III standalone flights.
- The *Trasvolata Intergalattica* campaign (`[p.21-24]`).
- Trucker title tiles and the *Strade Accidentate* mini-expansion.

Player count is 2 to 4.

---

## 1. Levels and boards

Two level configurations exist. They differ in board geometry, rewards, deck
composition and which building rules apply.

| | Test flight | Level II |
|:--|:--|:--|
| Flight board | `flight-board_lvl-1.png` | `flight-board_lvl-2.png` |
| Route length | 18 spaces | 24 spaces |
| Start space offsets (1st…4th to finish building) | 4, 2, 1, 0 | 6, 3, 1, 0 |
| Ship board | `ship-grid_lvl-1.jpg`, 18 usable cells | `ship-grid_lvl-2.jpg`, 27 usable cells |
| Finish-order reward (1st…4th) | 4, 3, 2, 1 | 8, 6, 4, 2 |
| Prettiest ship reward | 2 | 4 |
| Penalty per component lost | 1 credit | 1 credit |
| Goods price (red/yellow/green/blue) | 4 / 3 / 2 / 1 | 4 / 3 / 2 / 1 |
| Adventure deck | the 8 cards marked **L** | 4 piles × (2 level-II + 1 level-I) = 12 cards |
| Hourglass | none `[p.8]` | 3 spaces, 2 flips `[p.17]` |
| Component reservation | no | yes, up to 2 `[p.17]` |
| Peeking at card piles | no | yes, the 3 lower piles `[p.16]` |
| Aliens and life support | no | yes `[p.18]` |
| Penalty for an illegal ship | none `[p.9]` | components discarded; 1 credit if found in flight `[p.17]` |

The route is a **closed loop**. Positions are counted as steps along the loop; the
route length is the number of spaces in the loop.

### 1.1 Route order

*Route order* is the order of the rocket markers along the route, leader first. The
leader is the player whose marker is furthest ahead. Route order can change at any
time during the flight; whenever it does, the new leader takes over revealing
cards `[p.10]`.

Whenever players act "in route order" they act leader-first. "Reverse route order"
means last-placed player first.

---

## 2. Ship geometry

### 2.1 Grid and coordinates

Both ship boards are a 5 × 7 grid. Internally rows are indexed `0…4` top to bottom
and columns `0…6` left to right. The boards print **column labels 4…10** and **row
labels 5…9**, so:

```
printedColumn = columnIndex + 4
printedRow    = rowIndex + 5
```

The printed labels are the ones the dice address (§ 7.4), so the mapping must be
explicit in the model and not left implicit in view code.

Cells outside the ship outline are *forbidden*: nothing may ever occupy them. The
forbidden sets, read off the artwork, are:

**Test flight** — usable cells are `(0,3)`, `(1,2..4)`, `(2,1..5)`, `(3,1..5)`,
`(4,1)`, `(4,2)`, `(4,4)`, `(4,5)`. 18 cells.

**Level II** — usable cells are `(0,2)`, `(0,4)`, `(1,1..5)`, `(2,0..6)`,
`(3,0..6)`, `(4,0..2)`, `(4,4..6)`. 27 cells.

The **starting cabin** occupies `(2,3)` on both boards and is placed there during
setup. It is a normal cabin in every respect except that it can never host an alien
`[QR]`.

The reservation area is printed at the top right of the board, outside the grid. It
holds at most 2 components (§ 4.4).

### 2.2 Directions

`NORTH` is away from the player and is the ship's **bow** (*"in avanti"*, the
forward-facing side). `SOUTH` is the stern, toward the player. Each component tile
has four sides, one per direction, and a rotation of 0°, 90°, 180° or 270°.

### 2.3 Connectors

Each side carries exactly one of `[p.5]`:

| Connector | Joins to |
|:--|:--|
| `SINGLE` (one pipe) | `SINGLE`, `UNIVERSAL` |
| `DOUBLE` (two pipes) | `DOUBLE`, `UNIVERSAL` |
| `UNIVERSAL` (three pipes) | `SINGLE`, `DOUBLE`, `UNIVERSAL` |
| `PLAIN` (smooth side, not a connector) | nothing |

Two `PLAIN` sides **may** sit next to each other: that is not a connection and not a
violation, provided the new tile is legally connected on at least one other side
`[p.5]`.

A connector is **exposed** when the neighbouring cell in that direction is empty or
outside the grid. Exposed connectors are legal, but they cost flight days on
Stardust (§ 8.8), invite small-meteor damage (§ 8.6) and lose the prettiest-ship
reward (§ 10). A side counts **once** regardless of whether it carries one, two or
three pipes `[p.8]`. `PLAIN` sides are never exposed connectors.

---

## 3. Components

152 component tiles plus 4 starting cabins `[p.3]`. Counts by kind:

| Kind | Count | Behaviour |
|:--|--:|:--|
| Structural module | 8 | Nothing but connectors `[p.7]` |
| Single cannon | 25 | Firepower, big-meteor defence |
| Double cannon | 11 | As above, requires 1 battery per use |
| Single engine | 21 | Engine power |
| Double engine | 9 | As above, requires 1 battery per use |
| Cabin | 17 | Holds 2 humans, or 1 alien if life-supported |
| Starting cabin | 4 (1 per colour) | Cabin that can never hold an alien |
| Cargo hold, 2 slots | 9 | Any goods except red |
| Cargo hold, 3 slots | 6 | Any goods except red |
| Special cargo hold, 1 slot | 6 | Any goods including red |
| Special cargo hold, 2 slots | 3 | Any goods including red |
| Battery, 2 charges | 11 | Powers doubles and shields |
| Battery, 3 charges | 6 | As above |
| Shield generator | 8 | Protects two adjacent sides, 1 battery per activation |
| Purple life support | 6 | Enables a purple alien in an adjacent cabin |
| Brown life support | 6 | Enables a brown alien in an adjacent cabin |

### 3.1 Placement constraints beyond connectors

- **Engine.** Its exhaust must point `SOUTH`. The cell immediately south of it must
  stay empty for the whole flight `[p.6]`.
- **Cannon.** May point in any direction. The cell immediately in front of its
  muzzle must stay empty for the whole flight `[p.6]`.
- **Shield.** Covers two *adjacent* sides determined by its rotation, e.g. north+east.
  Its position on the ship is irrelevant; only its orientation matters `[p.7]`.
- **Life support.** Has no effect unless it is directly connected to a cabin `[p.18]`.

Both the engine and the cannon constraint are permanent, not just placement-time:
if damage or removal later puts a component into a forbidden cell, the ship is
illegal (§ 5).

### 3.2 Interconnection

Two components are *interconnected* when they are adjacent **and** the two facing
sides form a legal connection. Adjacency alone is not enough. This distinction
matters for aliens (§ 6.2) and for Epidemic (§ 8.9).

---

## 4. Building phase

All players build simultaneously from one shared face-down pool `[p.4]`.

### 4.1 Drawing

A player may hold **at most one** component at a time. A component is drawn either

- face down from the pool — its identity is revealed only to the drawing player, or
- face up from the discard area — every player can already see it.

A component that is returned goes **face up** and stays visible to everyone. A face-up
component may not be turned back over `[p.4]`.

### 4.2 Placing

A drawn component may be placed on any empty, non-forbidden cell that is adjacent
to at least one occupied cell, and all four of its sides must form legal
connections with whatever they touch. The ship must be a single connected piece at
all times `[p.5]`.

While a component is still the most recently placed one, it may be moved or rotated
freely. It becomes **welded** — permanently fixed — as soon as the player draws
another component or looks at a card pile `[p.5, p.16]`.

The server enforces cell-level legality at placement time (in the grid, adjacent to
an existing tile). Connector legality, engine/cannon clearance and connectivity are
enforced at the end of building (§ 5), matching the physical game's *controllo
visuale*.

### 4.3 Peeking at card piles — level II only

After welding at least one component, a player may look at any of the **3 lower
piles**. The 4th pile, at the top of the board, is never visible before the flight
`[p.16]`.

- One pile at a time.
- No components may be placed while looking.
- Looking welds the last placed component.
- Peeking may be repeated any number of times until the player finishes building.

### 4.4 Reserving components — level II only

A player may set aside up to **2** components in the reservation area `[p.17]`.

- A reserved component may be attached to the ship at any later point in building.
- A reserved component may **never** be returned to the pool.
- Reserved components still on the board when building ends count as **lost
  components** and cost 1 credit each at the end of the flight.

### 4.5 The hourglass — level II only

The level II board has **3 hourglass spaces**, so the timer runs three times
`[p.17]`, verified on `flight-board_lvl-2.png` (spaces marked `II`, unmarked, `S`).

1. The timer starts on the first space when building begins.
2. When it runs out, **any** player may flip it onto the second space.
3. When that runs out, only a player **who has already finished building** may flip
   it onto the last space.
4. When the last one runs out, building ends immediately for everybody. Players
   still building must take a start space at once.

If nobody flips the timer, building continues until someone does or until all
players have finished `[p.17]`. The sand running out is therefore **permission to
flip, not an event**: a player still building has every reason to leave the glass
alone, and the rules let them.

The test flight has no timer `[p.8]`.

### 4.6 Finishing

A player finishes by claiming a free **start space** on the route. The two levels
differ, and the difference is easy to miss because each is stated only once:

**Test flight** — spaces are assigned in finishing order. "The first player to finish
occupies space 1, the second occupies space 2, and so on" `[p.8]`. There is no choice.

**Level II** — the player **chooses** any free start space, except those numbered
higher than the number of players in the game `[p.17]`. With two players only spaces
1 and 2 are on offer, whoever finishes first. Most players will want to start as far
forward as possible, but the choice is theirs.

In both levels, a player forced to stop because the last hourglass period ran out
takes the best free space rather than choosing — "the quickest will take the best
free space" `[p.17]`.

Once finished, a player may no longer attach components or look at card piles `[QR]`.

---

## 5. Ship validation

After building ends, every ship is checked `[p.8, p.17]`. A ship is **illegal** if any
of the following holds:

1. Two adjacent sides carry incompatible connectors (`SINGLE`↔`DOUBLE`).
2. A connector is adjacent to a `PLAIN` side of a neighbouring tile.
3. An engine does not point `SOUTH`.
4. A component sits directly behind an engine's exhaust.
5. A component sits directly in front of a cannon's muzzle.
6. A component lies outside the usable area.
7. The ship is not a single connected piece.

Rule 6 is enforced **at placement**, not at validation: the server refuses a cell
outside the outline the moment it is chosen (§ D4), so no such component can exist
by the time a ship is checked. The other six are what the validator reports.

Rules 1 and 2 are two halves of one question — do these touching sides weld? Two
`PLAIN` sides touching answer it with "there is no joint here", which is legal. Any
other pair that fails to weld is a violation, reported as rule 1 when both sides
carry pipes and rule 2 when one of them is smooth.

The owning player removes components of their own choosing until the ship is legal.
Removed components — and components that fly off because removal disconnected
them — go to the **discard pile** and count as lost components.

- In the **test flight**, no credit penalty is applied `[p.9]`.
- In **level II**, the discarded components cost 1 credit each at the end like any
  other loss. If a violation is discovered *after* the flight has started, the
  player fixes it immediately and pays **1 extra credit** `[p.17]`.

---

## 6. Launch preparation

Performed after validation, before the first card `[p.9, p.18]`.

### 6.1 Batteries

Every battery component is filled to its printed capacity (2 or 3) `[p.9]`.

### 6.2 Crew

- The starting cabin always receives **2 humans**. It can never hold an alien `[p.18]`.
- A cabin not interconnected to a life support module receives **2 humans**.
- A cabin interconnected to a life support module may receive **2 humans** or
  **1 alien** of that module's colour.
- A cabin interconnected to both a purple and a brown module may receive 2 humans,
  1 purple alien, or 1 brown alien.
- A ship may host **at most 1 alien per colour** `[p.18]`.

Crew placement happens in route order starting from the leader, so that later
players can react to earlier choices `[p.18]`.

Aliens count as crew for every purpose (Combat Zone, Abandoned Station, Abandoned
Ship, Slavers) `[p.18]`.

### 6.3 Adventure deck

**Test flight** — the 8 cards marked **L**, shuffled `[p.9]`.

**Level II** — before building starts, 4 piles are built, each holding **2 level-II
cards and 1 level-I card**, drawn face down from separately shuffled level decks
`[p.16]`, verified on `flight-board_lvl-2.png`. Three piles go to the bottom of the
board (peekable), one to the top (never peekable). When building ends the leader
merges and shuffles all four piles; the shuffle is repeated until the top card
matches the flight level `[p.16]`.

---

## 7. Flight mechanics

The leader reveals the top card; all players resolve it; then the (possibly new)
leader reveals the next. The flight ends when the last card has been resolved
`[p.10]`.

### 7.1 Moving on the route

Movement counts **empty spaces**: occupied spaces are skipped and do not count
`[p.10]`. This applies to forward movement and to losing flight days alike.

When several players lose flight days from the same card, they move in **reverse
route order** — the last player moves first `[p.10]`.

### 7.2 Lapping

If the leader is more than a full lap ahead of a player, that player is forced to
give up (§ 9) `[p.20]`.

### 7.3 Ship attributes

**Engine power** `[p.11]`

- each single engine: +1
- each double engine on which a battery is spent: +2
- brown alien: +2, **only if** engine power before the alien is greater than 0

**Firepower** `[p.11]`

- each single cannon pointing `NORTH`: +1
- each double cannon pointing `NORTH` on which a battery is spent: +2
- any cannon pointing east, south or west: **half** the above (½ single, 1 double)
- purple alien: +2, **only if** firepower before the alien is greater than 0

Firepower is a half-integer and must be kept exact: 5½ beats 5 and loses to 6
`[p.11]`. Never round.

**Crew size** — humans plus aliens.

Batteries are optional and are declared per activation. Single cannons, single
engines and aliens are **always** counted; a player may not choose to under-declare
them `[p.19]`.

### 7.4 Dice and targeting

Threats (meteors and cannon fire) address a **printed row or column** by the
sum of two dice, so values range 2…12. Only 4…10 name a real column and only 5…9
name a real row; any other sum is an automatic miss.

Direction determines which line and from which end:

| Incoming from | Addresses | Hits |
|:--|:--|:--|
| `NORTH` | a column | the topmost occupied cell of that column |
| `SOUTH` | a column | the bottommost occupied cell |
| `WEST` | a row | the leftmost occupied cell |
| `EAST` | a row | the rightmost occupied cell |

If the line is empty, nothing happens.

A **meteor swarm** uses one roll per meteor made by the leader, and that roll applies
to **all** players simultaneously; each player then checks it against their own ship
`[p.13]`. Likewise, when an enemy card inflicts cannon fire, a single roll set is
made and applied to every defeated player `[QR, p.19]`.

### 7.5 Damage

A destroyed component is removed and placed in the player's discard pile. Any
component that is no longer connected to the ship flies off and is also lost. If
the ship splits into several pieces, the player chooses which piece to keep; the
rest is lost `[p.10]`.

Tokens (crew, batteries, goods) sitting on a lost component return to the bank
immediately `[p.10]`.

Every component in the discard pile costs **1 credit** at the end of the flight.

### 7.6 Goods

Each cargo slot holds exactly 1 cube. Red cubes may only go in **special** holds;
all other colours fit anywhere `[QR]`.

Whenever a card lets a player load goods, that player may also redistribute cubes
between holds and jettison cubes to make room. **This is the only moment cubes may
be moved** `[QR]`. Cubes that do not fit are returned to the bank.

**Goods shortage** — if the bank runs out of a colour, players load in route order,
first come first served. Cubes jettisoned by an earlier ship become available to
later ones. A player who ends up with nothing to load still pays the flight days
`[p.19]`.

**Losing goods** — always the most valuable first. When goods run out, batteries are
surrendered instead. When both are gone, nothing further is taken `[p.11]`.

**Losing crew** — the player chooses which humans or aliens to give up, and from
which cabins `[QR]`.

---

## 8. Adventure cards

Thirteen card types. The eight marked **L** make up the test flight deck: Planets,
Abandoned Station, Abandoned Ship, Smugglers, Open Space, Meteor Swarm, Combat
Zone, Stardust `[p.12-13]`.

Cards that grant a reward in exchange for flight days always let the player decline
the reward and keep the days `[p.19]`.

### 8.1 Planets

2 to 4 planets, each with a fixed set of goods `[p.12]`.

1. In route order, each player may land on one **free** planet. One ship per planet.
2. Landing is optional; a player may land purely to deny a planet to someone else.
3. Once everyone has decided, players who landed load their planet's goods.
4. Then, in **reverse route order**, every player who landed loses the flight days
   printed on the card.

### 8.2 Abandoned Ship

Only one player may take it `[p.12]`.

1. In route order, starting with the leader, each player may accept.
2. Accepting costs the printed number of **crew** (humans and/or aliens, player's
   choice) and the printed **flight days**, and pays the printed **credits**.
3. A player must have enough crew to pay the cost.
4. As soon as somebody accepts, the card is over for everyone else.

### 8.3 Abandoned Station

Only one player may take it `[p.12]`.

1. Requires crew (humans + aliens) **at least** equal to the number printed.
2. In route order, the first player who qualifies and accepts loads the printed
   goods and loses the printed flight days.
3. **No crew is lost.**
4. As soon as somebody accepts, the card is over for everyone else.

### 8.4 Smugglers, Pirates, Slavers

Enemies attack in route order until someone defeats them `[p.12, p.19]`.

For each player in turn, compare the player's declared firepower with the enemy's:

| Outcome | Effect |
|:--|:--|
| firepower **>** enemy | Player wins. They may take the reward at the cost of the printed flight days, or decline both. The enemy leaves; **no later player is attacked**. |
| firepower **=** enemy | Nothing happens to this player. The enemy is not defeated and moves on to the next player. |
| firepower **<** enemy | The player suffers the penalty. The enemy moves on to the next player. |

Rewards and penalties by enemy:

| Enemy | Reward | Penalty |
|:--|:--|:--|
| Smugglers | goods shown on the card | lose the 2 most valuable goods (batteries if goods run out) |
| Slavers | credits | lose the printed number of crew, player's choice |
| Pirates | credits | the ship is fired on with the shots printed on the card |

Pirate cannon fire is resolved for all defeated players at once, from a single roll
set made by the first defeated player `[p.19]`.

### 8.5 Open Space

In route order, each player declares engine power and immediately advances that
many **empty** spaces `[p.13]`.

A player who cannot declare engine power greater than 0 is forced to give up
`[QR]`.

### 8.6 Meteor Swarm

The card lists meteors in order, each with a size and an incoming direction. They
are resolved top to bottom and hit all players simultaneously `[p.13]`.

For each meteor, the leader rolls two dice; the sum selects the line (§ 7.4). For
each player, find the first component in that line from that direction.

**Small meteor**

- the facing side is `PLAIN` → the meteor bounces, no damage;
- the facing side carries a connector → the component is destroyed, **unless** the
  player activates a shield covering that direction, at a cost of 1 battery.

**Big meteor** — a shield is useless. The only defence is shooting it `[p.13, p.19]`:

- arriving from `NORTH`: only a cannon pointing `NORTH` **in that exact column**;
- arriving from `SOUTH`, `EAST` or `WEST`: any cannon pointing toward the meteor in
  the **same or an adjacent** row/column.

Using a double cannon to shoot costs 1 battery. Unshot, the component is destroyed.

### 8.7 Combat Zone

Three lines, evaluated top to bottom. Each line names an attribute and a penalty
`[p.13]`. The level-I card, used in the test flight, reads:

1. fewest crew → lose 3 flight days
2. lowest engine power → lose 2 crew
3. lowest firepower → one light and one heavy shot from behind

Level-II cards use the same structure with their own attributes and penalties; the
values come from the card data, not from hard-coded logic.

- Players compute and declare in route order, deciding on batteries as they go
  `[QR]`.
- The **lowest** value suffers the penalty.
- On a tie, only the tied player **furthest ahead** on the route is penalised
  `[p.13]`.
- Losing flight days on one line can reorder the route before the next line is
  evaluated — the new order is used `[QR]`.
- The whole card is **ignored** if every other player has given up `[p.20]`.

### 8.8 Stardust

In **reverse route order**, each player counts their exposed connectors and moves
back that many empty spaces `[p.13]`. Every exposed side counts once regardless of
pipe count.

### 8.9 Epidemic — level II only

Remove 1 crew member (human or alien) from **every occupied cabin that is
interconnected to another occupied cabin** `[p.19]`. Interconnection means a legal
connector joint, not mere adjacency (§ 3.2).

### 8.10 Sabotage — out of scope

Sabotage is a **level III** card. Level III is excluded by `requirements.pdf` § 2.1,
so no level II deck can contain it and it is not implemented. It is described on
manual p.19 alongside Epidemic, which is why the two are easy to conflate; only
Epidemic belongs to a level II deck.

The consequence for the rest of this document: wherever the manual pairs Combat
Zone with Sabotage — the "last player standing" clause of § 9.4 — only Combat Zone
applies here.

### 8.11 Card summary

| Card | Test flight | Level II | Acts in |
|:--|:-:|:-:|:--|
| Planets | ✔ | ✔ | route order, days in reverse |
| Abandoned Ship | ✔ | ✔ | route order, first taker only |
| Abandoned Station | ✔ | ✔ | route order, first taker only |
| Smugglers | ✔ | ✔ | route order until defeated |
| Pirates | — | ✔ | route order until defeated |
| Slavers | — | ✔ | route order until defeated |
| Open Space | ✔ | ✔ | route order |
| Meteor Swarm | ✔ | ✔ | simultaneous |
| Combat Zone | ✔ | ✔ | route order, per line |
| Stardust | ✔ | ✔ | reverse route order |
| Epidemic | — | ✔ | simultaneous |

---

## 9. Giving up

### 9.1 Forced

A player must give up when `[p.20]`:

- they lose their last **human** — aliens cannot fly the ship alone;
- they declare engine power 0 on an **Open Space** card;
- the leader laps them.

These conditions are checked **only after the current card has been fully
resolved** `[p.20]`. A player who loses their last human mid-Combat-Zone still
suffers the rest of the card.

### 9.2 Voluntary

A player may give up at any point **before the next card is revealed**. After a card
is revealed they must see it through `[p.20]`.

### 9.3 Effects

- The rocket marker leaves the route; no later card affects that player.
- No finish-order reward.
- Excluded from the prettiest-ship contest.
- Goods sell at **half price, rounded up** `[QR]`.
- The penalty for lost components is still paid in full.

### 9.4 Last player standing

If only one player is left, **Combat Zone** is skipped `[p.20]`. The manual pairs
it with Sabotage, which is a level III card and out of scope (§ 8.10).

---

## 10. End of flight

Resolved once the last card is done `[p.15]`.

1. **Finish-order reward** — by final route order, to players who completed the
   flight: 8/6/4/2 at level II, 4/3/2/1 in the test flight.
2. **Prettiest ship** — fewest exposed connectors among players who completed the
   flight wins 4 (level II) or 2 (test flight). **All** tied players receive it
   `[QR]`.
3. **Selling goods** — full price for finishers, half rounded up for players who
   gave up. Red 4, yellow 3, green 2, blue 1.
4. **Lost components** — −1 credit each, including reserved components never
   attached.

### 10.1 Winning

Any player finishing with 1 credit or more has "won" in the game's own terms; the
player with the most credits wins the match `[p.15]`. The application reports the
full ranking.

---

## 11. Decisions and data defects

Points where the manual leaves room, and every place the shipped data disagreed with
it. Defects marked *fixed* were corrected in the data and are now guarded by tests.

### Decisions

| # | Item | Decision |
|:--|:--|:--|
| D1 | Test flight timer | **No timer.** `[p.8]` states the test flight is untimed, and the level I board carries a single hourglass circle with no *Stop* space, against three on the level II board. |
| D2 | Test flight card level | The 8 **L** cards are level I cards that additionally bear the mark `[p.9]`. Modelled as `level = LEVEL_I` plus a `testFlight` flag, **not** as a third exclusive level. |
| D3 | Firepower representation | Half-integers are exact. Stored as an integer count of halves, never rounded and never a float `[p.11]`. |
| D4 | Placement checks | Cell-level legality is enforced immediately; connector, clearance and connectivity checks run at the end of building, mirroring the physical *controllo visuale* `[p.8]`. |
| D5 | Sabotage | **Out of scope.** A level III card, excluded with level III by `requirements.pdf` § 2.1. See § 8.10. |
| D6 | Component orientation | Not stored per tile. All 30 engines are printed exhausting south, all 36 cannons facing north, and all 8 shields covering north and east, verified against every image. A placed component's facing is its kind plus its rotation. |
| D7 | Hourglass duration | **90 seconds** per period. The manual gives no number — it ships a physical hourglass — so this is a project choice: long enough to place several tiles under pressure, short enough that three periods do not outlast anyone's patience. Configurable, not hard-coded. |

### Data defects found and fixed

Each was found by checking the shipped JSON against the artwork and the manual.

| # | Defect | Fix |
|:--|:--|:--|
| B1 | The test flight was modelled as an exclusive level, leaving the level I pool with 13 cards instead of 20 — so a level II deck, which draws one level I card per pile, drew from a truncated pool. | Level plus a separate mark. Both pools now hold 20 cards. |
| B2 | Only 7 cards carried the test flight mark. `smugglers_lvl1.jpg` shows the **L** badge in its bottom-left corner but was tagged level I only, leaving the deck one card short of the 8 the manual requires. | Marked. The deck now holds 8 cards covering the 8 printed types. |
| B3 | Battery capacities split 12 two-charge to 5 three-charge. Manual p.3 prints 11 and 6, and the artwork of `battery_UD-SD` shows three cells. | Corrected to 3. The split is now 11 / 6. |
| B4 | `double-engine_U-S` carried its single connector on the east side in the data; both its file name and its artwork put it on the north side. | Connectors are now derived from the tile file names, which encode sides and connector types and agree with the artwork on all 152 tiles. |

### Verified against the source material

| What | How |
|:--|:--|
| Tile counts per kind | All 13 counts match the component overview on manual p.3. |
| Connectors | All 152 drawable tiles agree with their encoded file names. |
| Cargo and battery capacities | Match the printed slot counts. |
| Board geometry, rewards, price lists | Read off `assets/cardboard`; 18 and 27 usable cells, routes of 18 and 24, rewards 4/3/2/1 and 8/6/4/2. |
| Deck composition | The level II flight board prints `II II I` beneath the route: two level II cards and one level I card per pile. |
| Card pools | 20 level I and 20 level II cards, 8 of them marked. |

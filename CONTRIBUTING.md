# Contributing

This document defines how work is organised on this repository. It exists because
"utilizzo degli strumenti (IntelliJ IDEA, Git, Maven, ...)" is an explicit grading
criterion of the project (`requirements.pdf` § 3): the history has to read like a
deliberate engineering process, not a dump of commits.

## Language

Everything that ends up in the deliverable is in **English**: identifiers, code
comments, Javadoc, specifications, commit messages, issue and PR text
(`requirements.pdf` § 2.2). Only the user-facing strings of the application may be
Italian, and this project keeps them English too for consistency.

**One exception: quoting the requirements.** `requirements.pdf` is written in Italian
and is the normative document. Where a comment or a specification turns on the exact
words of a requirement, quote them verbatim and put an English gloss beside them —
paraphrasing a rule and then arguing from the paraphrase is how `docs/specs` came to
assert things the PDF does not say (M15). The quote is evidence; the gloss is for the
reader; neither stands alone.

## Branches

| Branch | Purpose | Who merges into it |
|:--|:--|:--|
| `main` | The submission. Receives the rebuild once it is deliverable, and nothing before that. | The `v1.0.0` release PR |
| `develop` | Where the project is built. Always compiles, tests always green, every milestone tagged here. | Feature PRs |
| `<type>/<issue>-<slug>` | One issue, one branch. | — |

`main` still holds the previous implementation, which does not compile. It is left
alone deliberately: replacing it before the rebuild is deliverable would trade a
broken submission for an incomplete one, and the history is what makes the old code
recoverable either way. It is also on the remote branch `goback`.

Feature branch names mirror the commit types below, e.g.

```
feat/12-meteor-swarm-resolution
fix/47-shield-consumes-two-batteries
docs/3-communication-protocol
```

Nothing is committed directly to `main` or `develop`.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/), scoped to the area
touched:

```
<type>(<scope>): <imperative summary, lowercase, no trailing period>

<body: why the change, not what — the diff already says what>

Refs #<issue>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
Scopes: `model`, `controller`, `network`, `protocol`, `tui`, `gui`, `persistence`,
`build`, `docs`.

Examples:

```
feat(model): reject cabin placement when a connector faces a plain side
fix(network): drop the RMI callback registry entry when a client disconnects
test(model): cover big-meteor deflection from side-facing cannons
```

A commit that leaves the build red is not acceptable; squash locally before
pushing if needed.

## Pull requests

Every change reaches `develop` through a PR, even a one-line fix. A PR:

1. references the issue it closes (`Closes #N`);
2. cites the rule or requirement it implements, by page or spec section;
3. is green on CI (compile, unit tests, Javadoc);
4. is reviewed by at least one other team member.

The reviewer checks the change against the **specification**, not against the
author's intent. If the spec is ambiguous, the fix is to sharpen the spec in the
same PR.

## Issues

Issues are the unit of planning. Each one carries:

- a **type** label: `rule`, `feature`, `bug`, `refactor`, `docs`, `test`, `infra`;
- an **area** label: `model`, `controller`, `network`, `tui`, `gui`, `persistence`;
- a **milestone** (see `docs/roadmap.md`);
- explicit acceptance criteria and the tests that must exist to close it.

An issue with no acceptance criteria cannot be closed, because there is no way to
tell whether it is done.

## Versioning

[Semantic versioning](https://semver.org/):

- `v0.x.0` — a milestone completed. Tagged on `develop`, where the work happens.
- `v1.0.0` — the submission: complete rules, TUI, GUI, RMI, Socket, all four advanced
  features, Javadoc, protocol documentation, jars in `deliverables/`. This one is
  merged into `main` and tagged there.

Each tag gets a GitHub release listing the issues it closes, so that every milestone
is traceable from the repository alone.

## Definition of done

A unit of work is done when all of the following hold.

- [ ] The behaviour matches `docs/specs/game-rules.md`, which in turn cites the manual.
- [ ] Unit tests cover the nominal path and every rule edge case named in the issue.
- [ ] `./mvnw test` is green.
- [ ] Every public type and method has English Javadoc explaining *why*, not restating the signature.
- [ ] No method longer than roughly 30 lines and no duplicated logic — both are graded (`requirements.pdf` § 3).
- [ ] The protocol document is updated if any message changed.

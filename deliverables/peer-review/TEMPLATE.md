# Peer review — <team> — <first | second>

*Reviewed by:* <names>
*Date:* <date>
*Material reviewed:* <exactly which documents or diagrams, and which version of them>

## What this review is against

Name the standard being applied, so a reader can tell a rule from an opinion — the
requirements document, the rules of the game, or a stated design goal of the team being
reviewed. A review that only says what the reviewer would have done differently is not
useful to them.

## What the design does well

Specific, and with the reason. "The protocol is clean" tells them nothing; "commands are a
sealed hierarchy, so a new message cannot be added without every switch that handles them
failing to compile" tells them what to keep.

## Findings

One heading per finding. For each:

- **What.** The observation, in one sentence.
- **Why it matters.** What goes wrong, concretely, and to whom. If nothing goes wrong, it
  is a preference and should be labelled one.
- **Evidence.** The diagram, class or paragraph it comes from.
- **Suggestion.** What could be done instead — offered, not demanded.

Rank them by what would actually hurt, not by how easy they are to spot.

## Questions

Things that could not be judged from the material. These are often the most useful part of
a review: a question nobody can answer from the documents usually means the documents are
missing something.

## What changed as a result

Filled in **after** the discussion with the other team. What they took, what they did not,
and — where they disagreed — why. A review with nothing in this section either found nothing
or was not delivered.

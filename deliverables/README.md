# Deliverables

This directory holds the artifacts handed in for evaluation. It is populated at
release time, not on every build, so that the working tree stays clean.

| Artifact | Produced by |
|:--|:--|
| `server.jar`, `client.jar` | `./mvnw clean package`, copied here from `target/` when a version is tagged |
| `peer-review/` | The two peer review documents required by `requirements.pdf` § 1 |

The jars from the previous implementation were removed on `develop`; they remain
available on `main` and in the git history. Fresh ones are built from the tagged
commit as part of `v1.0.0` (issue #62).

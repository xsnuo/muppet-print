# ADR-001: Memory-Driven Documentation Baseline

## Status

Accepted

## Context

The repository previously had code but no stable, visible AI-oriented project memory. That makes future AI-assisted iteration harder because project intent, architecture, constraints, and unresolved questions are reconstructed from code every time.

The user requested a durable and explicit documentation set in the working tree, without hidden directories, and asked that future AI work continue maintaining that memory.

## Decision

The project will maintain a visible AI memory system rooted in `AGENTS.md` and `docs/ai/`.

The baseline memory set is:

- `AGENTS.md`
- `docs/ai/project-overview.md`
- `docs/ai/architecture.md`
- `docs/ai/domain-model.md`
- `docs/ai/decisions/*.md`
- `docs/ai/known-constraints.md`
- `docs/ai/status/current-iteration.md`
- `docs/ai/status/backlog.md`
- `docs/ai/status/open-questions.md`

Future AI changes must keep these files synchronized with relevant code changes.

## Consequences

### Positive

- Project understanding becomes cumulative instead of repeatedly rediscovered.
- AI agents gain explicit boundaries for coding style and documentation maintenance.
- Open questions and deferred work become visible artifacts.
- Documentation remains in the repository and reviewable by humans.

### Negative

- Every meaningful change now carries a documentation maintenance cost.
- If not curated, duplicated or stale memory may accumulate.

## Follow-Up Rule

When code and memory disagree, agents should trust verified code behavior first and update the corresponding memory file instead of preserving stale documentation.

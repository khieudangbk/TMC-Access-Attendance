# GitHub Copilot instructions

Read `AGENTS.md` at the repository root first. It is the authoritative rule set.

Key points:

- Java 21 / Spring Boot 4.1 / Spring Modulith backend, React 19 + TypeScript frontend, PostgreSQL 17.
- Preserve the scan invariants: evidence before gate effect, stable IDs, idempotency, `UNKNOWN` outcome, append-only data, server-side RBAC, schema ownership, bounded offline mode.
- Update docs and `docs/04-api/openapi.yaml` with behavior/API changes.
- Run `make verify` before proposing a change is complete.

Task-specific reading list: `docs/08-agents/agent-context.md`.

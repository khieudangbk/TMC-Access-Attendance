# AGENTS.md — TMC-AACS

Rules for AI coding agents working in this repository. Read this file first, every session.
`CLAUDE.md`, `.cursor/rules/project.mdc`, and `.github/copilot-instructions.md` point here; this file is authoritative.

## 1. What this system is

TMC-AACS is an access-control and attendance system for TMC's two factories:

- Bình Dương: ~800 employees, 6 gate stations (3 IN, 3 OUT), central site.
- Long An: ~600 employees, 4 gate stations (2 IN, 2 OUT), DR site.
- Employees show a short-lived signed QR token at a kiosk.
- The kiosk calls the on-site **edge agent**; the edge verifies the token, evaluates access policy, persists evidence, then commands the turnstile.
- Access events become attendance data; a monthly snapshot is exported to HR/Payroll before 12:00 on the 1st.
- Target p95 end-to-end scan response: **≤ 2 seconds** at 800 scans / 30 minutes / factory.

The full target design lives in `docs/`. The repository currently contains a walking skeleton (see `README.md`).

## 2. Source of truth (in order)

1. **Domain invariants in this file** — never violate, even if code or docs disagree.
2. `docs/03-decisions/` — accepted architecture decisions (ADRs).
3. `docs/01-requirements/` — functional, non-functional requirements, user stories.
4. `docs/02-architecture/` — target structure, flows, data model.
5. `docs/04-api/openapi.yaml` — API contract.
6. Code and tests — current behavior.

If code and docs disagree, do **not** silently pick one. State the mismatch and update docs/ADR in the same change.

## 3. Non-negotiable domain invariants

These are release blockers. Every change must preserve them.

1. **Evidence before effect.** Persist the access event, decision, gate intent and outbox row in one transaction *before* calling the turnstile API. Never open a gate first and record later.
2. **Stable identifiers.** `scan_id` is created by the kiosk and survives retries. One `scan_id` maps to at most one original access event and at most one gate `command_id`. Same ID + different payload is rejected, not merged.
3. **Idempotency is enforced in the database**, with unique constraints and transactions, not only with an application-level `find` check.
4. **`UNKNOWN` is a real outcome.** A turnstile timeout is `UNKNOWN`, never `CONFIRMED`, never `FAILED`, never success. Do not blindly retry an unknown command.
5. **Append-only evidence.** Access events, status history, audit records and exported snapshots are never updated in place or deleted. Corrections are new adjustment records with reason, actor, before/after values and approval.
6. **No false attendance.** A valid QR, an accepted command, a gate opening, and a person actually passing are four different facts. Do not infer passage without a confirmed device/sensor signal.
7. **QR is server-signed and short-lived.** TTL ~60 seconds. Token contains no name, phone, email or other direct PII. Signing private keys never reach kiosks, browsers, mobile apps or this repository.
8. **Server-side authorization.** Every protected endpoint checks authentication, role and data scope on the server. Hiding a UI button is not authorization. Sensitive actions produce audit records, including denied attempts where required.
9. **Scan path isolation.** The scan path must not synchronously call HR/Payroll, reporting, admin or the central database. Edge uses a local durable store and a signed, versioned policy/credential snapshot.
10. **Schema ownership.** Each service owns its PostgreSQL schema. No cross-schema joins, foreign keys or writes. Cross-service data travels through APIs or events.
11. **Offline is bounded.** Edge may auto-authorize only while policy/keys are inside the trust window (target 30 minutes) and the local durable store is writable. After expiry, stop automatic authorization and fall back to a documented, audited manual process. Never open all gates because software failed.
12. **No secrets or raw QR in logs.** Never log tokens, signing keys, passwords, full QR payloads or unnecessary PII. Use `employee_ref`, `scan_id`, `command_id`, `batch_id` for correlation.

## 4. Stack and layout

```
frontend/                 React 19 + Vite 8 + TypeScript 7 + TanStack Query + Tailwind 4
backend/central-api/      Spring Boot 4.1 + Spring Modulith; modules:
                            identity  (ICS)
                            access    (AAS central)
                            station   (GSS)
                            reporting (RIS)
                            admin     (ADS)
                            system    (health, config)
                            shared    (open shared kernel)
backend/edge-agent/       Spring Boot 4.1; scan path (C05–C11), local PostgreSQL
docs/                     requirements, architecture, ADRs, API, DB, runbooks
infra/                    docker compose: PostgreSQL 17, Adminer, Mailpit
```

Java 21 language level, Maven (wrapper at `backend/mvnw`), PostgreSQL 17, Flyway.
Web uses React Router 7 and feature-first folders. API base path is `/api/v1`.

Read `docs/02-architecture/tech-stack.md` before adding a dependency or a new module.

## 5. Commands

```bash
cp .env.example .env
make up                 # postgres + adminer + mailpit
make backend            # central API :8080
make edge               # edge agent :8081
make web                # web :5173
make test               # Maven tests + Vitest
make build              # backend jars + frontend bundle
make verify             # build + tests + lint
make reset-db           # DESTRUCTIVE: drops local DB volumes
```

Run a single backend test:

```bash
cd backend && ./mvnw -pl central-api -Dtest=StationControllerTest test
```

## 6. How to make changes

### Before coding

1. Find the requirement ID (`FR-*`, `NFR-*`, `US-*`) or ADR this task serves.
2. Read the matching docs from the context map in `docs/08-agents/agent-context.md`.
3. Check `docs/06-development/roadmap.md` to see whether the area is in the current phase.
4. For anything security-, schema-, API- or architecture-significant, confirm there is an ADR. If not, propose one before implementing.

### Backend rules

- Package by feature/module, then `domain`, `application`, `application/port/{in,out}`, `adapter/{in,out}`.
- `domain` must not import Spring, JDBC, HTTP, JSON or Flyway.
- Use constructor injection. No field injection. Prefer records for immutable data and DTOs.
- Application services own transactions and use cases. Controllers translate HTTP, nothing else.
- Use `JdbcClient` with explicit SQL for idempotent/transactional paths. Do not introduce JPA for new areas without an ADR.
- All timestamps are `Instant` in UTC. Local date/time uses `Asia/Ho_Chi_Minh` only at the presentation or business-calendar boundary.
- New schema change = new Flyway migration. Never edit an applied migration.
- New config property = document it in `docs/02-architecture/tech-stack.md` and `.env.example` when it is environment-specific.
- New module dependency must pass the Spring Modulith verification test.

### Frontend rules

- TypeScript strict; no `any`, no `@ts-ignore` unless justified in a comment.
- Feature-first: `src/features/<feature>/{components,api,hooks,pages}`; shared primitives in `src/shared`.
- Server state goes through TanStack Query. Do not copy API data into global client state.
- All network calls go through `src/shared/api/client.ts`; no ad-hoc `fetch` in components.
- Vietnamese UI copy; English identifiers and code comments.
- Every async screen has loading, empty and error states.
- Accessibility basics: labels for inputs, focus states, semantic table/headings, no color-only status.

### API rules

- Contract-first: update `docs/04-api/openapi.yaml` in the same change as the controller.
- Never break an existing field or status code inside `v1`; add fields compatibly, version breaking changes.
- Error body: `{ "code", "message", "details", "timestamp" }`.
- Mutating endpoints accept an idempotency key. Scan endpoints use the body `scan_id` as the idempotency key.
- Cursor pagination for event/audit streams; page/size only for small config lists.

## 7. Testing gates

Every change must pass:

```bash
make test
make build
cd frontend && npm run lint
```

Required coverage by change type:

| Change | Tests |
|---|---|
| Domain rule / policy | Unit tests for allow, deny, boundary and exception paths |
| REST controller | Controller slice test with success, validation, not-found/denied |
| SQL / migration | Integration test against PostgreSQL (Testcontainers when added) |
| Scan pipeline / idempotency | Replay same `scan_id`, concurrent duplicate, crash before/after gate call, timeout → `UNKNOWN` |
| Outbox / sync | Commit then fail to publish, publish then lose ack, duplicate delivery, restart consumer |
| Auth / scope | Positive and negative matrix tests per role and factory/team scope |
| Frontend | Component test for meaningful behavior; loading/empty/error states |

Do not weaken or delete a test to make a change pass. If a test encodes a wrong invariant, change the test and the doc together.

## 8. Documentation duties

Update docs in the same change when you alter:

- behavior or acceptance criteria → `docs/01-requirements/`
- architecture boundary, flow or deployment unit → `docs/02-architecture/`
- a significant decision or trade-off → new ADR in `docs/03-decisions/`
- API shape → `docs/04-api/openapi.yaml` + conventions
- schema/index/constraint → `docs/05-database/`
- workflow, commands or test strategy → `docs/06-development/`

Use ADR template from `docs/03-decisions/README.md`. ADRs are immutable once accepted; supersede with a new ADR.

## 9. Security and privacy rules

- Never commit `.env`, keys, keystores, certificates, real employee data or production dumps.
- Never log raw QR tokens, JWTs, passwords, or full request bodies containing them.
- Do not add a "temporary" bypass of token verification, policy, RBAC or audit. If a demo needs it, isolate it behind a `dev` profile and document the risk.
- Do not trust client-provided `employee_ref`, `factory_id`, `role` or scope. Derive them from the authenticated principal/server-side state.
- New PII field requires: purpose, retention, access scope, masking rule, and an update to `docs/02-architecture/security-privacy.md`.
- Audit records are written by the application role with insert-only permission. Do not add update/delete endpoints for audit.

## 10. Do not

- Do not implement microservices split, Kubernetes, Kafka, or a second database "for cleanliness" in the current phase. ADR-008 chooses a split-ready central modular monolith + separate edge.
- Do not call the central API synchronously from the scan path.
- Do not retry a gate command with an unknown outcome unless the adapter has confirmed idempotency and the latency budget allows it.
- Do not change a station's factory/direction silently; configuration changes need revision + audit.
- Do not use `LocalDateTime` for persisted timestamps.
- Do not put business logic in React components or SQL strings in controllers.
- Do not edit generated build output, `target/`, `dist/`, or `node_modules/`.
- Do not reformat unrelated files or mix refactors with behavior changes.

## 11. Definition of Done

A task is done only when:

- [ ] Behavior matches the requirement/ADR and preserves all invariants in §3.
- [ ] Code follows module/layer boundaries; `ModularityTests` passes.
- [ ] Tests cover the new behavior and failure paths listed in §7.
- [ ] `make verify` passes locally.
- [ ] API contract and docs are updated.
- [ ] No secrets, raw QR or unnecessary PII in code/logs/fixtures.
- [ ] Error, empty and loading states are handled (UI changes).
- [ ] Migration is forward-only and tested against a fresh database.
- [ ] Summary states: what changed, why, tests run, docs touched, remaining risks.

## 12. When to stop and ask

Ask the human before proceeding if a task requires:

- changing an invariant in §3, a schema ownership boundary, or an accepted ADR;
- a breaking API change or a new external integration contract;
- a security trade-off (offline authorization window, token TTL, audit retention, PII field);
- destructive data operations or production configuration;
- choosing between conflicting requirements.

Otherwise, make the smallest correct change, keep the diff focused, and report assumptions explicitly.

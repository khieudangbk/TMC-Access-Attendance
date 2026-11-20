# ADR-002 — Ports & adapters, domain không phụ thuộc hạ tầng

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư, chủ dự án
- Supersedes: —
- Superseded by: —

## Context

Hệ thống tích hợp với turnstile, HR/Payroll, email và có thể thay đổi nhà cung cấp. Nghiệp vụ cần được kiểm thử độc lập với hạ tầng. Đường quét cần thay simulator/thiết bị thật mà không đổi logic.

## Decision

Áp dụng **Hexagonal Architecture / Ports & Adapters** trong từng module:

- `domain`: entity, value object, rule, exception thuần Java.
- `application`: use case, transaction boundary; phụ thuộc port.
- `application/port/in`: interface use case.
- `application/port/out`: interface repository/gateway.
- `adapter/in/web`: controller, DTO.
- `adapter/out/persistence|client`: JDBC, HTTP client, simulator.

Quy tắc phụ thuộc: adapter → application → domain. Domain không import Spring/JDBC/HTTP/JSON/Flyway. Không adapter nào gọi trực tiếp adapter khác.

## Consequences

Tích cực:

- Test domain/use case không cần DB/thiết bị.
- Thay turnstile/HR bằng adapter mới, giữ nguyên policy.
- Ranh giới rõ, hỗ trợ tách service sau này.
- Simulator cổng dùng được cho test lỗi/timeout.

Tiêu cực:

- Nhiều interface/mapping hơn; cần kỷ luật tránh over-engineering.
- Có thể trùng lặp DTO giữa web/persistence.

## Alternatives considered

- **Controller gọi thẳng JDBC:** nhanh ban đầu nhưng trộn nghiệp vụ, khó test lỗi, khó thay adapter.
- **JPA entity dùng chung domain:** loại vì coupling và khó kiểm soát SQL/idempotency.

## References

`AGENTS.md` §6, ADR-008.

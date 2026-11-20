# ADR-003 — ReactJS + Spring Boot + PostgreSQL thay cho TypeScript/NestJS

- Status: Accepted
- Date: 2026-11-20
- Deciders: Chủ dự án, kiến trúc sư
- Supersedes: —
- Superseded by: —

## Context

Đặc tả gốc mô tả frontend ReactJS và backend TypeScript/NestJS. Chủ dự án yêu cầu stack **ReactJS + Spring Boot + PostgreSQL** cho code environment và các phase tiếp theo. Đội ngũ hiện có kinh nghiệm Java/Spring; cần transaction, unique constraint và SQL tường minh cho đường quét.

## Decision

1. Backend dùng **Java 21 + Spring Boot 4.1 + Spring Modulith + Spring JDBC (`JdbcClient`) + Flyway**.
2. Frontend dùng **React 19 + Vite + TypeScript + TanStack Query + React Router + Tailwind CSS**.
3. Database **PostgreSQL 17**.
4. Không dùng JPA cho đường ghi/idempotency; nếu một khu vực cần ORM, phải có ADR riêng.
5. Các quyết định kiến trúc còn lại (service-based, edge, outbox/inbox, schema-per-service) giữ nguyên.

## Consequences

Tích cực:

- Kiểm soát transaction/unique constraint/idempotency rõ ràng.
- Tận dụng kinh nghiệm Java/Spring của đội.
- Spring Modulith kiểm tra ranh giới module tự động.
- Flyway + SQL dễ audit, phù hợp dữ liệu chứng cứ.

Tiêu cực / rủi ro:

- Khác đặc tả gốc; tài liệu phải nêu rõ để tránh hiểu nhầm.
- Không dùng ORM => viết SQL/mapping thủ công nhiều hơn.
- Một số thư viện/pattern NestJS không tương đương trực tiếp.

## Alternatives considered

- **NestJS theo đặc tả:** loại vì yêu cầu chủ dự án và kinh nghiệm đội.
- **Spring Boot + JPA:** loại cho đường quét vì khó kiểm soát SQL/transaction chính xác; có thể dùng giới hạn nếu có ADR.
- **Node/TypeScript + PostgreSQL:** loại cùng lý do trên.

## References

`docs/02-architecture/tech-stack.md`, FR-A1–A4, NFR-R1/R2.

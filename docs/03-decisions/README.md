# Architecture Decision Records

ADR ghi lại quyết định kiến trúc quan trọng, bối cảnh và hệ quả. ADR đã **accepted** là bất biến; thay đổi bằng ADR mới và đánh dấu `Supersedes`.

## Danh mục

| ADR | Tiêu đề | Trạng thái |
|---|---|---|
| [ADR-001](ADR-001-service-based-with-edge.md) | Service-based architecture với edge agent tại nhà máy | Accepted |
| [ADR-002](ADR-002-ports-and-adapters.md) | Ports & adapters, domain không phụ thuộc hạ tầng | Accepted |
| [ADR-003](ADR-003-react-spring-postgres.md) | ReactJS + Spring Boot + PostgreSQL thay cho TypeScript/NestJS | Accepted |
| [ADR-004](ADR-004-signed-short-lived-qr.md) | QR ký số, ngắn hạn, không chứa PII trực tiếp | Accepted |
| [ADR-005](ADR-005-commit-evidence-before-gate.md) | Commit chứng cứ trước khi điều khiển cổng | Accepted |
| [ADR-006](ADR-006-outbox-inbox-idempotency.md) | Outbox/inbox và idempotency bằng database | Accepted |
| [ADR-007](ADR-007-postgres-schema-per-service.md) | Schema-per-service, không join/FK chéo schema | Accepted |
| [ADR-008](ADR-008-central-modular-monolith.md) | Central là modular monolith split-ready giai đoạn đầu | Accepted |
| [ADR-009](ADR-009-rbac-scope-server-side.md) | RBAC + data scope kiểm tra phía server | Accepted |
| [ADR-010](ADR-010-append-only-audit.md) | Audit/dữ liệu chứng cứ append-only, có checkpoint | Accepted |

## Khi nào cần ADR

- Thay đổi bất biến miền, ranh giới schema/dịch vụ, mô hình triển khai.
- Thêm công nghệ hạ tầng (broker, cache phân tán, search, K8s).
- Thay đổi hợp đồng API/integration theo cách phá vỡ.
- Đánh đổi bảo mật/riêng tư (TTL token, cửa sổ offline, retention, PII).
- Chọn giữa các yêu cầu xung đột.

## Template

```markdown
# ADR-NNN — Tiêu đề

- Status: Proposed | Accepted | Superseded by ADR-NNN | Deprecated
- Date: YYYY-MM-DD
- Deciders: ...
- Supersedes / Superseded by: ...

## Context
Vấn đề, ràng buộc, lực lượng ảnh hưởng.

## Decision
Quyết định, phạm vi, chi tiết đủ để thực thi.

## Consequences
Tích cực, tiêu cực, rủi ro, việc phải làm.

## Alternatives considered
Phương án khác và lý do loại.

## References
FR/NFR/US/ADR liên quan, tài liệu, thử nghiệm.
```

## Quy ước

- Số ADR tăng dần, không tái sử dụng.
- Ngày theo ISO `YYYY-MM-DD`.
- Liên kết tới requirement/ADR liên quan.
- Cập nhật `docs/README.md` và các tài liệu bị ảnh hưởng trong cùng thay đổi.

# ADR-006 — Outbox/inbox và idempotency bằng database

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư
- Supersedes: —
- Superseded by: —

## Context

Gửi sự kiện từ edge về central và gửi lô sang HR có thể thất bại giữa chừng: commit xong nhưng không publish, publish xong nhưng mất ack, consumer restart. Cần tránh mất dữ liệu và tránh nhân đôi nghiệp vụ.

## Decision

1. **Transactional outbox** ở edge: ghi outbox cùng transaction với event/intent. Relay gửi theo `outbox_id`, chỉ đánh dấu published sau khi có ack.
2. **Inbox/unique key** ở central: `event_id` unique; insert cùng transaction cập nhật projection; item trùng trả `duplicate`.
3. **Idempotency key**:
   - Scan: `scan_id` unique trên `access_event`; so `payload_fingerprint`.
   - Command: `event_id` unique trên `gate_intent`; `command_id` unique.
   - HR export: `batch_id` + `snapshot_version`.
4. Khử trùng bằng **constraint/transaction**, không chỉ `find` ở application.
5. Gửi lại là hành vi mặc định; mọi consumer phải idempotent.
6. `schema_version` cho payload; central từ chối version không hỗ trợ.

## Consequences

Tích cực:

- Không mất sự kiện đã commit.
- Không nhân đôi khi retry/restart.
- Có thể replay/đối soát an toàn.

Tiêu cực / rủi ro:

- Outbox/inbox cần job relay, giám sát backlog, dọn dữ liệu cũ.
- Có thể có độ trễ giữa commit và central nhận.
- Xung đột hash cần điều tra thủ công, không tự merge.

## Alternatives considered

- **Gửi trực tiếp trong transaction:** loại vì network không transactional, dễ mất/treo.
- **“At-least-once” không khử trùng:** loại vì nhân đôi chấm công/lệnh.
- **Message broker ngay từ đầu:** chưa cần; outbox DB đủ cho tải hiện tại. Broker có thể thêm sau mà giữ outbox.

## References

FR-A4/A6, FR-R4, NFR-R2, QS-01/QS-05, `edge-central-sync.md`.

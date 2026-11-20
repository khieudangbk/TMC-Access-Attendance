# ADR-005 — Commit chứng cứ trước khi điều khiển cổng

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư, chủ dự án
- Supersedes: —
- Superseded by: —

## Context

Nếu mở cổng trước rồi mới ghi dữ liệu, mất điện/mất mạng/lỗi ghi có thể tạo lượt qua cổng không có chứng cứ, hoặc mất dữ liệu chấm công. Hệ thống cần chứng minh được ai đã được cho phép và thiết bị đã nhận lệnh gì.

## Decision

Trong một transaction tại edge PostgreSQL, commit **đồng thời**:

1. Access event (bất biến).
2. Decision + lý do + `policy_version`.
3. Gate intent (`command_id`, `gate_id`, action).
4. Outbox row để gửi central.

**Chỉ sau khi transaction commit thành công** mới gọi API turnstile. Nếu commit thất bại → không gọi thiết bị. Outcome được append sau khi có kết quả (hoặc `UNKNOWN`).

Không giữ transaction DB mở trong lúc chờ thiết bị.

## Consequences

Tích cực:

- Không có cổng mở mà thiếu chứng cứ.
- Crash trước/sau khi gọi cổng đều có thể đối soát.
- Outbox bảo đảm sự kiện được gửi central.

Tiêu cực / rủi ro:

- Thêm một lần commit trước khi gửi lệnh → tăng latency; phải tối ưu/fsync và đo.
- Nếu commit xong nhưng gọi cổng thất bại, cần xử lý outcome/đối soát.
- Có thể có intent không có outcome (crash) → cần job đối soát.

## Alternatives considered

- **Mở cổng trước, ghi sau:** loại vì mất chứng cứ.
- **Ghi async sau khi trả kết quả:** loại vì rủi ro mất dữ liệu, vi phạm NFR-R1.
- **Ghi intent không transaction:** loại vì có thể mất một phần.

## References

`AGENTS.md` §3 (bất biến 1), FR-A2/A4, QS-01, `scan-pipeline.md`.

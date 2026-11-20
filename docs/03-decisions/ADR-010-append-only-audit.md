# ADR-010 — Audit và dữ liệu chứng cứ append-only, có checkpoint

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư bảo mật, chủ dự án
- Supersedes: —
- Superseded by: —

## Context

Dữ liệu chấm công và audit có thể bị sửa/xóa để che giấu sai phạm. Cần bảo đảm:

- Không sửa/xóa bản ghi gốc.
- Phát hiện được can thiệp.
- Vẫn hỗ trợ sửa sai nghiệp vụ có kiểm soát.
- Không tuyên bố bất biến tuyệt đối khi DBA/backup vẫn có quyền.

## Decision

1. Access event, status history, audit record, snapshot đã xuất là **append-only**.
2. Sửa sai bằng **adjustment** mới: lý do, người đề xuất, người duyệt, before/after, thời điểm.
3. Không cấp `UPDATE`/`DELETE` cho role ứng dụng trên các bảng này; audit dùng role chỉ `INSERT`/`SELECT`.
4. Audit có `prev_hash` + `record_hash`; checkpoint định kỳ được ký và lưu **tách quyền** khỏi bảng audit.
5. Verify chain + checkpoint định kỳ; cảnh báo khi lệch.
6. Snapshot tháng bất biến; điều chỉnh sau chốt tạo phiên bản/lô bổ sung.
7. Retention/xóa/ẩn danh theo chính sách đã duyệt; áp dụng cả backup; mọi thao tác ghi audit.

## Consequences

Tích cực:

- Truy vết được ai sửa gì, vì sao.
- Phát hiện can thiệp chuỗi audit.
- Báo cáo tháng tái hiện được.

Tiêu cực / rủi ro:

- Dữ liệu tăng; cần partition/retention.
- Chuỗi hash có thể bị rebuild nếu kẻ tấn công có toàn quyền DB + checkpoint; cần tách quyền.
- Quy trình điều chỉnh phức tạp hơn sửa trực tiếp (đúng ý đồ).

## Alternatives considered

- **UPDATE trực tiếp:** loại vì mất dấu vết.
- **Chỉ hash chain, không checkpoint:** loại vì khó phát hiện sửa toàn bộ chuỗi.
- **Soft delete:** loại vì vẫn có thể ẩn dữ liệu khỏi báo cáo; dùng trạng thái nghiệp vụ thay thế.

## References

FR-A5, FR-R5, FR-S4, NFR-S1/S2, US-10/16, `security-privacy.md`.

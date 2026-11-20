# ADR-009 — RBAC + data scope kiểm tra phía server

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư bảo mật
- Supersedes: —
- Superseded by: —

## Context

Dữ liệu chấm công là PII và có thể bị lạm dụng để sửa quyền/sửa công. UI ẩn nút không ngăn được gọi API trực tiếp. Cần phân quyền theo vai trò **và** phạm vi dữ liệu (self/team/factory/system), có audit.

## Decision

1. Mọi endpoint bảo vệ khai báo `permission` + `scope`; service kiểm tra lại, không tin gateway/UI.
2. Vai trò: `EMPLOYEE`, `SUPERVISOR`, `HR`, `GUARD`, `ADMIN`, `EXECUTIVE`.
3. Scope: `self`, `team`, `factory`, `station`, `system`.
4. Không nhận `employee_ref`, `factory_id`, `role`, `scope` từ client nếu suy ra được từ principal.
5. Áp scope trong SQL/WHERE, không lọc sau khi đọc toàn bộ.
6. Hành động nhạy cảm ghi audit, kể cả khi bị từ chối (theo chính sách).
7. Tách nhiệm vụ: người đề xuất điều chỉnh công không tự phê duyệt nếu chính sách yêu cầu.
8. Admin kỹ thuật **không mặc nhiên** có quyền sửa chấm công; quyền đặc quyền cấp riêng + audit.

## Consequences

Tích cực:

- Giảm leo thang quyền và lộ dữ liệu chéo nhà máy/đội.
- Audit có ngữ cảnh ai/scope nào.
- Có thể kiểm thử ma trận dương/âm.

Tiêu cực / rủi ro:

- Ma trận quyền phức tạp, cần test tự động.
- Cache quyền có thể lệch; cần TTL/thu hồi.
- Một số endpoint cần cân nhắc 403 vs 404 để tránh lộ tồn tại.

## Alternatives considered

- **Chỉ kiểm tra ở gateway:** loại vì defense-in-depth và scope dữ liệu cần service biết.
- **Chỉ ẩn UI:** loại vì không phải authorization.
- **ACL theo từng bản ghi:** quá mịn/khó vận hành ở giai đoạn này.

## References

FR-S1, NFR-S1/PR1, US-15/16, `rbac-matrix.md`.

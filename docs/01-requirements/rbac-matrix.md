# Ma trận RBAC và phạm vi dữ liệu

## 1. Vai trò

`EMPLOYEE`, `SUPERVISOR`, `HR`, `GUARD`, `ADMIN`, `EXECUTIVE`.

## 2. Quyền theo vai trò

Ký hiệu: ✅ được phép · △ có điều kiện/giới hạn · ❌ không được phép.

| Năng lực | Employee | Supervisor | HR | Guard | Admin | Executive |
|---|---|---|---|---|---|---|
| Xem QR của bản thân | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Xem lịch sử ra vào của bản thân | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gửi giải trình | ✅ | ✅ | ✅ | △ | ❌ | ❌ |
| Xem hiện diện đội/ca | ❌ | ✅ | ✅ | ❌ | ❌ | △ |
| Xác minh giải trình | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Quản lý nhân viên + quyền nhà máy | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Cấp/làm mới/thu hồi QR | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Lập báo cáo chấm công tháng | ❌ | △ | ✅ | ❌ | ❌ | ✅ (chỉ đọc) |
| Điều chỉnh công — đề xuất | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Điều chỉnh công — phê duyệt | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Xuất HR / gửi lô | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Giám sát cổng + sự kiện | ❌ | ❌ | △ | ✅ | △ | △ (chỉ đọc) |
| Mở cổng thủ công | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Cấu hình trạm/kiosk | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Quản lý tài khoản, vai trò, scope | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Tra cứu audit | ❌ | ❌ | △ | ❌ | ✅ | ❌ |
| Backup / restore | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Xem báo cáo tổng hợp | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ (chỉ đọc) |

## 3. Phạm vi dữ liệu (scope)

| Vai trò | Scope mặc định | Ghi chú |
|---|---|---|
| Employee | `self` | Chỉ dữ liệu của chính mình; không sửa sự kiện, không mở cổng. |
| Supervisor | `team` + `factory` được giao | Chỉ nhân viên thuộc đội/ca; không cấp quyền hệ thống. |
| HR | `factory` được giao | Theo nhà máy; cơ chế người lập/người duyệt cấu hình được + audit. |
| Guard | `factory` + `station` được giao | Mở thủ công phải nhập lý do; không tự xác nhận công khi thiếu danh tính. |
| Admin | `system` (kỹ thuật) | **Không mặc nhiên** có quyền sửa công; quyền đặc quyền cấp riêng + audit. |
| Executive | `factory`/`all` theo cấp | Chỉ đọc; không điều khiển cổng, không sửa chấm công. |

## 4. Quy ước quyền

Tên quyền theo dạng `<resource>:<action>` hoặc `<resource>:<scope>:<action>`:

- `credential:self:read`, `credential:factory:issue`, `credential:factory:revoke`
- `attendance:self:read`, `attendance:team:read`, `attendance:factory:read`
- `statement:self:create`, `statement:team:verify`
- `adjustment:factory:propose`, `adjustment:factory:approve`
- `station:factory:read`, `station:system:configure`
- `audit:system:read`
- `gate:station:manual-open`
- `export:factory:run`
- `admin:system:manage`

Mỗi endpoint khai báo permission + scope; service kiểm tra lại, không tin gateway/UI.

## 5. Quy tắc thực thi

1. **Gateway** xác thực token và kiểm tra sơ bộ; **service** tự kiểm tra permission + scope trên từng request.
2. Không nhận `employee_ref`, `factory_id`, `role`, `scope` từ client nếu có thể suy ra từ principal.
3. Truy vấn dữ liệu phải áp scope trong SQL/WHERE, không lọc sau khi đã đọc toàn bộ.
4. Trả `403` thay vì `404` khi cần tránh lộ sự tồn tại (theo chính sách từng endpoint); ghi audit khi cần.
5. Thao tác nhạy cảm: `employee`, `credential`, `role`, `scope`, `config`, `adjustment`, `export`, `manual-open`, `audit`, `backup/restore`.
6. Tách nhiệm vụ: người đề xuất điều chỉnh không tự phê duyệt nếu chính sách yêu cầu; quyền được cấu hình có version.
7. Admin kỹ thuật không có quyền sửa chấm công trừ khi được cấp đặc quyền riêng, có lý do và audit.
8. Phiên/token có thời hạn; thay đổi quyền phải có hiệu lực trong thời gian cấu hình.

## 6. Kiểm thử bắt buộc

- Ma trận dương: mỗi vai trò thực hiện được đúng năng lực được phép.
- Ma trận âm: mọi vai trò thử năng lực không được phép → `403`.
- Ngang scope: HR nhà máy BD không đọc được dữ liệu LA; supervisor đội A không đọc đội B.
- Leo thang: employee sửa `employee_ref`/`role` trong request → bị bỏ qua/từ chối.
- Audit: hành động nhạy cảm thành công và bị từ chối đều có bản ghi theo chính sách.
- UI: ẩn nút không thay thế kiểm tra server; test gọi API trực tiếp.

## 7. Truy vết

- Yêu cầu: FR-S1, FR-U1, FR-R5, NFR-S1, NFR-PR1.
- ADR: [ADR-009](../03-decisions/ADR-009-rbac-scope-server-side.md), [ADR-010](../03-decisions/ADR-010-append-only-audit.md).
- Story: US-15, US-16, US-18.

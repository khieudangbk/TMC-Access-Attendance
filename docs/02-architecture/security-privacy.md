# Bảo mật và riêng tư

## 1. Mô hình mối đe dọa (rút gọn)

| Mối đe dọa | Biện pháp |
|---|---|
| Giả mạo QR | Token ký server, TTL ngắn, verify `issuer`/`audience`/`exp`/`key_id` |
| Chụp màn hình / chia sẻ QR | TTL 60 giây, nonce + chống phát lại tại edge; không cam kết chặn tuyệt đối |
| Phát lại token giữa hai nhà máy | Bộ nhớ nonce/usage có phạm vi; cửa sổ đồng bộ; cảnh báo |
| Kiosk bị chiếm | Xác thực kiosk (mTLS/token thiết bị); không cấp quyền dựa vào client; giới hạn mạng |
| Edge bị chiếm | Client cert + allowlist; kho khóa bảo vệ; không chứa private signing key |
| Nghe lén WAN | mTLS edge–central, TLS web, mã hóa ổ đĩa/backup |
| Leo thang quyền | RBAC + scope phía server; kiểm thử âm; không tin header từ client |
| Sửa dữ liệu chấm công | Append-only; quyền DB tối thiểu; audit chain + checkpoint độc lập |
| Lộ PII qua log | Không log raw QR/token/PII; dùng `employee_ref`, `scan_id`, `command_id` |
| Lộ khóa ký | Private key ở KMS/HSM; rotation có `key_id`; không đưa vào repo/client |
| Xóa dấu vết | Audit insert-only, checkpoint ký, backup tách quyền |

## 2. QR token

### Claims tối thiểu

```json
{
  "iss": "tmc-aacs/ics",
  "aud": "tmc-aacs/edge",
  "sub": "emp_7f3a",
  "jti": "nonce-...",
  "iat": 1763618400,
  "exp": 1763618460,
  "kid": "qr-2026-11",
  "ver": 1
}
```

- `sub` là `employee_ref`, **không** phải họ tên/email/số điện thoại.
- TTL mặc định 60 giây; cấu hình được nhưng thay đổi là quyết định bảo mật cần ADR/approval.
- Thuật toán ký bất đối xứng (EdDSA/ES256); kiosk **không** giữ khóa.
- `kid` cho phép rotation; edge giữ public key còn hiệu lực và từ chối khóa lạ.
- Token chỉ dùng một lần trong phạm vi cửa sổ chống phát lại; edge ghi `jti`/usage.

### Không nằm trong token

Họ tên, email, số điện thoại, mã nhân viên HR, ảnh, phòng ban, ca làm, lương. Nếu cần hiển thị, tra cứu server-side theo quyền.

### Phát hành

- Chỉ qua kênh đã xác thực (SSO/app/web/email link có token ngắn hạn).
- Không gửi QR tĩnh qua email; email chỉ gửi liên kết lấy QR động.
- Ghi `qr_issuance_log` (không ghi token) để điều tra.

### Rotation khóa

1. Tạo key mới `kid`, công bố public key tới edge.
2. Chuyển phát hành sang key mới; key cũ chỉ verify trong cửa sổ chuyển tiếp.
3. Thu hồi key cũ sau khi mọi edge xác nhận đã nhận.
4. Khóa bị xâm phạm: thu hồi ngay, phát hành lại credential, audit.

## 3. Xác thực và phân quyền

- Người dùng: OIDC/SSO hoặc tài khoản nội bộ; phiên ngắn, refresh có kiểm soát.
- Dịch vụ: mTLS hoặc client credential; không dùng chung khóa người dùng.
- Kiosk: định danh thiết bị + mTLS; request phải khớp `station_id` được gán.
- Mỗi endpoint khai báo permission + scope; service kiểm tra lại.
- Không nhận `employee_ref`, `factory_id`, `role` từ client khi suy ra được từ principal.
- Truy vấn áp scope trong SQL.
- Từ chối hành động nhạy cảm vẫn ghi audit khi chính sách yêu cầu.

Chi tiết: [RBAC matrix](../01-requirements/rbac-matrix.md).

## 4. Audit

### Bản ghi

`audit_id`, `occurred_at`, `actor` (user/service), `action`, `target_type`, `target_id`, `result`, `request_id`, `source_ip` (nếu cần), `before`/`after` (đã che PII), `prev_hash`, `record_hash`.

### Chuỗi và checkpoint

- `record_hash = H(prev_hash || canonical_record)`.
- Checkpoint định kỳ (ví dụ mỗi giờ/10.000 bản ghi) được ký và lưu **tách quyền** khỏi bảng audit.
- Phát hiện sửa/xóa bằng verify chain + checkpoint.
- Không tuyên bố “bất biến tuyệt đối”: DBA/backup vẫn là bề mặt rủi ro; cần tách quyền và giám sát.

### Hành động bắt buộc audit

Đăng nhập thất bại quan trọng, thay đổi nhân viên/quyền, cấp/thu hồi credential, thay đổi cấu hình trạm/policy, điều chỉnh công, phê duyệt, xuất HR, mở cổng thủ công, thay đổi role/scope, backup/restore, truy cập audit, thay đổi retention.

## 5. Dữ liệu cá nhân

| Dữ liệu | Mục đích | Lưu | Truy cập | Che/mask |
|---|---|---|---|---|
| `employee_ref` | Định danh kỹ thuật | 5 năm | Ứng dụng theo scope | Không hiển thị thô cho vai trò không cần |
| Họ tên | Vận hành, báo cáo | Theo HR/retention | HR, supervisor trong đội | Mask ở log; hiển thị theo quyền |
| Mã HR | Đối soát lương | 5 năm | HR | Không đưa vào token/log |
| Sự kiện vào/ra | Chấm công, an ninh | 5 năm | Self/team/factory theo scope | Không log raw |
| Ảnh/CCCD | **Không thu thập** | — | — | — |
| Sinh trắc học | **Ngoài phạm vi** | — | — | — |

- Trường PII mới phải được phê duyệt: mục đích, retention, scope, masking, cập nhật tài liệu này.
- Backup phải áp cùng chính sách retention/xóa/ẩn danh.
- Yêu cầu xóa/ẩn danh: thực hiện theo chính sách đã duyệt, giữ bản ghi tối thiểu cần thiết cho pháp lý/audit, ghi audit.

## 6. Logging

**Được log:** `request_id`, `scan_id`, `command_id`, `event_id`, `batch_id`, `employee_ref` (khi cần điều tra), `station_id`, `reason_code`, `policy_version`, latency, outcome.

**Không log:** raw QR/token, JWT, mật khẩu, khóa, full request body chứa token, PII không cần thiết, nội dung email.

- Log có cấu trúc, mức log phù hợp, không ghi ở mức DEBUG cho đường quét ở production.
- Bảo vệ log bằng quyền truy cập; retention log ngắn hơn dữ liệu nghiệp vụ.

## 7. Mã hóa và quản lý khóa

- TLS 1.2+ cho web; mTLS edge–central.
- Mã hóa ổ đĩa/volume và backup.
- Private signing key ở KMS/HSM; ứng dụng chỉ yêu cầu ký qua API.
- Secret qua secret manager/env, không commit; xoay định kỳ và khi có sự cố.
- Tách quyền: người vận hành DB không đồng thời duyệt thay đổi nghiệp vụ.

## 8. Chế độ offline

- Chỉ tự cấp quyền khi snapshot còn hạn (≤ 30 phút) và store ghi được.
- Hết hạn: dừng tự động, chuyển quy trình bảo vệ có nhật ký; không mở tất cả cổng.
- Hành động thủ công phải ghi ai, lý do, thời điểm, kết quả.
- Khi online lại: đối soát, bổ sung, cảnh báo các quyết định dựa trên dữ liệu cũ.

## 9. Checklist review bảo mật

- [ ] Endpoint mới có permission + scope và test âm.
- [ ] Không log raw token/PII; error message không lộ chi tiết.
- [ ] Không thêm bypass “tạm thời”.
- [ ] ID/token không dùng lại; nonce/exp được kiểm.
- [ ] Migration không mở quyền ghi audit.
- [ ] Backup/restore đã tính đến mã hóa và retention.
- [ ] Thay đổi policy/khóa có audit và rollback.

## 10. Truy vết

ADR-004, ADR-009, ADR-010, NFR-S1/S2/PR1, FR-U3, FR-S4, US-15, US-16.

# ADR-004 — QR ký số, ngắn hạn, không chứa PII trực tiếp

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư bảo mật, chủ dự án
- Supersedes: —
- Superseded by: —

## Context

QR là phương tiện nhận diện tại cổng. Nếu QR tĩnh hoặc chứa thông tin cá nhân, rủi ro chụp màn hình/chia sẻ và lộ dữ liệu tăng. Kiosk không phải thiết bị tin cậy để giữ khóa ký. Cần cân bằng giữa an toàn và trải nghiệm (không bắt nhân viên thao tác quá nhiều).

## Decision

1. QR là **token do server ký bất đối xứng**, TTL mục tiêu **60 giây**.
2. Claims tối thiểu: `iss`, `aud`, `sub` (`employee_ref`), `jti`, `iat`, `exp`, `kid`, `ver`.
3. **Không** chứa họ tên, email, số điện thoại, mã HR, ảnh, phòng ban, ca, lương.
4. Kiosk/edge chỉ giữ **public key** và verify; private key ở KMS/HSM, không vào repo/client.
5. Hỗ trợ rotation bằng `kid`; edge từ chối khóa lạ/hết hiệu lực.
6. Chống phát lại trong phạm vi edge bằng `jti`/nonce + cửa sổ thời gian; không tuyên bố chặn tuyệt đối chụp màn hình.
7. Email chỉ gửi **liên kết lấy QR động**, không gửi QR tĩnh.

## Consequences

Tích cực:

- Giảm giá trị của QR bị lộ; không lộ PII trực tiếp.
- Rotation khóa không cần đổi kiosk.
- Thu hồi credential có hiệu lực khi edge nhận snapshot mới.

Tiêu cực / rủi ro:

- Cần đồng bộ public key/snapshot và xử lý clock skew.
- Người dùng cần kết nối để lấy QR mới (trừ khi có cơ chế offline được phê duyệt).
- TTL quá ngắn gây ma sát; quá dài tăng rủi ro. 60 giây là mặc định, thay đổi cần phê duyệt.

## Alternatives considered

- **QR tĩnh:** loại vì lộ vĩnh viễn, không thu hồi hiệu quả.
- **QR chứa PII:** loại vì vi phạm tối thiểu hóa dữ liệu.
- **Khóa đối xứng trên kiosk:** loại vì kiosk bị chiếm sẽ lộ khóa ký.

## References

FR-U2/U3, NFR-PR1/S2, US-01/US-08, `security-privacy.md`.

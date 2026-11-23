# QR và Gate Station

API nghiệp vụ dùng [envelope chung](api-response.md), DTO nằm trong `data`. JWKS giữ JSON key set gốc.

## QR động

QR Service dùng RSA key set riêng với OAuth2 Identity. Tạo một lần từ root backend:

```powershell
.\tools\new-signing-key.ps1 -FileName qr-jwks.json
```

`QR_JWK_SET_FILE` mặc định `file:./.local/qr-jwks.json`; dùng đường dẫn tuyệt đối nếu chạy từ thư mục khác. `QR_ISSUER` mặc định `tmc-qr`; `QR_TTL` trong khoảng 30–60 giây. Phân phối cùng key set tới các instance QR; chỉ một private key hiện hành, public key cũ được giữ để xác minh token còn hạn. Không dùng key QR làm key JWT truy cập API.

| API tại QR Service | Quyền | Contract |
|---|---|---|
| `POST /me/qr` | `qr.issue` và JWT nhân viên | Body `{ "factoryId": "UUID" }`; trả `qrId`, token, expiresAt |
| `DELETE /me/qr` | `qr.issue` và JWT nhân viên | Thu hồi các QR của chính nhân viên tại factory đó |
| `POST /internal/qr/verifications` | `qr.verify` | Body token + factoryId; trả valid/reason và ID tham chiếu nếu hợp lệ |
| `GET /internal/qr/jwks` | `qr.verify` | Chỉ public keys |

Gateway prefix `/api/qr`; ví dụ `POST /api/qr/me/qr`. Nhân viên không truyền employee ID tùy ý; service dùng `sub` khớp claim `employee_id`. Trước khi cấp QR, adapter Identity kiểm quyền nhà máy hiện tại. Nếu adapter chưa bật hoặc dependency lỗi thì từ chối an toàn.

JWT QR có type `tmc-qr+jwt`, RS256, `kid`, issuer, audience `tmc-gate-qr`, purpose `factory-access`, version 1, subject employee ID, jti QR ID, factory ID, iat/exp. Verification kiểm chữ ký, purpose, site, expiry và credential hiện còn hiệu lực trong PostgreSQL. Không lưu raw token vào DB hoặc audit.

Cấp QR mới thu hồi QR cũ của cùng employee/factory trong transaction có khóa cạnh tranh nhiều instance. Đổi màn hình QR thường xuyên phải lấy token mới từ API. QR Service chỉ xác minh; Access Event là nơi tiêu thụ QR một lần nguyên tử. Verification hợp lệ nhiều lần không đồng nghĩa được mở cổng nhiều lần. Quyền employee/site được AccessPolicy kiểm lại trước khi cấp lệnh.

Thu hồi có hiệu lực với các lần verification bắt đầu sau khi transaction thu hồi đã commit. Kết quả verification trước đó là snapshot tại thời điểm đọc: scan đang xử lý có thể commit quyết định sau lần thu hồi xảy ra đồng thời, trong giới hạn transaction Access 2 giây và QR còn hạn. Không có transaction xuyên QR DB và Access DB, vì vậy không cam kết thu hồi tức thì hoặc hủy ngược quyết định/lệnh đã cấp. Reissue cũng theo ngữ nghĩa này. Bước E2E phải thử interleaving verify/revoke/consume và xác nhận policy; nếu cần thu hồi tuyến tính tới thời điểm cấp lệnh phải đổi protocol thành reservation/finalization có owner rõ ràng, không giả định transaction cục bộ đã giải quyết.

## Gate Station

Gate sở hữu factory và station; station gồm `factoryId`, mã, `deviceId`, hướng `ENTRY`/`EXIT`, `controllerReference`, active và revision. Controller reference là mã cấu hình của adapter kiosk, không phải URL tùy ý để backend gọi vào LAN.

| API tại Gate Service | Quyền | Contract |
|---|---|---|
| `POST /factories` | `stations.manage` | code/name; tạo factory đang hoạt động |
| `PUT /factories/{id}/status` | `stations.manage` | active; khóa factory làm station check từ chối |
| `POST /stations` | `stations.manage` | factoryId/code/deviceId/direction/controllerReference |
| `GET /stations/{id}` | `stations.manage` | Đọc cấu hình trạm |
| `PUT /stations/{id}/status` | `stations.manage` | active + expectedRevision; revision cũ trả 409 |
| `POST /stations/{id}/heartbeat` | `access.scan` | JWT phải có sub=deviceId và station_id khớp trạm |
| `POST /internal/stations/{id}/checks` | `stations.verify` | deviceId; trả factory/hướng/version/allowed/reason |

Gateway prefix `/api/stations`, ví dụ `POST /api/stations/factories` hoặc `POST /api/stations/stations`. Chưa thay quy ước route hiện tại. Quản trị cấu hình là scope riêng, không cấp cho kiosk hoặc service verifier.

Unique constraint ngăn device gắn vào nhiều trạm; foreign key nội bộ ngăn station trỏ factory không có. Thay đổi quản trị ghi audit cùng transaction. Identity và Gate đều giữ binding theo trách nhiệm của mình; AccessPolicy chỉ cho phép khi cả hai khớp và còn hoạt động. Gate heartbeat chưa tự kiểm trạng thái Identity mới nhất; quyền quyết định truy cập nằm ở AccessPolicy, không dùng heartbeat làm bằng chứng cấp quyền.

## Credential nội bộ và AccessPolicy

Admin Identity gọi `POST /service-clients` với clientId dạng `service-...`, secret riêng và scopes cần thiết. `PUT /service-clients/{id}/status` khóa/bật việc cấp token mới; JWT đã cấp vẫn có TTL, không bị thu hồi chữ ký tức thì.

| Caller | Scope tối thiểu |
|---|---|
| QR Service | `identity.verify` |
| Access Event | `identity.verify qr.verify stations.verify` |
| Công cụ quản trị trạm được cấp riêng | `stations.manage` |

Không dùng credential kiosk hoặc cùng một credential bootstrap cho mọi service. Với `.env` của QR và Access, bật `INTERNAL_APIS_ENABLED=true`, đặt `INTERNAL_CLIENT_ID`, `INTERNAL_CLIENT_SECRET`, `IDENTITY_TOKEN_URI`, `IDENTITY_API_URL`. Access thêm `STATION_API_URL`, `QR_API_URL`. Mặc định chưa bật để setup chưa provision không tự cho phép quét. Credential chỉ nằm trong môi trường/private `.env`, không trong YAML hoặc Git.

Adapter dùng OAuth client credentials, cache token tới gần expiry, không retry network tự động. Timeout mỗi HTTP request 250ms, connect 150ms; Access scan transaction timeout 2 giây. Access kiểm device hiện hành → station/factory/hướng tin cậy → QR ký/thu hồi → quyền employee/site. Response sai ID, thiếu dữ liệu bắt buộc hoặc dependency lỗi không tạo lệnh. Các URL triển khai phải là endpoint dịch vụ tin cậy, HTTPS hoặc kênh mạng riêng bảo vệ; local HTTP chỉ để phát triển.

Các giới hạn timeout là chính sách ban đầu cần đo với topology thực; không phải bằng chứng kiosk đạt ≤2 giây. Token cache cần credential lifecycle và giám sát; service JWT cũ vẫn dùng tới expiry khi client bị khóa. Rate limiting và authentication hardening thuộc nghiệm thu tiếp.

## Hợp đồng thiết bị và giới hạn

Kiosk gọi API turnstile trong LAN sau khi backend đã commit lệnh. Adapter phải truyền commandId ổn định; cùng ID trả cùng kết quả, không thực thi mở cổng lần hai. Timeout không rõ kết quả là UNKNOWN; chỉ truy vấn trạng thái/đối soát, không tự tạo command mới. Chỉ cảm biến passage tin cậy mới gửi PASSED sau OPENED. Mất WAN thì không tự cấp quyền offline; request/bằng chứng pending cần journal bền vững tại client.

Chưa có mã kiosk/adapter phần cứng hoặc API nhà cung cấp; hợp đồng này cần simulator và nghiệm thu tích hợp. Các test HTTP contract dùng server mô phỏng không thay thế end-to-end với bốn service thật, thiết bị, RabbitMQ hoặc kiểm thử tải.

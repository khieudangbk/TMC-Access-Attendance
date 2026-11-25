# Postman collections

Import các file `*.postman_collection.json` vào Postman. Mỗi collection có biến `baseURL` theo port local; collection API Gateway dùng prefix `/api/identity`, `/api/qr`, `/api/stations`, `/api/access`.

- Identity & User: OAuth2, nhân viên, thiết bị, service client và API nội bộ.
- QR: cấp, thu hồi, xác minh QR và public key.
- Gate Station: nhà máy, trạm, heartbeat và xác minh trạm.
- Access Event: scan, xác nhận lệnh cổng và review bằng chứng.
- API Gateway: các request đi qua Gateway; API nội bộ gọi trực tiếp service.
- Config Server, Discovery Server: health và endpoint hạ tầng chỉ đọc.

Điền token đúng vai trò vào `adminToken`, `employeeToken`, `deviceToken`, `serviceToken`; token thiết bị phải gắn đúng `stationId`. Service token cần scope tương ứng API nội bộ. Lấy token người dùng bằng Authorization Code + PKCE qua Identity (trình duyệt đăng nhập), không dùng password grant. Request đổi code PKCE dành cho public client; client credentials dùng HTTP Basic.

Điền `clientId`, `clientSecret`, `scope` trước khi xin service/device token; đăng ký client và scope phù hợp trước. Các biến mật khẩu và token để trống, không lấy từ `.env` hoặc commit credential thật.

Luồng tham khảo: lấy admin token → tạo factory → tạo employee có quyền factory → tạo station → đăng ký device gắn station → lấy employee/device token → cấp QR → scan → xác nhận OPENED/PASSED. Sao chép ID và token từ response vào biến tương ứng; response nghiệp vụ có wrapper `data`. Review và API nội bộ là các nhóm kiểm tra bổ sung.

`scanId`, `confirmationId`, `decisionId` là UUID: tạo mới cho thao tác mới, giữ nguyên khi retry cùng thao tác. `occurredAt` dùng UTC ISO-8601 thực tế. `expectedRevision` lấy từ station hiện tại. QR có TTL ngắn nên cấp lại trước khi scan. Các request thay đổi status, quyền, thu hồi và review chạy thủ công khi cần; không chạy toàn collection như một kịch bản tuần tự.

Config Server và Eureka dùng `basicUsername`/`basicPassword` riêng. Đây là collection mẫu đối chiếu mã nguồn, chưa phải kết quả kiểm thử hệ thống đang chạy.

# Response API nghiệp vụ

Thư viện `libraries/api-support` cung cấp format dùng chung cho 7 service nghiệp vụ. OAuth2 endpoints, metadata, JWKS và Actuator giữ response theo giao thức, không bọc envelope. Gateway chuyển tiếp response từ service đích.

## Thành công

```json
{
  "code": "SUCCESS",
  "message": "Xử lý thành công",
  "data": {"allowed": true},
  "timestamp": "2026-11-21T08:40:00Z",
  "traceId": "48aee738-5b58-41ee-a095-97be1bf9c192"
}
```

`code` là mã nghiệp vụ ổn định; `message` là mô tả cho người đọc; `data` là DTO hoặc null; `timestamp` là thời điểm tạo response UTC; `traceId` dùng đối chiếu request. HTTP status giữ đúng 200/201/400/401/403/404/409/503/500 tùy kết quả, không trả mọi lỗi dưới HTTP 200.

Quyết định scan bị từ chối theo nghiệp vụ vẫn là request xử lý thành công: HTTP 200, code SUCCESS, `data.allowed=false` và `data.reason` giải thích. Lỗi xác thực, payload, dependency hoặc xung đột dùng status lỗi tương ứng.

`X-Correlation-ID` hợp lệ (chữ/số/dấu gạch ngang, tối đa 64 ký tự) được giữ làm traceId; thiếu hoặc sai format thì server tạo UUID mới. Response trả lại header này. Đây là correlation identifier, không phải bằng chứng distributed tracing/trace span đã được triển khai.

Retry cùng scan/confirmation ID giữ nguyên kết quả nghiệp vụ trong `data`. `timestamp` và `traceId` thuộc request HTTP hiện tại nên không yêu cầu trùng với lần trước.

## Lỗi validation

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "data": null,
  "timestamp": "2026-11-21T08:40:00Z",
  "traceId": "48aee738-5b58-41ee-a095-97be1bf9c192",
  "errors": [{"field": "stationId", "message": "Giá trị không hợp lệ"}]
}
```

`errors` chỉ xuất hiện khi có lỗi field. Không trả rejected value, password, QR token, SQL hoặc stack trace. Lỗi bảo mật API có code `UNAUTHORIZED`/`FORBIDDEN`; thiếu dependency trả 503 với code an toàn. Các mã cụ thể như `IDEMPOTENCY_CONFLICT` được giữ ở `code`.

`HEAD`, HTTP 204 và 304 không có JSON body theo ngữ nghĩa HTTP. Gateway dùng chung security handlers cho lỗi xác thực/phân quyền của chính nó; payload từ service đích được chuyển tiếp, không bọc envelope lần hai.

## Phân trang

Danh sách phân trang trả:

```json
{
  "code": "SUCCESS",
  "message": "Xử lý thành công",
  "data": {"items": [], "limit": 50, "offset": 0, "total": 0},
  "timestamp": "2026-11-21T08:40:00Z",
  "traceId": "48aee738-5b58-41ee-a095-97be1bf9c192"
}
```

## Build và client nội bộ

Trước lần build đầu từ clone mới, cài shared library vào Maven local:

```powershell
mvn -f .\libraries\api-support\pom.xml install
```

Sau đó build/test/chạy từng service bằng POM riêng. Không có parent POM tại root. CI cần build/publish shared artifact trước các service; đóng gói ứng dụng vẫn có JAR/image riêng khi triển khai.

HTTP clients nội bộ đọc DTO từ `data`, kiểm `code=SUCCESS` và từ chối response thiếu/null/sai schema. Token response ở `/oauth2/token` và key response JWKS không có envelope. Contract tests phải theo cùng quy ước; không hỗ trợ song song raw DTO để che sai lệch protocol.

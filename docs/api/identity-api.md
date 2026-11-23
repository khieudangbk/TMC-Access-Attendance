# Identity & User API

Gateway chuyển `/api/identity/**` về Identity sau khi bỏ hai segment đầu. Authorization Server công bố issuer từ `IDENTITY_ISSUER`; client lấy metadata/JWKS theo issuer đó, không tự đoán URL qua Gateway.

API nghiệp vụ dùng [envelope chung](api-response.md), DTO nằm trong `data`; danh sách nhân viên có `items`, `limit`, `offset`, `total`. OAuth2 và JWKS vẫn trả format giao thức gốc.

## Thiết lập ban đầu

Chạy từ root backend để tạo khóa một lần:

```powershell
.\tools\new-signing-key.ps1
```

Khóa RSA riêng nằm tại `.local/identity-jwks.json`, được giới hạn quyền file và bỏ qua bởi Git. Script từ chối ghi đè. Đặt `IDENTITY_JWK_SET_FILE` tới file tương ứng khi working directory khác root, hoặc dùng URI tuyệt đối `file:///...`. Tất cả instance Identity cần cùng key set; không sinh khóa mới mỗi lần khởi động. Backup và cấp quyền đọc chỉ cho tài khoản chạy dịch vụ.

Key set có đúng một RSA private key đang ký, cùng các public key cũ còn cần xác minh. Mỗi key có `kid` duy nhất và tối thiểu 2048 bit. Khi rotation: chuẩn bị key set mới với private key mới + public key cũ, phân phối đồng nhất rồi restart có kiểm soát; giữ public key cũ ít nhất tới khi token liên quan hết hạn. Endpoint JWKS chỉ công bố phần public. Không commit key set riêng hoặc đưa vào Config Server.

Trong `.env` riêng của Identity, đặt `IDENTITY_BOOTSTRAP_USERNAME` và `IDENTITY_BOOTSTRAP_PASSWORD` để tạo admin khi database chưa có account. Mật khẩu tối thiểu 12 ký tự và tối đa 72 byte UTF-8. Nếu database đã có account, bootstrap không ghi đè account hoặc mật khẩu. Bỏ thông tin bootstrap sau khi đã cấp tài khoản quản trị. Không có mật khẩu admin mặc định trong source.

OAuth clients và authorization/consent lưu bằng JDBC, schema do Flyway quản lý theo schema Spring Security 7.2 đã điều chỉnh PostgreSQL. Client kỹ thuật bootstrap lấy ID/secret từ `LOCAL_OAUTH_CLIENT_ID`/`LOCAL_OAUTH_CLIENT_SECRET`; chỉ tạo nếu chưa có, không đổi secret từ `.env` ở mỗi lần restart. Client này có `identity.verify`, `qr.verify`, `stations.verify`, không có quyền quản trị nhân viên. Dùng credential riêng theo service khi hoàn thiện provisioning production; không dùng chung với kiosk.

## Authentication

- Nhân viên/admin: Authorization Code + PKCE S256 qua `/oauth2/authorize`, form `/login`, đổi code ở `/oauth2/token`. Client public `tmc-web`, redirect đăng ký bằng `IDENTITY_WEB_REDIRECT_URI`. Không dùng password grant. Redirect phải khớp chính xác và dùng HTTPS khi triển khai ngoài local.
- Token nhân viên có `sub=employeeId`, `employee_id`, `roles`, `factory_ids` và audience `tmc-api`; TTL 5 phút. Claims phản ánh dữ liệu tại lúc phát token. API quản trị Identity kiểm lại trạng thái/role trong DB, không chỉ tin role cũ trên JWT.
- Kiosk: client credentials, secret băm trong DB. `sub=clientId`, `station_id` lấy từ đăng ký thiết bị, scope chỉ `access.scan`/`access.confirm`, TTL 60 giây. Khóa thiết bị ngăn phát token mới ngay. Các service nhận token vẫn cần kiểm tra trạng thái thiết bị hiện tại trong policy; token cũ không tự mất hiệu lực chữ ký khi khóa thiết bị.
- Technical service: client credentials với scope nội bộ. Không được gọi API quản trị bằng token service.

OAuth2 do Spring Security Authorization Server xử lý. Session login không đồng nghĩa đã được cấp quyền truy cập mọi API; các API nghiệp vụ dùng Bearer JWT stateless. JWT phải đúng issuer và chữ ký; các scope nghiệp vụ tiếp tục được kiểm tra tại service đích.

## API nghiệp vụ

| Method và path tại Identity | Quyền | Nội dung |
|---|---|---|
| `POST /employees` | Admin hiện hành | Tạo nhân viên/account, role và danh sách factory IDs |
| `GET /employees?limit=50&offset=0` | Admin hiện hành | Danh sách tối đa 100 nhân viên mỗi request |
| `GET /employees/{employeeId}` | Chính mình đang hoạt động hoặc admin | Thông tin không chứa username/password hash |
| `PUT /employees/{employeeId}/profile` | Admin | Sửa `fullName` |
| `PUT /employees/{employeeId}/permissions` | Admin | Thay `roles`, `factoryIds`; không cho xóa admin cuối cùng |
| `PUT /employees/{employeeId}/status` | Admin | `{ "active": false }`; không vô hiệu hóa admin cuối cùng |
| `POST /devices` | Admin | Đăng ký `clientId`, `secret`, `stationId` |
| `PUT /devices/{clientId}/status` | Admin | Khóa/bật client kiosk |
| `POST /service-clients` | Admin | Provision client riêng, chỉ các scope nội bộ/quản trị trạm được hỗ trợ |
| `PUT /service-clients/{clientId}/status` | Admin | Khóa/bật phát token service mới |
| `POST /internal/access-checks` | `identity.verify` | Kiểm tra employee hiện hoạt động và có grant tại factory |
| `GET /internal/devices/{clientId}` | `identity.verify` | Danh tính, station và trạng thái kiosk hiện tại |

Role hợp lệ: `EMPLOYEE`, `SUPERVISOR`, `HR`, `SECURITY`, `ADMIN`, `EXECUTIVE`. Factory IDs là tham chiếu miền Gate Station, không có join/FK xuyên database; AccessPolicy còn phải kiểm cấu hình nhà máy/trạm thực tế.

Ví dụ tạo nhân viên:

```json
{
  "employeeCode": "EMP-001",
  "fullName": "Nguyen Van A",
  "username": "employee001",
  "password": "<set-private-password>",
  "roles": ["EMPLOYEE"],
  "factoryIds": ["846ee5de-d5dc-4bf4-871a-1bc855355c7b"]
}
```

Ví dụ đăng ký thiết bị: `clientId` dạng `kiosk-<mã>`, secret riêng tối thiểu 12 ký tự, `stationId` UUID. Không nhận station từ tham số xin token. Mỗi station có một device binding trong Identity; Gate Station phải có binding khớp trước khi cho phép scan.

Request kiểm quyền:

```json
{
  "employeeId": "09fd40d9-980a-49b3-bb9f-1c1cdd8949a0",
  "factoryId": "846ee5de-d5dc-4bf4-871a-1bc855355c7b"
}
```

Response chứa `employeeId`, `factoryId`, `allowed`, `reason`; mã từ chối gồm `EMPLOYEE_DISABLED`, `SITE_ACCESS_DENIED`, `EMPLOYEE_NOT_FOUND`. Không trả hồ sơ cá nhân qua API kiểm quyền.

## Dữ liệu và giới hạn

Mật khẩu/secret dùng delegating BCrypt encoder. Các thay đổi employee, quyền và device có audit actor/event/target/thời gian trong cùng transaction tại `identity_audit_events`, phần bằng chứng không được sửa/xóa. Chuyển audit sang MongoDB qua RabbitMQ và audit các hành vi từ chối còn thuộc phase reliability; chưa coi audit toàn hệ thống đã hoàn tất.

Private signing key cần tồn tại trước khi chạy. Thiếu/không hợp lệ thì ứng dụng không phát token. Session phân tán, rate limiting login, provisioning secret theo service và workflow đối soát cần hoàn thiện trong các bước tích hợp/triển khai; schema và signing file bền vững không tự chứng minh HA.

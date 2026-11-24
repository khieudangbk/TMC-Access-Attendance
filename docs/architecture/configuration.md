# Cấu hình tập trung

## 1. Hai tầng cấu hình

Mỗi Config Client giữ thông tin khởi động trong `services/<service>/src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: qr-service
  config:
    import:
      - "optional:file:./.env[.properties]"
      - "optional:file:./services/qr-service/.env[.properties]"
      - "classpath:config-client.yaml"
```

`config-client.yaml` đặt cạnh `application.yaml` và được nạp sau các file `.env`:

```yaml
spring:
  config:
    import: "configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
  cloud:
    config:
      username: ${CONFIG_SERVER_USERNAME:config-client}
      password: ${CONFIG_SERVER_PASSWORD:local-config-password}
```

Tên `spring.application.name` phải khớp tên file YAML trong kho tập trung. Import là bắt buộc, vì port/database/security lấy từ Config Server. Config Server được chạy trước các client.

Hai import `optional:file:` nạp `.env` riêng bằng Properties loader của Spring Boot. Khi chạy từ thư mục ứng dụng, dùng `./.env`; khi chạy từ `backend`, dùng `./services/<service>/.env`. Từ thư mục khác, đặt working directory về một trong hai vị trí này. Không dùng một `.env` chung ở `backend`; nếu cả hai vị trí có file, file trong thư mục service được ưu tiên.

Tách `config-client.yaml` thành một tầng import riêng giúp URL và credential trong `.env` sẵn sàng trước khi Config Client kết nối. Config Server chỉ import `.env` của chính nó, không dùng `config-client.yaml` hoặc phụ thuộc vào chính endpoint của nó.

Mỗi ứng dụng có `.env.example` tương ứng. Ví dụ `services/qr-service/.env` chứa credential Config Server/Eureka, issuer/JWKS, RabbitMQ, port QR, URL/user/password PostgreSQL của QR và QR TTL. Config Server chỉ giữ credential/kho native của chính nó trong `.env`; không gom credential database của mọi service vào đây.

Định dạng file là `KEY=value`, dùng comment bắt đầu bằng `#`, không thêm `export` hoặc bọc giá trị bằng dấu nháy như script shell. `.env` được bỏ qua bởi Git, `.env.example` được commit. Dùng cùng credential Config Server giữa server và các client; các giá trị trong mẫu đều giả cho local.

Kho cấu hình nằm tại:

```text
services/config-server/src/main/resources/configurations/
├── application.yaml
├── discovery-server.yaml
├── api-gateway.yaml
├── identity-user-service.yaml
├── qr-service.yaml
├── gate-station-service.yaml
├── access-event-service.yaml
├── attendance-report-service.yaml
├── hr-integration-service.yaml
└── audit-notification-service.yaml
```

Đường dẫn trên là vị trí mã nguồn trong repository. Config Server dùng `classpath:/configurations/` để đọc các tài nguyên đã đóng gói; không thêm `services/` vào đường dẫn classpath này.

`application.yaml` là cấu hình chung. File theo tên service bổ sung/ghi đè các giá trị cùng khóa trong cấu hình chung. Ví dụ QR nhận cấu hình chung cộng `qr-service.yaml`. Biến môi trường có thể thay giá trị placeholder tại ứng dụng sử dụng cấu hình.

## 2. Config Server

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `CONFIG_SERVER_PORT` | `8888` | Port Config Server |
| `CONFIG_REPOSITORY` | `classpath:/configurations/` | Vị trí kho native |
| `CONFIG_SERVER_USERNAME` | Mẫu `.env`: `tmc-config-local`; YAML fallback: `config-client` | Tài khoản HTTP Basic |
| `CONFIG_SERVER_PASSWORD` | Giá trị local trong YAML | Mật khẩu HTTP Basic |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | URL bootstrap tại client |

Nếu đổi username/password, cung cấp cùng giá trị cho Config Server và tất cả client. Khi dùng kho classpath, khởi động lại Config Server sau khi đổi file để nạp nội dung mới. Có thể đặt `CONFIG_REPOSITORY` thành URI `file:///D:/.../configurations/` để sử dụng thư mục ngoài.

Config Server dùng `@EnableConfigServer`, profile `native`. Endpoint `/{application}/{profile}` yêu cầu HTTP Basic; ví dụ `http://localhost:8888/qr-service/default`. Health endpoint ở `http://localhost:8888/actuator/health`.

## 3. Port ứng dụng

| Ứng dụng | Biến môi trường | Mặc định |
|---|---|---:|
| Config Server | `CONFIG_SERVER_PORT` | 8888 |
| Discovery | `DISCOVERY_SERVER_PORT` | 8761 |
| Gateway | `API_GATEWAY_PORT` | 8080 |
| Identity & User | `IDENTITY_USER_PORT` | 8081 |
| QR | `QR_SERVICE_PORT` | 8082 |
| Gate Station | `GATE_STATION_PORT` | 8083 |
| Access Event | `ACCESS_EVENT_PORT` | 8084 |
| Attendance Report | `ATTENDANCE_REPORT_PORT` | 8085 |
| HR Integration | `HR_INTEGRATION_PORT` | 8086 |
| Audit & Notification | `AUDIT_NOTIFICATION_PORT` | 8087 |

## 4. PostgreSQL

| Service | Prefix biến môi trường | Database mặc định | Username mặc định |
|---|---|---|---|
| Identity & User | `IDENTITY_DB` | `identity_user_db` | `identity_app` |
| QR | `QR_DB` | `qr_db` | `qr_app` |
| Gate Station | `GATE_STATION_DB` | `gate_station_db` | `gate_station_app` |
| Access Event | `ACCESS_EVENT_DB` | `access_event_db` | `access_event_app` |
| Attendance Report | `ATTENDANCE_REPORT_DB` | `attendance_report_db` | `attendance_report_app` |
| HR Integration | `HR_INTEGRATION_DB` | `hr_integration_db` | `hr_integration_app` |

Mỗi `.env.example` tách kết nối thành `_HOST`, `_PORT`, `_NAME`, `_USER`, `_PASSWORD`, theo cách tổ chức cấu hình của TicketBooking. YAML ghép thành JDBC URL; có thể đặt thêm `_URL` để ghi đè toàn bộ URL khi cần. `_USERNAME` cũ vẫn được giữ làm fallback tương thích. Ví dụ:

```powershell
$env:QR_DB_HOST = 'localhost'
$env:QR_DB_PORT = '5432'
$env:QR_DB_NAME = 'qr_db'
$env:QR_DB_USER = 'qr_app'
$env:QR_DB_PASSWORD = 'replace-with-your-local-password'
```

URL mặc định dùng `localhost:5432`. Mật khẩu fallback dành cho local là `local-db-password`. Shared YAML cấu hình `spring.jpa.open-in-view=false` và `spring.jpa.hibernate.ddl-auto=validate`.

Cấu hình chung đặt Hikari tối đa 10 connection, tối thiểu 2 connection nhàn rỗi, connection timeout 10 giây và validation timeout 5 giây. Flyway bật, đọc `classpath:db/migration` và validate migration khi chạy. Chỉ các service có starter SQL mới sử dụng nhóm cấu hình datasource/JPA/Flyway này.

## 5. MongoDB và SMTP

Chỉ Audit & Notification sử dụng các cấu hình sau:

| Biến | Mặc định |
|---|---|
| `MONGO_HOST`, `MONGO_PORT` | `localhost`, `27017` |
| `MONGO_DATABASE` | `audit_notification_db` |
| `MONGO_APP_USER`, `MONGO_APP_PASSWORD` | User ứng dụng `audit_app` và password local trong `.env` |
| `MONGODB_URI` | Tùy chọn ghi đè toàn bộ URI; mặc định được ghép từ các biến `MONGO_*`, `authSource` bằng `MONGO_DATABASE` |
| `SMTP_HOST` | `localhost` |
| `SMTP_PORT` | `1025` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Rỗng |
| `SMTP_AUTH` | `false` |
| `SMTP_STARTTLS` | `false` |

Boot 4.2 sử dụng `spring.mongodb.uri` trong cấu hình này. Giá trị MongoDB local đầy đủ nằm trong `audit-notification-service.yaml`. SMTP mặc định có thể dùng với Mailpit local; dùng SMTP thật thì cấu hình lại host, port, credential và TLS theo nhà cung cấp.

## 6. RabbitMQ

| Biến | Mặc định |
|---|---|
| `RABBITMQ_HOST` | `localhost` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USERNAME` | `tmc` |
| `RABBITMQ_PASSWORD` | Giá trị local trong YAML |
| `RABBITMQ_VIRTUAL_HOST` | `/` |

Cấu hình chung bật publisher confirms dạng correlated, publisher returns, mandatory publish, manual consumer ACK, prefetch 20 và không tự requeue vô hạn thông báo bị reject. Khi viết listener, ACK sau khi lưu dữ liệu thành công; retry/DLQ và tên queue/exchange cần được khai báo theo luồng nghiệp vụ tương ứng.

## 7. Eureka

- `EUREKA_URL`: URL registry mà client gọi; mặc định tại `localhost:8761/eureka/`, có credential HTTP Basic local.
- `EUREKA_USERNAME`, `EUREKA_PASSWORD`: credential của Discovery Server.
- `EUREKA_HOSTNAME`: hostname của Discovery Server, mặc định `localhost`.

Nếu đổi credential server, đổi `EUREKA_URL` tương ứng tại các client. Discovery local có `register-with-eureka=false`, `fetch-registry=false`; các ứng dụng khác đăng ký theo tên service. Instance ID gồm tên ứng dụng, địa chỉ IP và port.

## 8. OAuth2/JWT và QR

| Biến | Mặc định | Mục đích |
|---|---|---|
| `IDENTITY_ISSUER` | `http://localhost:8081` | Issuer OAuth2 và JWT |
| `IDENTITY_JWK_SET_URI` | `http://localhost:8081/oauth2/jwks` | Khóa công khai xác minh token API |
| `LOCAL_OAUTH_CLIENT_ID` | `tmc-local-client` | Client kỹ thuật local |
| `LOCAL_OAUTH_CLIENT_SECRET` | Mẫu local trong `.env.example`; YAML không có secret fallback | Secret client kỹ thuật local |
| `IDENTITY_JWK_SET_FILE` | `file:./.local/identity-jwks.json` | Key set riêng bền vững; tạo một lần ngoài Git |
| `IDENTITY_BOOTSTRAP_USERNAME`, `IDENTITY_BOOTSTRAP_PASSWORD` | Rỗng | Admin ban đầu, chỉ tạo khi database chưa có account |
| `IDENTITY_WEB_REDIRECT_URI` | `http://localhost:3000/callback` | Redirect chính xác cho client public PKCE |
| `QR_TTL` | `60s` | Giá trị cấu hình dự kiến cho thời hạn QR |

Client local dùng grant `client_credentials`, scope `identity.verify`, `qr.verify`, `stations.verify`; không có quyền quản trị nhân viên. Client `tmc-web` dùng Authorization Code + PKCE; kiosk có client riêng gắn station, scope `access.scan`/`access.confirm`. Client/OAuth authorization lưu trong PostgreSQL. Signing key phải được cung cấp trước khi chạy; chi tiết bootstrap và rotation tại [identity-api.md](identity-api.md). QR TTL cần được sử dụng bởi phần nghiệp vụ QR khi triển khai; việc đặt YAML không tự sinh token QR.

## 9. Gateway MVC

Route nằm dưới `spring.cloud.gateway.server.webmvc.routes`:

| Prefix ngoài Gateway | Service đích |
|---|---|
| `/api/identity/**` | `identity-user-service` |
| `/api/qr/**` | `qr-service` |
| `/api/stations/**` | `gate-station-service` |
| `/api/access/**` | `access-event-service` |
| `/api/reports/**` | `attendance-report-service` |
| `/api/hr/**` | `hr-integration-service` |
| `/api/audit/**` | `audit-notification-service` |

Mỗi route dùng `lb://<service-name>` và `StripPrefix=2`. Ví dụ `/api/qr/me/qr` được chuyển thành `/me/qr` tại QR Service. Prefix là quy ước định tuyến; API nghiệp vụ vẫn cần controller và cấu hình quyền tương ứng.

Gateway có `SecurityConfig` theo servlet/MVC: stateless Bearer JWT, tắt form login/HTTP Basic tại Gateway và cấu hình CORS tập trung. `CORS_ALLOWED_ORIGINS` trong `services/api-gateway/.env` là danh sách origin phân tách bằng dấu phẩy; mẫu cho phép `http://localhost:3000` và `http://localhost:5173`. Browser gửi Bearer token bằng header Authorization; cấu hình hiện không bật cookie cross-origin.

Các đường dẫn được phép đi qua Gateway mà không cần Bearer token:

- GET `/actuator/health` và các health probe con.
- GET `/api/identity/.well-known/**`, `/api/identity/oauth2/jwks`.
- POST `/api/identity/oauth2/token`: Identity vẫn kiểm tra credential OAuth client ở service đích.
- OPTIONS cho CORS preflight; origin/method/header vẫn chịu kiểm tra CORS.

Những route nghiệp vụ khác yêu cầu token. Không mở toàn bộ `/actuator/**` hoặc `/api/internal/**` thành public. JWT vẫn được xác minh tại service đích; Gateway không sở hữu khóa ký bí mật.

## 10. Đổi môi trường

Cung cấp giá trị trong `.env` của đúng service hoặc qua biến môi trường/Run Configuration của IDE. Bộ khung đã khai báo `spring.config.import` cho `.env[.properties]`, nên Java nạp file qua Spring Boot khi working directory đúng. Đây là property source của ứng dụng, không phải thao tác export toàn bộ giá trị vào môi trường hệ điều hành.

Khi các ứng dụng chạy trong container, `localhost` trỏ về chính container đó. Đổi URL/host sang DNS service tương ứng qua biến môi trường. Credential fallback trong repository chỉ phục vụ local; cấu hình deployment lấy từ biến môi trường/kho secret.


Attendance và HR dùng credential caller riêng để kiểm role/site hiện tại tại Identity. HR thêm endpoint Attendance, endpoint/API key HR và lịch worker; tên biến, scopes và chính sách deadline tại [Attendance/HR](attendance-hr-api.md#cấu-hình-caller). Giá trị rỗng mặc định từ chối thao tác phụ thuộc, không tự tạo credential giả để bỏ xác thực.

EVENTS_ENABLED bật topology/publisher/consumer của từng service. NOTIFICATIONS_ENABLED chỉ bật SMTP worker; MONITORED_FACTORY_IDS phải đầy đủ trong Attendance và HR. Chi tiết scopes, quyền broker/Mongo và quy trình tại [events-audit.md](events-audit.md).

# Setup và chạy local

## 1. Chuẩn bị

- JDK 21 và `JAVA_HOME` trỏ tới JDK này.
- Maven hoặc Maven Wrapper trong từng project.
- PostgreSQL tại port 5432, MongoDB tại 27017, RabbitMQ tại 5672; có thể dùng các server sẵn có và đổi biến môi trường.
- SMTP local tại 1025 hoặc thông tin SMTP riêng cho Audit & Notification.

Mở IDE tại thư mục `backend`, import 10 file `services/<service>/pom.xml` thành các project Maven. Mỗi application chạy bằng main class trong package riêng. Không có POM cha tại `backend`.

Cài thư viện response trước khi build/chạy các service nghiệp vụ:

```powershell
mvn -f .\libraries\api-support\pom.xml install
mvn -f .\libraries\event-support\pom.xml install
```

IDE cũng cần import POM library này; đây là shared artifact, không phải ứng dụng thứ 11. Quy ước response nằm tại [api-response.md](api-response.md).

### File `.env` của từng ứng dụng

Từ `backend`, tạo file local từ mẫu sau khi clone, giữ nguyên file đã có:

```powershell
Get-ChildItem -LiteralPath .\services -Directory | ForEach-Object {
    $sample = Join-Path $_.FullName '.env.example'
    $local = Join-Path $_.FullName '.env'
    if ((Test-Path -LiteralPath $sample) -and -not (Test-Path -LiteralPath $local)) {
        Copy-Item -LiteralPath $sample -Destination $local
    }
}
```

Sửa `.env` trong từng service để khớp hạ tầng của bạn. Mẫu đã điền URL, database, username, password giả và port tương ứng. `.env` không được commit. Spring Boot nạp file qua `spring.config.import` đã khai báo khi chạy từ `backend` hoặc thư mục service; trên IDE đặt Working directory về một trong hai vị trí này.

Các lệnh `mvn -f .\services\<service>\pom.xml` bên dưới chạy từ `backend`. Với Maven Wrapper, chuyển vào đúng thư mục `services/<service>` trước khi chạy `mvnw.cmd`. Khi chạy bằng IDE, đặt Working directory là `backend` hoặc thư mục ứng dụng; không đặt ở thư mục `services`.

## 2. Chuẩn bị dữ liệu và hạ tầng

### PostgreSQL

Tạo sáu database với sáu role tương ứng:

| Database | Owner/login |
|---|---|
| `identity_user_db` | `identity_app` |
| `qr_db` | `qr_app` |
| `gate_station_db` | `gate_station_app` |
| `access_event_db` | `access_event_app` |
| `attendance_report_db` | `attendance_report_app` |
| `hr_integration_db` | `hr_integration_app` |

Ví dụ SQL chạy bằng tài khoản quản trị cho QR, ngoài transaction:

```sql
CREATE ROLE qr_app LOGIN PASSWORD 'replace-with-your-local-password';
CREATE DATABASE qr_db OWNER qr_app;
REVOKE CONNECT ON DATABASE qr_db FROM PUBLIC;
```

Lặp theo bảng và sửa `<PREFIX>_DB_PASSWORD` trong `.env` của service khớp mật khẩu đã tạo. Username và URL mặc định được liệt kê trong [cấu hình](configuration.md#4-postgresql). Migration phát triển trong từng service; không thêm schema dùng chung để các service truy cập chéo.

### MongoDB

Chạy `infra/mongodb/audit-role.js` bằng tài khoản quản trị để tạo collection/index/role giới hạn, rồi tạo user ứng dụng:

```javascript
use audit_notification_db
db.createUser({
  user: 'audit_app',
  pwd: 'replace-with-your-local-password',
  roles: [{ role: 'tmcAuditApplication', db: 'audit_notification_db' }]
})
```

Đặt `MONGO_HOST`, `MONGO_PORT`, `MONGO_DATABASE`, `MONGO_APP_USER`, `MONGO_APP_PASSWORD` trong `.env` của Audit & Notification. YAML ghép URI với `authSource` bằng database ứng dụng. Có thể dùng `MONGODB_URI` để ghi đè toàn bộ kết nối nếu cần.

### RabbitMQ và SMTP

Tạo user RabbitMQ, gán quyền trên vhost ứng dụng sử dụng; cấu hình hiện dùng vhost mặc định `/`. Đặt `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` phù hợp.

SMTP mặc định hướng tới `localhost:1025`. Có thể dùng Mailpit local hoặc đổi cấu hình SMTP sang nhà cung cấp riêng.

### Script tham khảo khởi tạo dữ liệu

Script [init-databases.sh](../infra/postgres/init-databases.sh) ghi các lệnh tạo sáu database/role, sử dụng `POSTGRES_USER` và `TMC_DB_PASSWORD` khi gọi `psql`. [init-audit.js](../infra/mongodb/init-audit.js) ghi thao tác tạo user MongoDB, lấy password từ `TMC_MONGO_PASSWORD`. Có thể tham khảo các script này để chuẩn bị hạ tầng sẵn có. Chạy khởi tạo một lần trên môi trường mới; đổi biến môi trường ứng dụng không tự đổi password/database đã tồn tại.

## 3. Khởi động Config Server

Từ `backend`, chạy trong terminal riêng:

```powershell
mvn -f .\services\config-server\pom.xml spring-boot:run
```

Kho YAML mặc định được nạp bằng `classpath:/configurations/`, nên việc chuyển thư mục ứng dụng không đổi giá trị `CONFIG_REPOSITORY`. Ứng dụng dùng profile `native`, phục vụ YAML từ `services/config-server/src/main/resources/configurations` tại port 8888. Nếu dùng kho ngoài hoặc credential riêng, đặt `CONFIG_REPOSITORY`, `CONFIG_SERVER_USERNAME` và `CONFIG_SERVER_PASSWORD` trong môi trường chạy.

## 4. Khởi động Eureka

Terminal riêng:

```powershell
mvn -f .\services\discovery-server\pom.xml spring-boot:run
```

Eureka lấy cấu hình từ Config Server, dùng port 8761. Mở `http://localhost:8761` và dùng credential Discovery được cấu hình trong YAML/biến môi trường.

## 5. Khởi động các service nghiệp vụ

Sau khi hạ tầng dữ liệu đã được cấu hình, chạy Identity & User trước để cung cấp issuer/JWKS cho các request có token:

Tạo private signing key một lần từ root backend trước khi chạy Identity:

```powershell
.\tools\new-signing-key.ps1
```

Thiết lập `IDENTITY_JWK_SET_FILE`, bootstrap admin và redirect URI theo [Identity API](identity-api.md). Script giữ khóa trong `.local/` và không ghi đè khóa đã có. Từ thư mục ứng dụng, dùng đường dẫn key tuyệt đối trong `.env` hoặc đặt working directory về root backend.

```powershell
mvn -f .\services\identity-user-service\pom.xml spring-boot:run
```

Các lệnh dưới đây chạy trong terminal riêng cho từng ứng dụng:

```powershell
mvn -f .\services\qr-service\pom.xml spring-boot:run
```

```powershell
mvn -f .\services\gate-station-service\pom.xml spring-boot:run
```

```powershell
mvn -f .\services\access-event-service\pom.xml spring-boot:run
```

```powershell
mvn -f .\services\attendance-report-service\pom.xml spring-boot:run
```

```powershell
mvn -f .\services\hr-integration-service\pom.xml spring-boot:run
```

```powershell
mvn -f .\services\audit-notification-service\pom.xml spring-boot:run
```

Có thể thay Maven cài sẵn bằng `mvnw.cmd` khi chạy từ thư mục project tương ứng:

```powershell
Set-Location .\services\qr-service
.\mvnw.cmd spring-boot:run
```

Các port ứng dụng và biến môi trường nằm trong [configuration.md](configuration.md). Trên IDE có thể dùng `.env` theo Working directory hoặc ghi đè bằng biến trong Run Configuration của đúng application.

## 6. Khởi động Gateway

```powershell
mvn -f .\services\api-gateway\pom.xml spring-boot:run
```

Gateway tại `http://localhost:8080`, tra cứu tên service qua Eureka. Route tường minh nằm trong `configurations/api-gateway.yaml`. Request nghiệp vụ cần token/quyền tương ứng; các API OAuth2 trực tiếp của Identity dùng port 8081 trong cấu hình local.

## 7. Thay đổi cấu hình

1. Sửa YAML của ứng dụng trong `services/config-server/src/main/resources/configurations` hoặc kho native ngoài đã cấu hình.
2. Khởi động lại Config Server để nạp tài nguyên classpath mới khi dùng kho mặc định.
3. Khởi động lại ứng dụng client để nạp cấu hình mới.

Nếu đổi port Identity, cập nhật cả `IDENTITY_ISSUER` và `IDENTITY_JWK_SET_URI`. Nếu đổi credential Eureka, cập nhật `EUREKA_URL` ở client. Nếu đổi credential Config Server, cập nhật cùng giá trị cho server và client.

## 8. Dừng ứng dụng

Dừng từng Run Configuration trong IDE hoặc nhấn `Ctrl+C` tại terminal đang chạy application. Shared YAML bật graceful shutdown cho các client. Dừng service nghiệp vụ/Gateway trước, sau đó Discovery và Config Server.

Topology RabbitMQ, quyền broker/DB, cảnh báo và replay DLQ tại [events-audit.md](events-audit.md).


## Kiểm tra luồng sự kiện trên Windows bằng native tools

Harness phát triển có thể dùng bản zip chính thức của Erlang, RabbitMQ và MongoDB trong cache tạm, không cài service Windows hoặc dùng Docker. Chuẩn bị bằng `powershell -File tools/prepare-native-messaging.ps1`, rồi chạy `python tools/run-access-e2e.py --messaging` từ backend. Cần Python requests, JDK 21 và Maven. Harness dùng port/database/key tạm, sao chụp classes riêng sau build, không sửa `.env`, dừng process do nó tạo trong finally. HR là contract simulator, thiết bị vật lý và cluster HA chưa được mô phỏng đầy đủ. Các tools vẫn theo quy tắc Git ignore riêng của repository.

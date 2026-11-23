# Sự kiện, audit và thông báo

`libraries/event-support` dùng chung khai báo RabbitMQ, publisher confirms và ranh giới ACK. Thư viện không chứa domain chấm công hoặc truy cập database của service khác. Cài `api-support` và `event-support` trước khi build các service nghiệp vụ bằng POM riêng.

## Luồng và dữ liệu bền vững

| Nguồn | Outbox riêng | Đích |
|---|---|---|
| Access Event | `outbox_events` | Attendance nhận PASSAGE_CONFIRMED; Audit nhận access.* |
| Identity, QR, Gate, Attendance, HR | `integration_outbox` | Audit nhận audit.* |
| Attendance và HR deadline monitors | `integration_outbox` | Audit/Notification nhận monitor.* |

Audit SQL và outbox được tạo trong cùng transaction qua trigger. Migration backfill audit đã có, không sửa bằng chứng gốc. Payload chỉ có ID, actor, action, nguồn, factoryId, thời điểm và version; không gửi mật khẩu, QR thô, hồ sơ nhân viên hoặc secret. Access gắn actor từ device_id đã lưu; payload cũ không có actor được ghi rõ `legacy-device-unknown`.

Publisher khóa batch `FOR UPDATE SKIP LOCKED`, gửi persistent message với messageId=eventId; chỉ cập nhật published_at sau publisher confirm và không bị mandatory return. Lỗi mạng hoặc rollback giữ pending outbox; crash sau broker confirm nhưng trước DB commit có thể gửi trùng. Không tuyên bố exactly-once delivery.

| Exchange/queue | Binding | Chính sách |
|---|---|---|
| `tmc.events` | Topic, durable | access.*, audit.*, monitor.* |
| `tmc.attendance` | access.passage_confirmed | Durable quorum, delivery-limit=5 |
| `tmc.audit` | access.#, audit.#, monitor.# | Durable quorum, delivery-limit=5 |
| `tmc.dead` | Direct, durable | Routing key bằng tên queue nguồn |
| `tmc.attendance.dead`, `tmc.audit.dead` | Từ tmc.dead | Durable quorum, không TTL, không giới hạn redelivery |

Queue nguồn dùng overflow=reject-publish và dead-letter-strategy=at-least-once để giữ message trong quá trình chuyển sang DLQ. Cần RabbitMQ hỗ trợ quorum/at-least-once dead lettering; topology phải được chuẩn bị trước traffic. Một node trong môi trường phát triển không chứng minh HA; triển khai chịu lỗi cần quorum cluster trên các failure domain độc lập.

Attendance parse/validate schema và messageId trước ingest; transaction inbox/passage commit xong mới ACK. Audit insert Mongo có write concern majority+journal, sau đó bảo đảm notification job đã tồn tại rồi ACK. Mất ACK dẫn đến replay; unique ID và fingerprint ngăn cộng công/audit trùng. Cùng ID đổi bằng chứng bị từ chối. Event không hợp lệ vào DLQ ngay; lỗi phụ thuộc được reject/requeue có giới hạn rồi chuyển DLQ. Khi outage kéo dài, DLQ là backlog bền vững cần xử lý, không phải sự kiện đã hoàn tất nghiệp vụ.

## Audit MongoDB và quyền

`audit_events` lưu `_id=eventId`, fingerprint và payload chuẩn hóa; ứng dụng chỉ insert/find. `notification_jobs` giữ job theo cùng eventId, trạng thái, attempt và lease. Crash giữa insert audit và tạo job được sửa qua replay; không cần transaction hai collection để bỏ qua khoảng trống đó.

Chạy script role trước, rồi `infra/mongodb/init-audit.js` với TMC_MONGO_PASSWORD để tạo/cập nhật app user và loại bỏ role readWrite cũ. Các script có thể chạy lại. Chuẩn bị collection/index/role bằng [infra/mongodb/audit-role.js](../infra/mongodb/audit-role.js) với tài khoản quản trị. Gán role `tmcAuditApplication` cho app user thay role readWrite chung: audit chỉ find/insert; notification có update trạng thái. Không cấp update/delete audit cho tài khoản runtime. DBA vẫn có quyền quản trị; append-only ở mức ứng dụng và quyền DB không phải kho WORM chống DBA.

`GET /audit?factoryId=UUID&limit=50&offset=0` và `GET /notifications` cần scope `audit.read`. HR/SUPERVISOR/SECURITY phải có grant hiện tại của nhà máy; ADMIN được xem liên nhà máy. Không có factoryId chỉ ADMIN được phép. Kiểm quyền qua Identity; không tin role JWT cũ. Audit trả PageData trong envelope chuẩn, giới hạn 1–100. Không có API sửa/xóa audit.

## Thông báo

Các action DLQ_BACKLOG, BROKER_UNAVAILABLE, QUEUE_MISSING, ACCESS_DENIED, PERIOD_NOT_FINALIZED, HR_EXPORT_MISSING, HR_DEADLINE_APPROACHING, HR_OVERDUE, RECONCILIATION_REQUIRED và HR_EXPORT_REJECTED tạo job dedup theo eventId. Job tồn tại và truy vấn được ngay cả khi SMTP tắt/chưa cấu hình. Người vận hành sở hữu backlog/reconciliation của nhà máy; HR xử lý sai lệch và tạo snapshot mới khi cần.

Để gửi mail, bật NOTIFICATIONS_ENABLED và cấu hình sender, recipient vận hành cùng SMTP. Nội dung chỉ có ID/action/nhà máy, không gửi danh sách nhân viên hoặc dữ liệu công. Worker claim lease 30 giây, timeout SMTP 5 giây, retry sau 60 giây. Crash sau SMTP đã nhận nhưng trước khi lưu SENT có thể gửi mail trùng; SMTP không cung cấp idempotency như HR import. Không cam kết mail exactly-once.

## Giám sát và replay

MONITORED_FACTORY_IDS trong Attendance và HR là danh sách UUID nhà máy cần theo dõi, phải cấu hình đầy đủ trước triển khai. Từ 09:00 ngày 1, monitor cảnh báo thiếu snapshot hoặc thiếu batch/thời hạn sắp tới; từ 12:00 cảnh báo chưa hoàn tất. Event ID ổn định theo nhà máy/kỳ/loại cảnh báo để không phát lặp mỗi lần poll. Reconciliation/rejection cũng tạo cảnh báo có batchId. Monitor không tự phê duyệt kỳ công và không tự tạo export từ dữ liệu chưa được HR duyệt.

Vận hành theo dõi: số pending và tuổi bản ghi cũ nhất trong outbox từng DB; RabbitMQ ready/unacked/consumer count; DLQ depth; notification PENDING/RETRY; HR overdue và reconciliation issues. Không chỉ theo dõi liveness của JVM. EventMonitor ghi cảnh báo broker/DLQ trực tiếp vào Mongo theo cửa sổ giờ, nên broker down không chặn việc tạo cảnh báo khi Mongo còn hoạt động. `infra/sql/outbox-status.sql` là truy vấn operator cho từng DB để xem backlog/tuổi outbox. Dùng RabbitMQ management với tài khoản vận hành riêng, không nhúng credential quản trị vào app.

Sau khi sửa lỗi schema/consumer/phụ thuộc, dùng `tools/ReplayDeadLetters.java` với Rabbit client classpath và credential operator từ môi trường. Công cụ chỉ cho hai queue DLQ của ứng dụng, giới hạn số message; giữ nguyên event ID/body/routing gốc, publish confirm/mandatory trước khi ACK bản DLQ. Crash ở giữa có thể replay trùng và được consumer dedup. Không purge queue hoặc sửa payload bằng tay để bỏ qua quy tắc nghiệp vụ.

Không expire hoặc xóa pending outbox/DLQ để giảm dung lượng. Retention cho audit đã xử lý phải được HR/quản trị phê duyệt riêng; chưa tự đặt TTL xóa bằng chứng.

## Quyền RabbitMQ và môi trường

Mỗi service dùng user RabbitMQ riêng trên vhost ứng dụng, giới hạn write theo exchange/routing của nguồn và read theo queue consumer. Provision topology bằng tài khoản triển khai, tránh cho caller ngoài hệ thống quyền publish. Broker là ranh giới tin cậy cho event producer; scope OAuth của inbox HTTP không thay quyền RabbitMQ. Dùng TLS và secret riêng khi triển khai trên server thuê.

EVENTS_ENABLED bật event delivery; tắt chỉ cho chạy slice cô lập. Audit dùng caller OAuth scope identity.verify để kiểm quyền API. HR correction contract mô tả tại [Attendance/HR](attendance-hr-api.md); NOTIFICATIONS_ENABLED độc lập với việc lưu audit/job.

Tham khảo thiết kế: [RabbitMQ confirms](https://www.rabbitmq.com/docs/confirms), [quorum dead lettering](https://www.rabbitmq.com/docs/quorum-queues), [MongoDB write concern](https://www.mongodb.com/docs/manual/reference/write-concern/).


### Đối chiếu danh sách nhà máy trước triển khai

Người vận hành lấy danh sách factory_id từ database Gate Station bằng tài khoản operator (`SELECT factory_id, factory_code FROM factories ORDER BY factory_code`). Đối chiếu từng UUID với MONITORED_FACTORY_IDS của cả Attendance và HR; hai danh sách phải chứa đầy đủ nhà máy đang quản lý công. Sau khi thêm nhà máy mới, cập nhật cả hai cấu hình và xác nhận các monitor tạo alert thử trong môi trường staging. Danh sách rỗng chỉ phù hợp chạy slice phát triển; không được coi là đã bật giám sát production. Cảnh báo thiếu snapshot/batch và danh sách recipient phải có người phụ trách vận hành/HR cụ thể.

# Tổng hợp công và tích hợp HR

Attendance Report sở hữu read model PostgreSQL của lượt đi qua, điều chỉnh và snapshot. HR Integration sở hữu batch, item, attempt, receipt, reconciliation issue và audit PostgreSQL riêng. Không đọc chéo database. API nghiệp vụ dùng envelope trong [api-response.md](api-response.md).

## Nhận lượt đi qua và tổng hợp

`POST /internal/passage-events`, scope `attendance.ingest`, nhận payload outbox Access phiên bản 1:

```json
{
  "eventId": "00000000-0000-0000-0000-000000000001",
  "scanId": "00000000-0000-0000-0000-000000000002",
  "stationId": "00000000-0000-0000-0000-000000000003",
  "employeeId": "00000000-0000-0000-0000-000000000004",
  "factoryId": "00000000-0000-0000-0000-000000000005",
  "direction": "ENTRY",
  "eventType": "PASSAGE_CONFIRMED",
  "occurredAt": "2026-10-20T01:00:00Z",
  "version": 1
}
```

Chỉ `PASSAGE_CONFIRMED` được nhận; `ACCESS_ALLOWED` và `GATE_OPENED` không tạo giờ công. Transaction ghi inbox và passage cùng lúc. Cùng eventId/cùng payload trả `inserted=false`; thay payload hoặc dùng scanId đã nhận với eventId khác trả 409. Raw QR không nằm trong read model. API này dành cho caller kỹ thuật đã được cấp scope; không cho người dùng gửi lượt quét của mình.

Có inbox API và consumer RabbitMQ cùng dùng một transaction ingest. ACK chỉ sau commit; malformed/schema conflict vào DLQ. [Luồng sự kiện](events-audit.md) mô tả broker, retry và giới hạn vận hành.

| API | Scope | Quyền hiện tại tại Identity |
|---|---|---|
| `GET /factories/{factoryId}/periods/{yyyy-MM}` | attendance.read | HR, SUPERVISOR hoặc EXECUTIVE có grant nhà máy; ADMIN |
| `GET /me/factories/{factoryId}/periods/{yyyy-MM}` | attendance.read | Nhân viên active có grant nhà máy; chỉ trả bản thân |
| `POST /factories/{factoryId}/adjustments` | attendance.write | HR/SUPERVISOR có grant; ADMIN |
| `POST /factories/{factoryId}/adjustments/{adjustmentId}/decisions` | attendance.write | HR có grant; ADMIN; khác người đề xuất |
| `POST /factories/{factoryId}/periods/{yyyy-MM}/finalizations` | attendance.write | HR có grant; ADMIN |
| `GET /internal/snapshots/{snapshotId}` | attendance.export | Caller kỹ thuật HR Integration |

Role và grant được kiểm lại qua Identity, không dựa riêng vào role JWT cũ. ADMIN quản trị báo cáo liên nhà máy; quyền vào cổng vẫn dùng access-check riêng, không tự mở rộng theo quyền này. Identity ngừng hoạt động hoặc chưa cấu hình credential thì API cần authorization/roster từ chối an toàn.

Roster lấy từ Identity theo grant hiện tại và hợp với các employeeId có bằng chứng trong kỳ. Nhân viên không có event vẫn xuất hiện với 0 giây; đây không phải kết luận nghỉ không phép. Chưa có lịch sử hợp đồng, lịch phân ca hoặc định mức hiện diện để tính ngày vắng.

## Chính sách tính công giả định

- Múi giờ mặc định `Asia/Ho_Chi_Minh`, dùng chung cho hai nhà máy; thay đổi qua `ATTENDANCE_ZONE` cần đánh giá lại kỳ và snapshot đã chốt.
- Sắp theo occurredAt rồi eventId, độc lập thứ tự nhận. Ghép ENTRY với EXIT tiếp theo, cho phép qua nửa đêm; interval qua ranh giới tháng được chia ở 00:00 địa phương.
- Giới hạn một ca mặc định 16 giờ, cấu hình 1–24 giờ qua `ATTENDANCE_MAXIMUM_SHIFT_HOURS`. Không tự trừ nghỉ, tính đi muộn, làm thêm, ngày công hoặc tiền lương.
- Thiếu ENTRY/EXIT, ENTRY liên tiếp, interval 0 hoặc ca quá dài tạo exception. Interval mơ hồ không cộng giờ. Dữ liệu trả `workedSeconds`, danh sách interval và exception; không làm tròn phút từng lượt.
- Đọc thêm bằng chứng hai phía ranh giới tháng trong cửa sổ tối đa một ca. EXIT đến muộn có thể đổi bản tổng hợp đang đọc; snapshot trước đó giữ nguyên.

Đây là policy cho triển khai ban đầu, chưa phải quy chế HR đã phê duyệt.

## Điều chỉnh và snapshot

Đề xuất điều chỉnh gồm `adjustmentId`, `employeeId`, `direction`, `occurredAt`, `replacesEventId` tùy chọn và `reason`. Nếu thay bằng chứng gốc, event phải thuộc đúng nhân viên và nhà máy. Bổ sung lượt thiếu dùng replacesEventId=null. Người duyệt khác người đề xuất, gửi `{ "approved": true, "reason": "..." }`. Một đề xuất chỉ có một quyết định; không tự duyệt, không sửa proposal/decision hoặc passage gốc. Một event chỉ có một correction được duyệt. Bằng chứng trước điều chỉnh giữ tại passage_events, bằng chứng sau ở proposal và decision; audit lưu actor/thời điểm.

Chốt kỳ gửi `{ "snapshotId": "UUID", "expectedRevision": 0 }`. Kỳ phải đã kết thúc. Snapshot đầu revision=1; chốt tiếp phải gửi revision hiện tại. Tranh chấp revision trả 409. Retry cùng snapshotId và cùng nhà máy/kỳ/revision trả snapshot gốc. Factory lock dùng chung cho ingest, approval và finalization để snapshot nhất quán. Snapshot lưu cutoff, checksum SHA-256 của records, actor và payload bất biến. Điều chỉnh hoặc event nhận sau đó cần snapshot revision mới; không ghi đè bản đã xuất. Snapshot có exception có thể dùng để kiểm tra, nhưng HR từ chối xuất cho đến khi hết exception.

## HR export, receipt và đối soát

| API | Chức năng |
|---|---|
| `POST /exports` | Body factoryId/snapshotId; lấy snapshot bằng caller OAuth riêng và tạo batch |
| `GET /exports/{batchId}` | Xem payload, trạng thái, deadline và receipt |
| `POST /exports/{batchId}/retries` | Yêu cầu xử lý lại; UNKNOWN phải lookup trước |
| `GET /exports/{batchId}/reconciliation` | Xem trạng thái và lịch sử sai lệch |

Tất cả cần scope `hr.export` và HR active có grant nhà máy, hoặc ADMIN, được kiểm từ Identity. Snapshot phải đúng nhà máy, hết exception và checksum phù hợp. Một snapshot có một batch và idempotency key ổn định. Revision mới tạo batch khác theo hợp đồng correction dưới đây; không thay payload batch cũ.

State: NEW → UNKNOWN trước khi gọi hệ thống ngoài → COMPLETED khi receipt khớp. Timeout/5xx hoặc mất tiến trình giữ UNKNOWN. Lookup xác nhận chưa có import mới cho phép RETRYABLE → gửi lại cùng key; thiếu phản hồi tiếp tục UNKNOWN. Receipt sai batch/key/checksum/count chuyển RECONCILIATION_REQUIRED và ghi issue. Manual retry ở trạng thái này chỉ lookup để đối soát, không tự gửi thêm. 400/422 của HR là REJECTED, cần xử lý nguyên nhân và snapshot mới. Batch COMPLETED không gửi lại.

Worker ghi attempt và lease 30 giây trong transaction trước I/O; không giữ transaction DB khi chờ mạng. Claim khóa row, có thể chạy nhiều instance. Lease hết hạn phục hồi bằng lookup. Scheduler kiểm batch đến hạn mỗi 10 giây, xử lý tối đa 50 batch mỗi lượt. Retry backoff 60 giây. HTTP có timeout 3 giây và không tự redirect/retry.

### Hợp đồng HR giả lập cần được thống nhất với hệ thống ngoài

`HR_PAYROLL_URL` + `POST /attendance-imports`, header `Authorization: Bearer <HR_API_KEY>` và `Idempotency-Key`. Body là `{ batchId, idempotencyKey, snapshot }`. HR phải lưu key bền vững, cùng key/cùng payload trả cùng receipt; key trùng/payload khác không được nhập lần nữa.

`GET /attendance-imports/by-key/{key}` trả receipt khi đã commit; 404 chỉ được dùng khi chắc chắn chưa có import theo key, với tính nhất quán đủ để retry an toàn. Nếu HR chưa hỗ trợ lookup/idempotency, giữ UNKNOWN và đối soát thủ công; không tự coi timeout là chưa nhận.

200/201 có JSON receipt thuần (giao thức ngoài, không phải envelope TMC): `receiptId`, `batchId`, `idempotencyKey`, `checksum`, `acceptedCount`. Chỉ xác nhận thành công khi tất cả khớp dữ liệu gửi. HTTP thành công đơn thuần chưa đủ.

### Mốc trước 12:00 ngày 1

Kỳ xuất là tháng vừa kết thúc; deadline là 12:00 ngày 1 kế tiếp theo `HR_ZONE=Asia/Ho_Chi_Minh`. `deadlineMet` chỉ true nếu TMC nhận receipt khớp trước mốc đó. Tại đúng 12:00 vẫn chưa hoàn tất thì overdue=true; nhận receipt muộn không biến thành đạt hạn. Worker vẫn phục hồi batch sau restart và sau deadline.

HR cần xử lý exception, chốt kỳ và tạo batch trước deadline. Scheduler hiện xử lý các batch đã tạo, không tự chốt dữ liệu chưa được HR duyệt hoặc tự chứng nhận tháng đã đủ event. Monitor theo MONITORED_FACTORY_IDS phát cảnh báo kỳ chưa chốt/chưa có batch, sắp hạn/quá hạn và reconciliation qua Audit/Notification; không cam kết SLA thực tế khi chưa tích hợp HR thật.

## Cấu hình caller

Mỗi service dùng `.env` riêng, không commit credential:

- Attendance: client riêng scope `identity.verify`; cấu hình INTERNAL_CLIENT_ID/SECRET, IDENTITY_TOKEN_URI/API_URL.
- Projector: client riêng scope `attendance.ingest`, không dùng credential nhân viên.
- HR: client riêng scopes `identity.verify attendance.export`; thêm ATTENDANCE_API_URL, HR_PAYROLL_URL/API_KEY, HR_ZONE và HR_SCHEDULER_ENABLED/DELAY_MS.
- Web client dùng PKCE; scope attendance.read/write/hr.export chỉ cho phép gọi route, role và site vẫn kiểm tại service. Migration Identity bổ sung scopes cho client tmc-web đã tồn tại.

Key và endpoint HR trong ví dụ chỉ là cấu hình giả. Khi triển khai dùng HTTPS và credential riêng theo hệ thống HR thực tế.


## Snapshot revision sau khi HR đã nhận bản cũ

Không xuất revision mới khi batch trước còn NEW/UNKNOWN/RETRYABLE/RECONCILIATION_REQUIRED. Phải đối soát batch trước để biết HR đang giữ bản nào. Batch REJECTED xác nhận chưa nhập không chặn snapshot đã sửa.

Mặc định HR_CORRECTIONS_ENABLED=false: nếu kỳ đã có receipt thành công, revision mới trả 409 HR_CORRECTION_CONTRACT_REQUIRED. Chỉ bật sau khi HR hỗ trợ thay thế toàn bộ kỳ theo (factoryId, period), kiểm revision tăng và supersedesReceiptId khớp receipt hiện hành. Không cộng thêm toàn bộ giờ của revision mới vào bản cũ.

Payload correction có supersedesReceiptId của batch COMPLETED gần nhất; receipt phải echo cả revision và supersedesReceiptId, ngoài batch/key/hash/count. Thiếu hoặc sai chuyển RECONCILIATION_REQUIRED; không tuyên bố hoàn tất chỉ vì HTTP 2xx. Bản cũ và receipt cũ giữ nguyên làm lịch sử; batch mới trỏ về receipt bị thay thế. HR phải thực hiện replace/receipt atomically với idempotency key. Đây là hợp đồng adapter đề xuất, chưa được xác nhận với HR thật.


## Passage được duyệt sau khi nhận bằng chứng muộn

PASSAGE_CONFIRMED version1 có thể kèm reviewDecisionId và sourceConfirmationId từ workflow Access. Hai trường phải cùng có hoặc cùng rỗng; Attendance lưu immutable cùng passage và đặt unique theo review/confirmation để ngăn một evidence được project hai lần. Payload thường không có links vẫn giữ fingerprint version1 cũ khi replay sau nâng cấp schema. Chỉ Access có quyền quyết định review; Attendance không tự diễn giải OPENED hoặc quyết định chưa approved thành lượt đi qua. Chi tiết [Access review](access-api.md#review-bằng-chứng-đến-muộn).

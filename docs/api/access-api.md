# Access Event API

## Phạm vi hiện tại

API nằm trong `access-event-service`; Gateway bỏ prefix `/api/access` trước khi chuyển tiếp. API lưu quyết định, lệnh cổng và xác nhận vào PostgreSQL, kèm outbox trong cùng transaction.

Response DTO bên dưới nằm trong trường `data` của [envelope chung](api-response.md); lỗi cũng theo cùng format. Timestamp/traceId ở envelope mô tả lần gọi HTTP, không thuộc kết quả idempotency được lưu.

`AccessPolicy` là port xác minh QR, nhân viên và quyền nhà máy/trạm. Khi chưa có adapter thật, ứng dụng trả `503`, không cấp quyền mặc định. Test thay port bằng dữ liệu kiểm thử; điều này không chứng minh chữ ký QR hoặc quyền nhân viên thực tế đã được xác minh.

## Danh tính thiết bị

Bearer JWT phải có `sub` là định danh kiosk và claim `station_id` dạng UUID. Backend đối chiếu trạm yêu cầu với claim đã ký. Quyền scope:

| API | Scope |
|---|---|
| `POST /api/access/scans` | `access.scan` |
| `POST /api/access/commands/{commandId}/confirmations` | `access.confirm` |

Client kỹ thuật local hiện có trong Identity không tự có claim/quyền thiết bị này. Cấp danh tính thiết bị thuộc bước tích hợp Identity/Gate Station.

## Gửi lượt quét

```json
{
  "scanId": "d1fb1560-1ff1-4f5c-ab7f-ec3093608c53",
  "stationId": "caf5846a-b489-451c-af71-4a3f60ad6696",
  "qrToken": "<signed-qr-token>"
}
```

Kiosk tạo và giữ `scanId` trước khi gửi. Mất response thì retry đúng ID, trạm và QR; không tạo một lượt mới để đoán kết quả. Cùng ID khác nội dung hoặc danh tính thiết bị trả `409`. QR đã được chấp nhận chỉ dùng cho một lượt; ID scan mới dùng lại cùng QR bị từ chối. Retry scan cũ vẫn trả quyết định đã lưu dù QR đã hết hạn.

Response `200` chứa `scanId`, `stationId`, tham chiếu `employeeId`, `factoryId`, `direction`, `allowed`, `reason`, `commandId`, `commandExpiresAt`. Quyết định từ chối không có lệnh cổng. Chỉ lưu hash của QR trong request fingerprint, không lưu token gốc.

Lệnh mở có thời hạn 10 giây theo chính sách thiết kế hiện tại. Kiosk gọi adapter turnstile trong LAN, chuyển `commandId` làm khóa chống thực thi trùng. Retry response không gia hạn lệnh. Kiosk phải bỏ lệnh đã hết hạn; nếu API thiết bị không hỗ trợ idempotency hoặc truy vấn kết quả, không tự gọi lại sau timeout có kết quả chưa rõ.

## Gửi xác nhận

```json
{
  "confirmationId": "912b3162-9adb-4326-9c8b-89bd4ba8e507",
  "outcome": "OPENED",
  "occurredAt": "2026-11-21T07:00:00Z"
}
```

- `OPENED`: thiết bị xác nhận đã mở, xảy ra trong hạn lệnh.
- `FAILED`: thiết bị xác nhận thất bại; không tạo công.
- `UNKNOWN`: timeout hoặc mất kết nối, chưa biết kết quả; không suy diễn đã mở hoặc đã đi qua.
- `PASSED`: có bằng chứng người đã qua sau `OPENED`; lúc này mới tạo `PASSAGE_CONFIRMED` cho Attendance.

`confirmationId` ổn định qua retry; đổi nội dung với cùng ID trả `409`. Callback từ trạm khác trả `403`. Trạng thái `PASSED` và `FAILED` không được lùi; `UNKNOWN` có thể đối soát thành `OPENED` hoặc `FAILED`. Không chấp nhận thời gian tương lai quá 5 giây hoặc passage trước thời điểm mở. Thời điểm nhận ở server được lưu riêng với `occurredAt` do thiết bị báo. Xác nhận mở đến server sau hạn lệnh, hoặc bằng chứng có độ trễ quá 30 giây, được lưu với `reviewRequired=true`, không chuyển trạng thái cổng và không tự tạo passage. Các ngưỡng này là chính sách thiết kế, không phải yêu cầu số học của đề. Kiosk cần gửi lại bằng chứng với ID ổn định sau khi mạng phục hồi; không tự cấp quyền khi backend không kết nối được.

Response confirmation chứa `confirmationId`, `outcome` đã báo, `reviewRequired` và `reason`. Retry cùng ID/payload trả kết quả gốc đã lưu, kể cả khi trạng thái command đã tiến triển. `outcome` trong response không đại diện cho trạng thái hiện tại của command; phải kiểm tra `reviewRequired` trước khi coi bằng chứng được chấp nhận tự động.

Bằng chứng đến muộn đi qua workflow review có quyền và audit mô tả ở mục dưới. Không dùng timestamp tự khai để tự động hợp thức hóa một lần mở cổng quá hạn. Định danh kiosk và freshness chỉ giới hạn rủi ro, chưa thay thế chứng cứ phần cứng đáng tin cậy.

Thiết bị xác nhận `OPENED` và cảm biến passage phải là nguồn tin cậy của kiosk, không lấy từ thao tác tự khai của nhân viên. Chưa có adapter phần cứng thật ở bước hiện tại.

## Dữ liệu và sự kiện

Migration `V1__access_events.sql` tạo `scan_requests`, `qr_redemptions`, `gate_commands`, `command_confirmations`, `access_events`, `outbox_events`. `V2__confirmation_evidence.sql` bổ sung kết quả confirmation, thời điểm thiết bị và cờ đối soát. Confirmation cũ thiếu bằng chứng được giữ ở trạng thái cần đối soát; không suy diễn outcome gốc. PostgreSQL trigger cấm update/delete lịch sử event. Unique constraint và khóa dòng bảo vệ nhiều instance cùng xử lý scan/callback.

Outbox gửi JSON version 1 tới topic exchange bền vững `tmc.events`, routing key `access.<event_type viết thường>`, ví dụ `access.passage_confirmed`. Payload chứa ID event/scan/trạm/nhân viên/nhà máy, hướng, loại và thời điểm sự kiện; không chứa raw QR hoặc hồ sơ cá nhân. `messageId` chính là `eventId`.

Publisher dùng persistent message, mandatory routing và publisher confirm. Chỉ đánh dấu published sau ACK và không bị trả lại. Nếu broker ngắt hoặc thiếu binding, hàng đợi outbox còn nguyên để retry. Crash sau publish nhưng trước commit có thể gửi trùng: mỗi consumer phải deduplicate theo `eventId`, có queue riêng và ACK sau commit.

Các service consumer/binding thật và kiểm chứng broker restart vẫn thuộc bước tích hợp tiếp theo. `tmc.outbox.enabled=false` chỉ dùng cho test cô lập; môi trường ứng dụng mặc định bật publisher. Chu kỳ poll mặc định 1 giây qua `tmc.outbox.poll-interval-ms`.

## Lỗi và giới hạn

`400`: payload/thời gian không hợp lệ. `401`: thiếu token. `403`: thiếu scope hoặc sai trạm. `404`: không có lệnh. `409`: xung đột ID/trạng thái. `503`: chưa có policy hoặc dependency không sẵn sàng.

Mỗi scan chờ dependency trong transaction; adapter mạng phải có timeout ngắn khi được tích hợp. Chưa có phép đo tải, nghiệm thu thiết bị thật hoặc bảo đảm hoạt động ngoại tuyến.


## Review bằng chứng đến muộn

Các API dưới đây nằm trong Access Event, cần scope `access.review` và HR hoặc SECURITY có grant nhà máy hiện tại, hoặc ADMIN. Service kiểm quyền qua Identity bằng caller riêng scope identity.verify; role/site trong JWT cũ không đủ. Người có employeeId trùng nhân viên của passage không được tự review, kể cả có role ADMIN. Callback kiosk vẫn dùng scope access.confirm/station_id; kiosk không có quyền review.

| API | Mục đích |
|---|---|
| `GET /factories/{factoryId}/reviews?stationId=UUID&limit=50&offset=0` | PageData của confirmations reviewRequired chưa có quyết định, lọc đúng factory và tùy chọn station |
| `GET /factories/{factoryId}/reviews/{confirmationId}` | Bằng chứng gốc cùng decision nếu đã xử lý |
| `POST /factories/{factoryId}/reviews/{confirmationId}/decisions` | Body decisionId UUID, approved boolean, reason bắt buộc tối đa 500 ký tự |

Confirmation gốc không sửa/xóa, kể cả review_required và response callback ban đầu. Chưa có decision mới là pending review; không suy ra pending chỉ từ cờ gốc. access_evidence_reviews lưu actor, thời gian, lý do, effect và eventId áp dụng. Database trigger giữ decision/confirmation bất biến. Audit event riêng trong integration_outbox được ghi cùng transaction và chuyển tới Audit; eventId của audit chính là decisionId, entityId là confirmationId và actor là reviewer. Lý do đầy đủ giữ tại access_evidence_reviews bất biến, đọc qua detail API đã kiểm quyền; không nhân bản free-text có thể chứa dữ liệu cá nhân vào Mongo audit.

Retry cùng decisionId/confirmation/actor/approved/reason trả data gốc; đổi nội dung hoặc reviewer khác quyết định lần nữa trả 409. Command row lock dùng chung với callback thông thường, cùng unique confirmation decision ngăn concurrent approve tạo hai passage. ID trùng giữa command khác cũng rollback toàn bộ transaction. Quyết định chỉ có approve/reject, không sửa thời điểm/outcome gốc; legacy thiếu outcome/occurredAt chỉ có thể reject, không tự gán UNKNOWN rồi duyệt.

Approve OPENED chỉ được áp dụng nếu occurredAt trước command expiry; không suy ra passage. Approve PASSED cần GATE_OPENED hợp lệ đã ghi nhận trước occurredAt; nếu mở cổng cũng là late evidence, phải duyệt nó trước. Command FAILED/PASSED là terminal; không đảo ngược bằng review. Nếu cùng outcome đã tồn tại thì decision effect ALREADY_RECORDED, không phát thêm event. Reject chỉ ghi decision/audit, không đổi command hoặc tạo công.

Khi approved PASSED thật sự chuyển OPENED→PASSED, Access tạo PASSAGE_CONFIRMED với eventId mới, version=1 và hai trường bổ sung `reviewDecisionId`, `sourceConfirmationId`. Đây là passage được xác nhận sau review, không phải sửa passage cũ. Payload vẫn dùng access.passage_confirmed nên consumer cũ bỏ qua metadata mới có thể đọc cấu trúc chuẩn. Consumer hiện tại lưu hai liên kết, yêu cầu cả hai có hoặc cả hai rỗng, unique từng review/confirmation và scanId. Inbox dedup giữ replay an toàn. API nội bộ/broker chỉ dành cho producer được cấp quyền; không cho nhân viên tự gửi dữ liệu công.

Attendance không thay snapshot đã chốt khi nhận event muộn. HR phải kiểm lại bản tổng hợp, chốt revision mới và xuất theo correction/supersession contract; event approved không tự sửa batch hoặc receipt cũ. Các quy tắc xác minh sensor/controller là hợp đồng nghiệp vụ cần thống nhất với thiết bị; simulator timestamps không là bằng chứng turnstile vật lý.

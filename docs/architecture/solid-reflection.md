# SOLID và reflection

## Áp dụng vào code hiện tại

Ví dụ dựa trên Access Event đã có code; không coi các adapter/service chưa triển khai là minh chứng.

| Nguyên tắc | Ví dụ thực tế | Lợi ích và giới hạn |
|---|---|---|
| SRP | `AccessEventController` kiểm payload/device identity; `AccessService` xử lý use case; `JdbcAccessStore` lưu dữ liệu; `OutboxPublisher` phát event | Không trộn network publish vào request transaction. Use case hiện còn dùng HTTP exception của Spring; chưa tách framework tuyệt đối |
| OCP | `AccessPolicy` là điểm thay adapter xác minh QR/nhân viên/trạm | Có thể thêm tích hợp thật mà không đổi thuật toán idempotency. Không tạo thêm provider/mode khi chưa cần |
| LSP | `AccessStore` quy định claim nguyên tử, cùng key khác payload phải conflict, ghi dữ liệu cùng transaction | Implementation mới phải giữ các bảo đảm này; một map in-memory không tự thay thế được JDBC khi chạy nhiều instance |
| ISP | `AccessPolicy` chỉ trả kết quả xác minh cho luồng scan; `AccessStore` chỉ chứa thao tác cần cho scan/confirmation | Outbox đọc phần lưu trữ của chính nó, không buộc policy biết database hay broker. Có thể tách policy thành các port nhỏ hơn khi adapter thật cần |
| DIP | `AccessService` nhận `AccessStore`, `AccessPolicy`, `Clock` qua constructor | Test điều khiển clock/policy và dùng PostgreSQL thật cho invariant lưu trữ; adapter HTTP kiểm Identity/QR/Gate; cấu hình credential chưa đủ thì fail-closed |

## Reflection

Khó khăn chính là bảo toàn invariant qua nhiều thành phần: QR phải chỉ được tiêu thụ một lần, request retry không mở cổng thêm lần nữa, còn event/broker không nằm trong cùng transaction. Chỉ tách controller/service/repository không giải quyết được những vấn đề đó.

Tách port xác minh khỏi use case giúp kiểm thử đường từ chối và dependency failure trước khi các service còn lại hoàn thiện. Transaction ở use case và unique constraint ở PostgreSQL bảo vệ cạnh tranh giữa nhiều instance; mutex trong một process không đủ. Outbox giúp request không chờ broker và giữ event để retry. Đổi lại, consumer phải chấp nhận gửi trùng và hệ thống cần giám sát backlog.

Một điểm quan trọng khi thiết kế là tách ALLOW, OPENED và PASSED. Nếu gộp các trạng thái, phần báo cáo có thể tính công khi thiết bị chưa mở hoặc người chưa qua. Các lớp domain/port làm rõ hợp đồng, nhưng tính đúng vẫn cần state-machine tests và bằng chứng thiết bị, không thể chỉ dựa vào sơ đồ SOLID.

Giới hạn hiện tại là adapter policy và Rabbit trong test được thay thế, vì vậy chưa chứng minh tích hợp thật, thời gian phản hồi hay độ sẵn sàng. Các bước tiếp theo phải kiểm chứng hợp đồng QR/Identity/Gate, broker recovery và consumer dedup. Sau khi các bước đó có code, cập nhật reflection bằng bài học thực tế, không mô tả lợi ích dự kiến như kết quả đã đạt.


## Tổng hợp công và HR

AttendanceCalculator giữ quy tắc ghép thời gian riêng với JDBC và HTTP. AttendanceService quản lý transaction inbox/approval/snapshot; factory advisory lock và unique constraints bảo vệ nhiều instance. Nhờ snapshot bất biến, sửa công tạo revision mới thay vì đổi dữ liệu HR đã nhận.

ExportBatchService phụ thuộc HrPayroll gồm send/lookup, vì timeout không xác định được hệ thống ngoài đã commit hay chưa. HrPayrollClient giữ hợp đồng mạng; scheduler chỉ chọn batch tới hạn, còn lease và trạng thái nằm trong PostgreSQL. Request không giữ transaction DB trong lúc chờ HR. Đây là điểm đánh đổi thêm state machine để bảo toàn idempotency khi process khởi động lại; không gọi HTTP thành công là đủ đối soát.


## Audit và reliability

EventDelivery gom ranh giới ACK sau durableProcessing; consumer domain vẫn tự parse và kiểm contract. ConfirmedPublisher chỉ gom persistent message/mandatory confirm, không gộp transaction các database. JdbcEventPublisher dùng bảng outbox chuẩn của từng owner, còn trigger SQL tạo audit event cùng transaction thay vì gọi broker trực tiếp trong nghiệp vụ.

AuditService dùng insert-only và fingerprint cho evidence; NotificationWorker chỉ cập nhật job. Tách quyền collection ở MongoDB là phần thực thi invariant ngoài class Java. Worker không hứa exactly-once SMTP: recovery sau gửi nhưng trước lưu SENT có thể gửi trùng. Đổi lại, notification job và event ID vẫn duy nhất, giúp người vận hành đối chiếu.


## Late-evidence review

Review tách với scan/confirmation trên đường vận hành thường; ReviewAccess lo quyền hiện tại, EvidenceReviewService giữ state transition và transaction, Attendance consumer chỉ chiếu passage đã được Access chấp nhận. Cùng command row lock và unique confirmation decision chống cuộc đua giữa callback/reviewer mà không dùng mutex process. Sự phân biệt OPENED/PASSED được giữ ở cả luồng thường và review để không tự suy diễn attendance. Đổi lại, validation transition hiện xuất hiện ở cả hai use case; cần giữ regression nhất quán trước khi cân nhắc tách policy, không tạo abstraction mới chỉ để rút vài dòng.

Confirmation/review và snapshot là bằng chứng bất biến; review event có identity mới và liên kết evidence gốc. Giờ công có thể đổi khi late passage được duyệt, nhưng snapshot/receipt đã xuất giữ nguyên và cần revision/correction mới. Review chỉ chứng minh quy trình thẩm quyền và ràng buộc dữ liệu; chất lượng sensor/controller phải được kiểm với hợp đồng và thiết bị thật.

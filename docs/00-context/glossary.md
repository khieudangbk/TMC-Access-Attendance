# Thuật ngữ

## Miền nghiệp vụ

| Thuật ngữ | Nghĩa trong TMC-AACS |
|---|---|
| **Lượt quét (scan)** | Một lần kiosk gửi yêu cầu xác thực QR cho một người tại một trạm, theo một hướng. Đơn vị nghiệp vụ nhỏ nhất của đường quét. |
| **Sự kiện truy cập (access event)** | Bản ghi bất biến của một lượt quét: nhân viên (nếu xác định), trạm, hướng, thời điểm, quyết định, lý do. |
| **Quyết định (decision)** | Kết quả chính sách: `ALLOWED` hoặc `DENIED`, kèm lý do và `policy_version`. |
| **Gate intent** | Ý định điều khiển cổng được lưu **trước** khi gọi thiết bị; gắn `command_id`. |
| **Gate outcome** | Kết quả thiết bị: `CONFIRMED`, `REJECTED`, `FAILED`, `UNKNOWN`. |
| **Điều chỉnh (adjustment)** | Bản ghi bổ sung sửa sai lệch chấm công, có lý do, người xác minh/phê duyệt, giá trị trước/sau. Không sửa sự kiện gốc. |
| **AttendanceDay** | Dữ liệu chiếu (projection) theo nhân viên/ngày, tái tạo được từ sự kiện + adjustment. |
| **Snapshot kỳ** | Ảnh chụp dữ liệu chấm công của một tháng, có `snapshot_version`; không bị ghi đè sau khi xuất. |
| **Batch xuất HR** | Một lần gửi dữ liệu sang HR/Payroll, có `batch_id`, checksum và receipt. |
| **Receipt** | Phản hồi của HR cho biết bản ghi nào được nhận/từ chối/chưa xác định. |
| **Watermark** | Mốc dữ liệu mới nhất đã xử lý/đồng bộ, dùng để biết nguồn đã đủ chưa. |
| **Cửa sổ tin cậy offline** | Khoảng thời gian edge được phép dùng policy/khóa đã đồng bộ khi mất WAN (mục tiêu 30 phút). |

## Kiến trúc

| Thuật ngữ | Nghĩa |
|---|---|
| **Edge** | Máy chủ tại nhà máy, xử lý đường quét và lưu sự kiện cục bộ. |
| **Central** | Cụm dịch vụ tại Bình Dương: ICS, AAS-central, GSS, RIS, ADS. |
| **ICS** | Identity and Credential Service — hồ sơ nhân viên, quyền nhà máy, credential, khóa ký. |
| **AAS** | Access and Attendance Service — xử lý quét (edge) và chiếu chấm công (central). |
| **GSS** | Gate Station Service — nhà máy, trạm, kiosk, ánh xạ cổng, heartbeat, yêu cầu mở thủ công. |
| **RIS** | Reporting and Integration Service — báo cáo, snapshot, xuất HR, đối soát. |
| **ADS** | Administration and Audit Service — tài khoản, vai trò, cấu hình, audit, retention. |
| **Port / Adapter** | Hợp đồng nghiệp vụ (port) và hiện thực hạ tầng (adapter). Domain không phụ thuộc adapter. |
| **Outbox** | Bảng ghi sự kiện cùng transaction nghiệp vụ, được relay gửi đi sau; bảo đảm không mất sự kiện. |
| **Inbox** | Bảng khử trùng ở consumer, ghi cùng transaction cập nhật nghiệp vụ trước khi ack. |
| **Projection** | Mô hình đọc dựng từ sự kiện, có thể rebuild; không phải nguồn sự thật. |
| **Fencing** | Cơ chế chặn writer cũ sau failover (lease/epoch), tránh split-brain. |
| **mTLS** | Xác thực TLS hai chiều giữa edge và central. |

## Định danh

| ID | Ai tạo | Ý nghĩa | Bất biến |
|---|---|---|---|
| `scan_id` | Kiosk | Một lượt quét logic; giữ nguyên khi retry | Unique; cùng ID phải cùng payload fingerprint |
| `command_id` | Edge (server) | Một ý định điều khiển cổng cho scan | Tối đa một command nghiệp vụ / scan |
| `event_id` | Server | Một envelope sự kiện bất biến | Unique toàn hệ |
| `batch_id` | RIS | Một lần xuất snapshot sang HR | Unique; lô đã gửi không sửa |
| `employee_ref` | ICS | Mã giả danh nhân viên dùng giữa các miền | Không chứa PII trực tiếp; vẫn là dữ liệu cần bảo vệ |
| `credential_id` | ICS | Định danh credential QR | Có `revision`, `key_id` |
| `station_id` | GSS | Trạm cổng | Có `config_revision` |
| `audit_id` | ADS | Bản ghi audit | Chỉ thêm; có `prev_hash` |

## Trạng thái

| Trạng thái | Nghĩa |
|---|---|
| `RECEIVED` | Kiosk đã gửi scan, edge đã nhận. |
| `DENIED` | Từ chối rõ ràng; **không** gọi cổng. |
| `AUTHORIZED` | Chính sách cho phép; chưa chắc cổng đã mở. |
| `INTENT_COMMITTED` | Event + intent + outbox đã commit bền vững. |
| `COMMAND_SENT` | Đã gửi lệnh tới thiết bị. |
| `CONFIRMED` | Thiết bị xác nhận đã thực hiện. |
| `REJECTED` | Thiết bị từ chối rõ ràng. |
| `FAILED` | Lỗi xác định, không thực hiện. |
| `UNKNOWN` | Không xác định được thiết bị đã thực hiện hay chưa. **Không phải** thành công/thất bại. |
| `RECONCILIATION` | Đang đối soát outcome bằng `command_id` hoặc xử lý có thẩm quyền. |

## Từ viết tắt

| Viết tắt | Diễn giải |
|---|---|
| PII | Personally Identifiable Information |
| RBAC | Role-Based Access Control |
| RPO / RTO | Recovery Point / Time Objective |
| TTL | Time To Live |
| WAL | Write-Ahead Log |
| ADR | Architecture Decision Record |
| FR / NFR | Functional / Non-functional Requirement |
| US | User Story |
| CN | Nhóm chức năng (đặc tả gốc) |

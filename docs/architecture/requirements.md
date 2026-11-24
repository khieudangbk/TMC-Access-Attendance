# Yêu cầu TMC-AACS M2

## Nguồn và mục tiêu

Theo Assignment M2: thiết kế và triển khai hệ thống kiểm soát ra/vào, chấm công bằng QR cho TMC, áp dụng SOLID và mô tả nhiều góc nhìn kiến trúc. Nguồn gốc nằm tại `../../Assignment.pdf` tính từ root backend; phần truy vết dưới đây diễn giải yêu cầu, không thay thế đề gốc.

Hai nhà máy: Bình Dương khoảng 800 nhân viên và Long An khoảng 600 nhân viên. Nhân viên hiển thị QR trên smartphone qua ứng dụng, web hoặc email. Kiosk/scanner đọc QR, turnstile có API điều khiển riêng. HR/Payroll đã tồn tại và nhận dữ liệu qua API.

Mục tiêu triển khai theo lựa chọn hiện tại là 7 service nghiệp vụ Spring Boot trên server thuê; Config Server, Eureka và API Gateway MVC hỗ trợ. Các đề xuất cũ về 5 service, NestJS hoặc Edge không còn là quyết định triển khai hiện hành.

## Actors và quyền dự kiến

| Actor | Công việc | Giới hạn dữ liệu |
|---|---|---|
| Nhân viên | Xem QR và lịch sử công cá nhân | Chính mình, nhà máy được cấp quyền |
| Giám sát sản xuất | Theo dõi/đề nghị điều chỉnh công | Nhóm/nhà máy được phân công |
| HR | Kỳ công, báo cáo, đối soát và xuất HR | Phạm vi nhân sự được giao |
| Bảo vệ | Theo dõi trạm, xử lý ngoại lệ có lý do | Nhà máy/trạm được phân công |
| Quản trị hệ thống | Tài khoản, role, cấu hình | Quyền quản trị được cấp, có audit |
| Ban lãnh đạo | Báo cáo tổng hợp | Phạm vi báo cáo được cấp |
| Kiosk/thiết bị | Gửi scan và bằng chứng thực hiện | Trạm gắn với danh tính thiết bị |

Quyền vai trò và phạm vi nhà máy phải được kiểm tra tại service sở hữu nghiệp vụ. Có token hợp lệ không đồng nghĩa được đọc mọi nhân viên hoặc điều khiển mọi cổng. Không tin factory/station tùy ý từ client.

## Truy vết chức năng

| ID | Yêu cầu | Service sở hữu | Tiêu chí kiểm chứng |
|---|---|---|---|
| FR-01 | Quản lý nhân viên, tài khoản, role và quyền nhà máy | Identity & User | CRUD cần thiết; khóa tài khoản/quyền; test đọc chéo user/site |
| FR-02 | Quản lý QR cá nhân | QR | Cấp, ký, TTL, thu hồi; không tái dùng credential đã tiêu thụ |
| FR-03 | Quản lý nhà máy/cổng/kiosk và hướng quét | Gate Station | Cấu hình trạm tin cậy, trạng thái, kiểm quyền thiết bị |
| FR-04 | Ghi nhận sự kiện vào/ra | Access Event | Quyết định/lệnh/bằng chứng passage riêng; idempotency và append-only |
| FR-05 | Báo cáo công theo tháng | Attendance Report | Tổng hợp theo múi giờ nhà máy, ca qua đêm, dữ liệu thiếu, snapshot |
| FR-06 | Audit và thông báo | Audit & Notification | Ai, hành động, thời gian, target, kết quả; lưu bền và không xử lý trùng |
| FR-07 | Quản trị hệ thống | Identity & User + service sở hữu cấu hình | Thay đổi quyền/cấu hình có kiểm soát và audit |
| FR-08 | Truyền dữ liệu sang HR/Payroll qua API | HR Integration | Batch có ID, retry, receipt và đối soát trước 12:00 ngày 1 mỗi tháng |
| FR-09 | Tích hợp API turnstile hiện có | Gate Station + Access Event + kiosk | Command ID ổn định, timeout/unknown; không coi response cấp quyền là passage |

Payroll calculation và visitor management ngoài phạm vi. Report không tính lương. Không xây lại phần cứng hoặc hệ thống HR.

## Yêu cầu phi chức năng và tình huống lỗi

| ID | Yêu cầu từ đề | Cách đánh giá |
|---|---|---|
| NFR-01 | Kiosk phản hồi ≤ 2 giây với khoảng 800 giao dịch/30 phút/nhà máy lúc đổi ca | Đo latency, tỷ lệ lỗi và burst ở cả hai nhà máy; tách thời gian mạng và backend |
| NFR-02 | Không mất bản ghi chấm công trong vận hành bình thường | Commit trước khi xác nhận; transaction/outbox, consumer dedup; test crash/retry/recovery |
| NFR-03 | RBAC và audit đầy đủ | Test không có quyền, phạm vi nhà máy, mọi thay đổi nhạy cảm và luồng export |
| NFR-04 | Bảo vệ dữ liệu cá nhân theo quy định áp dụng | Tối thiểu hóa PII, phân quyền, TLS, secret ngoài Git, retention và audit truy cập |
| ISSUE-01 | Mất kết nối trong khi xử lý giao dịch | ID ổn định, trạng thái chưa rõ, retry/đối soát; không tự mở lại cổng khi chưa biết kết quả |
| ISSUE-02 | QR tĩnh bị lộ | Token động ký số, TTL ngắn, kiểm scope/site, thu hồi, chống replay và test giả mạo |

TTL 60 giây, hạn lệnh 10 giây và skew 5 giây là lựa chọn thiết kế hiện tại, không phải các con số do đề quy định. QR ngắn hạn giảm cửa sổ tấn công nhưng không ngăn người khác dùng ảnh chụp trước chủ sở hữu; biện pháp xác thực bổ sung tùy chính sách nhà máy cần được mô tả khi triển khai.

Khoảng 800 lượt/1.800 giây tương đương trung bình 0,44 lượt/giây/nhà máy; con số này không mô tả burst tức thời. Hai instance service giúp chia tải và dự phòng khi được triển khai trên miền lỗi phù hợp, không tự khắc phục mất WAN hoặc database đơn lẻ.

Không có backend xử lý cục bộ tại nhà máy: khi không kết nối trung tâm, kiosk không tự cấp quyền từ dữ liệu cũ. Bảo vệ xử lý ngoại lệ theo quy trình có audit; thoát hiểm vật lý độc lập. Kiosk cần giữ lại request/bằng chứng đang chờ gửi với ID ổn định. Phần client này phải được hiện thực và kiểm chứng, không được coi là có sẵn từ code backend.

## Deliverables M2

1. Bối cảnh, FR/NFR, phạm vi và tiêu chí thành công.
2. So sánh/lựa chọn architecture style; các bước logical và physical design.
3. Module view, component-and-connector view, allocation view.
4. Architectural decisions, design principles, SOLID và reflection dựa trên code thực tế.
5. Source và test ít nhất một module chính theo đề; tiếp tục toàn bộ plan 7 service theo mục tiêu người dùng.
6. Báo cáo có URL GitHub: [TMC-Access-Attendance](https://github.com/khieudangbk/TMC-Access-Attendance), nhánh `backend`.

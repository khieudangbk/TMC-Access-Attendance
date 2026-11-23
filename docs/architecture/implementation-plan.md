# Plan triển khai M2

Plan `tmc_m2_delivery` được lập với ChatGPT web sau khi đọc đề và snapshot source. Giữ 7 service nghiệp vụ và 3 ứng dụng hạ tầng hiện tại. Theo dõi trạng thái tại [PROGRESS.md](../PROGRESS.md), không dùng scaffold hoặc compile làm bằng chứng chức năng đã hoàn thành.

| Giai đoạn | Công việc | Điều kiện nghiệm thu |
|---|---|---|
| 0. Baseline | Đối chiếu đề, source, quy tắc; lập ma trận yêu cầu | Tất cả FR/NFR/deliverables có owner và cách kiểm chứng |
| 1. Access Event | Scan → policy → quyết định/lệnh bền vững → xác nhận cổng/passage → outbox | Không trùng scan/lệnh/QR; callback đúng trạm, state machine hợp lệ; migration và tests; adapter ngoài được ghi rõ |
| 2. Identity, QR, Gate | Nhân viên/RBAC/site; QR ký, TTL, thu hồi; cấu hình và danh tính kiosk; adapter thật | Nối slice với service thật; test signature/expiry/revocation, site isolation, contract và end-to-end |
| 3. Report, HR | Consumer passage; tổng hợp, điều chỉnh, snapshot; export/retry/receipt/reconcile | Ca qua đêm/kỳ tháng; không nhân đôi; dữ liệu đối soát trước hạn ngày 1 |
| 4. Audit, reliability | Mongo audit/notification; Rabbit queues/DLQ; retention; phục hồi | Consumer idempotent, broker outage/restart, audit đầy đủ, không lộ PII |
| 5. Nghiệm thu và báo cáo | Tải hai nhà máy; crash/network/security; 3 views, ADR, SOLID/reflection | Truy vết từng yêu cầu tới code/test/tài liệu; nêu giới hạn deployment và phần ngoài hệ thống |

## Thứ tự và giới hạn

- Hoàn thành từng luồng có API, lưu dữ liệu, phân quyền và test; tránh tạo CRUD rời rạc cho cả hệ thống rồi coi là hoàn tất.
- Test adapter có thể dùng mock theo contract. Mock không chứng minh thiết bị, broker hay hệ thống HR thật hoạt động; cần bước tích hợp riêng.
- Luồng cổng cơ sở là kiosk gọi API thiết bị trong LAN. Access cấp lệnh sau commit và nhận callback; backend thuê không tự truy cập địa chỉ LAN.
- Không thêm Edge, Kubernetes, visitor management hoặc tính lương.
- Không đổi `.env` local, build Docker image, commit/push trong các bước triển khai nếu chưa được yêu cầu.
- Sau mỗi phần có thay đổi, cập nhật tiến độ và đưa source/output được phép đọc cho vòng review `codex-with-chatgpt`. Giữ nguyên source đề gốc.

## Chi tiết Phase 2

Theo review iteration 2, làm theo thứ tự Identity → QR → Gate Station → adapter AccessPolicy; chưa mở rộng Report/HR/Audit trong cùng lát cắt.

1. Identity: employee, role/site grants và danh tính kiosk lưu trong PostgreSQL; API kiểm tra quyền; OAuth2 phát `sub`, `station_id`, scope thiết bị từ dữ liệu được quản lý. Kiểm chứng issuer/JWKS, token hết hạn, employee/device bị khóa và quyền bị thu hồi.
2. QR: credential có `qrId`, `employeeId`, purpose/version và expiry; ký bằng khóa cấu hình riêng, hỗ trợ xác minh/thu hồi và chính sách rotation. Access giữ nguồn tiêu thụ QR duy nhất qua `qr_redemptions`; không tạo hai nguồn chống replay độc lập.
3. Gate Station: factory/station/direction, binding thiết bị và trạng thái; API quản trị và API kiểm tra device–station; hợp đồng kiosk/turnstile có idempotent command ID và UNKNOWN khi timeout.
4. AccessPolicy: nối ba API thật, xác thực service-to-service, timeout hữu hạn và fail-closed. Kiểm tra transaction/concurrency bằng test trước khi đưa HTTP vào đường scan; giữ idempotency khi dependency chậm.
5. Chạy test từng POM và contract/end-to-end cho scan hợp lệ, sai nhà máy/trạm, QR giả/hết hạn/thu hồi/replay, dependency outage, callback trùng và passage. Giữ các regression Access đã có.

Bằng chứng muộn được xử lý ở Phase 5A bằng API review có quyền, lý do và audit; tuyệt đối không tự chấp nhận bằng chứng đã gắn `reviewRequired`. Dữ liệu legacy thiếu outcome cần thể hiện thiếu dữ liệu, không đánh đồng với thiết bị báo UNKNOWN khi hoàn thiện workflow này.

## Phase 3 — Attendance và HR

Triển khai inbox/pairing/exceptions/adjustments/snapshots và batch/receipt/reconciliation/deadline theo [hợp đồng](attendance-hr-api.md). RabbitMQ consumer/recovery, Audit và các phần full M2 còn lại vẫn thuộc Phase 4. Bằng chứng và tiến độ nằm trong PROGRESS.md.


## Phase 4 — reliability và Audit/Notification

Theo review Phase 3, bổ sung RabbitMQ publisher/consumer Attendance/Audit với durable inbox, ACK sau commit, DLQ và replay; triển khai MongoDB audit immutable, notification dedup và quyền theo site. Kiểm tra broker outage/restart, consumer commit-before-ACK/crash/replay, publisher restart; theo dõi outbox/DLQ và HR overdue/reconciliation. Định nghĩa correction/supersession khi xuất snapshot revision mới thay revision HR đã nhận, tránh nhập hai lần giờ công. Không đổi ranh giới service hoặc stack; chưa coi mô phỏng broker/HR là production acceptance.


## Phase 5 — nghiệm thu và phần còn lại

5A: Access liệt kê late confirmations theo site, kiểm quyền HR/SECURITY hiện tại tại Identity, lưu review approve/reject bất biến với actor/reason/time; không suy diễn OPENED thành passage và không thay evidence gốc. Quyết định passage hợp lệ có event riêng liên kết evidence, Attendance xử lý idempotent, snapshot cũ giữ nguyên. Kiểm concurrent/double review, wrong site/stale grants và audit.

5B: simulator kiosk/turnstile theo command ID, durable journal và UNKNOWN recovery; không mở hai lần khi retry. 5C: kiểm crash-boundary và mô tả cluster/failover với giới hạn chưa kiểm chứng. 5D: đối chiếu contract/correction/deadline HR thực tế khi có nhà cung cấp. 5E: tải hai nhà máy, RBAC/site isolation/PII/monitoring. 5F: đồng bộ views/ADR/SOLID/reflection/report và truy vết requirement→code/test/evidence. Thiết bị/HR thật và HA cần môi trường tương ứng, không coi simulator là nghiệm thu production.


### Phase 5B — lát cắt kế tiếp

Tạo kiosk simulator/client thử nghiệm với SQLite transactional journal (scanId, commandId, confirmationId, fingerprint, state và timestamps). Lưu trước network/device action; restart dùng lại ID. Adapter LAN có execute/status theo command ID và sensor PASSED tách OPENED; thiết bị không hỗ trợ idempotency/status phải giữ UNKNOWN sau timeout, không tự kích hoạt lại. Backend unavailable không được offline-authorize; command expiry phải kiểm trước execute. Kiểm restart trước/sau server commit, lost response, callback lost ACK, timeout và duplicate bằng backend thật + fake turnstile có state. Không thêm service thứ tám/Edge, không coi simulator là nghiệm thu phần cứng.

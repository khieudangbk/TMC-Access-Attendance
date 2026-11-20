# Phạm vi

## 1. Trong phạm vi

| Nhóm | Nội dung |
|---|---|
| Danh tính & QR | Quản lý nhân viên, trạng thái, quyền ra vào theo nhà máy; cấp/làm mới/thu hồi QR; hiển thị QR qua app/web/email |
| Trạm cổng | Nhà máy, kiosk, hướng vào/ra, ánh xạ cổng quay; heartbeat, trạng thái, xử lý sự cố |
| Lượt quét | Nhận QR, xác thực, kiểm tra quyền, chống phát lại, phối hợp mở cổng, ghi giao dịch vào/ra |
| Chấm công | Chiếu sự kiện thành dữ liệu hiện diện; phát hiện thiếu lượt vào/ra; giải trình, xác minh, điều chỉnh có thẩm quyền |
| Báo cáo & tích hợp | Báo cáo tháng, báo cáo giám sát; snapshot kỳ; xuất lô sang HR/Payroll; đối soát receipt |
| Quản trị & bảo mật | Tài khoản, vai trò, scope; cấu hình có version; audit log; retention; sao lưu/khôi phục |
| Vận hành | Theo dõi dịch vụ, backlog, cảnh báo; chế độ suy giảm khi mất WAN/thiết bị |

## 2. Ngoài phạm vi

- Quản lý khách đến thăm (visitor management).
- Tính lương, làm thêm giờ, phụ cấp, quy tắc ngày công — thuộc HR/Payroll.
- Thiết kế lại/thay thế HR/Payroll hoặc hệ thống điều khiển cổng quay.
- Nhận diện sinh trắc học (khuôn mặt, vân tay).
- Điều khiển cơ khí cổng, thoát hiểm phần cứng.
- Ứng dụng mobile native trong giai đoạn hiện tại (QR có thể hiển thị qua web responsive; mobile app là hạng mục sau).

## 3. Ranh giới trách nhiệm

| Việc | TMC-AACS | Hệ thống ngoài |
|---|---|---|
| Xác thực QR, quyết định quyền | ✅ | |
| Ghi sự kiện, intent, outcome | ✅ | |
| Mở/đóng cổng vật lý | Ra lệnh qua API | Turnstile Control System thực thi |
| Tính lương | ❌ | HR/Payroll |
| Cấp hồ sơ nhân viên gốc | Đọc/đồng bộ tối thiểu | HR (nếu có API) |
| Xác nhận người đã đi qua | Chỉ khi có tín hiệu thiết bị/cảm biến | Thiết bị cổng |
| Thoát hiểm khi mất điện | ❌ | Quy trình an toàn độc lập của TMC |

## 4. Phạm vi theo giai đoạn

Xem [roadmap](../06-development/roadmap.md). Tóm tắt:

1. **Phase 0** — code environment, docs, walking skeleton (hiện tại).
2. **Phase 1** — ICS: nhân viên, QR credential, trạm CRUD, auth/RBAC nền tảng.
3. **Phase 2** — edge scan pipeline: idempotency, gate simulator, outbox; kiểm thử lỗi.
4. **Phase 3** — central ingestion, attendance projection, adjustment workflow.
5. **Phase 4** — báo cáo, snapshot, HR export, đối soát.
6. **Phase 5** — audit đầy đủ, retention, load/chaos test, HA, hardening.

## 5. Điều kiện chấp nhận phạm vi

Một hạng mục chỉ được coi là “trong phase” khi:

- có mã yêu cầu (`FR-*`, `US-*`, `NFR-*`) hoặc ADR;
- có tiêu chí kiểm chứng rõ ràng;
- không vi phạm bất biến trong `AGENTS.md`;
- có đường lùi vận hành nếu phụ thuộc hệ thống ngoài.

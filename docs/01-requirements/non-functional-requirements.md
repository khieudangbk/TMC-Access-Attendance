# Yêu cầu phi chức năng

## 1. Bảng NFR

| Mã | Chỉ tiêu / ràng buộc | Cách kiểm chứng |
|---|---|---|
| **NFR-P1** | Kiosk phản hồi ≤ 2 giây ở ~800 giao dịch/30 phút/nhà máy. | Đo end-to-end; báo cáo số mẫu vi phạm, max, p95, p99. Không diễn giải lại thành “p99 đạt 2 giây là đủ”. |
| **NFR-R1** | Không mất bản ghi chấm công trong vận hành bình thường. | Đối chiếu tập `scan_id` đã xác nhận với kho sự kiện, dữ liệu central và báo cáo; số thiếu = 0. |
| **NFR-S1** | RBAC và audit logging toàn diện. | Kiểm tra mọi thao tác bảo vệ theo vai trò + scope; hành động nhạy cảm có audit kể cả khi bị từ chối quyền. |
| **NFR-PR1** | Bảo vệ dữ liệu cá nhân theo quy định áp dụng. | Rà soát trường dữ liệu, quyền, log, retention, quy trình; đầu mối phụ trách xác nhận. Không coi mã hóa là đủ. |
| **NFR-I1** | Tích hợp HR/Payroll và điều khiển cổng qua API hiện hữu; dữ liệu sẵn sàng trước 12:00 ngày 01. | Contract test + đối soát lô được nhận; phân biệt HTTP success với tiếp nhận nghiệp vụ. |
| **NFR-R2** | Gửi lại sự kiện không tạo tác động nghiệp vụ trùng. | Lặp cùng `scan_id`/`event_id`, đổi thứ tự giao, restart consumer; số sự kiện/lệnh không tăng sai. |
| **NFR-A1** | Đường xử lý tại nhà máy sẵn sàng 99,9%/tháng; central 99,5%. | Đo trong khung vận hành; tách downtime do từ chối hợp lệ và do hệ thống. Mục tiêu RTO edge 5 phút, central 60 phút phải diễn tập. |
| **NFR-P2** | Chịu burst 10 yêu cầu/giây/nhà máy, vẫn theo dõi giới hạn 2 giây. | Chạy đồng thời báo cáo, ghi phân phối độ trễ và tài nguyên; không coi burst là tải đề bài. |
| **NFR-M1** | Thêm trạm không sửa mã lõi; thay adapter không sửa nghiệp vụ. | Kiểm tra dependency, thử adapter giả lập, thêm cấu hình trạm; contract test + sơ đồ thành phần. |
| **NFR-O1** | Phát hiện/cảnh báo trạm mất kết nối, backlog, lỗi xuất trong 2 phút. | Heartbeat 20 giây, cảnh báo sau 3 lần thiếu; thử ngắt mạng, kiểm tra người nhận. |
| **NFR-D1** | Khôi phục được dữ liệu; tái hiện được phép kiểm kiến trúc. | Restore môi trường riêng, kiểm hash/tập ID; lưu báo cáo kiểm quyền, tải, lỗi, phụ thuộc. |
| **NFR-S2** | Mã hóa truyền/lưu, quản lý khóa, audit phát hiện can thiệp. | TLS/mTLS, quyền kho khóa, checkpoint audit độc lập; không tuyên bố hash chain đơn lẻ là bất biến tuyệt đối. |

## 2. Kịch bản chất lượng

| Mã | Kích thích / môi trường | Phản ứng / thước đo |
|---|---|---|
| **QS-01** Toàn vẹn | Kiosk gửi scan hợp lệ rồi mất phản hồi, gửi lại cùng mã. | Edge trả trạng thái đã lưu/đang đối soát; central nhận lặp không nhân đôi; 1 `scan_id`, tối đa 1 `command_id` nghiệp vụ. |
| **QS-02** Hiệu năng | 800 lượt/30 phút/nhà máy, có burst + báo cáo chạy nền. | Mỗi phản hồi ≤ 2 giây; công bố số vi phạm + phân vị. Timeout trả `UNKNOWN`, không giả thành công. |
| **QS-03** Bảo mật | Nhân viên đọc dữ liệu người khác hoặc sửa quyền. | Server từ chối; không lộ dữ liệu; audit hành động nhạy cảm + kết quả kiểm tra. |
| **QS-04** Sẵn sàng | WAN gián đoạn, LAN và kho edge còn hoạt động. | Edge dùng policy còn hiệu lực, lưu outbox; hết 30 phút dừng tự cấp quyền; mạng lại thì chuyển bù không thiếu ID. |
| **QS-05** Tích hợp | HR lỗi/timeout/chỉ nhận một phần lô. | Giữ `batch_id`/snapshot; đối soát, chỉ gửi phần thiếu; cảnh báo sớm, mục tiêu nội bộ 08:00, hạn ngoài 12:00. |
| **QS-06** Thay đổi | API cổng đổi phiên bản hoặc thêm quy tắc truy cập. | Thay adapter/strategy; nghiệp vụ lõi giữ nguyên; contract test chạy cho adapter mới. |
| **QS-07** Phục hồi | Tắt một node edge trong lúc xử lý, rồi phục hồi. | Chỉ writer có lease hợp lệ phát lệnh; bản ghi đã commit không thiếu; mục tiêu chuyển vai trò 5 phút, phải diễn tập. |

## 3. Xung đột và đánh đổi

| Xung đột | Quyết định cân bằng | Rủi ro còn lại |
|---|---|---|
| Độ bền vs độ trễ | Commit cục bộ trước khi gửi lệnh; không bỏ fsync để đẹp chỉ số; dành ngân sách cho replication đồng bộ. | Không đạt ngân sách thì tối ưu/đổi hạ tầng hoặc điều chỉnh mục tiêu với chủ hệ thống; không xác nhận dữ liệu chưa bền. |
| Sẵn sàng vs quyền mới nhất | Edge xử lý trong cửa sổ tin cậy có giới hạn; thu hồi online mục tiêu 60 giây. | Mất WAN vẫn có rủi ro quyền cũ; hết 30 phút chuyển kiểm soát thủ công. |
| Sẵn sàng vs RPO 0 khi lỗi node | Hai bản dữ liệu đồng bộ trong site; writer có lease/fencing; không tự hạ chuẩn commit. | Dừng ghi tự động để bảo toàn; cần quy trình tại cổng; mất cả site có RPO riêng. |
| Bảo mật vs tiện lợi QR | QR ngắn hạn do server ký; email ưu tiên liên kết; chống phát lại trong phạm vi edge. | Không ngăn tuyệt đối chụp màn hình/đưa điện thoại; chống phát lại liên nhà máy có độ trễ. |
| Kiểm toán vs riêng tư | Log tối thiểu, dùng mã tham chiếu; chỉ người có quyền mới liên kết danh tính; checkpoint độc lập. | Mã giả danh vẫn có thể liên kết cá nhân; phải quản lý quyền DBA và backup. |
| Cô lập lỗi vs đơn giản | Năm miền lớn, một nền tảng dữ liệu có schema sở hữu; tách worker báo cáo khỏi đường quét. | Cụm DB dùng chung vẫn là miền lỗi chung; phải giới hạn tài nguyên và đo tải. |

## 4. Khối lượng và ngân sách độ trễ

Tải trung bình: `800 / (30 × 60) ≈ 0,44 giao dịch/giây/nhà máy`. Đây là trung bình 30 phút, không mô tả dồn cục vài giây. Hai nhà máy có thể giao ca đồng thời.

Với 2–4 lượt/người/ngày, 26 ngày/tháng:

- 2.800–5.600 sự kiện/ngày.
- 72.800–145.600 sự kiện/tháng.
- ~73–146 MB dữ liệu thô/tháng nếu 1 KB/sự kiện; thực tế lớn hơn do index, status history, audit, WAL, backup.

Ngân sách phản hồi 2.000 ms:

| Bước | Ngân sách |
|---|---|
| LAN, tiếp nhận, xác thực kiosk | 100 ms |
| Xác minh QR + snapshot quyền | 100 ms |
| Chính sách, chống trùng, xếp hàng | 100 ms |
| Commit bền vững + replication | 250 ms |
| API controller + chờ kết quả | 700 ms |
| Ghi outcome + dựng phản hồi | 250 ms |
| LAN trả kết quả / hiển thị | 100 ms |
| **Tổng dự kiến / dự phòng** | **1.600 / 400 ms** |

Mỗi giới hạn là deadline trong tổng ngân sách, không phải timeout độc lập cộng dồn. Không thêm một lần gọi cổng 700 ms khi chỉ còn 400 ms.

## 5. Phép đo bắt buộc trước khi tuyên bố đạt NFR

- Phân bố độ trễ end-to-end: max, p95, p99, số mẫu > 2.000 ms.
- Tỷ lệ scan thành công/bị từ chối/lỗi thiết bị, `UNKNOWN` và thời gian đối soát.
- Số bản ghi thiếu giữa edge, central và báo cáo (phải 0).
- Backlog outbox theo tuổi; lag đồng bộ policy/credential.
- Thời gian heartbeat; thời gian phát hiện trạm mất kết nối.
- Kết quả restore + đối chiếu hash/tập ID.
- Kết quả kiểm thử quyền âm/dương theo vai trò và scope.

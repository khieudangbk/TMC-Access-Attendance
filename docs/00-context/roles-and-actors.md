# Vai trò và tác nhân

## 1. Người dùng trực tiếp

| Vai trò | Mối quan tâm | Phạm vi chính |
|---|---|---|
| **Nhân viên** | Được ghi nhận đúng, phản hồi nhanh, bảo vệ PII | QR cá nhân, lịch sử của mình, gửi giải trình |
| **Giám sát sản xuất** | Theo dõi hiện diện đội/ca, xác minh bất thường | Đội/ca được giao; xác minh giải trình |
| **Nhân viên nhân sự** | Quản lý trạng thái, đối soát, xuất HR đúng hạn | Nhà máy được giao; duyệt điều chỉnh theo thẩm quyền |
| **Nhân viên bảo vệ** | Giám sát cổng, xử lý sự cố, mở thủ công | Nhà máy/trạm được giao; bắt buộc lý do |
| **Quản trị viên hệ thống** | Tài khoản, quyền, cấu hình, log, backup/restore | Kỹ thuật; **không mặc nhiên** sửa chấm công |
| **Ban điều hành** | Báo cáo tổng hợp an ninh/hiệu quả | Chỉ đọc trong phạm vi; không điều khiển cổng |

## 2. Bên phối hợp (không mặc định có tài khoản)

- **IT / đầu mối tích hợp**: thống nhất API với HR/Payroll và đơn vị cổng quay, kiểm thử kết nối, xử lý sự cố. Có thể kiêm nhiệm quản trị viên.
- **Đầu mối bảo vệ dữ liệu**: rà soát thu thập, lưu giữ, khai thác PII; phê duyệt retention/xóa/ẩn danh. TMC cần phân công trách nhiệm cụ thể.

## 3. Tác nhân phần mềm bên ngoài

| Tác nhân | Tương tác |
|---|---|
| **HR/Payroll** | Nhận lô chấm công qua API, trả receipt theo bản ghi. Tính lương thuộc hệ thống này. |
| **Turnstile Control System** | Nhận lệnh mở/đóng theo `command_id`, trả kết quả; có thể hỗ trợ truy vấn trạng thái. |
| **Kiosk / máy quét QR** | Client xác thực tại cổng; gửi `scan_id`, station, token, direction; hiển thị kết quả. Không phải người dùng. |
| **Điện thoại nhân viên** | Hiển thị QR; không giữ khóa ký; không phải thiết bị tin cậy để quyết định quyền. |

## 4. Nguyên tắc phân quyền

1. **Least privilege**: mỗi vai trò chỉ có quyền tối thiểu cần thiết.
2. **Scope theo dữ liệu**: vai trò không đủ; phải giới hạn nhà máy/đội/cá nhân.
3. **Tách nhiệm vụ**: người lập và người duyệt điều chỉnh có thể phải khác nhau (cấu hình được).
4. **Kiểm tra phía máy chủ**: UI chỉ hỗ trợ trải nghiệm, không phải lớp bảo vệ.
5. **Audit cả từ chối**: hành động nhạy cảm bị từ chối vẫn ghi nhận khi chính sách yêu cầu.
6. **Quản trị kỹ thuật không mặc nhiên sửa công**: quyền đặc quyền phải được cấp riêng và audit.

Ma trận chi tiết: [RBAC matrix](../01-requirements/rbac-matrix.md).

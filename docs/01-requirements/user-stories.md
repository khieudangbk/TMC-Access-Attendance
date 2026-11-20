# User stories và tiêu chí chấp nhận

Mã giữ nguyên từ đặc tả gốc. Trạng thái triển khai xem [roadmap](../06-development/roadmap.md).

| Mã | User story | Tiêu chí chấp nhận | Nhóm | Ưu tiên |
|---|---|---|---|---|
| **US-01** | Là nhân viên, tôi muốn hiển thị QR cá nhân qua kênh công ty hỗ trợ để ra/vào nhà máy. | Được xác thực và chỉ nhận mã của tài khoản mình; mã còn hiệu lực được kiosk đọc; tài khoản bị vô hiệu hóa không được cấp mã mới. | CN-01 | Cốt lõi |
| **US-02** | Là nhân viên, tôi muốn quét QR tại cổng để được kiểm tra quyền và ghi nhận thời điểm chấm công. | Mã hợp lệ + có quyền mới phát lệnh mở cổng; yêu cầu không hợp lệ bị từ chối và ghi lý do; giao dịch lưu nhân viên (nếu xác định được), trạm, thời điểm, hướng, kết quả; kiosk phản hồi ≤ 2 giây ở tải đã nêu. | CN-03 | Cốt lõi |
| **US-03** | Là nhân viên, tôi muốn xem lịch sử vào/ra của bản thân để kiểm tra dữ liệu chấm công. | Lọc theo khoảng thời gian; hiển thị giao dịch đã ghi nhận; không xem được dữ liệu người khác khi không có quyền. | CN-04 | Bổ sung |
| **US-04** | Là nhân viên, tôi muốn gửi giải trình cho trường hợp thiếu/sai lượt quét để bộ phận có thẩm quyền kiểm tra. | Giải trình có ngày, nội dung, trạng thái; theo dõi được kết quả; không tự sửa bản ghi chấm công gốc. | CN-03 | Bổ sung |
| **US-05** | Là giám sát sản xuất, tôi muốn xem tình hình hiện diện của đội/ca phụ trách để bố trí công việc. | Lọc theo ngày và nhóm phụ trách; chỉ xem nhân viên trong phạm vi; dữ liệu dựa trên sự kiện đã ghi nhận và có thời điểm cập nhật. | CN-04 | Cốt lõi |
| **US-06** | Là giám sát sản xuất, tôi muốn xác minh giải trình của nhân viên trong đội để nhân sự có căn cứ xử lý. | Chỉ xử lý giải trình thuộc phạm vi; lưu người xử lý, ý kiến, thời điểm; nhân viên thấy trạng thái; không ghi đè lịch sử giao dịch gốc. | CN-03 | Bổ sung |
| **US-07** | Là nhân viên nhân sự, tôi muốn quản lý trạng thái nhân viên và quyền vào nhà máy để việc cấp quyền phù hợp hồ sơ được duyệt. | Chỉ người có quyền được cập nhật; thay đổi hợp lệ được lưu và kiểm tra khi quét; ghi audit ai thay đổi, nội dung, thời điểm. | CN-01 | Cốt lõi |
| **US-08** | Là nhân viên nhân sự, tôi muốn cấp/làm mới/thu hồi QR để kiểm soát việc dùng thông tin nhận diện. | Mỗi thao tác kiểm tra quyền + trạng thái; mã hết hạn/đã thu hồi không được chấp nhận khi trạng thái hiện hành đã áp dụng tại trạm; lưu người thao tác, thời điểm. | CN-01 | Cốt lõi |
| **US-09** | Là nhân viên nhân sự, tôi muốn lập báo cáo chấm công tháng để kiểm tra, đối soát trước khi chuyển HR/Payroll. | Chọn kỳ và nhà máy trong phạm vi; truy vết từ số liệu tổng hợp về sự kiện; thể hiện các trường hợp thiếu lượt vào/ra. | CN-04 | Cốt lõi |
| **US-10** | Là nhân viên nhân sự, tôi muốn điều chỉnh dữ liệu tổng hợp sau xác minh để xử lý sai lệch có kiểm soát. | Ghi lý do + căn cứ; chỉ người có thẩm quyền phê duyệt mới áp dụng; lưu trước/sau, người xử lý, thời điểm; không xóa sự kiện gốc. | CN-03, CN-04 | Bổ sung |
| **US-11** | Là nhân viên nhân sự, tôi muốn chuyển dữ liệu tháng sang HR/Payroll và kiểm tra kết quả để sẵn sàng đối soát đúng hạn. | Gửi qua API, sẵn sàng trước 12:00 ngày 01; hiển thị kỳ, kết quả, lỗi; gửi thất bại không báo thành công; đối chiếu dữ liệu gửi với dữ liệu HR xác nhận. | CN-04 | Cốt lõi |
| **US-12** | Là bảo vệ, tôi muốn theo dõi lượt ra vào và trạng thái trạm để phát hiện, hỗ trợ xử lý sự cố. | Xem trạm/sự kiện thuộc nhà máy được phân quyền; phân biệt được phép, bị từ chối, lỗi điều khiển cổng; người không có quyền không truy cập màn hình giám sát. | CN-02, CN-03 | Cốt lõi |
| **US-13** | Là bảo vệ được cấp quyền, tôi muốn gửi yêu cầu mở cổng thủ công khi sự cố để xử lý theo quy trình được duyệt. | Bắt buộc chọn trạm + lý do; kiểm tra quyền trước khi gửi; ghi người yêu cầu, kết quả, thời điểm; không tự tạo công hợp lệ nếu chưa xác định nhân viên. | CN-02, CN-03 | Bổ sung |
| **US-14** | Là quản trị viên, tôi muốn đăng ký/cấu hình trạm để xác định đúng nơi và hướng của mỗi lượt quét. | Trạm có định danh, nhà máy, kiosk, hướng, ánh xạ cổng; kiểm tra dữ liệu; trạm vô hiệu hóa không xử lý như trạm hoạt động; thay đổi được audit. | CN-02 | Cốt lõi |
| **US-15** | Là quản trị viên, tôi muốn quản lý vai trò và phạm vi quyền để mỗi người chỉ truy cập dữ liệu/chức năng được giao. | Mỗi thao tác bảo vệ kiểm tra quyền phía server; từ chối khi vượt vai trò/scope; thay đổi quyền được audit; kiểm thử tài khoản có/không có quyền. | CN-05 | Cốt lõi |
| **US-16** | Là quản trị viên được cấp quyền, tôi muốn tra cứu audit log để xác định ai đã thao tác và hỗ trợ điều tra. | Lọc theo thời gian, người thao tác, loại hành động; thao tác nhạy cảm có đối tượng, thời điểm, kết quả; người thường không sửa/xóa audit qua ứng dụng. | CN-04, CN-05 | Cốt lõi |
| **US-17** | Là quản trị viên, tôi muốn theo dõi trạng thái dịch vụ và sao lưu/khôi phục theo quyền để duy trì vận hành. | Hiển thị trạng thái/lỗi; ghi kết quả backup; kiểm tra restore bằng dữ liệu kiểm thử + đối chiếu số bản ghi; thao tác quản trị được audit. | CN-05 | Cốt lõi |
| **US-18** | Là thành viên ban điều hành, tôi muốn xem báo cáo tổng hợp hai nhà máy để theo dõi hiện diện và chấm công. | Lọc theo nhà máy/kỳ trong phạm vi; số liệu cùng nguồn với báo cáo nhân sự; tài khoản chỉ xem không sửa chấm công hoặc điều khiển cổng. | CN-04 | Cốt lõi |

## Truy vết story → màn hình/API dự kiến

| Story | Màn hình / API |
|---|---|
| US-01, US-08 | `POST /api/v1/credentials/{employeeRef}/qr`, web QR page |
| US-02 | `POST /api/v1/scans` (edge), kiosk result screen |
| US-03, US-04 | `GET /api/v1/me/attendance`, `POST /api/v1/me/statements` |
| US-05, US-06 | Team dashboard, statement verification queue |
| US-07 | Employee admin, site authorization |
| US-09, US-10 | Monthly report, adjustment workflow |
| US-11 | HR export + receipt screen |
| US-12, US-13 | Gate monitor, manual open request |
| US-14, US-15 | Station config, role/scope admin |
| US-16 | Audit search |
| US-17 | Ops dashboard, backup/restore |
| US-18 | Executive dashboard |

## Quy tắc chung cho acceptance test

- Kiểm thử cả luồng thuận lợi và các nhánh từ chối/lỗi.
- Với thao tác nhạy cảm: kiểm tra audit trước/sau và trường hợp bị từ chối.
- Với dữ liệu: kiểm tra không sửa/xóa bản ghi gốc.
- Với UI: kiểm tra loading, empty, error và quyền truy cập.

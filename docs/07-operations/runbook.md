# Runbook vận hành

## 1. Nguyên tắc khi xử lý sự cố

1. **An toàn cổng trước, dữ liệu trước tiện.** Không mở cổng loạt để “cho thông”; dùng quy trình bảo vệ có audit.
2. Không xóa/sửa sự kiện, audit, outbox để “dọn dẹp”.
3. Mọi thao tác thủ công nhạy cảm ghi: ai, khi nào, lý do, kết quả.
4. Ghi `X-Request-Id`/`scan_id`/`command_id` khi báo cáo.
5. Nếu không chắc cổng đã mở: coi là `UNKNOWN`, đối soát theo `command_id`.

## 2. Cấp độ sự cố

| Cấp | Tình huống | Phản ứng |
|---|---|---|
| P1 | Cổng không xử lý được lượt quét tại nhà máy; mất đường quét | Bảo vệ chuyển chế độ thủ công; IT vào ngay; thông báo quản lý |
| P1 | Mất dữ liệu chấm công đã xác nhận | Dừng write nếu có thể; giữ backup; điều tra |
| P2 | WAN mất nhưng edge vẫn xử lý | Giám sát backlog/outbox; chuẩn bị hết hạn snapshot 30 phút |
| P2 | HR export lỗi/chậm trước 12:00 ngày 01 | Chạy quy trình gửi bù; báo HR |
| P3 | Một trạm heartbeat mất | Kiểm tra kiosk/mạng; đặt bảo trì |
| P3 | Alert nhỏ, backlog tăng chậm | Theo dõi; lên lịch xử lý |

## 3. Đường quét trả lỗi

| Triệu chứng | Kiểm tra | Xử lý |
|---|---|---|
| Kiosk nhận `DENIED` hàng loạt | `reason_code`, `policy_version`, trạng thái nhân viên, trạm | Nếu policy hết hạn → kiểm tra sync; nếu đúng quyền → hướng dẫn nhân viên |
| Kiosk nhận `UNKNOWN` | `command_id`, log turnstile, heartbeat cổng | Query trạng thái thiết bị theo `command_id`; không retry mù; đối soát |
| Kiosk timeout | Latency edge, DB, outbox, turnstile | Kiểm tra ngân sách/DB; nếu `UNKNOWN` giữ nguyên kết quả |
| Trạm `MAINTENANCE`/mất heartbeat | GSS status, mạng LAN, kiosk | Đặt bảo trì có kiểm soát; thông báo bảo vệ |
| Outbox backlog tăng | Edge `outbox.status`, `next_attempt_at`, kết nối central | Không xóa; khắc phục mạng/central; relay tiếp |
| Policy hết hạn offline | `expires_at`, `policy_version` trong heartbeat | Dừng tự cấp quyền; chuyển thủ công; audit |

## 4. Mất WAN giữa edge và central

1. Xác nhận edge vẫn ghi được DB (`access_event`, `outbox`).
2. Kiểm tra `policy_snapshot.expires_at`; đếm ngược tới 30 phút.
3. Báo bảo vệ chuẩn bị chế độ thủ công khi hết hạn.
4. Không đổi cấu hình trạm trong lúc mất kết nối nếu không có quy trình đã duyệt.
5. Khi mạng lại: theo dõi outbox giảm, đối soát `event_id`, kiểm tra watermark.
6. Ghi biên bản sự cố.

## 5. Edge active chết

1. Xác nhận lease/owner hiện tại; **không** chạy thêm writer.
2. Standby chỉ nhận vai trò sau hết lease + kiểm tra dữ liệu.
3. Kiểm tra `access_event` cuối cùng, outbox chưa gửi.
4. Không tạo command mới cho `scan_id` đã có intent.
5. Sau phục hồi: đối soát, báo cáo RTO thực tế.

## 6. Turnstile timeout / `UNKNOWN`

1. Giữ nguyên kết quả `UNKNOWN`; không đổi thành thành công.
2. Nếu adapter hỗ trợ: query `command_id`.
3. Nếu không có cơ chế xác nhận: bảo vệ kiểm tra thực tế + ghi biên bản.
4. Đối soát sự kiện/outbox; nếu cần điều chỉnh công → quy trình adjustment có phê duyệt.
5. Cảnh báo nếu tỷ lệ `UNKNOWN` vượt ngưỡng.

## 7. HR export lỗi/chậm

1. Kiểm tra `batch_id`, `snapshot_id`, `checksum`, `row_count`.
2. Phân biệt: lỗi gửi (5xx/timeout) vs receipt từ chối từng bản ghi.
3. Không tạo snapshot mới để “thay” lỗi; giữ phiên bản.
4. Gửi lại lô bị lỗi theo idempotency; chỉ phần thiếu nếu HR xác nhận nhận một phần.
5. Nếu quá hạn nội bộ 08:00 → báo ngay cho HR/quản lý; hạn ngoài 12:00 ngày 01.
6. Lưu receipt + đối soát tập ID, không chỉ số lượng.

## 8. Phát hiện audit bất thường

1. Dừng thao tác trên hệ thống bị nghi ngờ.
2. Giữ backup/log; không xóa/rebuild.
3. Chạy verify chain + checkpoint; xác định khoảng lệch.
4. Tách quyền DBA; điều tra theo `actor`, `request_id`.
5. Báo đầu mối bảo vệ dữ liệu/ quản lý.
6. Không tự “sửa chuỗi”.

## 9. Backup / restore

```bash
# Backup local (minh họa)
docker exec tmc-postgres pg_dump -U tmc -Fc tmc_central > backup_central.dump
docker exec tmc-postgres pg_dump -U tmc -Fc tmc_edge > backup_edge.dump

# Restore vào DB kiểm thử, KHÔNG ghi đè production
docker exec -i tmc-postgres pg_restore -U tmc -d tmc_restore_test < backup_central.dump
```

Production: dùng PITR/managed backup, lưu backup tách quyền, diễn tập restore định kỳ.

Sau restore: đối chiếu số `event_id`, checksum/tập ID, watermark, audit checkpoint. Ghi audit việc restore.

## 10. Reset database local (DESTRUCTIVE)

```bash
make reset-db
```

Chỉ dùng local/dev. Không chạy trên môi trường có dữ liệu thật.

## 11. Checklist sau sự cố

- [ ] Nguyên nhân và mốc thời gian.
- [ ] Tác động tới cổng/chấm công/số bản ghi.
- [ ] Dữ liệu còn đủ? Đối soát edge–central–báo cáo.
- [ ] Hành động thủ công đã audit.
- [ ] Alert/runbook cập nhật nếu phát hiện thiếu.

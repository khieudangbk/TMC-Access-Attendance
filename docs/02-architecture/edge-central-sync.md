# Edge–central sync

## 1. Mục tiêu

- Edge xử lý độc lập khi mất WAN; không mất sự kiện đã xác nhận cho kiosk.
- Central nhận đúng một bản ghi nghiệp vụ cho mỗi `event_id`, kể cả khi gửi lặp hoặc gửi lại sau sự cố.
- Có watermark để biết dữ liệu đã đủ cho báo cáo tháng.
- Không dùng cơ chế “gửi rồi quên”; luôn có ack và đối soát.

## 2. Luồng

```mermaid
sequenceDiagram
    autonumber
    participant R as C08 Recorder (edge)
    participant DB as PostgreSQL edge
    participant F as C11 Event Forwarder
    participant I as C12 Event Ingestor (central)
    participant CDB as PostgreSQL central
    participant P as C13 Projector

    R->>DB: BEGIN; event + intent + outbox; COMMIT
    F->>DB: SELECT outbox WHERE published_at IS NULL ORDER BY outbox_id
    F->>I: POST /internal/v1/event-batches (mTLS) {batch, event_id...}
    I->>CDB: BEGIN; insert inbox/event unique; COMMIT
    CDB-->>I: committed
    I-->>F: 202 {accepted, duplicates, watermark}
    F->>DB: mark outbox published (ack received)
    P->>CDB: project attendance asynchronously
```

Quy tắc ack: **ack chỉ sau khi central commit**. Edge chỉ đánh dấu `published_at` sau khi nhận ack. Mất ack → gửi lại; central khử trùng theo `event_id`.

## 3. Hợp đồng lô

```json
{
  "edge_id": "EDGE-BD-01",
  "schema_version": 1,
  "sent_at": "2026-11-20T06:00:00Z",
  "events": [
    {
      "event_id": "evt_01J...",
      "scan_id": "scn_01J...",
      "station_id": "BD-IN-01",
      "direction": "IN",
      "employee_ref": "emp_7f3a",
      "occurred_at": "2026-11-20T05:59:58.120Z",
      "decision": "ALLOWED",
      "reason_code": null,
      "policy_version": 42,
      "config_revision": 3,
      "command": {
        "command_id": "cmd_01J...",
        "gate_id": "BD-TS-01",
        "state": "CONFIRMED",
        "outcomes": [
          { "outcome": "CONFIRMED", "observed_at": "2026-11-20T05:59:58.700Z" }
        ]
      },
      "payload_hash": "sha256:..."
    }
  ]
}
```

- Kích thước lô mục tiêu 100–500 event hoặc tối đa 1 MB.
- `payload_hash` tính trên nội dung nghiệp vụ; central so sánh khi `event_id` đã tồn tại.
- Không gửi raw QR/token.
- `schema_version` cho phép tiến hóa; central từ chối version không hỗ trợ với lỗi rõ ràng.

## 4. Khử trùng và xung đột

| Tình huống | Xử lý |
|---|---|
| `event_id` mới | Insert, trả `accepted` |
| `event_id` đã có, hash trùng | Bỏ qua, trả `duplicate` (idempotent) |
| `event_id` đã có, hash khác | Từ chối item, ghi audit, cảnh báo; không ghi đè |
| `scan_id` đã có với event khác | Từ chối item; điều tra edge |
| Lô có item lỗi | Trả kết quả theo từng item; item hợp lệ vẫn commit; item lỗi retry/đối soát |
| Central quá tải | `429` + `Retry-After`; edge backoff có jitter |

Không dùng “last write wins”. Xung đột là sự kiện cần điều tra.

## 5. Thứ tự và watermark

- Edge gửi theo `outbox_id` tăng dần; central không yêu cầu thứ tự tuyệt đối nhưng projection phải xử lý được event đến trễ.
- Central công bố `watermark` = `occurred_at` lớn nhất đã ingest **liên tục** (không có khoảng trống đã biết) theo từng edge.
- Báo cáo tháng chỉ chốt khi mọi edge active có watermark vượt mốc kỳ + cửa sổ trễ (ví dụ 15 phút) hoặc có ngoại lệ được duyệt.
- Edge lưu `last_ack_outbox_id` để biết tiến độ; central lưu `last_seen_outbox_id` theo edge để phát hiện thiếu.

## 6. Đồng bộ xuống edge (policy/credential)

| Bước | Cơ chế |
|---|---|
| Phát hành | ICS/GSS/ADS tạo snapshot/delta có `revision`, `effective_at`, `expires_at` |
| Ký | Snapshot được ký; edge verify bằng public key tin cậy |
| Phân phối | Edge pull định kỳ + push khi có thay đổi quan trọng (thu hồi) |
| Áp dụng | Edge chỉ áp revision mới hơn; lưu revision đã dùng |
| Hết hạn | Snapshot quá `expires_at` → dừng tự cấp quyền |

- Thu hồi online mục tiêu 60 giây.
- Mất WAN: dùng snapshot còn hạn tối đa **30 phút**.
- Không nhận snapshot không ký/không đúng issuer.
- Edge báo `policy_version` đang dùng trong heartbeat.

## 7. Đồng bộ thời gian

- Edge và central dùng NTP nội bộ; cảnh báo lệch > 2 giây.
- Token verify dùng clock edge nhưng có dung sai nhỏ (`clock_skew_seconds`, mặc định 5).
- `occurred_at` do edge ghi; central giữ nguyên, thêm `received_at`.
- Khi lệch clock phát hiện, vẫn giữ dữ liệu gốc + đánh dấu ngoại lệ, không tự sửa thời gian.

## 8. Phục hồi và vận hành

| Sự cố | Hành vi |
|---|---|
| Edge restart | Đọc outbox chưa ack, gửi tiếp; không tạo event mới |
| Central restart | Inbox/unique constraint bảo đảm không nhân đôi |
| Mất ack | Gửi lại; central trả `duplicate` |
| Outbox hỏng | Cảnh báo; không xóa; phục hồi từ backup/đối soát |
| Backlog tăng | Cảnh báo theo tuổi bản ghi cũ nhất; điều tra mạng/central |
| Đổi `edge_id` | Cấm; phải giữ định danh để watermark đúng |
| Edge thay thế phần cứng | Khôi phục DB/backup; giữ `edge_id`; đối soát trước khi chạy |

## 9. Kiểm thử

1. Commit outbox rồi tắt mạng trước khi gửi → bật mạng, gửi đủ, không mất.
2. Gửi thành công nhưng mất ack → gửi lại, central trả duplicate, không nhân đôi.
3. Gửi cùng lô hai lần → kết quả giống nhau.
4. Restart edge giữa lô → không mất/nhân đôi.
5. Restart central giữa lô → inbox bảo vệ.
6. Event đến trễ sau watermark → projection cập nhật đúng, báo cáo chưa chốt.
7. Policy hết hạn khi offline → từ chối tự động đúng hạn.
8. Thu hồi credential khi online → edge áp dụng trong mục tiêu 60 giây.
9. Lệch clock > ngưỡng → cảnh báo, dữ liệu giữ nguyên.

## 10. Truy vết

ADR-006, FR-A6, FR-A7, NFR-R2, QS-04, QS-07, AR-06/07/08.

# Observability và fitness functions

## 1. Correlation

| ID | Dùng cho |
|---|---|
| `X-Request-Id` | Mọi HTTP request; log + response header |
| `scan_id` | Một lượt quét từ kiosk tới edge/central |
| `command_id` | Một lệnh cổng; đối soát thiết bị |
| `event_id` | Một sự kiện bất biến |
| `batch_id` | Một lô xuất HR |
| `snapshot_id` / `policy_version` | Truy vết nguồn dữ liệu/quyền |

Log pattern hiện tại đã đưa `requestId` vào MDC. Không log token/PII.

## 2. Metrics tối thiểu

### Đường quét (edge)

| Metric | Nhãn | Ngưỡng cảnh báo |
|---|---|---|
| `scan.latency` | `factory`, `station`, `result` | p95 > 1.600 ms; bất kỳ mẫu > 2.000 ms |
| `scan.count` | `result`, `reason_code` | Tăng bất thường `DENIED`/`UNKNOWN` |
| `gate.outcome` | `gate`, `outcome` | `UNKNOWN` vượt ngưỡng (ví dụ > 1%/5 phút) |
| `gate.command.inflight` | `station` | Command treo quá 700 ms |
| `edge.store.write` | `result` | Lỗi ghi → P1, dừng mở cổng |
| `policy.snapshot.age` | `edge_id` | > 30 phút hoặc gần hết hạn |
| `policy.version` | `edge_id` | Lệch so với central |
| `outbox.backlog` | `edge_id` | Tăng liên tục; bản ghi cũ nhất > 5 phút |
| `outbox.ack.lag` | `edge_id` | Ack chậm |
| `station.heartbeat` | `station` | Thiếu 3 lần (20 giây/lần) → cảnh báo |

### Central

| Metric | Ngưỡng |
|---|---|
| `ingest.duplicate_rate`, `ingest.reject_rate` | Reject tăng |
| `attendance.projection.lag` | Theo watermark; quá trễ trước kỳ |
| `export.batch.status`, `export.receipt.mismatch` | Lỗi/partial receipt |
| `audit.chain.verify` | Bất kỳ lệch hash/checkpoint → P1 |
| `db.pool`, `db.latency`, `flyway.migration` | Migration fail → dừng deploy |
| HTTP 4xx/5xx theo endpoint/role | 5xx tăng; 403 tăng bất thường |

## 3. Dashboard

1. **Kiosk experience:** p50/p95/p99 latency, số > 2s, tỷ lệ theo `result`.
2. **Gate health:** outcome distribution, `UNKNOWN`, command age.
3. **Edge health:** heartbeat, policy age, disk, outbox backlog.
4. **Data integrity:** event count edge vs central, duplicates, rejects, watermark gap.
5. **Monthly readiness:** snapshot version, batch status, receipt, thời gian còn tới 12:00 ngày 01.
6. **Security:** 403/401, audit sensitive actions, audit verify.

## 4. Alert routing

| Alert | Mức | Người nhận |
|---|---|---|
| Store write fail / dừng tự cấp quyền | P1 | IT trực + bảo vệ + quản lý |
| `UNKNOWN` vượt ngưỡng | P2 | IT + bảo vệ |
| Outbox backlog | P2 | IT |
| Policy hết hạn | P2 | IT + bảo vệ |
| Export lỗi sau 08:00 ngày 01 | P2 | HR + IT |
| Audit chain lệch | P1 | Bảo mật + quản lý |
| Trạm mất heartbeat | P3 | Bảo vệ/IT ca |

Alert phải có runbook link, ID tương quan, hành động đầu tiên.

## 5. Fitness functions

Kiểm tra tự động/CI hoặc script định kỳ:

| Kiểm tra | Hành vi khi fail |
|---|---|
| `ModularityTests` pass | Block build |
| Domain package không import Spring/JDBC/HTTP | Block PR (ArchUnit/enforced rule) |
| Không FK/join chéo schema | Block migration |
| Mọi bảng append-only không có `UPDATE/DELETE` grant | Block deploy |
| Tên migration đúng, không sửa file cũ | Block CI |
| OpenAPI khớp controller (đường implemented) | Block CI |
| Không log raw token/PII (quét pattern log) | Block PR |
| Tỷ lệ `UNKNOWN` có alert | Cảnh báo |
| Đối soát event edge/central | Cảnh báo, báo cáo thiếu |
| Verify audit chain + checkpoint | P1 |

## 6. Sức khỏe ứng dụng

- Central: `GET /api/v1/system/status`, `/actuator/health`, `/actuator/info`, `/actuator/modulith`.
- Edge: `/actuator/health`, `/actuator/info`.
- Health indicator phải kiểm tra DB thật, không trả UP cứng.
- Không expose actuator công khai qua gateway; chỉ internal.

## 7. Retention log/metric

- Log ứng dụng 30–90 ngày, tách khỏi audit 5 năm.
- Metric aggregate lâu hơn raw; không lưu raw token/PII.
- Cảnh báo được lưu kèm quyết định xử lý.

## 8. Đo lường trước go-live

- Load test: 800/30 phút/nhà máy + burst 10 req/s; báo cáo max/p95/p99, số > 2s.
- Chaos: mất WAN, edge chết, gate timeout, central restart.
- Restore drill + checksum.
- Ma trận quyền dương/âm.
- Verify audit chain.

Kết quả lưu cùng ADR/roadmap, không chỉ trong slide.

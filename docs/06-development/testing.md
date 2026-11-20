# Testing strategy

## 1. Mục tiêu

Test phải chứng minh:

- Hành vi đúng requirement/ADR.
- Bất biến miền không bị vi phạm (đặc biệt trên đường quét).
- Failure path an toàn: timeout, crash, retry, trùng lặp, mất mạng, sai quyền.
- Không mất/không nhân đôi dữ liệu.

## 2. Kim tự tháp

| Tầng | Công cụ | Tỷ lệ | Ví dụ |
|---|---|---|---|
| Unit domain | JUnit 5 | Nhiều | Policy allow/deny, token claims, state machine |
| Application | JUnit + fake ports | Vừa | Commit-before-command, idempotency |
| Adapter/controller | MockMvc | Vừa | Validation, 404/403, error envelope |
| Persistence | Testcontainers PostgreSQL (khi thêm) | Ít nhưng bắt buộc cho SQL | Unique constraint, transaction |
| Contract | Simulator/contract test | Theo tích hợp | Turnstile, HR receipt |
| E2E | API + DB | Ít | Scan → event → outbox → ingest |
| Frontend | Vitest + Testing Library | Theo feature | Badge, filter, loading/error |

## 3. Bắt buộc theo loại thay đổi

| Thay đổi | Test tối thiểu |
|---|---|
| Domain rule/policy | Allow, deny, boundary, exception |
| REST controller | Success, validation, not-found, denied |
| SQL/migration | Chạy PostgreSQL thật; constraint + rollback |
| Scan/idempotency | Replay `scan_id`, concurrent duplicate, crash trước/sau gate, timeout → `UNKNOWN` |
| Outbox/inbox | Commit rồi fail publish, publish rồi mất ack, duplicate, restart |
| Auth/scope | Ma trận dương/âm theo role + factory/team |
| Frontend | Component test hành vi + loading/empty/error |
| Báo cáo/export | Partial receipt, retry, checksum, snapshot bất biến |

## 4. Ca kiểm thử bắt buộc cho scan

1. Token hợp lệ + quyền → `ALLOWED`, 1 event, 1 intent, 1 command, `CONFIRMED`.
2. Token sai/hết hạn/sai audience/key lạ → `DENIED`, 0 command, event có lý do.
3. Nhân viên inactive/không có quyền nhà máy → `DENIED`.
4. Trạm `INACTIVE`/`MAINTENANCE` → `DENIED`, không mở.
5. Sai hướng/anti-passback theo policy → `DENIED`.
6. Cùng `scan_id`, cùng payload, tuần tự → cùng kết quả, 1 event.
7. Cùng `scan_id`, payload khác → `409`, không merge.
8. Hai request đồng thời cùng `scan_id` → 1 event, 1 command.
9. Timeout gate → `UNKNOWN`; không success; retry không tạo command mới.
10. Crash sau commit trước gửi lệnh → recovery an toàn (không mở mù).
11. Gate đã mở nhưng mất outcome → `UNKNOWN`, đối soát theo `command_id`.
12. Policy hết hạn/offline > 30 phút → dừng tự cấp quyền.
13. Outbox: commit không publish, publish mất ack, restart → không mất/nhân đôi.
14. Tải 800/30 phút + burst 10 req/s → báo cáo max/p95/p99, số vi phạm 2 giây.

## 5. Test hiện có

| Test | Nội dung |
|---|---|
| `StationControllerTest` | List, get, 404 |
| `ModularityTests` | Ranh giới module Spring Modulith |
| `StatusBadge.test.tsx` | Nhãn trạng thái/hướng |

Chạy:

```bash
cd backend && ./mvnw -B -ntp test
cd frontend && npm test
```

## 6. Testcontainers (khi thêm)

- Dùng PostgreSQL 17 image giống production.
- Mỗi test class một container hoặc schema sạch; không dùng DB dev.
- Test migration + constraint + transaction.
- Không skip test DB vì “chậm”; đánh dấu tag nếu cần nhưng CI phải chạy.

## 7. Contract test

- **Turnstile simulator:** trả `CONFIRMED`, `REJECTED`, timeout, lỗi mạng; kiểm tra edge không retry mù.
- **HR simulator:** nhận toàn bộ, nhận một phần, lỗi 5xx, timeout, receipt sai; kiểm tra đối soát/gửi bù.
- Hợp đồng thật chưa xác nhận → giữ adapter + simulator, không hard-code giả định.

## 8. Frontend

- Test hành vi người dùng, không test implementation.
- Mock network ở lớp `apiGet`/MSW; không gọi backend thật trong unit test.
- Kiểm tra loading, empty, error, filter, quyền (ẩn nút không thay server).
- `npm run build` + `npm run lint` là gate.

## 9. Không được làm

- Xóa/giảm test để build xanh.
- Test chỉ happy path cho thay đổi rủi ro.
- Mock DB cho logic transaction/idempotency.
- Dùng `Thread.sleep` thay cho điều kiện đồng bộ.
- Commit test phụ thuộc thời gian thực/mạng ngoài không kiểm soát.

## 10. Báo cáo test

Trong PR nêu:

- Lệnh đã chạy + kết quả.
- Ca lỗi đã kiểm.
- Với scan: replay/concurrency/timeout evidence.
- Với tải: số mẫu, p95/p99, số vi phạm.
- Rủi ro chưa kiểm được.

# Roadmap

## 1. Nguyên tắc

- Mỗi phase có đầu ra kiểm chứng được, không chỉ “đã code”.
- Không bắt đầu phase sau khi bất biến của phase trước chưa có test.
- Ưu tiên đường quét và tính toàn vẹn dữ liệu trước báo cáo/UI đẹp.
- Không mở rộng hạ tầng (broker/K8s) khi chưa có ADR và tải chứng minh.

## 2. Phase 0 — Code environment (hiện tại)

| Hạng mục | Trạng thái | Đầu ra |
|---|---|---|
| Monorepo, build, test, Docker Compose | ✅ | `make verify` |
| Docs/ADR/agent rules | ✅ | `docs/`, `AGENTS.md` |
| Central read slice trạm | ✅ | `GET /api/v1/stations` |
| Web overview + stations | ✅ | Trang chủ, danh mục trạm |
| Schema 5 miền + edge event store | ✅ | Flyway migrations |
| Edge scan pipeline | ⏳ | Phase 2 |

## 3. Phase 1 — Danh tính, credential, nền tảng quyền

**Mục tiêu:** có thể cấp QR hợp lệ và quản trị trạm có kiểm soát.

| Việc | FR/US | Đầu ra |
|---|---|---|
| `ics.employee`, `employee_site_access` | FR-U1, US-07 | CRUD + import tối thiểu |
| Credential + signing key + rotation | FR-U2/U3, US-01/08 | Cấp/thu hồi QR, public key cho edge |
| QR issuer service | FR-U3 | Token ký, TTL 60s, `kid` |
| Auth/RBAC nền tảng | FR-S1, US-15 | Đăng nhập, role/scope, test âm |
| Station CRUD + config revision + audit | FR-G1/G2, US-14 | Tạo/sửa trạm, history |
| Audit writer + schema | FR-R5, US-16 | Insert-only, `prev_hash` |
| Web: nhân viên, QR, trạm | US-01/07/08/14 | Màn hình + trạng thái lỗi |

**Gate:** không thể cấp QR nếu thiếu quyền/audit; token không chứa PII; test quyền âm pass.

## 4. Phase 2 — Edge scan pipeline

**Mục tiêu:** đường quét chạy đúng bất biến, có simulator và test lỗi.

| Việc | FR/US | Đầu ra |
|---|---|---|
| C05 Scan Endpoint + xác thực kiosk | FR-A1 | API scan |
| C06 QR Verifier | FR-A1 | Verify chữ ký/claims/key |
| C07 Policy Engine | FR-A1/A7 | Allow/deny + lý do |
| C08 Recorder: event+intent+outbox | FR-A2/A4 | Transaction + unique |
| C09 Gate Commander + simulator | FR-A2 | Outcome, timeout → `UNKNOWN` |
| C10 Policy/credential sync | FR-A6 | Snapshot ký, revision |
| C11 Outbox forwarder | FR-A6 | Gửi lô + ack |
| Test idempotency/crash/concurrency | NFR-R2 | Bộ test bắt buộc |
| Web giám sát trạm/sự kiện (cơ bản) | US-12 | Bảng sự kiện + trạng thái |

**Gate:** replay/concurrent duplicate/crash/timeout test pass; không có đường mở cổng trước commit.

## 5. Phase 3 — Ingestion, chấm công, điều chỉnh

| Việc | FR/US | Đầu ra |
|---|---|---|
| C12 Event Ingestor + inbox | FR-A6 | Nhận lô, khử trùng |
| C13 Attendance Projector | FR-A3, US-05 | AttendanceDay + ngoại lệ |
| C14 Statement/adjustment workflow | FR-A5, US-04/06/10 | Đề xuất/duyệt, audit |
| Web: lịch sử cá nhân, đội, giải trình | US-03/05/06 | Màn hình + quyền |
| Đối soát edge–central | NFR-R1 | Báo cáo thiếu/0 |

**Gate:** không mất/nhân đôi; adjustment không sửa event gốc; quyền self/team/factory đúng.

## 6. Phase 4 — Báo cáo, snapshot, HR export

| Việc | FR/US | Đầu ra |
|---|---|---|
| C18/C19 Report + dashboard | FR-R1/R2, US-09/18 | Báo cáo tháng, tổng hợp |
| C20 Period/snapshot | FR-R4, US-09 | Snapshot version, checksum |
| C21 HR adapter | FR-R3, US-11 | Gửi lô, receipt |
| C22 Reconciliation | FR-R4 | Đối soát phần thiếu |
| Web export + receipt | US-11 | Trạng thái lô, lỗi |
| Job nhắc hạn 12:00 ngày 01 | NFR-I1 | Cảnh báo trước hạn |

**Gate:** snapshot bất biến; HTTP 200 ≠ tiếp nhận; gửi bù an toàn; đối soát checksum.

## 7. Phase 5 — Hardening, vận hành, HA

| Việc | NFR | Đầu ra |
|---|---|---|
| Audit chain + checkpoint + verify job | NFR-S2 | Kiểm tra can thiệp |
| Retention/xóa/ẩn danh | NFR-PR1 | Job + audit |
| Load test 800/30 phút + burst | NFR-P1/P2 | Báo cáo p95/p99 |
| Chaos: mất WAN/edge/gate timeout | QS-04/07 | Biên bản diễn tập |
| Backup/restore drill | NFR-D1 | Báo cáo restore |
| HA central, DR Long An | NFR-A1 | Runbook + diễn tập |
| Observability dashboards/alerts | NFR-O1 | Alert đến người |

**Gate:** NFR đo được, runbook đã diễn tập, không còn “unknown unknowns” trọng yếu.

## 8. Thứ tự ưu tiên khi thời gian hạn chế

1. Bất biến scan + idempotency + `UNKNOWN`.
2. Không mất dữ liệu/outbox.
3. Quyền + audit.
4. Báo cáo/export đúng hạn.
5. UI/tiện dụng.
6. Tối ưu hạ tầng.

## 9. Việc không làm trong phase hiện tại

- Microservices/Kubernetes/Kafka.
- Mobile native.
- Sinh trắc học/visitor management.
- Tính lương.
- Search engine/cache phân tán.

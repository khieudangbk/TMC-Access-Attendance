# Truy vết đặc tả gốc

Tài liệu này giữ **mã gốc** từ đặc tả PDF (`main-1.pdf`) và ánh xạ sang tài liệu/ADR trong repo. Dùng khi cần kiểm tra “đã phủ hết yêu cầu chưa”.

## 1. Quy ước mã

| Tiền tố | Nghĩa |
|---|---|
| `CN-*` | Nhóm chức năng |
| `FR-U/G/A/R/S*` | Yêu cầu chức năng |
| `NFR-*` | Yêu cầu phi chức năng |
| `US-*` | User story |
| `QS-*` | Kịch bản chất lượng |
| `SC-01…08` | Kịch bản liên góc nhìn 4+1 (có bằng chứng) |
| `AR-01…12` | Quy tắc triển khai kiến trúc + cách kiểm |
| `ADR-01…15` | Hồ sơ quyết định trong **đặc tả gốc** |

Repo này có **ADR-001…010** của riêng dự án. Hai bộ ADR không trùng số; đọc theo bảng ánh xạ §4.

## 2. Component C01–C26

| Nhóm | Mã | Thành phần | Tài liệu |
|---|---|---|---|
| ICS | C01 | Employee Directory / HR Reader | `02-architecture/overview.md` |
| ICS | C02 | Credential Manager | overview, `security-privacy.md` |
| ICS | C03 | QR Token Issuer | overview, `security-privacy.md` |
| ICS | C04 | Notification Adapter | overview |
| AAS-Edge | C05 | Scan Endpoint | `scan-pipeline.md` |
| AAS-Edge | C06 | QR Verifier | `scan-pipeline.md` |
| AAS-Edge | C07 | Access Policy Engine | `scan-pipeline.md` |
| AAS-Edge | C08 | Attendance Recorder | `scan-pipeline.md`, ADR-005 |
| AAS-Edge | C09 | Gate Commander | `scan-pipeline.md` |
| AAS-Edge | C10 | Policy / Credential Sync | `edge-central-sync.md` |
| AAS-Edge | C11 | Event Forwarder | `edge-central-sync.md` |
| AAS-Central | C12 | Event Ingestor | `edge-central-sync.md` |
| AAS-Central | C13 | Attendance Projector | `overview.md`, `data-model.md` |
| AAS-Central | C14 | Adjustment Workflow | `functional-requirements.md` |
| GSS | C15 | Station Registry | `overview.md`, `05-database/schema.md` |
| GSS | C16 | Station Health | `overview.md`, `observability.md` |
| GSS | C17 | Manual Access Coordinator | `functional-requirements.md`, `rbac-matrix.md` |
| RIS | C18 | Report Builder | `functional-requirements.md` |
| RIS | C19 | Dashboard Query | `functional-requirements.md` |
| RIS | C20 | Period / Snapshot Manager | `data-model.md`, `edge-central-sync.md` |
| RIS | C21 | HR Export Adapter | `edge-central-sync.md` |
| RIS | C22 | Reconciliation Service | `edge-central-sync.md` |
| ADS | C23 | Identity / Authorization | `rbac-matrix.md`, ADR-009 |
| ADS | C24 | Audit Writer / Reader | `security-privacy.md`, ADR-010 |
| ADS | C25 | Configuration Manager | `functional-requirements.md` |
| ADS | C26 | Privacy / Retention | `security-privacy.md` |

## 3. Kịch bản SC-01–SC-08

| Mã | Kịch bản | Bằng chứng bắt buộc | Tài liệu |
|---|---|---|---|
| SC-01 | QR hợp lệ giờ giao ca | Tập scan/command không trùng; trace + độ trễ từng bước | `scan-pipeline.md` §10, `non-functional-requirements.md` §5 |
| SC-02 | WAN mất, quyền offline hết hạn | Không mất ID đã commit; log chuyển chế độ; thử trước/sau 30 phút | `edge-central-sync.md` §9 |
| SC-03 | Cổng tác động nhưng mất phản hồi | Simulator áp dụng lệnh rồi cắt response; tổng lần tác động không tăng | `scan-pipeline.md` §5 |
| SC-04 | Nhân viên bị thu hồi quyền | Đo lag online mục tiêu 60 s; không hứa tức thì khi mất WAN | `edge-central-sync.md` §6 |
| SC-05 | HR nhận một phần/mất ack | Tập khóa nhận khớp snapshot; không chốt thành công khi thiếu | `edge-central-sync.md`, `runbook.md` §7 |
| SC-06 | Đọc ngoài scope hoặc sửa audit | Test âm RBAC; log/QR không lộ định danh trực tiếp | `rbac-matrix.md`, `security-privacy.md` |
| SC-07 | Edge active chết sau commit intent | RTO/RPO đo được; stale leader bị chặn | `deployment.md`, `runbook.md` §5 |
| SC-08 | Thêm nhà máy/cổng hoặc đổi controller | Contract test; policy/use case không đổi theo hãng | `tech-stack.md`, `testing.md` §7 |

## 4. Quy tắc AR-01–AR-12 và ánh xạ ADR

| Mã | Quy tắc gốc | ADR gốc | ADR/đoạn repo |
|---|---|---|---|
| AR-01 | Đường quét không chờ HR, báo cáo, DB central | 03 | ADR-001, ADR-008, `scan-pipeline.md` §9 |
| AR-02 | Commit event/intent/outbox trước lệnh cổng | 05 | ADR-005 |
| AR-03 | Giữ `scan_id`/`command_id`/`event_id`; không retry lệnh chưa rõ | 07/10 | ADR-005, ADR-006, `scan-pipeline.md` §5 |
| AR-04 | Chỉ truy cập schema sở hữu; domain không import hạ tầng | 02/08 | ADR-002, ADR-007 |
| AR-05 | Nghiệp vụ + outbox/inbox commit cùng transaction | 06 | ADR-006 |
| AR-06 | QR không chứa định danh trực tiếp; khóa không ở kiosk | 04/13 | ADR-004, `security-privacy.md` §2 |
| AR-07 | Endpoint bảo vệ tự kiểm role + scope; audit nhạy cảm | 11/12 | ADR-009, ADR-010 |
| AR-08 | Event/audit append-only; adjustment có thẩm quyền; snapshot có phiên bản | 05/09/12 | ADR-010 |
| AR-09 | Một writer có lease; failover fencing; không hạ chuẩn commit | 03/08/15 | `deployment.md` §5, `runbook.md` §5 |
| AR-10 | TLS + mTLS; secret không nằm trong source | 13 | `security-privacy.md` §6 |
| AR-11 | Cấu hình nhà máy/trạm/policy/retention có version + quyền + audit | 01/15 | `functional-requirements.md`, ADR-010 |
| AR-12 | Không đánh đồng QR hợp lệ / command accepted / gate opened / đã đi qua | 05/10 | ADR-005, `glossary.md`, `data-model.md` |

## 5. ADR gốc 01–15 → repo

| ADR gốc | Tiêu đề gốc | Đặt tại repo |
|---|---|---|
| ADR-01 | Kiến trúc dịch vụ theo miền + edge + sự kiện | ADR-001 |
| ADR-02 | Ports & adapters trong mỗi miền | ADR-002 |
| ADR-03 | Đưa xử lý cổng và kho sự kiện về nhà máy | ADR-001, `deployment.md` |
| ADR-04 | QR do máy chủ ký, có thời hạn | ADR-004 |
| ADR-05 | Commit chứng cứ trước tác động thiết bị | ADR-005 |
| ADR-06 | Transactional outbox + inbox khử trùng | ADR-006 |
| ADR-07 | Định danh ổn định cho lượt quét và lô | ADR-006, `data-model.md` §4 |
| ADR-08 | PostgreSQL theo schema, HA, RPO theo miền lỗi | ADR-007, `deployment.md` |
| ADR-09 | Xuất HR theo snapshot + lô đối soát | `edge-central-sync.md`, ADR-010 (snapshot bất biến) |
| ADR-10 | Cô lập API cổng, timeout có nghĩa | `scan-pipeline.md`, ADR-005 |
| ADR-11 | RBAC + scope tại dịch vụ | ADR-009 |
| ADR-12 | Audit chỉ thêm + checkpoint độc lập | ADR-010 |
| ADR-13 | Bảo vệ dữ liệu theo mục đích, tối thiểu hóa | `security-privacy.md` |
| ADR-14 | Quan sát + fitness function từ thiết kế | `observability.md` |
| ADR-15 | Chế độ suy giảm, giới hạn quyền offline | `scan-pipeline.md` §7, `edge-central-sync.md` §6 |

**Khác biệt có chủ đích:** ADR-003 repo ghi việc đổi stack sang React/Spring/Postgres theo yêu cầu chủ dự án (xem ADR-003 repo). Các quyết định kiến trúc gốc vẫn được giữ thông qua ánh xạ trên.

## 6. FR/NFR/US/QS

- FR-U1…U4, FR-G1…G3, FR-A1…A7, FR-R1…R5, FR-S1…S4: `01-requirements/functional-requirements.md`.
- NFR-P1/P2, R1/R2, S1/S2, PR1, I1, A1, M1, O1, D1: `01-requirements/non-functional-requirements.md`.
- US-01…US-18: `01-requirements/user-stories.md`.
- QS-01…QS-07: `01-requirements/non-functional-requirements.md` §2.

## 7. Nguyên tắc cập nhật

- Thêm/sửa yêu cầu: giữ mã gốc, cập nhật doc tương ứng + bảng này nếu mã mới.
- Không “đổi tên” mã gốc để khớp code.
- Khi implement xong một mã, cập nhật trạng thái ở `docs/README.md` và roadmap.

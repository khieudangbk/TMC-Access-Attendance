# Context map cho AI agent

File này là bảng định tuyến: task → tài liệu phải đọc → checklist. Luôn đọc `AGENTS.md` trước.

## 1. Đọc theo task

| Task | Đọc (theo thứ tự) | Không quên |
|---|---|---|
| Hiểu hệ thống | `00-context/product-context.md` → `02-architecture/overview.md` → `02-architecture/scan-pipeline.md` | Walking skeleton ≠ target design |
| Kiểm tra phủ yêu cầu/ADR gốc | `00-context/traceability.md` → requirement/ADR tương ứng | Mã gốc không đổi để khớp code |
| Thêm/sửa endpoint | `04-api/conventions.md` → `04-api/openapi.yaml` → module liên quan → `06-development/testing.md` | Cập nhật OpenAPI + controller slice test |
| Đụng đường quét C05–C11 | `02-architecture/scan-pipeline.md` → `02-architecture/data-model.md` → `edge-central-sync.md` → ADR-005/006 → `AGENTS.md` §3 | Evidence before effect; `UNKNOWN`; replay/concurrency test |
| Idempotency/outbox | ADR-006 → `edge-central-sync.md` → `02-architecture/data-model.md` → `06-development/testing.md` | Constraint DB, không chỉ `find` |
| Token/QR/crypto | ADR-004 → `02-architecture/security-privacy.md` → `01-requirements/functional-requirements.md` §CN-01 | TTL 60s, no PII, private key không repo |
| Quyền/scope/audit | `01-requirements/rbac-matrix.md` → ADR-009/010 → `02-architecture/security-privacy.md` → `00-context/roles-and-actors.md` | Test âm; audit cả từ chối |
| Schema/migration | `05-database/schema.md` → `05-database/migrations.md` → ADR-007 | Forward-only; không join/FK chéo |
| Thay đổi module/dependency | `02-architecture/tech-stack.md` → ADR-002/008 → `06-development/coding-conventions.md` | `ModularityTests` pass |
| UI/frontend | `02-architecture/tech-stack.md` → `04-api/conventions.md` → `01-requirements/rbac-matrix.md` → feature liên quan | Loading/empty/error; không business logic trong component |
| Báo cáo/xuất HR | `02-architecture/edge-central-sync.md` → `01-requirements/functional-requirements.md` §CN-04 → ADR-006 | Snapshot bất biến; receipt nghiệp vụ |
| Vận hành/sự cố | `07-operations/runbook.md` → `07-operations/observability.md` → `01-requirements/non-functional-requirements.md` | Không mở cổng loạt; ghi audit |
| Chọn task | `06-development/roadmap.md` → tài liệu phase đó | Không làm phase sau khi gate trước chưa pass |

## 2. Bất biến — checklist nhanh

Trước khi commit, trả lời `có` cho tất cả:

- [ ] Event + decision + intent + outbox commit **trước** khi gọi cổng?
- [ ] `scan_id` giữ nguyên khi retry; cùng ID khác payload → từ chối?
- [ ] Idempotency dựa trên unique constraint/transaction, không chỉ `find`?
- [ ] Timeout cổng → `UNKNOWN`, không success/failed, không retry mù?
- [ ] Dữ liệu gốc/audit/snapshot append-only; sửa bằng adjustment?
- [ ] Không suy diễn người đã qua cổng khi thiếu tín hiệu thiết bị?
- [ ] Token server-signed, TTL ngắn, không PII; không secret trong repo/client?
- [ ] Auth + role + scope kiểm tra server-side; hành động nhạy cảm có audit?
- [ ] Scan path không gọi central/HR/reporting/admin đồng bộ?
- [ ] Mỗi service giữ schema riêng; không join/FK/ghi chéo?
- [ ] Offline có hạn (≤ 30 phút), hết hạn dừng tự động, không mở tất cả cổng?
- [ ] Log không chứa token/PII; có `scan_id`/`command_id`/`request_id`?

## 3. Nguồn sự thật

1. `AGENTS.md` §3 — bất biến (ưu tiên trên mọi tài liệu/code).
2. `docs/03-decisions/` — ADR accepted.
3. `docs/01-requirements/` — FR/NFR/US.
4. `docs/02-architecture/` — thiết kế đích.
5. `docs/04-api/openapi.yaml` — contract.
6. Code/tests — trạng thái hiện tại.

Nếu code lệch docs: ghi rõ, cập nhật cùng change. Không tự chọn một bên.

## 4. Phạm vi tự quyết

| Có thể tự làm | Phải hỏi người |
|---|---|
| Implement đúng requirement đã có | Thay đổi bất biến/ADR accepted |
| Thêm test failure path | Breaking API/hợp đồng mới |
| Sửa bug nhỏ trong module | Đổi TTL/cửa sổ offline/retention/PII |
| Thêm migration forward-only | Thao tác dữ liệu phá hủy/production |
| Cập nhật docs theo change | Chọn giữa yêu cầu xung đột |

## 5. Định dạng output mong đợi

```markdown
## Đã làm
- <file>: <thay đổi>

## Vì sao
- <requirement/ADR>

## Test
- <lệnh> → <kết quả>

## Docs
- <file cập nhật>

## Rủi ro / tiếp theo
- <...>
```

Ngắn gọn, nêu file cụ thể. Không nói “xong” khi chưa chạy `make verify`.

## 6. Liên kết nhanh

- Rules: [`../../AGENTS.md`](../../AGENTS.md)
- Index docs: [`../README.md`](../README.md)
- Roadmap: [`../06-development/roadmap.md`](../06-development/roadmap.md)
- Workflow: [`../06-development/agent-workflow.md`](../06-development/agent-workflow.md)
- Testing: [`../06-development/testing.md`](../06-development/testing.md)

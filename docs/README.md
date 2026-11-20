# TMC-AACS Documentation

Tài liệu cho người và AI agent. Code hiện tại là **walking skeleton**; các tài liệu kiến trúc mô tả **thiết kế đích** đã được chốt trong ADR.

## Đọc gì trước?

| Bạn cần làm gì | Đọc theo thứ tự |
|---|---|
| Hiểu hệ thống | `00-context/product-context.md` → `02-architecture/overview.md` → `02-architecture/scan-pipeline.md` |
| Implement một feature | `AGENTS.md` → requirement liên quan → `02-architecture/tech-stack.md` → `04-api/conventions.md` |
| Đụng vào đường quét QR | `02-architecture/scan-pipeline.md` → `02-architecture/data-model.md` → ADR-005/006 → `06-development/testing.md` |
| Làm UI | `02-architecture/tech-stack.md` → `04-api/conventions.md` → `01-requirements/rbac-matrix.md` |
| Đổi database | `05-database/schema.md` → `05-database/migrations.md` → ADR-007 |
| Làm bảo mật / quyền | `02-architecture/security-privacy.md` → `01-requirements/rbac-matrix.md` → ADR-004/009/010 |
| Vận hành / debug | `07-operations/runbook.md` → `07-operations/observability.md` → `01-requirements/non-functional-requirements.md` |
| Chọn task tiếp theo | `06-development/roadmap.md` → `08-agents/agent-context.md` |

## Mục lục

### 00 — Bối cảnh
- [Bối cảnh sản phẩm](00-context/product-context.md)
- [Phạm vi](00-context/scope.md)
- [Thuật ngữ](00-context/glossary.md)
- [Vai trò và tác nhân](00-context/roles-and-actors.md)
- [Truy vết đặc tả gốc (SC/AR/ADR/C01–C26)](00-context/traceability.md)

### 01 — Yêu cầu
- [Yêu cầu chức năng](01-requirements/functional-requirements.md)
- [Yêu cầu phi chức năng](01-requirements/non-functional-requirements.md)
- [User stories](01-requirements/user-stories.md)
- [Ma trận RBAC & phạm vi dữ liệu](01-requirements/rbac-matrix.md)

### 02 — Kiến trúc
- [Tổng quan kiến trúc](02-architecture/overview.md)
- [Đường quét QR](02-architecture/scan-pipeline.md)
- [Mô hình dữ liệu](02-architecture/data-model.md)
- [Edge–central sync](02-architecture/edge-central-sync.md)
- [Bảo mật & riêng tư](02-architecture/security-privacy.md)
- [Tech stack & cấu trúc mã](02-architecture/tech-stack.md)
- [Triển khai](02-architecture/deployment.md)

### 03 — Quyết định
- [Danh mục ADR](03-decisions/README.md)

### 04 — API
- [Quy ước API](04-api/conventions.md)
- [OpenAPI contract](04-api/openapi.yaml)

### 05 — Database
- [Schema & quyền sở hữu dữ liệu](05-database/schema.md)
- [Migration](05-database/migrations.md)

### 06 — Phát triển
- [Getting started](06-development/getting-started.md)
- [Coding conventions](06-development/coding-conventions.md)
- [Testing strategy](06-development/testing.md)
- [Agent workflow](06-development/agent-workflow.md)
- [Roadmap](06-development/roadmap.md)

### 07 — Vận hành
- [Runbook](07-operations/runbook.md)
- [Observability & fitness functions](07-operations/observability.md)

### 08 — AI agents
- [Context map theo task](08-agents/agent-context.md)

## Trạng thái triển khai

| Khu vực | Trạng thái | Ghi chú |
|---|---|---|
| Monorepo, build, Docker Compose | ✅ | `make verify` chạy được |
| Central API `GET /api/v1/system/status` | ✅ | Kiểm tra DB |
| Central API `GET /api/v1/stations` | ✅ | GSS read-only, 10 trạm seed |
| Web portal (tổng quan + trạm) | ✅ | React + TanStack Query |
| Schema 5 miền + edge event store | ✅ | Flyway, chưa có domain code |
| Edge scan pipeline (C05–C11) | ⏳ | Package `edge/scan`, chưa implement |
| ICS: nhân viên, QR credential | ⏳ | Ưu tiên phase 1 |
| Auth/RBAC, audit | ⏳ | Phase 1–2 |
| Attendance projection, báo cáo, HR export | ⏳ | Phase 3–4 |
| Load/chaos test, HA | ⏳ | Phase 5 |

## Quy ước tài liệu

- Mã yêu cầu giữ nguyên từ đặc tả gốc: `CN-*`, `FR-*`, `NFR-*`, `US-*`, `QS-*`, `SC-*`, `AR-*`, `ADR-*`.
- Mọi thay đổi hành vi phải cập nhật tài liệu tương ứng trong cùng thay đổi.
- ADR đã accepted là bất biến; thay đổi bằng ADR mới (supersedes).
- Sơ đồ dùng Mermaid để render trên GitHub và đọc được dạng text.
- Ngôn ngữ: tài liệu và UI tiếng Việt; code, identifier, commit message tiếng Anh.

## Liên kết nhanh

- Luật agent: `../AGENTS.md`
- OpenAPI: `04-api/openapi.yaml`
- Sprint/backlog: `06-development/roadmap.md`

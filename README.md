# TMC-AACS — Access & Attendance

Hệ thống chấm công và kiểm soát ra vào bằng QR Code cho Công ty Cổ phần Sản xuất TMC (Bình Dương + Long An).

Monorepo gồm:

| Thư mục | Nội dung | Stack |
|---|---|---|
| `frontend/` | Web portal (quản trị, báo cáo, giám sát) | React 19, Vite, TypeScript, TanStack Query, Tailwind CSS |
| `backend/central-api/` | Dịch vụ trung tâm: ICS, AAS-central, GSS, RIS, ADS | Java 21, Spring Boot 4.1, Spring Modulith, JDBC, Flyway |
| `backend/edge-agent/` | Edge Access Agent chạy tại từng nhà máy | Java 21, Spring Boot 4.1, PostgreSQL cục bộ |
| `docs/` | Nguồn sự thật cho người và AI agent | Markdown + OpenAPI + ADR |
| `infra/` | PostgreSQL, Adminer, Mailpit cho local | Docker Compose |

## Quick start

Chỉ cần 1 lệnh Docker chạy toàn bộ hệ thống:

```bash
cp .env.example .env
make up          # Tự động build & chạy toàn bộ (Web + Backend + Edge + DB)
```

Hoặc nếu muốn chạy backend/frontend trên máy host để debug code:

```bash
make up-infra    # Chỉ bật Database, Adminer, Mailpit
make backend     # http://localhost:8080 (Central API)
make web         # http://localhost:5173 (Web portal)
```

Kiểm tra:

```bash
curl http://localhost:8080/api/v1/system/status
curl http://localhost:8080/api/v1/stations
```

Dịch vụ local:

| URL | Dịch vụ |
|---|---|
| http://localhost:5173 | Web portal |
| http://localhost:8080 | Central API |
| http://localhost:8081 | Edge agent (actuator) |
| http://localhost:8082 | Adminer (server `postgres`, DB `tmc_central`) |
| http://localhost:8025 | Mailpit |

## Tài liệu

Bắt buộc đọc trước khi sửa code:

1. [`AGENTS.md`](AGENTS.md) — luật làm việc cho AI agent.
2. [`docs/README.md`](docs/README.md) — mục lục, thứ tự đọc theo loại task.
3. [`docs/02-architecture/overview.md`](docs/02-architecture/overview.md) — kiến trúc mục tiêu.
4. [`docs/02-architecture/scan-pipeline.md`](docs/02-architecture/scan-pipeline.md) — đường quét và các bất biến.

## Trạng thái

Đây là **code environment + walking skeleton**, chưa phải bản hoàn chỉnh:

- ✅ Cấu trúc monorepo, build, test, Docker Compose.
- ✅ Central API đọc danh mục trạm từ PostgreSQL (Flyway + seed 10 trạm).
- ✅ Web portal hiển thị sức khỏe hệ thống và danh mục trạm.
- ✅ Edge agent có schema kho sự kiện/outbox, chưa có pipeline quét.
- ⏳ QR credential, scan pipeline, attendance projection, báo cáo/HR export, RBAC/audit.

Lộ trình: [`docs/06-development/roadmap.md`](docs/06-development/roadmap.md).

## Lệnh thường dùng

```bash
make help
make up / up-infra / down / reset-db
make backend / edge / web
make test / build / lint / verify
```

Chi tiết: [`docs/06-development/getting-started.md`](docs/06-development/getting-started.md).

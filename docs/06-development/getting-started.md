# Getting started

## 1. Yêu cầu

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| JDK | 21+ | Repo dùng language level 21; JDK 25 đã test |
| Docker Desktop / Engine | Có compose | PostgreSQL, Adminer, Mailpit |
| Node.js | 22+ | Vite 8 |
| npm | 10+ | Đi kèm Node |
| Make | Có sẵn trên macOS/Linux | Hoặc chạy lệnh tương đương |

## 2. Cài đặt lần đầu

```bash
git clone <repo> && cd TMC-Access-Attendance
cp .env.example .env
make setup        # npm install + Maven dependencies
make up           # PostgreSQL + Adminer + Mailpit
```

Nếu không dùng Make:

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend && ./mvnw -B -ntp dependency:go-offline
cd ../frontend && npm install
```

## 3. Chạy ứng dụng

Mở ba terminal:

```bash
make backend      # central API :8080
make edge         # edge agent :8081
make web          # web :5173
```

Kiểm tra:

```bash
curl http://localhost:8080/api/v1/system/status
curl http://localhost:8080/api/v1/stations
curl http://localhost:8080/api/v1/stations/BD-IN-01
```

Web: http://localhost:5173 — trang tổng quan hiển thị trạng thái DB và 10 trạm seed.

## 4. Database local

| Hạng mục | Giá trị |
|---|---|
| Host/port | `localhost:5432` |
| DB central | `tmc_central` |
| DB edge | `tmc_edge` |
| User/password | `tmc` / `tmc_dev_password` |
| Adminer | http://localhost:8082 (server `postgres`) |
| Mailpit | http://localhost:8025 |

```bash
make db-shell
# psql (17)
\dn
select station_id, factory_id, direction, status from gss.gate_station order by station_id;
```

Reset (xóa dữ liệu local):

```bash
make reset-db     # DESTRUCTIVE: xóa volume
```

## 5. Test và build

```bash
make test         # Maven tests + Vitest
make build        # backend jars + frontend dist
make lint         # oxlint
make verify       # build + test + lint
```

Chạy một test backend:

```bash
cd backend && ./mvnw -pl central-api -Dtest=StationControllerTest test
cd backend && ./mvnw -pl central-api -Dtest=ModularityTests test
```

Chạy một test frontend:

```bash
cd frontend && npx vitest run src/shared/ui/StatusBadge.test.tsx
```

## 6. Cấu trúc đọc nhanh

```
frontend/src/app/            layout, routing
frontend/src/features/       feature-first (overview, stations, system)
frontend/src/shared/         api client, UI primitives, format
backend/central-api/.../     modules: identity, access, station, reporting, admin, system, shared
backend/edge-agent/.../scan/ C05–C11 (khung)
backend/*/resources/db/migration/ Flyway
infra/                       docker compose
docs/                        nguồn sự thật
```

## 7. Lỗi thường gặp

| Triệu chứng | Nguyên nhân / xử lý |
|---|---|
| `Connection refused` khi start backend | Postgres chưa lên: `make up`, kiểm tra `docker compose ps` |
| Flyway báo migration lệch | Không sửa migration cũ; tạo migration mới hoặc reset DB local |
| Cổng 8080 bận | Tắt process cũ: `lsof -i :8080`; đổi `SERVER_PORT` |
| Web gọi API 404 | Proxy Vite chỉ hoạt động khi chạy `npm run dev`; kiểm tra backend :8080 |
| `npm run build` lỗi TS | Chạy `npm run typecheck`; không dùng `any` để lách |
| Docker hết dung lượng | `docker system prune` (cẩn thận), kiểm tra volume |
| Đăng nhập Adminer sai | Server `postgres`, DB `tmc_central`, user `tmc` |

## 8. Quy trình làm việc

1. Đọc `AGENTS.md` + tài liệu theo [agent-context](../08-agents/agent-context.md).
2. Tạo branch; giữ diff nhỏ.
3. Viết test trước/song song cho hành vi và failure path.
4. Chạy `make verify`.
5. Cập nhật docs/OpenAPI/ADR.
6. PR theo template; nêu rõ test và rủi ro.

## 9. Biến môi trường

Xem `.env.example` và [tech-stack](../02-architecture/tech-stack.md#6-cấu-hình). Không commit `.env` hoặc secret thật.

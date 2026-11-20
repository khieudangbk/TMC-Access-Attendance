# Tech stack và cấu trúc mã

## 1. Stack

| Lớp | Công nghệ | Phiên bản | Ghi chú |
|---|---|---|---|
| Web | React | 19 | TypeScript strict |
| Build web | Vite | 8 | Proxy `/api` → `:8080` |
| Ngôn ngữ web | TypeScript | 7 | `tsc -b`, không `any` |
| Data fetching | TanStack Query | 5 | Server state, không copy vào global store |
| Routing | React Router | 7 | `BrowserRouter` |
| Styling | Tailwind CSS | 4 | `@tailwindcss/vite`, token trong `index.css` |
| Icons | lucide-react | — | Không tự vẽ SVG rời rạc |
| Test web | Vitest + Testing Library | — | jsdom, setup tại `src/test/setup.ts` |
| Lint web | oxlint | — | `npm run lint` |
| Backend | Java | 21 language level | JDK 25 có thể build |
| Framework | Spring Boot | 4.1.2 | `spring-boot-starter-webmvc`, `-jdbc`, `-actuator` |
| Modularity | Spring Modulith | 2.1.3 | `ModularityTests` là gate |
| Build | Maven Wrapper | 3.10.1 | `backend/mvnw` |
| DB | PostgreSQL | 17 | Docker local; production dùng managed/HA |
| Migration | Flyway | — | `validate-migration-naming: true` |
| Access DB | `JdbcClient` | — | SQL tường minh; không JPA cho đường ghi |
| Test backend | JUnit 5 + MockMvc | — | Controller slice + Modulith |

## 2. Vì sao khác đặc tả gốc

Đặc tả gốc đề xuất TypeScript/NestJS cho backend. Quyết định của dự án này là **ReactJS + Spring Boot + PostgreSQL** theo yêu cầu chủ dự án. Hệ quả:

- Dùng Spring Modulith thay vì Nest module để giữ ranh giới miền và kiểm tra tự động.
- Dùng `JdbcClient` + Flyway thay vì ORM để kiểm soát transaction, unique constraint và SQL của đường quét.
- Các khái niệm kiến trúc (service-based, ports & adapters, outbox/inbox, schema-per-service) giữ nguyên.

Chi tiết: [ADR-003](../03-decisions/ADR-003-react-spring-postgres.md).

## 3. Cấu trúc monorepo

```
backend/
  pom.xml                    parent: Boot 4.1.2, Modulith 2.1.3, Java 21
  mvnw, mvnw.cmd, .mvn/
  central-api/
    src/main/java/com/tmc/aacs/central/
      CentralApplication.java
      identity/    ICS
      access/      AAS central
      station/     GSS
      reporting/   RIS
      admin/       ADS
      system/      health, config
      shared/      error envelope, request id, web primitives
    src/main/resources/
      application.yml
      db/migration/V1__create_service_schemas.sql
      db/migration/V2__gss_station.sql
      db/migration/V3__seed_gss_stations.sql
    src/test/java/.../architecture/ModularityTests.java
  edge-agent/
    src/main/java/com/tmc/aacs/edge/
      EdgeApplication.java
      scan/        C05–C11 (khung)
    src/main/resources/db/migration/V1__edge_event_store.sql

frontend/
  src/
    app/                 AppShell, NotFoundPage
    features/
      overview/          trang tổng quan
      stations/          danh mục trạm
      system/            hook trạng thái hệ thống
    shared/
      api/               client.ts, types.ts
      format/            date.ts
      ui/                StatusBadge, KpiCard, ErrorPanel
    test/setup.ts
  vite.config.ts
  tsconfig.app.json

infra/
  docker-compose.yml
  postgres/init/01-create-edge-db.sql
```

## 4. Quy ước package backend

```
<module>/
  domain/                 entity, value object, domain rule, exception
  application/            use case service, transaction boundary
  application/port/in/    inbound port (interface)
  application/port/out/   outbound port (repository, gateway)
  adapter/in/web/         controller, DTO, HTTP mapping
  adapter/out/persistence/ JDBC repository
  adapter/out/client/     external client (HR, gate)
  config/                 wiring nếu cần
```

Quy tắc:

- `domain` **không** import Spring, JDBC, HTTP, Jackson, Flyway.
- `application` không biết HTTP; controller không chứa nghiệp vụ.
- DTO là record; mapping rõ ràng, không trả entity trực tiếp.
- Transaction do application service sở hữu (`@Transactional` ở use case, không ở controller).
- Port đặt tên theo nghiệp vụ (`StationRepository`), không theo công nghệ (`JdbcDao`).

## 5. Quy ước frontend

- Feature-first: `src/features/<feature>/{api,hooks,components,pages}`.
- Network chỉ qua `src/shared/api/client.ts`; không `fetch` rải rác.
- Query key tập trung theo feature (`stationKeys`).
- Loading/empty/error là bắt buộc, không chỉ happy path.
- Copy UI tiếng Việt; identifier/comment tiếng Anh.
- Tailwind utility + component nhỏ; không thêm UI kit nặng nếu chưa có ADR.
- Accessibility: `<label>`, focus ring, semantic table, không chỉ dùng màu.

## 6. Cấu hình

| Biến | Mặc định local | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tmc_central` | DB central |
| `EDGE_DB_URL` | `jdbc:postgresql://localhost:5432/tmc_edge` | DB edge |
| `DB_USER` / `DB_PASSWORD` | `tmc` / `tmc_dev_password` | Chỉ local |
| `SERVER_PORT` | `8080` | Central API |
| `EDGE_SERVER_PORT` | `8081` | Edge API |
| `APP_ENV` | `local` | Hiển thị môi trường |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | CORS web |
| `FACTORY_ID` | `TMC-BD` | Định danh edge local |
| `VITE_API_BASE_URL` | `/api` | Base path web |

Biến môi trường production dùng secret manager; không commit giá trị thật.

## 7. Thêm dependency/module mới

1. Kiểm tra [ADR](../03-decisions/README.md) và roadmap.
2. Chứng minh không thay thế được bằng thứ có sẵn.
3. Cập nhật bảng stack + lý do.
4. Nếu là module mới: cập nhật `package-info.java`, chạy `ModularityTests`, khai báo dependency được phép.
5. Nếu là schema mới: tạo migration + cập nhật `docs/05-database/schema.md`.

Không thêm message broker, cache phân tán, search engine, Kubernetes trong phase hiện tại nếu chưa có ADR.

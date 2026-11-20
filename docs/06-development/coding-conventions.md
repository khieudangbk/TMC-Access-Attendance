# Coding conventions

## 1. Nguyên tắc chung

- Thay đổi nhỏ, mục đích rõ; không trộn refactor với thay đổi hành vi.
- Đặt tên theo ngôn ngữ nghiệp vụ (`scan`, `gateIntent`, `commandId`), không theo bảng/công nghệ.
- Ưu tiên rõ ràng hơn ngắn gọn; tránh abstraction chưa cần.
- Không commit code chết, `TODO` không có mã tham chiếu, log debug ở đường quét.
- Comment giải thích **vì sao**, không lặp lại code.

## 2. Java / Spring

- Java 21: record cho DTO/value object, `sealed`/pattern matching khi hợp lý; không lạm dụng.
- Constructor injection; không field injection.
- `@Transactional` ở application service, không ở controller/repository.
- Controller chỉ map HTTP ↔ use case; không SQL, không nghiệp vụ.
- Domain không import Spring/JDBC/HTTP/Jackson/Flyway.
- Dùng `JdbcClient`; SQL có tham số, không nối chuỗi.
- `Instant` cho timestamp; `LocalDate` chỉ cho ngày nghiệp vụ; không `LocalDateTime` cho dữ liệu lưu.
- Exception domain rõ nghĩa; map sang HTTP ở adapter.
- Không trả entity trực tiếp; DTO record + mapping tường minh.
- Log có cấu trúc: `scanId`, `commandId`, `eventId`, `stationId`; không log token/PII.
- `Optional` cho kết quả có thể không có; không trả `null` cho collection.

### Ví dụ cấu trúc module

```
station/
  domain/GateStation.java
  domain/Direction.java
  application/StationQueryService.java
  application/port/in/StationQuery.java
  application/port/out/StationRepository.java
  adapter/in/web/StationController.java
  adapter/out/persistence/JdbcStationRepository.java
```

## 3. SQL / PostgreSQL

- Bảng/cột `snake_case`; khóa chính rõ (`station_id`, `event_id`).
- `timestamptz`; `CHECK` cho enum; `UNIQUE` cho idempotency.
- Không `SELECT *` trong code ứng dụng.
- Index có tên và lý do; tránh index thừa.
- Migration forward-only; xem [migrations](../05-database/migrations.md).
- Append-only: không viết `UPDATE/DELETE` cho bảng chứng cứ/audit.

## 4. TypeScript / React

- Strict; không `any`, không `@ts-ignore` trừ khi có comment giải thích.
- Function component + hooks; không class component.
- Server state qua TanStack Query; không copy vào `useState`/global store.
- Network qua `src/shared/api/client.ts`; xử lý lỗi tập trung.
- Query key theo feature, ví dụ `stationKeys.all`.
- Props interface rõ; không truyền object khổng lồ.
- Loading/empty/error cho mọi màn async.
- Semantic HTML: `table`, `th scope`, `label`, `button`.
- Không chỉ dùng màu để biểu đạt trạng thái; kèm text/icon.
- UI copy tiếng Việt; identifier/comment tiếng Anh.
- Tailwind utility; component tái sử dụng ở `shared/ui` khi xuất hiện ≥ 2 nơi.

## 5. API

- Theo [conventions](../04-api/conventions.md).
- Error envelope cố định; `code` ổn định.
- Idempotency cho mutate; scan dùng `scan_id`.
- Cursor cho stream; page/size cho danh mục nhỏ.
- Cập nhật OpenAPI cùng controller.

## 6. Git

- Commit nhỏ, message Conventional Commits:
  - `feat(station): add station detail endpoint`
  - `fix(scan): return UNKNOWN on gate timeout`
  - `docs(adr): record outbox decision`
- Không commit `.env`, secret, dữ liệu thật, `target/`, `dist/`, `node_modules/`.
- PR theo `.github/PULL_REQUEST_TEMPLATE.md`; nêu test + rủi ro.

## 7. Định dạng

- Java 4 spaces; TS/SQL/YAML 2 spaces; LF; final newline.
- `.editorconfig` ở root.
- Không reformat file không liên quan.

## 8. Definition of Done

Xem `AGENTS.md` §11. Tóm tắt: đúng requirement/ADR, test đủ failure path, `make verify`, docs cập nhật, không secret/PII, UI đủ trạng thái.

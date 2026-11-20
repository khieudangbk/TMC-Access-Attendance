# Migration

## 1. Công cụ

- Flyway, chạy lúc khởi động ứng dụng (`spring.flyway`).
- Vị trí: `src/main/resources/db/migration`.
- Đặt tên: `V<number>__<snake_case_description>.sql`.
- `validate-migration-naming: true`.
- Không dùng `flyway clean` ngoài local.

## 2. Quy tắc

1. **Forward-only.** Migration đã áp dụng không sửa. Sai → migration mới.
2. **Một migration = một thay đổi có mục đích.** Không gộp refactor không liên quan.
3. **Không phá vỡ tương thích** trong cùng một lần deploy nếu có instance cũ đang chạy:
   - Thêm cột nullable/default trước.
   - Backfill theo lô.
   - Thêm constraint sau khi dữ liệu sạch.
   - Bỏ cột sau khi code cũ ngừng dùng (deploy riêng).
4. **Không FK/join chéo schema.** FK trong cùng schema được phép khi hợp lý.
5. **Constraint là tuyến phòng thủ cuối** cho idempotency (`UNIQUE (scan_id)`).
6. **Append-only:** không tạo đường `UPDATE/DELETE` cho bảng chứng cứ/audit.
7. **Dữ liệu seed** tách khỏi DDL; chỉ seed dữ liệu cấu hình cần thiết, không seed PII thật.
8. **Comment** cho bảng/cột quan trọng, đặc biệt bảng chứng cứ.

## 3. Central migrations hiện có

| File | Nội dung |
|---|---|
| `V1__create_service_schemas.sql` | Tạo `ics`, `aas`, `gss`, `ris`, `ads` + comment |
| `V2__gss_station.sql` | `gss.factory`, `gss.gate_station`, index, constraint |
| `V3__seed_gss_stations.sql` | 2 nhà máy + 10 trạm (BD 6, LA 4) |

## 4. Edge migrations hiện có

| File | Nội dung |
|---|---|
| `V1__edge_event_store.sql` | `aas_edge.access_event`, `gate_intent`, `outbox`, index |

## 5. Quy trình thêm migration

```bash
# 1. Tạo file mới, không sửa file cũ
backend/central-api/src/main/resources/db/migration/V4__add_ics_employee.sql

# 2. Chạy test + khởi động app để Flyway áp dụng
make test
make backend

# 3. Kiểm tra schema
make db-shell
# \dn
# \d ics.employee
```

Với edge:

```bash
backend/edge-agent/src/main/resources/db/migration/V2__add_gate_outcome.sql
make edge
```

## 6. Migration có dữ liệu lớn

1. Thêm cột nullable.
2. Backfill theo batch (ví dụ 10.000 dòng/lần), có thể dừng/tiếp tục.
3. Tạo index `CONCURRENTLY` nếu PostgreSQL và không trong transaction.
4. Thêm `NOT NULL`/constraint sau khi kiểm tra.
5. Theo dõi lock/time; chạy ngoài giờ cao điểm.

Không chạy `ALTER TABLE ... SET NOT NULL` trên bảng lớn khi chưa backfill.

## 7. Rollback

- Flyway không tự rollback. Kế hoạch:
  - Lỗi trước khi deploy: sửa migration mới (nếu chưa áp dụng) hoặc revert PR.
  - Lỗi sau khi áp dụng: viết migration bù (drop/rename/backfill) hoặc restore từ backup theo runbook.
- Không sửa file migration đã áp dụng để “rollback”.
- Migration phá vỡ dữ liệu cần review bắt buộc + backup trước.

## 8. Kiểm thử

- Chạy trên DB sạch: `make reset-db` rồi `make backend`.
- Chạy trên DB có dữ liệu mẫu/tiền production.
- Kiểm tra constraint/idempotency bằng test tích hợp.
- Kiểm tra thời gian migration và lock.

## 9. Checklist review migration

- [ ] Tên đúng quy ước, số tăng.
- [ ] Không sửa migration cũ.
- [ ] Đúng schema sở hữu.
- [ ] Có `NOT NULL`/default/backfill hợp lý.
- [ ] Constraint/index có tên rõ.
- [ ] Không seed PII/secrets.
- [ ] Có kế hoạch rollback/bù dữ liệu.
- [ ] Cập nhật `docs/05-database/schema.md`.

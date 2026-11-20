# Quy ước API

Base path: `/api/v1`. Contract: [`openapi.yaml`](openapi.yaml). Thay đổi controller phải cập nhật cả hai.

## 1. Định dạng

| Hạng mục | Quy ước |
|---|---|
| Media type | `application/json; charset=utf-8` |
| Field naming | `camelCase` trong JSON; `snake_case` chỉ trong event payload nếu đã chốt |
| Thời gian | ISO-8601 UTC (`2026-11-20T06:22:05.776Z`); không gửi local time không kèm offset |
| ID | Chuỗi opaque, có prefix (`scn_`, `cmd_`, `evt_`, `bat_`); không phụ thuộc độ dài |
| Tiền/số | Không áp dụng; số nguyên dùng `integer`/`int64` |
| Null | Trường optional có thể `null`; không dùng chuỗi `"null"` |

## 2. Lỗi

```json
{
  "code": "STATION_NOT_FOUND",
  "message": "Station BD-IN-09 does not exist",
  "details": [
    { "field": "stationId", "message": "unknown station" }
  ],
  "timestamp": "2026-11-20T06:22:05.776Z"
}
```

- `code`: stable, SCREAMING_SNAKE_CASE; client dùng để xử lý, không parse `message`.
- `message`: tiếng Anh cho log/API; UI có thể map sang tiếng Việt.
- `details`: mảng, có thể rỗng.
- `timestamp`: UTC.
- `X-Request-Id`: trả về trong mọi response; dùng để hỗ trợ điều tra.

| HTTP | Khi dùng |
|---|---|
| 200 | Đọc/thành công đồng bộ |
| 201 | Tạo mới |
| 202 | Nhận xử lý bất đồng bộ (scan accepted, batch accepted) |
| 204 | Thành công không body |
| 400 | Payload sai định dạng/validation |
| 401 | Chưa xác thực |
| 403 | Không đủ quyền/scope |
| 404 | Không tồn tại (khi không cần che) |
| 409 | Xung đột/idempotency khác payload/trạng thái |
| 422 | Vi phạm quy tắc nghiệp vụ |
| 429 | Quá tải/rate limit; kèm `Retry-After` |
| 500 | Lỗi không xác định |
| 503 | Phụ thuộc không sẵn sàng |

Không trả stack trace, SQL, tên bảng, raw token, PII.

## 3. Idempotency

- Endpoint mutate nhận `Idempotency-Key` (header) hoặc khóa trong body.
- Scan dùng `scan_id` trong body làm khóa; cùng khóa + payload khác → `409`.
- Tạo credential/export/adjustment: header `Idempotency-Key`; lưu kết quả theo khóa trong thời gian cấu hình.
- Retry phải trả cùng kết quả nghiệp vụ; không tạo bản ghi mới.

## 4. Phân trang

| Loại | Cơ chế |
|---|---|
| Danh mục nhỏ (trạm, role, config) | `page`, `size` (mặc định 20, tối đa 100) |
| Sự kiện/audit/scan stream | Cursor: `cursor`, `limit`; response có `nextCursor` |
| Báo cáo lớn | Cursor hoặc export job |

Cursor opaque, không phải offset; ổn định khi dữ liệu mới chèn vào.

## 5. Lọc và sắp xếp

- Query param rõ nghĩa: `factoryId`, `stationId`, `status`, `from`, `to`.
- `from`/`to` là ISO-8601; `to` exclusive.
- Sắp xếp mặc định mới nhất trước cho stream; danh mục theo `name`/`stationId`.
- Không nhận biểu thức SQL/field tùy ý từ client.

## 6. Versioning

- `/api/v1` ổn định trong vòng đời major.
- Thêm field optional: không phá vỡ.
- Đổi kiểu/ý nghĩa field, bỏ field, đổi status code: cần version mới hoặc deprecation có kế hoạch.
- Deprecation: header `Deprecation`, `Sunset`; tài liệu hóa.

## 7. Auth và scope

- `Authorization: Bearer <token>` cho người dùng; mTLS cho edge/service.
- Mỗi endpoint khai báo permission + scope trong OpenAPI (`x-permission`, `x-scope`).
- Server kiểm tra lại; không tin header `X-Employee-Ref`, `X-Factory-Id`, `X-Role` từ client.
- Danh sách trả về phải đã lọc theo scope.

## 8. Endpoint hiện có

| Method | Path | Trạng thái | Mô tả |
|---|---|---|---|
| GET | `/api/v1/system/status` | ✅ | Trạng thái ứng dụng + DB |
| GET | `/api/v1/stations` | ✅ | Danh sách trạm |
| GET | `/api/v1/stations/{stationId}` | ✅ | Chi tiết trạm, 404 nếu không có |

Endpoint đích (chưa implement) được liệt kê trong OpenAPI với `x-status: planned`; không gọi là đã có.

## 9. Endpoint nội bộ (edge ↔ central)

- Prefix `/internal/v1`, **không** public qua gateway.
- mTLS bắt buộc; certificate map tới `edge_id`.
- `POST /internal/v1/event-batches`: nhận lô sự kiện.
- `GET /internal/v1/policy-snapshots/current`: snapshot policy/khóa.
- `POST /internal/v1/heartbeats`: heartbeat trạm/edge.
- Trả kết quả theo item; không “all-or-nothing” trừ khi hợp đồng yêu cầu.

## 10. Checklist endpoint mới

- [ ] Có requirement/ADR.
- [ ] Permission + scope + test âm.
- [ ] Idempotency cho mutate.
- [ ] Phân trang phù hợp.
- [ ] Lỗi dùng envelope chung, không lộ chi tiết.
- [ ] `X-Request-Id` và log tương quan.
- [ ] Cập nhật OpenAPI + tài liệu.
- [ ] Không log raw token/PII.

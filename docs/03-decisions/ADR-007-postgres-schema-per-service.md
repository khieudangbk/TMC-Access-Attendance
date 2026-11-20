# ADR-007 — Schema-per-service, không join/FK chéo schema

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư
- Supersedes: —
- Superseded by: —

## Context

Năm miền (ICS, AAS, GSS, RIS, ADS) có thể tách deployment khi tải tăng. Nếu dùng chung bảng/join chéo, việc tách sẽ kéo theo refactor lớn và tạo coupling ẩn. Đồng thời cần bảo vệ dữ liệu chứng cứ/audit bằng quyền DB.

## Decision

1. Mỗi miền sở hữu một PostgreSQL schema: `ics`, `aas`, `gss`, `ris`, `ads`; edge sở hữu `aas_edge`.
2. Không **join**, **foreign key** hay **ghi** chéo schema.
3. Tham chiếu chéo chỉ bằng ID bất biến (`employee_ref`, `station_id`, `event_id`, `batch_id`).
4. Dữ liệu cần cho báo cáo được **project** sang `ris` qua sự kiện/API, không đọc trực tiếp kho edge.
5. Quyền DB theo nguyên tắc tối thiểu:
   - Ứng dụng: DML trên schema của mình.
   - Audit: role riêng chỉ `INSERT`/`SELECT`.
   - Không cấp `SUPERUSER` cho ứng dụng.
6. Flyway quản lý migration theo dịch vụ; không sửa migration đã áp dụng.

## Consequences

Tích cực:

- Tách service không cần đổi schema.
- Ranh giới dữ liệu rõ, dễ audit quyền.
- Projection có thể rebuild, không phụ thuộc DB ngoài.

Tiêu cực / rủi ro:

- Không có FK bảo vệ tham chiếu chéo → phải kiểm tra ở application/event.
- Báo cáo cần pipeline projection, không query trực tiếp.
- Cùng một cụm PostgreSQL vẫn là miền lỗi chung; cần giới hạn tài nguyên.

## Alternatives considered

- **Một schema chung:** đơn giản nhưng vi phạm ranh giới, khó tách.
- **Nhiều database vật lý ngay:** tốn vận hành; schema-per-service đủ cho giai đoạn này.
- **FK chéo schema:** loại vì khóa coupling và cản tách.

## References

`AGENTS.md` §3 (bất biến 10), NFR-M1, `docs/05-database/schema.md`.
